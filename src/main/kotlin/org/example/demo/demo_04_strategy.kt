package org.example.demo

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.tool.SayToUser
import kotlinx.coroutines.runBlocking
import kotlin.random.Random

fun main() {
    runBlocking {
        val (executor, model) = ministral3_3b()

        val strategy = strategy<String, String>("custom strategy") {
            val str2int by node<String, Int> { input -> input.toInt() }
            val increment by node<Int, Int> { input -> input + 1 }
            val spanish by node<Int, String>("spell in spanish") { input ->
                llm.writeSession {
                    appendPrompt {
                        user("Spell $input in Spanish")
                    }
                    val response = requestLLMWithoutTools()
                    response.content
                }
            }
            val italian by node<Int, String>("spell in italian") { input ->
                llm.writeSession {
                    appendPrompt {
                        user("Spell $input in Italian")
                    }
                    val response = requestLLMWithoutTools()
                    response.content
                }
            }

            edge(nodeStart forwardTo str2int)
            edge(str2int forwardTo increment)
            edge(increment forwardTo spanish onCondition { it % 2 == 0})
            edge(increment forwardTo italian onCondition { it % 2 != 0})
            edge(spanish forwardTo nodeFinish)
            edge(italian forwardTo nodeFinish)
        }

        val agent = AIAgent(
            promptExecutor = executor,
            systemPrompt = """
                                    You are a helpful assistant that can answer general questions.
                                    Answer any user query and provide a detailed response.
                                    Once you have the answer, tell it to the user
                    """.trimIndent(),
            strategy = strategy,
            llmModel = model,
            toolRegistry = ToolRegistry {
                tools(
                    listOf(SayToUser)
                )
            }
        )

        val input = Random.nextInt(1000).toString().also { println("Input: $it") }
        agent.run(input).also { println(it) }
    }
}

