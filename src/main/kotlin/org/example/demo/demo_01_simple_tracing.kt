package org.example.demo

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.feature.message.FeatureMessage
import ai.koog.agents.core.feature.message.FeatureMessageProcessor
import ai.koog.agents.core.feature.writer.FeatureMessageLogWriter
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.tool.SayToUser
import ai.koog.agents.features.tracing.feature.Tracing
import ai.koog.agents.features.tracing.writer.TraceFeatureMessageLogWriter
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory


fun main(args: Array<String>) {

    runBlocking {
        val (executor, model) = gptoss()

        val agent = AIAgent(
            promptExecutor = executor,
            systemPrompt = """
                You are a helpful assistant that can answer general questions.            
                Answer any user query and provide a detailed response.
                Once you have the answer, tell it to the user.
            """.trimIndent(),
            llmModel = model,
            temperature = 1.0,
            toolRegistry = ToolRegistry {
                tool(SayToUser)
            }
        ) {
            //region tracing
            install(Tracing) {
                addMessageProcessor(object : FeatureMessageProcessor() {
                    private val sep = "---------------------------"

                    override val isOpen: StateFlow<Boolean>
                        get() = TODO("Not yet implemented")

                    override suspend fun processMessage(message: FeatureMessage) = println("$message\n$sep")
                    override suspend fun close() = println("close")
                })
            }
            //endregion
        }

        // Why do programmers always mix up Halloween and Christmas? Because Oct 31 == Dec 25!
        agent.run("Tell me a friendly joke about software developers?")//.also { println(it) }
    }
}









