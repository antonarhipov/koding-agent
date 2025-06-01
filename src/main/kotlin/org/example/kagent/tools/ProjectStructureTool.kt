package org.example.kagent.tools

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import ai.koog.agents.core.tools.ToolResult
import kotlinx.serialization.Serializable
import java.io.File

object ProjectStructureTool : Tool<ProjectStructureTool.Args, ToolResult>() {
    @kotlinx.serialization.Serializable
    data class Args(
        val projectName: String,
        val baseDir: String = ".",
    ) : Tool.Args

    @Serializable
    data class Result(
        val success: Boolean,
        val message: String,
        val structure: Map<String, String>,
    ) : ToolResult {
        override fun toStringDefault(): String = message
    }

    override val argsSerializer = Args.serializer()
    override val descriptor = ToolDescriptor(
        name = "project_structure",
        description = "Create a basic Kotlin project structure",
        requiredParameters = listOf(
            ToolParameterDescriptor("projectName", "Name of the project", ToolParameterType.String)
        ),
        optionalParameters = listOf(
            ToolParameterDescriptor("baseDir", "Base directory for project", ToolParameterType.String)
        )
    )

    override suspend fun execute(args: Args): Result {
        return try {
            val projectDir = File(args.baseDir, args.projectName)
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

            Result(true, "Project structure created for ${args.projectName}", structure)
        } catch (e: Exception) {
            Result(false, "Failed to create project structure: ${e.message}", emptyMap())
        }
    }
}