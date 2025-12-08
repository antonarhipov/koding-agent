package org.example.demo

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.singleRunStrategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.tool.file.EditFileTool
import ai.koog.agents.ext.tool.file.ListDirectoryTool
import ai.koog.agents.ext.tool.file.ReadFileTool
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.rag.base.files.JVMFileSystemProvider


suspend fun main(args: Array<String>) {
//    val (executor, model) = openai()
    val (executor, model) = gptoss()

//val executor = simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY"))
    val agent = AIAgent(
        promptExecutor = executor,
        llmModel = model,
        toolRegistry = ToolRegistry {
            tool(ListDirectoryTool(JVMFileSystemProvider.ReadOnly))
            tool(ReadFileTool(JVMFileSystemProvider.ReadOnly))
            tool(EditFileTool(JVMFileSystemProvider.ReadWrite))
            tool(createExecuteShellCommandToolFromEnv())
        },
        systemPrompt = """
            You are a coding assistant helping the user to write programs according to their requests.
            Implement the user request according to the supplied plan.
            The code should be generated a dedicated directory.  
            Once you have the answer, tell it to the user.
        """.trimIndent(),
        strategy = singleRunStrategy(),
        maxIterations = 400
    ) {
        handleEvents {
            onToolCallStarting { ctx ->
                println("Tool '${ctx.tool.name}' called with args: ${ctx.toolArgs.toString().take(100)}")
            }
        }
    }

    val path = "/Users/anton/IdeaProjects/kagent/demo"
    val task = """
        Implement a fizzbuzz program in Kotlin.
        1) Create a Gradle project in the target directory
        2) Create a Fizzbuzz class in the src/main/kotlin directory
        3) Implement the fizzbuzz function in the Fizzbuzz class
        4) Create a test class in the src/test/kotlin directory
        5) Implement the test function in the test class
        """.trimIndent()
    val input = "Project absolute path: $path\n\n## Task\n$task"
    try {
        val result = agent.run(input)
        println(result)
    } finally {
        executor.close()
    }
}
