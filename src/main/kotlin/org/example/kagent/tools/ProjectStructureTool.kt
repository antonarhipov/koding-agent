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
data class ProjectStructureResult(
    val success: Boolean,
    val message: String,
    val structure: Map<String, String>,
) : ToolResult {
    override fun toStringDefault(): String = message
}

@Tool
@LLMDescription("Create a basic Kotlin project structure")
suspend fun projectStructure(
    @LLMDescription("Name of the project")
    projectName: String,
    @LLMDescription("Base directory for project")
    baseDir: String = "."
): ProjectStructureResult {
    return try {
        val projectDir = File(baseDir, projectName)
        val srcDir = File(projectDir, "src/main/kotlin")
        val testDir = File(projectDir, "src/test/kotlin")
        val buildDir = File(projectDir, "build/classes")

        srcDir.mkdirs()
        testDir.mkdirs()
        buildDir.mkdirs()

        val structure = mapOf(
            "project" to projectDir.absolutePath,
            "src" to srcDir.absolutePath,
            "test" to testDir.absolutePath,
            "build" to buildDir.absolutePath
        )

        ProjectStructureResult(true, "Project structure created for ${projectName}", structure)
    } catch (e: Exception) {
        ProjectStructureResult(false, "Failed to create project structure: ${e.message}", emptyMap())
    }
}
