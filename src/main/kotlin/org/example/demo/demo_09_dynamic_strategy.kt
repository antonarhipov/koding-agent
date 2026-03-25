package org.example.demo

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.entity.AIAgentNodeBase
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {

        val numbers = listOf(1, 2, 3, 4, 5) // input
        val nodes = mutableListOf<AIAgentNodeBase<Int, Int>>()

        val s = strategy<Int, Int>("dynamic") {

            //region generate nodes
            for (i in numbers) {
                val node by node<Int, Int>("node-$i") { input ->
                    input + i
                }
                nodes.add(node)
            }
            //endregion

            //region connect nodes
            nodeStart then nodes.first()
            nodes.windowed(2) {
                it[0] then it[1]
            }
            nodes.last() then nodeFinish
            //endregion
        }


        val (executor, model) = ministral3_3b()
        val agent = AIAgent(
            promptExecutor = executor,
            systemPrompt = """
                        You are a helpful assistant that can answer general questions.
                        Answer any user query and provide a response along with an explanation.
                    """.trimIndent(),
            llmModel = model,
            strategy = s,
        )

        agent.run(1).also { println(it) }

    }
}









