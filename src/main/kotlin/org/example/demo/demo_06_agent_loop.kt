package org.example.demo

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.AIAgentService
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.context.RollbackStrategy
import ai.koog.agents.core.agent.entity.ToolSelectionStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.*
import ai.koog.agents.core.system.getEnvironmentVariableOrNull
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.tool
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.agents.ext.tool.SayToUser
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.agents.snapshot.feature.Persistence
import ai.koog.agents.snapshot.providers.InMemoryPersistenceStorageProvider
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import kotlinx.coroutines.runBlocking
import org.example.kagent.tools.fileOperations
import org.example.kagent.tools.timestamp

fun main(args: Array<String>) {

    val inMemoryStorage = InMemoryPersistenceStorageProvider()

    runBlocking {
        val (executor, model) = gptoss()

        val codingStrategy = strategy<String, String>("coding strategy") {
            val nodePlanWork by subgraphWithTask<String, String>(
                tools = emptyList(),
                name = "plan work"
            ) { input ->
                """
                    Create a minimal list of tasks as a plan, how to implement the request.
                    Assume that this is a new project and all new code should be written in a new folder.
                    The first step in the plan should always be creating the new directory for the project.
                    Enumerate the tasks. Provide the plan in JSON format.

                    User input: $input
                """
            }

            val nodeImplementTask by subgraphWithTask<String, String>(
                ToolSelectionStrategy.ALL,
                name = "implement task",
            ) { input ->
                """
                    Implement the user request according to the supplied plan.
                    The code should be generated a new dedicated directory.  
                    Once you have the answer, tell it to the user.

                    User input: $input
                """
            }

            nodeStart then nodePlanWork then nodeImplementTask then nodeFinish
        }

        val agent = AIAgent(
            promptExecutor = executor,
            strategy = codingStrategy,
            agentConfig = AIAgentConfig(
                prompt = prompt("system prompt") {
                    system {
                        """
                            You are a coding assistant helping the user to write programs according to their requests.
                            Implement the user request according to the supplied plan.
                            The code should be generated a new dedicated directory.
                            Once you have the answer, tell it to the user.
                        """.trimIndent()
                    }
                },
                model = model,
                maxAgentIterations = 100
            ),
            toolRegistry = ToolRegistry {
                tool(SayToUser)
                tool(createExecuteShellCommandToolFromEnv())
            }
        ) {
            // Checkpoint feature requires unique node names in the strategy metadata
            install(Persistence) {
                this.storage = inMemoryStorage
                this.rollbackStrategy = RollbackStrategy.MessageHistoryOnly
                this.enableAutomaticPersistence = true
            }

            handleEvents {
                onToolCallStarting { ctx ->
                    println("Calling tool: ${ctx.tool.name}(${ctx.toolArgs})")
                }
                onLLMCallCompleted { ctx ->
                    println("LLM Response =========================  ")
                    ctx.responses.forEach(::println)
                    println("=======================================  ")
                }
                onLLMCallStarting {ctx ->
                    println("LLM Call =========================  ")
                    println("Prompt: ${ctx.prompt}")
                    println("Tools: ")
                    ctx.tools.forEach(::println)
                    println("=======================================  ")
                }
            }
        }


        agent.run("Write and test fizzbuzz program in Kotlin").also { println(it) }


//        val service = AIAgentService.fromAgent(agent)
//
//        while (true) {
//            println("Enter command: ")
//            print("$ ")
//
//            readln().also { userPrompt ->
//                service.createAgentAndRun(userPrompt)
//                agent.run(userPrompt).also { println(it) }
//            }
//        }
    }
}

