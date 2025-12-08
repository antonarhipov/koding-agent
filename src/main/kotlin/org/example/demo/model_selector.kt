package org.example.demo

import ai.koog.agents.core.system.getEnvironmentVariableOrNull
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.executor.ollama.client.toLLModel

suspend fun gptoss() = autoselect("gpt-oss:20b")
suspend fun mistral() = autoselect("mistral:latest")
suspend fun qwen314b() = autoselect("qwen3:14b")
suspend fun qwen_latest() = autoselect("qwen3:latest")
suspend fun devstral24b() = autoselect("devstral:24b")
suspend fun ministral3_3b() = autoselect("ministral-3:3b")
suspend fun ministral3_14b() = autoselect("ministral-3:14b")

suspend fun autoselect(selector: String) = run {
    val client = OllamaClient()
    val model = client.getModelOrNull(selector, pullIfMissing = true)!!.toLLModel()
    SingleLLMPromptExecutor(client) to model
}

suspend fun openai() = (simpleOpenAIExecutor(
    getEnvironmentVariableOrNull("OPENAI_API_KEY")
        ?: throw RuntimeException("OPENAI_API_KEY environment variable is not set")
) to OpenAIModels.Chat.GPT5)

