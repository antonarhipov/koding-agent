package org.example.demo

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.tool.SayToUser
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.agents.mcp.defaultStdioTransport
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
                        - Summarize whatever you find in the documentation and make sure that it matches the user's query.
                    """.trimIndent(),
                    executor = executor,
                    llmModel = model,
                    toolRegistry = toolRegistry + mcpRegistry,
                )
                val result = agent.run("How to configure database connection for Spring Data JDBC?")
                println(result)
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
