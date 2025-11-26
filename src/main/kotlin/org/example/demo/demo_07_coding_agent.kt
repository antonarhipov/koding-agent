package org.example.demo

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.singleRunStrategy
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.system.getEnvironmentVariableOrNull
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.tool.file.EditFileTool
import ai.koog.agents.ext.tool.file.ListDirectoryTool
import ai.koog.agents.ext.tool.file.ReadFileTool
import ai.koog.agents.ext.tool.shell.ExecuteShellCommandTool
import ai.koog.agents.ext.tool.shell.JvmShellCommandExecutor
import ai.koog.agents.ext.tool.shell.PrintShellCommandConfirmationHandler
import ai.koog.agents.ext.tool.shell.ShellCommandConfirmation
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.rag.base.files.JVMFileSystemProvider
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {

    val (executor, model) = gptoss()


    val agent = AIAgent(
//        promptExecutor = executor,
        promptExecutor = simpleOpenAIExecutor(getEnvironmentVariableOrNull("OPENAI_API_KEY") ?: throw RuntimeException("OPENAI_API_KEY environment variable is not set")),
        llmModel = OpenAIModels.Chat.GPT5Codex,
        toolRegistry = ToolRegistry {
            tool(ListDirectoryTool(JVMFileSystemProvider.ReadOnly))
            tool(ReadFileTool(JVMFileSystemProvider.ReadOnly))
            tool(EditFileTool(JVMFileSystemProvider.ReadWrite))
            tool(createExecuteShellCommandToolFromEnv())
        },
        systemPrompt = """
        You are a highly skilled programmer. Your job is to implement the programming assignments according to the description.
    """.trimIndent(),
        strategy = singleRunStrategy(),
        maxIterations = 100
    ) {
        handleEvents {
            onToolCallStarting { ctx ->
                println(
                    "Tool '${ctx.tool.name}' called with args:" +
                            " ${ctx.toolArgs.toString().take(100)}"
                )
            }
        }
    }

    val path = "/Users/anton/IdeaProjects/kagent/demo"
    val task = "Write fizzbuzz program in Kotlin"
    val input = "Location, where to create the new files: $path\n\n$task"
    val result = agent.run(input)
    println(result)
}

fun createExecuteShellCommandToolFromEnv(): ExecuteShellCommandTool {
//    return if (System.getenv("BRAVE_MODE")?.lowercase() == "true") {
       return ExecuteShellCommandTool(JvmShellCommandExecutor()) { _ -> ShellCommandConfirmation.Approved }
//    } else {
//        ExecuteShellCommandTool(JvmShellCommandExecutor(), PrintShellCommandConfirmationHandler())
//    }
}


