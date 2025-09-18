package org.example.demo

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.tool.SayToUser
import ai.koog.prompt.dsl.prompt
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    runBlocking {
        val (executor, model) = autoselect("gpt-oss:20b")
        val stringToIntStrategy = strategy<String, Int>("str2int") {
            val toInt by node<String, Int> { it.toInt() }
            val inc by node<Int, Int> { it + 1 }

            edge(nodeStart forwardTo toInt)
            edge(toInt forwardTo inc)
            edge(inc forwardTo nodeFinish)
        }

        val agent = AIAgent(
            promptExecutor = executor,
            strategy = stringToIntStrategy,
            agentConfig = AIAgentConfig(
                prompt = prompt("example") {
                    system {
                        """
                            You are a helpful assistant that can answer general questions.
                            Answer any user query and provide a detailed response.
                            Once you have the answer, tell it to the user
                        """.trimIndent()
                    }
                },
                model = model,
                maxAgentIterations = 50
            ),
            toolRegistry = ToolRegistry {
                tools(
                    listOf(SayToUser)
                )
            }
        )

        agent.run("555").also { println(it) }
    }
}

