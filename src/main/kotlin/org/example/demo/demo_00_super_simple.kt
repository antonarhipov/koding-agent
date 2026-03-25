package org.example.demo

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.feature.handler.tool.ToolCallStartingContext
import ai.koog.agents.core.feature.message.FeatureMessage
import ai.koog.agents.core.feature.message.FeatureMessageProcessor
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.tool.AskUser
import ai.koog.agents.ext.tool.SayToUser
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.agents.features.tracing.feature.Tracing
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.executor.ollama.client.toLLModel
import ai.koog.prompt.llm.LLModel
import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {

        val (executor, model) = gptoss()
        val agent = AIAgent(
            promptExecutor = executor,
            systemPrompt = """
                        You are a helpful assistant specialized in answering general questions accurately and thoroughly.
                        
                        **Critical Instructions:**
                        - You MUST use the ${SayToUser.name} tool to communicate your answer to the user. This is mandatory.
                        - After providing your answer via ${SayToUser.name}, you MUST use the ${AskUser.name} tool to ask a relevant follow-up question.
                        - Never respond with plain text. Always use the designated tools for communication.
                        - If you attempt to respond without using tools, you have failed the task.
                        
                        **Workflow:**
                        1. Analyze the user's query carefully
                        2. Formulate a comprehensive answer with explanation
                        3. Call ${SayToUser.name} with your complete answer
                        4. Call ${AskUser.name} with a thoughtful follow-up question related to the topic
                    """.trimIndent(),
            llmModel = model,
            toolRegistry = ToolRegistry {
                tool(SayToUser)
                tool(AskUser)
            },
            maxIterations = 10
        ) {
            handleEvents {
                onToolCallStarting { ctx ->
                    println("Calling tool: ${ctx.toolName}(${ctx.toolArgs})")
                }
                onLLMCallCompleted { ctx ->
                    println("LLM Response =========================  ")
                    ctx.responses.forEach(::println)
                    println("=======================================  ")
                }
                onLLMCallStarting { ctx ->
                    println("LLM Call =========================  ")
                    println("Prompt: ${ctx.prompt}")
                    println("Tools: ")
                    ctx.tools.forEach(::println)
                    println("=======================================  ")
                }
            }
        }
        agent.run("What is the capital of France?")


    }
}









