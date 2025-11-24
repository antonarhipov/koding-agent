package org.example.demo

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.tool.SayToUser
import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
        val (executor, model) = autoselect("gpt-oss:20b")

        val agent = AIAgent(
            promptExecutor = executor,
            systemPrompt = """
                You are a helpful assistant that can answer general questions.
                Answer any user query and provide a detailed response.
                IMPORTANT!!! YOO MUST DO FOLLOWING: Once you have the answer, tell it to the user
            """.trimIndent(),
            llmModel = model,
            temperature = 1.0,
            toolRegistry = ToolRegistry {
                tool(SayToUser)
            }
        )

        // Why do programmers always mix up Halloween and Christmas? Because Oct 31 == Dec 25!
        agent.run("Tell me a friendly juke about software developers?")//.also { println(it) }
    }
}









