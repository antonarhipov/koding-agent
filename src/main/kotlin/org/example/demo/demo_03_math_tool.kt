package org.example.demo

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.GraphAIAgent.FeatureContext
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.*
import ai.koog.agents.core.feature.handler.tool.ToolCallStartingContext
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.tool
import ai.koog.agents.ext.tool.SayToUser
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.executor.llms.Executors.promptExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable

fun main(args: Array<String>): Unit {
    runBlocking {
        //select executor based on command line parameter
        val (executor, model) = gptoss()

        val toolRegistry = ToolRegistry {
            tool(SayToUser)
            tool(::mathTool)
        }

        val agent: AIAgent<String, String> = AIAgent(
            promptExecutor = executor,
            systemPrompt = """
            You are an AI assistant helping users to solve tasks based on available tools.
            For any task:
            1. Analyze the request and determine required tools.
            2. Execute tools to perform the changes.
            3. Do not engage in conversation before completing the task.
            4. Say the final result to the user.
            
            MANDATORY INSTRUCTIONS:
            - Only use provided tools
            - No direct conversation - complete task first
            - All changes must be made through tool execution
            - You must use the SAYTOOL to provide the result to the user
        """.trimIndent(),
            llmModel = model,
            toolRegistry = toolRegistry,
        ) {
            handleEvents {
                onToolCallStarting { ctx: ToolCallStartingContext ->
                    println("Calling the tool '${ctx.tool.name}'(${ctx.toolArgs})")
                }
            }
        }
        val mathProblemToSolve =
            "I had 5 apples. I ate 3 apples. My friend gave me 2 more apples. How many apples do I have left?"
        agent.run(mathProblemToSolve)
    }
}

@Tool
@LLMDescription("calculate the sum of numbers")
suspend fun mathTool(
    @LLMDescription("list of numbers")
    numbers: List<Int>,
): Int = numbers.sumOf { it }

