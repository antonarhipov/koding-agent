package org.example.demo

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.entity.ToolSelectionStrategy
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.feature.handler.tool.ToolCallStartingContext
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import kotlinx.coroutines.runBlocking
import ai.koog.agents.core.tools.reflect.tool
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.agents.ext.tool.SayToUser
import ai.koog.agents.features.eventHandler.feature.handleEvents

fun main(args: Array<String>) {
    runBlocking {
        val (executor, model) = ministral3_3b()

        val agent = AIAgent(
            promptExecutor = executor,
            systemPrompt = """
                You are a helpful assistant that can answer general questions.
                Answer any user query and provide a detailed response.
                Once you have the answer, tell it to the user.
            """.trimIndent(),
            llmModel = model,
            temperature = 1.0,
            strategy = strategy<String,String>("my strategy") {
                val sg by subgraphWithTask<String,String>(
                    ToolSelectionStrategy.ALL,
                    name = "enforcement") { userInput ->
                    "Help the user with their request: $userInput"
                }
                nodeStart then sg then nodeFinish
            },
            toolRegistry = ToolRegistry {
//                tool(SayToUser)
                tool(::observeTemperature)
            }
        ) {
            handleEvents {
                onToolCallStarting { ctx: ToolCallStartingContext ->
                    println("Calling the tool: ${ctx.tool.name}")
                }
            }
        }

        agent.run("What is the current temperature in Tallinn? What should I wear?").also { println(it) }
    }
}

@Tool
@LLMDescription("The tool provides real time temperature")
fun observeTemperature(
    city: String
) = when(city) {
    "Brisbane" -> 30L
    "Sydney" -> 24L
    "Melbourne" -> 26L
    "Tallinn" -> -1L
    else -> 0
}

