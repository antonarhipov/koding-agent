package org.example.demo

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.entity.ToolSelectionStrategy
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.feature.message.FeatureMessage
import ai.koog.agents.core.feature.message.FeatureMessageProcessor
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.agents.ext.tool.SayToUser
import ai.koog.agents.ext.tool.file.EditFileTool
import ai.koog.agents.ext.tool.file.ListDirectoryTool
import ai.koog.agents.ext.tool.file.ReadFileTool
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.agents.features.tracing.feature.Tracing
import ai.koog.rag.base.files.JVMFileSystemProvider
import kotlinx.coroutines.flow.StateFlow


suspend fun main(args: Array<String>) {
    val (executor, model) = gptoss()

    val codingStrategy = strategy<String, String>("coding strategy") {
        val nodePlanWork by subgraphWithTask<String, String>(tools = listOf(SayToUser)) { input ->
            """
                Create a minimal list of tasks as a plan, how to implement the request.
                Assume that this is a new project and all new code should be written in a new folder.
                The first step in the plan should always be creating the new directory for the project.
                Enumerate the tasks. 
                Provide the plan in JSON format as follows: a JSON collection of JSON elements with id and task fields.
                Once the plan is ready, show it to the user.
                
                User input: $input
            """.trimIndent()
        }

        val nodeImplementTask by subgraphWithTask<String, String>(
            ToolSelectionStrategy.ALL,
            name = "implement task",
        ) { input: String ->
            """
                Implement the user request according to the supplied plan.
                The code should be generated in a new dedicated directory.
                Once you have the answer, tell it to the user.

                $input
            """.trimIndent()
        }

        nodeStart then nodePlanWork then nodeImplementTask then nodeFinish
    }

    val agent = AIAgent(
        promptExecutor = executor,
        llmModel = model,
        toolRegistry = ToolRegistry {
            tool(SayToUser)
            tool(ListDirectoryTool(JVMFileSystemProvider.ReadOnly))
            tool(ReadFileTool(JVMFileSystemProvider.ReadOnly))
            tool(EditFileTool(JVMFileSystemProvider.ReadWrite))
            tool(createBraveExecuteShellCommandToolFromEnv()) // NB! Brave mode
        },
        systemPrompt = """
            You are a coding assistant helping the user to write programs according to their requests.
            Implement the user request according to the supplied plan.
            The code should be generated a dedicated directory.  
            Once you have the answer, tell it to the user.
        """.trimIndent(),
        strategy = codingStrategy,
        maxIterations = 400
    ) {
        handleEvents {
            onToolCallStarting { ctx ->
                println("Tool '${ctx.tool.name}' called with args: ${ctx.toolArgs.toString().take(100)}")
            }
            install(Tracing) {
                addMessageProcessor(object : FeatureMessageProcessor() {
                    private val sep = "---------------------------"

                    override val isOpen: StateFlow<Boolean>
                        get() = TODO("Not yet implemented")

                    override suspend fun processMessage(message: FeatureMessage) = println("$message\n$sep")
                    override suspend fun close() = println("Tracing closed")
                })
            }
        }
    }

    val path = "/Users/anton/IdeaProjects/kagent/demo"
    val task = """
        Implement a fizzbuzz program in Kotlin.
    """.trimIndent()
    val input = """
        Project absolute path: ${path}
        
        ### Task
        $task
    """.trimIndent()
    try {
        val result = agent.run(input)
        println(result)
    } finally {
        executor.close()
    }
}
