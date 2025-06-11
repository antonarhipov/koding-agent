package org.example.kagent.mcp

import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.mcp.McpToolRegistryProvider
import org.example.kagent.tools.FileOperationsTool
import org.example.kagent.tools.KotlinCompilerTool
import org.example.kagent.tools.ProjectStructureTool
import org.example.kagent.tools.TestRunnerTool

object McpIntegration {
    
    suspend fun createToolRegistryWithMcpFallback(): ToolRegistry {
        return try {
            // Try to initialize MCP in a blocking way for now
            // In a real implementation, this could be done asynchronously during agent startup
            createMcpToolRegistryBlocking()
        } catch (e: Exception) {
            println("📦 MCP initialization failed, using custom tools: ${e.message}")
            createFallbackToolRegistry()
        }
    }
    
    private suspend fun createMcpToolRegistryBlocking(): ToolRegistry {
        return try {
            println("🔌 Attempting to connect to JetBrains MCP server...")

            println("Starting JetBrains MCP server...")
            // Start the JetBrains MCP proxy process
            val process = ProcessBuilder(
                "npx", "-y", "@jetbrains/mcp-proxy"
            ).start()
            Thread.sleep(5000)
            println("✅ JetBrains MCP server started")

            println("Connecting to MCP server...")
            val toolRegistry = McpToolRegistryProvider.fromTransport(
                transport = McpToolRegistryProvider.defaultStdioTransport(process)
            )

            println("✅ Successfully connected to JetBrains MCP server")
            println("🛠️  Using JetBrains IDE tools for development tasks")

            toolRegistry
            
        } catch (e: ClassNotFoundException) {
            println("📦 MCP support not available in current build, using custom tools")
            throw e
        } catch (e: Exception) {
            println("❌ Failed to connect to MCP server: ${e.message}")
            throw e
        }
    }
    
    private fun createFallbackToolRegistry(): ToolRegistry {
        println("🛠️  Using custom Kotlin development tools")
        return ToolRegistry {
            tool(ProjectStructureTool)
            tool(FileOperationsTool)
            tool(KotlinCompilerTool)
            tool(TestRunnerTool)
        }
    }
}