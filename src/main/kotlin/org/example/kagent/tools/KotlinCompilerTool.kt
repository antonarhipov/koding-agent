package org.example.kagent.tools

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import ai.koog.agents.core.tools.ToolResult
import kotlinx.serialization.Serializable
import java.io.File

object KotlinCompilerTool : Tool<KotlinCompilerTool.Args, ToolResult>() {
    @kotlinx.serialization.Serializable
    data class Args(
        val sourceFile: String,
        val outputDir: String = "build/classes",
    ) : Tool.Args

    @Serializable
    data class Result(
        val success: Boolean,
        val message: String,
        val errors: List<String> = emptyList(),
    ) : ToolResult {
        override fun toStringDefault(): String = message
    }

    override val argsSerializer = Args.serializer()
    override val descriptor = ToolDescriptor(
        name = "kotlin_compiler",
        description = "Compile Kotlin source files",
        requiredParameters = listOf(
            ToolParameterDescriptor("sourceFile", "Path to Kotlin source file", ToolParameterType.String)
        ),
        optionalParameters = listOf(
            ToolParameterDescriptor("outputDir", "Output directory for compiled classes", ToolParameterType.String)
        )
    )

    override suspend fun execute(args: Args): Result {
        return try {
            val outputDir = File(args.outputDir)
            outputDir.mkdirs()

            val processBuilder = ProcessBuilder(
                "kotlinc",
                args.sourceFile,
                "-d", args.outputDir
            )

            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()

            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                Result(true, "Compilation successful", emptyList())
            } else {
                val errors = output.lines().filter { it.isNotBlank() }
                Result(false, "Compilation failed", errors)
            }
        } catch (e: Exception) {
            Result(false, "Compilation error: ${e.message}", listOf(e.message ?: "Unknown error"))
        }
    }
}