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
data class KotlinCompilerResult(
    val success: Boolean,
    val message: String,
    val errors: List<String> = emptyList(),
) : ToolResult {
    override fun toStringDefault(): String = message
}

@Tool
@LLMDescription("Compile Kotlin source files")
suspend fun kotlinCompiler(
    @LLMDescription("Path to Kotlin source file")
    sourceFile: String,
    @LLMDescription("Output directory for compiled classes")
    outputDir: String = "build/classes"
): KotlinCompilerResult {
    return try {
        val outputDirFile = File(outputDir)
        outputDirFile.mkdirs()

        val processBuilder = ProcessBuilder(
            "kotlinc",
            sourceFile,
            "-d", outputDir
        )

        processBuilder.redirectErrorStream(true)
        val process = processBuilder.start()

        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()

        if (exitCode == 0) {
            KotlinCompilerResult(true, "Compilation successful", emptyList())
        } else {
            val errors = output.lines().filter { it.isNotBlank() }
            KotlinCompilerResult(false, "Compilation failed", errors)
        }
    } catch (e: Exception) {
        KotlinCompilerResult(false, "Compilation error: ${e.message}", listOf(e.message ?: "Unknown error"))
    }
}
