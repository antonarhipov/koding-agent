package org.example.demo

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.singleRunStrategy
import ai.koog.agents.features.eventHandler.feature.handleEvents
import org.example.kagent.mcp.McpIntegration


suspend fun main(args: Array<String>) {
    val (executor, model) = openai()
//    val (executor, model) = gptoss()

    val agent = AIAgent(
        promptExecutor = executor,
        llmModel = model,
        toolRegistry = McpIntegration.createMcpToolRegistry(),
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
    """.trimIndent()
    val input = """
        Project absolute path: ${path}
        
        ### Task
        $task
    """.trimIndent()
    try {
        val result = agent.run(input)
        println(result)
    } finally {
        executor.close()
    }
}
