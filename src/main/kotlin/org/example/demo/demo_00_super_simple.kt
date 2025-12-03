package org.example.demo

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.executor.ollama.client.toLLModel
import ai.koog.prompt.llm.LLModel
import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {

        // see gptoss()
        val client = OllamaClient()
        val model = client.getModelOrNull("gpt-oss:20b", pullIfMissing = true)!!.toLLModel()
        val executor = SingleLLMPromptExecutor(client)

        val agent = AIAgent(
            promptExecutor = executor,
            systemPrompt = """
                You are a helpful assistant that can answer general questions.
                Answer any user query and provide a response along with an explanation.
            """.trimIndent(),
            llmModel = model,
            temperature = 1.0,
        )

        agent.run("Tell me a silly joke Australia?").also { println(it) }
    }
}









