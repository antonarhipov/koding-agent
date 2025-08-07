package org.example.kagent.mcp

import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.tool
import ai.koog.agents.mcp.McpToolRegistryProvider
import org.example.kagent.tools.fileOperations
import org.example.kagent.tools.kotlinCompiler
import org.example.kagent.tools.projectStructure
import org.example.kagent.tools.testRunner

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

            println("Connecting to MCP server...")
            val toolRegistry = McpToolRegistryProvider.fromTransport(
                transport = McpToolRegistryProvider.defaultSseTransport("http://localhost:64342")
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
            tool(::projectStructure)
            tool(::fileOperations)
            tool(::kotlinCompiler)
            tool(::testRunner)
        }
    }
}

