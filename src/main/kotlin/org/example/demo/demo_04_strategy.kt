package org.example.demo

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import kotlinx.coroutines.runBlocking
import kotlin.random.Random

fun main() {
    runBlocking {
        val (executor, model) = ministral3_3b()

        val strategy = strategy<String, Int>("custom strategy") {

            val str2int by node<String, Int> { it.toInt() }
            val increment by node<Int, Int> { it + 1 }

            nodeStart then str2int then increment then nodeFinish
        }

        val agent = AIAgent(
            promptExecutor = executor,
            systemPrompt = """
                        You are a helpful assistant that can answer general questions.
                        Answer any user query and provide a detailed response.
                        Once you have the answer, tell it to the user
                    """.trimIndent(),
            strategy = strategy,
            llmModel = model
        )

        val input = Random.nextInt(1000).toString().also { println("Input: $it") }
        agent.run(input).also { println(it) }
    }
}

