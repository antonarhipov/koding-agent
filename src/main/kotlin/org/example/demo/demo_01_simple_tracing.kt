package org.example.demo

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.feature.message.FeatureMessage
import ai.koog.agents.core.feature.message.FeatureMessageProcessor
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.tool.SayToUser
//import ai.koog.agents.features.common.message.FeatureMessage
//import ai.koog.agents.features.common.message.FeatureMessageProcessor
import ai.koog.agents.features.tracing.feature.Tracing
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    runBlocking {
        //select executor based on command line parameter
        val (executor, model) = autoselect("gpt-oss:20b")

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
            //region tracing
            install(Tracing) {
                addMessageProcessor(object : FeatureMessageProcessor() {
                    override val isOpen: StateFlow<Boolean>
                        get() = TODO("Not yet implemented")

                    override suspend fun processMessage(message: FeatureMessage) = println("$message\n")
                    override suspend fun close() = TODO("Not yet implemented")
                })
            }
            //endregion
        }

        agent.run("Tell me a friendly juke about software developers?")
    }
}









