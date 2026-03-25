package org.example.demo

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.dsl.prompt
import kotlinx.coroutines.runBlocking


fun main(args: Array<String>) {
    runBlocking {
        val (executor, model) = gptoss()

        val codingStrategy = strategy<String, TaskPlan>("coding strategy") {
            val nodePlanWorkStructured by node<String, TaskPlan> { input ->
                val structuredResponse = llm.writeSession {
                    appendPrompt {
                        user(
                            """
                                Create a development plan with the enumerated task list for the implementing the user's request.
                                User input: $input
                            """.trimIndent()
                        )
                    }

                    requestLLMStructured<TaskPlan>(
                        examples = examplePlan
                    )
                }

                val default = TaskPlan(0, items = emptyList())

                (structuredResponse.getOrNull()?.data ?: default).also {
                    println("I've got the structured plan! Number of tasks: ${it.items.size}")
                }
            }


            nodeStart then nodePlanWorkStructured then nodeFinish
        }

        val agent = AIAgent(
            promptExecutor = executor,
            strategy = codingStrategy,
            agentConfig = AIAgentConfig(
                prompt = prompt("system prompt") {
                    system {
                        """
                            You are a helpful assistant helping the user plan the development tasks for the user's request.
                            For the user request, you should create a development plan to ensure the successful implementation.
                        """.trimIndent()
                    }
                },
                model = model,
                maxAgentIterations = 50
            )
        ) {
            handleEvents {
                onToolCallStarting { ctx ->
                    println("Calling tool: ${ctx.toolName}(${ctx.toolArgs})")
                }
                onLLMCallCompleted { ctx ->
                    println("LLM Response =========================  ")
                    ctx.responses.forEach(::println)
                    println("=======================================  ")
                }
                onLLMCallStarting { ctx ->
                    println("LLM Call =========================  ")
                    println("Prompt: ${ctx.prompt}")
                    println("Tools: ")
                    ctx.tools.forEach(::println)
                    println("=======================================  ")
                }
            }
        }

        val input = "Task: Implement a fizzbuzz program in Kotlin"
        try {
            val result = agent.run(input)
            println(result)
        } finally {
            executor.close()
        }

    }
}

