/*
 * Copyright 2023 Kagent Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.example.kagent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.*
import ai.koog.agents.features.common.message.FeatureMessage
import ai.koog.agents.features.common.message.FeatureMessageProcessor
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.agents.features.tracing.feature.Tracing
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.executor.ollama.client.toLLModel
import kotlinx.coroutines.runBlocking
import org.example.kagent.mcp.McpIntegration
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun createCodingAgent(selector: String): AIAgent {
    val (executor, model) = when (selector) {
        "openai" -> simpleOpenAIExecutor(
            System.getenv("OPENAI_API_KEY") ?: throw IllegalStateException("OPENAI_API_KEY not set")
        ) to OpenAIModels.Chat.GPT4o

        "mistral" -> {
            val client = OllamaClient()
            val model = runBlocking { client.getModelOrNull("mistral")!!.toLLModel() }
            SingleLLMPromptExecutor(client) to model
        }

        else -> throw IllegalArgumentException("Invalid argument: ${selector}")
    }

    // Create an agent strategy
    val strategy = createCodingAgentStrategy()

    // Configure the agent with a detailed system prompt
    val agentConfig = AIAgentConfig(
        prompt = prompt("coding-assistant") {
            system(
                """
                You are an expert Kotlin coding assistant that can help users with:
                
                1. **Code Generation**: Create Kotlin code based on requirements
                2. **Test Creation**: Write comprehensive unit tests
                3. **Code Compilation**: Compile Kotlin code and handle errors
                4. **Test Execution**: Run tests and report results
                5. **Project Setup**: Create proper project structures
                
                ## Workflow:
                
                1. Understand the user's requirements
                2. Create appropriate project structure if needed
                3. Generate the main code file
                4. Create comprehensive tests
                5. Compile both main code and tests
                6. Run tests and report results
                7. Fix any compilation or test failures
                
                ## Best Practices:
                
                - Write clean, idiomatic Kotlin code
                - Include proper error handling
                - Create meaningful test cases
                - Follow Kotlin naming conventions
                - Add appropriate documentation
                
                ***IMPORTANT*** YOU MUST USE TOOLS TO IMPLEMENT THE TASK!!!
                ***IMPORTANT*** DON'T CHAT WITH ME BEFORE YOU FINISH
                
                Always explain what you're doing at each step and provide clear feedback about success or failure.
                """.trimIndent()
            )
        },
        model = model,
        maxAgentIterations = 25
    )

    val toolRegistry = runBlocking {
        McpIntegration.createToolRegistryWithMcpFallback()
    }

    // Create and return the agent
    return AIAgent(
        promptExecutor = executor,
        strategy = strategy,
        agentConfig = agentConfig,
        toolRegistry = toolRegistry,
    ) {
        handleEvents {
            onBeforeAgentStarted { strategy, _ ->
                println("🚀 Starting coding session with strategy: ${strategy.name}")
            }
            onAgentFinished { strategyName, result ->
                println("✅ Coding session completed: $strategyName")
                println("📋 Final result: $result")
            }
            onToolCall { tool, args ->
                println("🔧 Executing tool: ${tool.name}($args)")
            }
            onToolCallResult { tool, args, result ->
                println("✅ Tool completed: ${tool.name}($args): $result")
            }
            onAgentRunError { strategyName, uuid, exception
                ->
                println("🚨 Error occurred for strategy: $strategyName [$uuid]: $exception")
            }
        }

        install(Tracing) {
            addMessageProcessor(object : FeatureMessageProcessor() {
                override suspend fun processMessage(message: FeatureMessage) {
                    println(">>>>> TRACING: $message")
                }

                override suspend fun close() {
                    TODO("Not yet implemented")
                }
            })
        }
    }
}

fun createCodingAgentStrategy() = strategy("Coding Assistant") {
    // Define nodes for the coding workflow
    val nodeAnalyzeRequest by nodeLLMRequest()
    val nodeExecuteTool by nodeExecuteTool()
    val nodeSendToolResult by nodeLLMSendToolResult()
//    val compressNode by nodeLLMCompressHistory<ReceivedToolResult>()

    // Define the workflow edges
    // Start -> Analyze user request
    edge(nodeStart forwardTo nodeAnalyzeRequest) // Analyze request -> Finish (if no tools needed)
    edge(nodeAnalyzeRequest forwardTo nodeFinish onAssistantMessage { true })

    // Analyze request -> Execute tool (if tool call is made)
    edge(nodeAnalyzeRequest forwardTo nodeExecuteTool onToolCall { true })

    // Execute tool -> Send a tool result back to LLM
    edge(nodeExecuteTool forwardTo nodeSendToolResult)

    // Send tool result -> Finish (if final response)
    edge(nodeSendToolResult forwardTo nodeFinish onAssistantMessage { true })

    // Send tool result -> Execute another tool (for chained operations)
    edge(nodeSendToolResult forwardTo nodeExecuteTool onToolCall { true })
}