package org.example.demo

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.ToolResult
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.ext.tool.SayToUser
import ai.koog.agents.features.common.message.FeatureMessage
import ai.koog.agents.features.common.message.FeatureMessageProcessor
import ai.koog.agents.features.tracing.feature.Tracing
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.executor.ollama.client.toLLModel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import ai.koog.agents.core.tools.reflect.tool
import com.sun.tools.javac.tree.TreeInfo.args

fun main(args: Array<String>) {
    runBlocking {
        //select executor based on command line parameter
        val (executor, model) = when (args.firstOrNull() ?: "devstral") {
            "openai" -> {
                val openAIApiToken = System.getenv("OPENAI_API_KEY") ?: error("OPENAI_API_KEY environment variable not set")
                simpleOpenAIExecutor(openAIApiToken) to OpenAIModels.Chat.GPT4o
            }

            "devstral" -> {
                val client = OllamaClient()
                val model = runBlocking { client.getModelOrNull("devstral")!!.toLLModel() }
                SingleLLMPromptExecutor(client) to model
            }

            else -> throw IllegalArgumentException("Invalid argument: ${args.firstOrNull()}")
        }

        val toolRegistry = ToolRegistry {
            tool(SayToUser)
            tool(::temperatureTool)
        }

        val agent = AIAgent(
            executor = executor,
            systemPrompt = """
            You are a helpful assistant that can answer general questions.            
            Answer any user query and provide a detailed response.
            Once you have the answer, tell it to the user.
            
            ***IMPORTANT*** YOU MUST USE TOOLS TO IMPLEMENT THE TASK!!!
            ***IMPORTANT*** DON'T CHAT WITH ME BEFORE YOU FINISH
            ***IMPORTANT*** USE THE SAYTOOL TO PROVIDE THE ANSWER TO THE USER
        """.trimIndent(),
            llmModel = model,
            toolRegistry = toolRegistry,
        ) {
            install(Tracing) {
                addMessageProcessor(object : FeatureMessageProcessor() {
                    override suspend fun processMessage(message: FeatureMessage) = println(message)
                    override suspend fun close() = TODO("Not yet implemented")
                })
            }
        }

        agent.run("What is the current temperature?")
    }
}

@Serializable
data class TemperatureResult(val value: Int) : ToolResult {
    override fun toStringDefault(): String = "Temperature: $value"
}

@Tool
@LLMDescription("provide current temperature")
suspend fun temperatureTool(): TemperatureResult {
    println(">>>>>>>>>>>> TRACE: temperatureTool")
    return TemperatureResult(100000)
}



