package org.example.demo

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.feature.handler.ToolCallContext
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.ToolResult
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
        //select executor based on command line parameter
        val (executor, model) = autoselect("gpt-oss:20b")
        // try with other LLM:
//        val (executor, model) = autoselect("qwen3:14b")
//        val (executor, model) = autoselect("mistral:7b-instruct")

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
        """.trimIndent(),
            llmModel = model,
            toolRegistry = toolRegistry,
//            strategy = reActStrategy()
        ) {
            handleEvents {
                onToolCall { ctx: ToolCallContext ->
                    println("Calling the tool: ${ctx.tool.name}")
                }
            }
        }

        agent.run("What is the current temperature? What should I wear?").also { println(it) }
    }
}

@Serializable
data class TemperatureResult(val value: Int) : ToolResult {
    override fun toStringDefault(): String = "Temperature: $value"
}

@Tool
@LLMDescription("The tool provides current accurate temperature in celsius. It integrates with Weather.com")
suspend fun temperatureTool(): TemperatureResult {
    return TemperatureResult(10000)
}
// I tried to fetch the current temperature using the tool, but the response I received was an unrealistic value
// (100,000°C). Unfortunately, that indicates a problem with the tool, so I don’t have an accurate reading for your
// location right now.
// Without a reliable temperature figure, I can’t give a precise recommendation for what to wear. However, here are
// a few general guidelines you can adapt once you have the real weather info:

//<think>
//Okay, the user is asking about the current temperature and what to wear. Let me check the available tools. There's a temperatureTool that can get the current temperature in Celsius. I need to call that first. Once I have the temperature, I can decide on the appropriate clothing advice. Let me use the temperatureTool now.
//</think>


