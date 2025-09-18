package org.example.demo

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.ext.tool.SayToUser
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
//        val (executor, model) = autoselect("gpt-oss:20b")
        val (executor, model) = autoselect("qwen3:8b")

        val agent = AIAgent(
            executor = executor,
            systemPrompt = """
            You are a helpful assistant that can answer general questions.            
            Answer any user query and provide a detailed response. 
            IMPORTANT!!! YOO MUST DO FOLLOWING: Once you have the answer, tell it to the user
        """.trimIndent(),
            llmModel = model,
//            toolRegistry = ToolRegistry {
//                tools(
//                    listOf(SayToUser)
//                )
//            },
        )
        // Why do programmers always mix up Halloween and Christmas? Because Oct 31 == Dec 25!
        agent.run("Tell me a friendly juke about software developers?")//.also { println(it) }
    }
}









