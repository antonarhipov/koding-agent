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


//> Task :org.example.demo.Demo_03_math_toolKt.main()
//[main] INFO ai.koog.prompt.executor.ollama.client.OllamaClient - Loaded Ollama model card for gpt-oss:20b
//[main] INFO ai.koog.agents.features.eventHandler.feature.EventHandler - Start installing feature: EventHandler
//[main] INFO ai.koog.agents.core.agent.entity.AIAgentSubgraph - Executing subgraph 'single_run' [single_run, single_run, 4f7aa00c-3108-45a5-8ebe-b0ff8cdcdc0d]
//[main] INFO ai.koog.agents.core.agent.entity.AIAgentSubgraph - No enforced execution point, starting from __start__ [single_run, single_run, 4f7aa00c-3108-45a5-8ebe-b0ff8cdcdc0d]
//[main] INFO ai.koog.agents.core.agent.GraphAIAgent - [agent id: 3506080e-111b-47d4-9db3-35865ae48f11, run id: 4f7aa00c-3108-45a5-8ebe-b0ff8cdcdc0d] Executing tools: [say_to_user]
//Calling the tool 'say_to_user'(Args(message=You have 4 apples left.))
//Agent says: You have 4 apples left.


//> Task :org.example.demo.Demo_03_math_toolKt.main()
//[main] INFO ai.koog.prompt.executor.ollama.client.OllamaClient - Loaded Ollama model card for gpt-oss:20b
//[main] INFO ai.koog.agents.features.eventHandler.feature.EventHandler - Start installing feature: EventHandler
//[main] INFO ai.koog.agents.core.agent.entity.AIAgentSubgraph - Executing subgraph 'single_run' [single_run, single_run, 0e3e4c5d-7c7c-4c83-8a4f-aaaa9f2526e9]
//[main] INFO ai.koog.agents.core.agent.entity.AIAgentSubgraph - No enforced execution point, starting from __start__ [single_run, single_run, 0e3e4c5d-7c7c-4c83-8a4f-aaaa9f2526e9]
//[main] INFO ai.koog.agents.core.agent.GraphAIAgent - [agent id: c0f86e1c-eb92-4c07-8b4c-8805f4339206, run id: 0e3e4c5d-7c7c-4c83-8a4f-aaaa9f2526e9] Executing tools: [mathTool]
//Calling the tool 'mathTool'(VarArgs(args={parameter #0 numbers of fun mathTool(kotlin.collections.List<kotlin.Int>): kotlin.Int=[5, -3, 2]}))
//[main] INFO ai.koog.agents.core.agent.GraphAIAgent - [agent id: c0f86e1c-eb92-4c07-8b4c-8805f4339206, run id: 0e3e4c5d-7c7c-4c83-8a4f-aaaa9f2526e9] Executing tools: [say_to_user]
//Calling the tool 'say_to_user'(Args(message=You have 4 apples left.))
//Agent says: You have 4 apples left.