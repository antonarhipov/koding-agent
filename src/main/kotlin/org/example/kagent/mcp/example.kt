package org.example.kagent.mcp

import ai.koog.agents.ext.agent.simpleSingleRunAgent
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import kotlinx.coroutines.runBlocking

fun main() {
    // Get the API key from environment variables
    val openAIApiToken = System.getenv("OPENAI_API_KEY") ?: error("OPENAI_API_KEY environment variable not set")

    val process = ProcessBuilder(
        "npx", "-y", "@jetbrains/mcp-proxy",
    ).start()

    // Wait for the server to start
    Thread.sleep(5000)


    try {
        runBlocking {
            try {
                // Create the ToolRegistry with tools from the MCP server
                println("Connecting to MCP server...")
                val toolRegistry = McpToolRegistryProvider.fromTransport(
                    transport = McpToolRegistryProvider.defaultStdioTransport(process)
                )
                println("Successfully connected to MCP server")

                // Create the runner
                val agent = simpleSingleRunAgent(
                    executor = simpleOpenAIExecutor(openAIApiToken),
                    llmModel = OpenAIModels.Chat.GPT4o,
                    toolRegistry = toolRegistry,
                )

                val request = "write a simple fizzbuzz program"
                println("Sending request: $request")
                agent.run(
                    request + "You can only call tools. Use JetBrains IDE tools to complete the task"
                )
            } catch (e: Exception) {
                println("Error connecting to  MCP server: ${e.message}")
                e.printStackTrace()
            }
        }
    } finally {
        // Shutdown the curl process
        println("Closing connection to MCP server")
    }
}