/*
 * Copyright 2023 Kagent Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.example.kagent

import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    println("🎯 Kotlin Coding Agent with Koog Framework")
    println("=".repeat(50))

    try {
        val agent = createCodingAgent()

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
