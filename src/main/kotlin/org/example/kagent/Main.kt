package org.example.kagent

import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) = runBlocking {
    println("🎯 Kotlin Coding Agent with Koog Framework")
    println("=".repeat(50))

    try {
        val agent = createCodingAgent(args.firstOrNull() ?: "openai")

        // Example usage scenarios
        val examples = listOf(
            "Create a simple calculator class with add, subtract, multiply, and divide operations, including comprehensive tests",
            "Build a data class for a Person with name, age, and email, with validation and tests",
            "Create a utility class for string operations like reverse, palindrome check, and word count with tests"
        )

        println("Example requests you can make:")
        examples.forEachIndexed { index, example ->
            println("${index + 1}. $example")
        }
        println()

        // Interactive loop
        while (true) {
            print("Enter your coding request (or 'quit' to exit): ")
            val userInput = readlnOrNull()?.trim()

            if (userInput.isNullOrEmpty()) {
                continue
            }

            if (userInput.lowercase() == "quit") {
                println("👋 Goodbye!")
                break
            }

            println("\n🔄 Processing your request...")
            println("-".repeat(40))

            try {
                agent.run(userInput)
            } catch (e: Exception) {
                println("❌ Error processing request: ${e.message}")
                e.printStackTrace()
            }

            println("\n" + "=".repeat(50))
        }

    } catch (e: Exception) {
        println("❌ Failed to initialize coding agent: ${e.message}")
        e.printStackTrace()
    }
}