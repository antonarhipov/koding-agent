package org.example.demo

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.tool.SayToUser
import ai.koog.agents.features.eventHandler.feature.handleEvents
import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
        val (executor, model) = autoselect("gpt-oss:20b")

        val agent = AIAgent(
            executor = executor,
            systemPrompt = """
            You are a helpful assistant that can answer general questions.            
            Answer any user query and provide a detailed response. 
            Once you have the answer, tell it to the user
        """.trimIndent(),
            llmModel = model,
            toolRegistry = ToolRegistry {
                tools(
                    listOf(SayToUser)
                )
            },
        )
        agent.run("Tell me a friendly programmers' joke about Amsterdam?")
    }
}









