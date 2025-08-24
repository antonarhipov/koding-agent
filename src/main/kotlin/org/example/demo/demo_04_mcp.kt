package org.example.demo

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteTool
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.dsl.extension.nodeLLMSendMessageForceOneTool
import ai.koog.agents.core.dsl.extension.nodeLLMSendToolResult
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.dsl.extension.onToolCall
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.tool.SayToUser
import ai.koog.agents.features.common.message.FeatureMessage
import ai.koog.agents.features.common.message.FeatureMessageProcessor
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.agents.features.tracing.feature.Tracing
import ai.koog.agents.mcp.McpToolRegistryProvider
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    // Get the API key from environment variables
//    val openAIApiToken = System.getenv("OPENAI_API_KEY") ?: error("OPENAI_API_KEY environment variable not set")

    val (executor, model) = autoselect("gpt-oss:20b")

    val process = ProcessBuilder(
        "npx", "-y", "@upstash/context7-mcp@latest",
    ).start()

    // Wait for the server to start
    Thread.sleep(3500)

    try {
        runBlocking {
            try {

                val toolRegistry = ToolRegistry { tool(SayToUser) }

                // Create the ToolRegistry with tools from the MCP server
                println("Connecting to MCP server...")
                val mcpRegistry = McpToolRegistryProvider.fromTransport(
                    transport = McpToolRegistryProvider.defaultStdioTransport(process)
                )

                println("Successfully connected to MCP server")

                // Create the runner
                val agent = AIAgent(
                    systemPrompt = """
                        You are a helpful assistant that can answer user questions with documentation help.            
                        Answer any user query and provide a detailed response.
                        Before answering the question, check the documentation about the subject. 
                        Once you have the answer, summarize all the information, and tell it to the user.
                        
                        MANDATORY INSTRUCTIONS:
                        - Only use provided tools
                        - No direct conversation - complete task first
                        - All changes must be made through tool execution
                        - You must use the SAYTOOL to provide the result to the user
                    """.trimIndent(),
                    executor = executor,
                    llmModel = model,
                    toolRegistry = toolRegistry + mcpRegistry
                )
                {
                    install(Tracing) {
                        addMessageProcessor(object : FeatureMessageProcessor() {
                            override suspend fun processMessage(message: FeatureMessage) = println("$message")
                            override suspend fun close() = TODO("Not yet implemented")
                        })
                    }
                }

                agent.run("How to use extension functions in Kotlin.")
            } catch (e: Exception) {
                println("Error connecting to MCP server: ${e.message}")
                e.printStackTrace()
            }
        }
    } finally {
        println("Closing connection to MCP server")
        process.destroy()
    }
}
