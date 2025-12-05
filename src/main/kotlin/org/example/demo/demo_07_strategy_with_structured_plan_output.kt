package org.example.demo

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.ToolSelectionStrategy
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.agents.ext.tool.SayToUser
import ai.koog.agents.ext.tool.file.EditFileTool
import ai.koog.agents.ext.tool.file.ListDirectoryTool
import ai.koog.agents.ext.tool.file.ReadFileTool
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.Executors.promptExecutor
import ai.koog.prompt.message.Message
import ai.koog.prompt.structure.StructureFixingParser
import ai.koog.prompt.structure.executeStructured
import ai.koog.rag.base.files.JVMFileSystemProvider
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Task")
@LLMDescription("A single task in a task plan.")
data class Task(
    @property:LLMDescription("The task identifier")
    val id: Int,
    @property:LLMDescription("The task description")
    val task: String)

@Serializable
@SerialName("TaskPlan")
@LLMDescription("A sequential plan for implementing a user request, contains a list of tasks.")
data class TaskPlan(
    @property:LLMDescription("The task plan identifier")
    val id: Int,
    @property:LLMDescription("The list of tasks in the plan")
    val items: List<Task>
)

val examplePlan = listOf(
    TaskPlan(1, listOf(
        Task(1, "Create the project directory at /some/path"),
        Task(2, "Initialize a new project "),
        Task(3, "Create Foo.kt and implement the function"),
        Task(4, "Create FooTest.kt and write unit tests that verify the function"),
        Task(5, "Run test to ensure all tests pass")
    ))
)

fun main(args: Array<String>) {
    runBlocking {
//        val (executor, model) = gptoss()
        val (executor, model) = openai()

        val codingStrategy = strategy<String, String>("coding strategy") {

            val nodePlanWork by subgraphWithTask<String, String>(
                name = "plan work",
                tools = emptyList()) { input ->
                """
                    Create a minimal list of tasks as a plan, how to implement the request.
                    Assume that this is a new project and all new code should be written in a new folder.
                    The first step in the plan should always be creating the new directory for the project.
                    Enumerate the tasks.
                    
                    User input: $input
                """
            }

            val nodePlanWorkStructured by node<String, TaskPlan> { _ ->
                val structuredResponse = llm.writeSession {
                    requestLLMStructured<TaskPlan>(
                        examples = examplePlan
                    )
                }

                val default = TaskPlan(0, items = emptyList())

                val plan = structuredResponse.getOrNull()?.data ?: default
                println("I've got the structured plan! Number of tasks: ${plan.items.size}")

                plan
            }


            // This is unreliable. For better results, see https://docs.koog.ai/structured-output/
            val nodeAnalyzePlan by node<TaskPlan, TaskPlan> { plan ->
                println("------- Implementation Plan --------")
                plan.items.forEach { println(it) }
                plan
            }

            val nodeImplementTask by subgraphWithTask<TaskPlan, String>(
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

            nodeStart then nodePlanWork then nodePlanWorkStructured then nodeAnalyzePlan then nodeImplementTask then nodeFinish
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
                onLLMCallStarting { ctx ->
                    println("LLM Call =========================  ")
                    println("Prompt: ${ctx.prompt}")
                    println("Tools: ")
                    ctx.tools.forEach { println(it.name) }
                    println("=======================================  ")
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

