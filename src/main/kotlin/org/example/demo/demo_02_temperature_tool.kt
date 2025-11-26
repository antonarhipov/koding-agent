package org.example.demo

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.feature.handler.tool.ToolCallStartingContext
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.ext.tool.SayToUser
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import ai.koog.agents.core.tools.reflect.tool
import ai.koog.agents.ext.agent.reActStrategy
import ai.koog.agents.features.eventHandler.feature.handleEvents

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
                tool(::temperatureTool)
            }
        ) {
            handleEvents {
                onToolCallStarting { ctx: ToolCallStartingContext ->
                    println("Calling the tool: ${ctx.tool.name}")
                }
            }
        }

        agent.run("What is the current temperature? What should I wear?").also { println(it) }
    }
}

@Tool
@LLMDescription("The tool provides current accurate temperature in celsius. It integrates with Weather.com")
suspend fun temperatureTool() = 10L
//suspend fun temperatureTool() = 10000L
