package org.example.kagent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteTool
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.dsl.extension.nodeLLMSendToolResult
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.dsl.extension.onToolCall
import ai.koog.agents.local.features.eventHandler.feature.EventHandler
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import kotlinx.coroutines.runBlocking
import org.example.kagent.mcp.McpIntegration

fun createCodingAgent(): AIAgent {
    // Create prompt executor
    val promptExecutor = simpleOpenAIExecutor(
        System.getenv("OPENAI_API_KEY") ?: throw IllegalStateException("OPENAI_API_KEY not set")
    )

    // Create an agent strategy
    val strategy = createCodingAgentStrategy()

    // Configure the agent with a detailed system prompt
    val agentConfig = AIAgentConfig(
        prompt = Prompt.build("coding-assistant") {
            system(
                """
                You are an expert Kotlin coding assistant that can help users with:
                
                1. **Code Generation**: Create Kotlin code based on requirements
                2. **Test Creation**: Write comprehensive unit tests
                3. **Code Compilation**: Compile Kotlin code and handle errors
                4. **Test Execution**: Run tests and report results
                5. **Project Setup**: Create proper project structures
                
                ## Available Tools:
                
                When JetBrains MCP server is available:
                - JetBrains IDE tools for advanced project management
                - Intelligent code completion and analysis
                - Integrated debugging and testing capabilities
                
                Fallback tools when MCP is unavailable:
                - **file_operations**: Create, read, write, delete files
                - **kotlin_compiler**: Compile Kotlin source files
                - **test_runner**: Execute Kotlin tests
                - **project_structure**: Set up project directories
                
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
                
                Always explain what you're doing at each step and provide clear feedback about success or failure.
                """.trimIndent()
            )
        },
        model = OpenAIModels.Chat.GPT4o,
        maxAgentIterations = 25
    )

    val toolRegistry = runBlocking {
        McpIntegration.createToolRegistryWithMcpFallback()
    }

    // Create and return the agent
    return AIAgent(
        promptExecutor = promptExecutor,
        strategy = strategy,
        agentConfig = agentConfig,
        toolRegistry = toolRegistry,
        installFeatures = {
            install(EventHandler) {
                onBeforeAgentStarted = { strategy, _ ->
                    println("🚀 Starting coding session with strategy: ${strategy.name}")
                }
                onAgentFinished = { strategyName, result ->
                    println("✅ Coding session completed: $strategyName")
                    println("📋 Final result: $result")
                }
                onToolCall = { tool, args ->
                    println("🔧 Executing tool: ${tool.name}($args)")
                }
                onToolCallResult = { tool, args, result ->
                    println("✅ Tool completed: ${tool.name}($args): $result")
                }
                onAgentRunError =
                    { strategyName, exception -> println("🚨 Error occurred for strategy: $strategyName: $exception") }
            }
        }
    )
}

fun createCodingAgentStrategy() = strategy("Coding Assistant") {
    // Define nodes for the coding workflow
    val nodeAnalyzeRequest by nodeLLMRequest()
    val nodeExecuteTool by nodeExecuteTool()
    val nodeSendToolResult by nodeLLMSendToolResult()

    // Define the workflow edges
    // Start -> Analyze user request
    edge(nodeStart forwardTo nodeAnalyzeRequest) // Analyze request -> Finish (if no tools needed)
    edge((nodeAnalyzeRequest forwardTo nodeFinish) transformed { it } onAssistantMessage { true })

    // Analyze request -> Execute tool (if tool call is made)
    edge((nodeAnalyzeRequest forwardTo nodeExecuteTool) onToolCall { true })

    // Execute tool -> Send a tool result back to LLM
    edge(nodeExecuteTool forwardTo nodeSendToolResult)

    // Send tool result -> Finish (if final response)
    edge((nodeSendToolResult forwardTo nodeFinish) transformed { it } onAssistantMessage { true })

    // Send tool result -> Execute another tool (for chained operations)
    edge((nodeSendToolResult forwardTo nodeExecuteTool) onToolCall { true }
    )
}