package org.example.demo

import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.executor.ollama.client.toLLModel
import kotlinx.coroutines.runBlocking

fun autoselect(selector: String) = run {
    val client = OllamaClient()
    val model = runBlocking { client.getModelOrNull(selector, pullIfMissing = true)!!.toLLModel() }
    SingleLLMPromptExecutor(client) to model
}