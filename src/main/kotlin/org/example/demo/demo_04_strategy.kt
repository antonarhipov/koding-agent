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
        val (executor, model) = ministral3b()

        val myStrategy = strategy<String, String>("my custom strategy") {
            val str2int by node<String, Int> { it.toInt() }
            val inc by node<Int, Int> { it + 1 }

            val spellInSpanish by node<Int, String> {
                llm.writeSession {
                    appendPrompt {
                        user("Spell $it in Spanish")
                    }

                    val response = requestLLMWithoutTools()
                    response.content
                }
            }
            val spellInItalian by node<Int, String> {
                llm.writeSession {
                    appendPrompt {
                        user("Spell $it in Italian")
                    }

                    val response = requestLLMWithoutTools()
                    response.content
                }
            }

            edge(nodeStart forwardTo str2int)
            edge(str2int forwardTo inc)
            edge(inc forwardTo spellInSpanish onCondition { it % 2 == 0 })
            edge(inc forwardTo spellInItalian onCondition { it % 2 != 0 })
            edge(spellInSpanish forwardTo nodeFinish)
            edge(spellInItalian forwardTo nodeFinish)
        }

        val agent = AIAgent(
            promptExecutor = executor,
            systemPrompt = """
                                    You are a helpful assistant that can answer general questions.
                                    Answer any user query and provide a detailed response.
                                    Once you have the answer, tell it to the user
                    """.trimIndent(),
            strategy = myStrategy,
            llmModel = model,
            maxIterations = 50,
            toolRegistry = ToolRegistry {
                tools(
                    listOf(SayToUser)
                )
            }
        )

        agent.run(Random.nextInt(0, 100).toString()).also { println(it) }
    }
}

