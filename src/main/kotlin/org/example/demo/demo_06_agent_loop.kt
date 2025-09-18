package org.example.demo

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.AIAgentSubgraph
import ai.koog.agents.core.agent.entity.ToolSelectionStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.*
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.agents.core.tools.reflect.tool
import ai.koog.agents.ext.agent.StringSubgraphResult
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.agents.ext.tool.SayToUser
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.message.Message
import ai.koog.prompt.xml.xml
import kotlinx.coroutines.runBlocking
import org.example.kagent.tools.UserTools
import org.example.kagent.tools.fileOperations
import org.example.kagent.tools.timestamp

fun main(args: Array<String>) {
    runBlocking {
        val (executor, model) = gptoss()
        val codingStrategy = strategy<String, String>("coding strategy") {
            val nodeAnalyzeRequest by nodeLLMRequest()
            val nodeExecuteTool by nodeExecuteTool()
            val nodeSendToolResult by nodeLLMSendToolResult()

            val nodePlanWork by node<String, String> { input ->
                llm.writeSession {
                    updatePrompt {
                        system {
                            +"""
                                Create a minimal list of tasks as a plan, how to implement the request.
                                Assume that this is a new project and all new code should be written in a new folder.
                                The first step in the plan should always be creating the new directory for the project.
                                Enumerate the tasks. Provide the plan in JSON format.
                            """.trimIndent()
                        }
                        user(input)
                    }

                    val response: Message.Response = requestLLMWithoutTools()
                    response.content
                }
            }


            // TODO implement subgraph for plan review

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
                onToolCall { ctx ->
                    println("Calling tool: ${ctx.tool.name}(${ctx.toolArgs})")
                }
                onAfterLLMCall { ctx ->
                    println("LLM responded =========================  ")
                    ctx.responses.forEach(::println)
                    println("=======================================  ")
                }
            }
        }

        while (true) {
            println("Enter command: ")
            print("$ ")
            readln().also { userPrompt ->
                agent.run(userPrompt).also { println(it) }
            }
        }
    }
}

