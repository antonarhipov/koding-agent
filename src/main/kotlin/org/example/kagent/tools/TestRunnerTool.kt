package org.example.kagent.tools

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import ai.koog.agents.core.tools.ToolResult
import kotlinx.serialization.Serializable
import java.io.File

object TestRunnerTool : Tool<TestRunnerTool.Args, ToolResult>() {
    @kotlinx.serialization.Serializable
    data class Args(
        val testFile: String,
        val mainClass: String? = null
    ) : Tool.Args

    @Serializable
    data class Result(
        val success: Boolean,
        val message: String,
        val testResults: List<String> = emptyList()
    ) : ToolResult {
        override fun toStringDefault(): String = message
    }

    override val argsSerializer = Args.serializer()
    override val descriptor = ToolDescriptor(
        name = "test_runner",
        description = "Run Kotlin tests and return results",
        requiredParameters = listOf(
            ToolParameterDescriptor("testFile", "Path to test file", ToolParameterType.String)
        ),
        optionalParameters = listOf(
            ToolParameterDescriptor("mainClass", "Main class to run", ToolParameterType.String)
        )
    )

    override suspend fun execute(args: Args): Result {
        return try {
            val className = args.mainClass ?: extractClassName(args.testFile)
            
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
            
            Result(
                success = exitCode == 0,
                message = if (exitCode == 0) "Tests passed" else "Tests failed",
                testResults = results
            )
        } catch (e: Exception) {
            Result(false, "Test execution error: ${e.message}", emptyList())
        }
    }
    
    private fun extractClassName(filePath: String): String {
        val fileName = File(filePath).nameWithoutExtension
        return "${fileName}Kt"
    }
}