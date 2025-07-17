package org.example.demo

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.tool.SayToUser
import ai.koog.agents.features.common.message.FeatureMessage
import ai.koog.agents.features.common.message.FeatureMessageProcessor
import ai.koog.agents.features.tracing.feature.Tracing
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.executor.ollama.client.toLLModel
import com.sun.tools.javac.tree.TreeInfo.args
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    runBlocking {
        //select executor based on command line parameter
        val (executor, model) = when (args.firstOrNull() ?: "mistral") {
            "openai" -> {
                val openAIApiToken = System.getenv("OPENAI_API_KEY") ?: error("OPENAI_API_KEY environment variable not set")
                simpleOpenAIExecutor(openAIApiToken) to OpenAIModels.Chat.GPT4o
            }

            "mistral" -> {
                val client = OllamaClient()
                val model = runBlocking { client.getModelOrNull("mistral")!!.toLLModel() }
                SingleLLMPromptExecutor(client) to model
            }

            else -> throw IllegalArgumentException("Invalid argument: ${args.firstOrNull()}")
        }

        val toolRegistry = ToolRegistry {
            tools(
                listOf(SayToUser)
            )
        }

        val agent = AIAgent(
            executor = executor,
            systemPrompt = """
            You are a helpful assistant that can answer general questions.            
            Answer any user query and provide a detailed response.
            Once you have the answer, tell it to the user.
        """.trimIndent(),
            llmModel = model,
            toolRegistry = toolRegistry,
        ) {
//        install(Tracing) {
//            addMessageProcessor(object : FeatureMessageProcessor() {
//                override suspend fun processMessage(message: FeatureMessage) = println(message)
//                override suspend fun close() = TODO("Not yet implemented")
//            })
//        }
        }

        agent.run("What are the main benefits of using Kotlin in server-side applications?")
    }
}









