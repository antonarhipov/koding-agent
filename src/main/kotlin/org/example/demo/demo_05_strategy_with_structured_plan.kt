package org.example.demo

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.ToolSelectionStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.*
import ai.koog.agents.core.system.getEnvironmentVariableOrNull
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.tool
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.agents.ext.tool.SayToUser
import ai.koog.agents.ext.tool.file.EditFileTool
import ai.koog.agents.ext.tool.file.ListDirectoryTool
import ai.koog.agents.ext.tool.file.ReadFileTool
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.rag.base.files.JVMFileSystemProvider
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.example.kagent.tools.fileOperations
import org.example.kagent.tools.timestamp

@Serializable
data class PlanItem(val id: Int, val task: String)

@Serializable
data class Plan(
    val id: Int,
    val items: List<PlanItem>
)


fun main(args: Array<String>) {
    runBlocking {
//        val (executor, model) = gptoss()
        val (executor, model) = openai()

        val codingStrategy = strategy<String, String>("coding strategy") {
            val nodePlanWork by subgraphWithTask<String, Plan>(
                name = "plan work",
                tools = emptyList()) { input ->
                """
                    Create a minimal list of tasks as a plan, how to implement the request.
                    Assume that this is a new project and all new code should be written in a new folder.
                    The first step in the plan should always be creating the new directory for the project.
                    Enumerate the tasks. 
                    
                    Provide the plan in JSON format as follows: a JSON collection of JSON elements with id:Int and task:String fields.
                    
                    User input: $input
                """
            }

            val nodeAnalyzePlan by node<Plan, Plan> { plan ->
                plan.items.forEach { println(it) }
                plan
            }

            val nodeImplementTask by subgraphWithTask<Plan, String>(
                ToolSelectionStrategy.ALL,
                name = "implement task",
            ) { input ->
                """
                    Implement the user request according to the supplied plan.
                    The code should be generated in a new dedicated directory.
                    Once you have the answer, tell it to the user.

                    User input: $input
                """
            }

            nodeStart then nodePlanWork then nodeAnalyzePlan then nodeImplementTask then nodeFinish
        }

        val agent = AIAgent(
            promptExecutor = executor,
            strategy = codingStrategy,
            agentConfig = AIAgentConfig(
                prompt = prompt("system prompt") {
                    system {
                        """
                            You are a coding assistant helping the user to write programs according to their requests.
                            Implement the user request according to the supplied plan.
                            The code should be generated a dedicated directory.  
                            Once you have the answer, tell it to the user.
                        """.trimIndent()
                    }
                },
                model = model,
                maxAgentIterations = 50
            ),
            toolRegistry = ToolRegistry {
                tool(SayToUser)
                tool(ListDirectoryTool(JVMFileSystemProvider.ReadOnly))
                tool(ReadFileTool(JVMFileSystemProvider.ReadOnly))
                tool(EditFileTool(JVMFileSystemProvider.ReadWrite))
                tool(createExecuteShellCommandToolFromEnv())
            }
        ) {
            handleEvents {
//                install(OpenTelemetry) {
//                    setVerbose(true)
//                    addLangfuseExporter()
//                }

                onToolCallStarting { ctx ->
                    println("Calling tool: ${ctx.tool.name}(${ctx.toolArgs})")
                }
                onLLMCallCompleted { ctx ->
                    println("LLM responded =========================  ")
                    ctx.responses.forEach(::println)
                    println("=======================================  ")
                }
            }
        }


        val (path, task) = ("/Users/anton/IdeaProjects/kagent/demo" to "Write and test a fizzbuzz program in Kotlin")
        val input = "Project absolute path: $path\n\n## Task\n$task"
        try {
            val result = agent.run(input)
            println(result)
        } finally {
            executor.close()
        }

    }
}

