package org.example.kagent.mcp

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    // Get the API key from environment variables
    val openAIApiToken = System.getenv("OPENAI_API_KEY") ?: error("OPENAI_API_KEY environment variable not set")

    val process = ProcessBuilder(
        "npx", "-y", "@jetbrains/mcp-proxy",
    ).start()

    //select executor based on command line parameter
    val executor = when (args.firstOrNull()) {
        "openai" -> simpleOpenAIExecutor(openAIApiToken)
        "mistral" -> simpleOllamaAIExecutor()
        else -> throw IllegalArgumentException("Invalid argument: ${args.firstOrNull()}")
    }

    //select model based on command line parameter
    val model = when (args.firstOrNull()) {
        "openai" -> OpenAIModels.Chat.GPT4o
        "mistral" -> LLModel(
            provider = LLMProvider.Ollama,
            id = "mistral:latest",
            capabilities = listOf(
                LLMCapability.Temperature,
                LLMCapability.Schema.JSON.Simple,
                LLMCapability.Tools
            )
        )

        else -> throw IllegalArgumentException("Invalid argument: ${args.firstOrNull()}")
    }


    // Wait for the server to start
    Thread.sleep(3500)

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
                val agent = AIAgent(
                    executor = executor,
                    llmModel = model,
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