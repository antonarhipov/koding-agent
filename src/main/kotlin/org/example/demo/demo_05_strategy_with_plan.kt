package org.example.demo

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.*
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.tool
import ai.koog.agents.ext.tool.SayToUser
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.dsl.PromptBuilder
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.message.Message
import kotlinx.coroutines.runBlocking
import org.example.kagent.tools.fileOperations
import org.example.kagent.tools.timestamp

fun main(args: Array<String>) {
    runBlocking {
        val (executor, model) = gptoss()
        val codingStrategy = strategy<String, String>("coding strategy") {
            val nodeAnalyzeRequest by nodeLLMRequest()
            val nodeExecuteTool by nodeExecuteTool()
            val nodeSendToolResult by nodeLLMSendToolResult()

            val nodePlanWork by node<String, String> { stageInput ->
                llm.writeSession {
                    appendPrompt {
                        system {
                            +"""
                                Create a minimal list of tasks as a plan, how to implement the request.
                                Assume that this is a new project and all new code should be written in a new folder.
                                The first step in the plan should always be creating the new directory for the project.
                                Enumerate the tasks. Provide the plan in JSON format.
                            """.trimIndent()
                        }
                        user(stageInput)
                    }

                    val response = requestLLMWithoutTools()
                    response.content
                }
            }

//            edge(nodeStart forwardTo nodePlanWork)
//            edge(nodePlanWork forwardTo nodeAnalyzeRequest)
            nodeStart then nodePlanWork then nodeAnalyzeRequest

            edge(nodeAnalyzeRequest forwardTo nodeFinish onAssistantMessage { true })
            edge(nodeAnalyzeRequest forwardTo nodeExecuteTool onToolCall { true })
            edge(nodeExecuteTool forwardTo nodeSendToolResult)
            edge(nodeSendToolResult forwardTo nodeFinish onAssistantMessage { true })
            edge(nodeSendToolResult forwardTo nodeExecuteTool onToolCall { true })
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
                            The code should be generated a dedicated directory.  
                            Once you have the answer, tell it to the user.
                        """.trimIndent()
                    }
                },
                model = model,
                maxAgentIterations = 50
            ),
            toolRegistry = ToolRegistry {
                tool(SayToUser)
                tool(::fileOperations)
                tool(::timestamp)
            }
        ) {
            handleEvents {
                onToolCallStarting { ctx ->
                    println("Calling tool: ${ctx.tool.name}(${ctx.toolArgs})")
                }
                onLLMCallCompleted { ctx ->
                    println("LLM responded =========================  ")
                    ctx.responses.forEach(::println)
                    println("=======================================  ")
                }
            }
        }

        agent.run("Write and test fizzbuzz program in Kotlin").also { println(it) }
    }
}

