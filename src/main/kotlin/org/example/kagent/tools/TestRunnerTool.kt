/*
 * Copyright 2023 Kagent Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.example.kagent.tools

import ai.koog.agents.core.tools.ToolResult
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class TestRunnerResult(
    val success: Boolean,
    val message: String,
    val testResults: List<String> = emptyList()
) : ToolResult {
    override fun toStringDefault(): String = message
}

@Tool
@LLMDescription("Run Kotlin tests and return results")
suspend fun testRunner(
    @LLMDescription("Path to test file")
    testFile: String,
    @LLMDescription("Main class to run")
    mainClass: String? = null
): TestRunnerResult {
    return try {
        val className = mainClass ?: extractClassName(testFile)

        val processBuilder = ProcessBuilder(
            "kotlin",
            "-classpath", "build/classes:.",
            className
        )

        processBuilder.redirectErrorStream(true)
        val process = processBuilder.start()

        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()

        val results = output.lines().filter { it.isNotBlank() }

        TestRunnerResult(
            success = exitCode == 0,
            message = if (exitCode == 0) "Tests passed" else "Tests failed",
            testResults = results
        )
    } catch (e: Exception) {
        TestRunnerResult(false, "Test execution error: ${e.message}", emptyList())
    }
}

private fun extractClassName(filePath: String): String {
    val fileName = File(filePath).nameWithoutExtension
    return "${fileName}Kt"
}
