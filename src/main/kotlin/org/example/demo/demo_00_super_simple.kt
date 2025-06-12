package org.example.demo

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.agent.simpleSingleRunAgent
import ai.koog.agents.ext.tool.SayToUser
import ai.koog.agents.features.common.message.FeatureMessage
import ai.koog.agents.features.common.message.FeatureMessageProcessor
import ai.koog.agents.features.tracing.feature.Tracing
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val apiKey = System.getenv("OPENAI_API_KEY")

    val toolRegistry = ToolRegistry {
        tools(
            listOf(SayToUser)
        )
    }

    val agent = AIAgent(
        executor = simpleOpenAIExecutor(apiKey),
        systemPrompt = """
            You are a helpful assistant that can answer general questions.            
            Answer any user query and provide a detailed response. 
            Once you have the answer, tell it to the user
        """.trimIndent(),
        llmModel = OpenAIModels.Chat.GPT4o,
        toolRegistry = toolRegistry,
    )
    agent.run("Tell me a friendly programmers' joke about Amsterdam?")
}









