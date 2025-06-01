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

package org.example.kagent.tools

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import ai.koog.agents.core.tools.ToolResult
import kotlinx.serialization.Serializable
import java.io.File

object FileOperationsTool : Tool<FileOperationsTool.Args, ToolResult>() {
    @Serializable
    data class Args(
        val operation: String, // "create", "read", "write", "delete"
        val filePath: String,
        val content: String? = null
    ) : Tool.Args

    @Serializable
    data class Result(
        val success: Boolean,
        val message: String,
        val content: String? = null
    ) : ToolResult {
        override fun toStringDefault(): String = message
    }

    override val argsSerializer = Args.serializer()
    override val descriptor = ToolDescriptor(
        name = "file_operations",
        description = "Perform file operations: create, read, write, delete files",
        requiredParameters = listOf(
            ToolParameterDescriptor("operation", "Operation type", ToolParameterType.String),
            ToolParameterDescriptor("filePath", "Path to the file", ToolParameterType.String)
        ),
        optionalParameters = listOf(
            ToolParameterDescriptor("content", "File content for write operations", ToolParameterType.String)
        )
    )

    override suspend fun execute(args: Args): Result {
        return try {
            val file = File(args.filePath)
            when (args.operation.lowercase()) {
                "create" -> {
                    file.parentFile?.mkdirs()
                    file.createNewFile()
                    Result(true, "File created: ${args.filePath}")
                }
                "read" -> {
                    if (file.exists()) {
                        val content = file.readText()
                        Result(true, "File read successfully", content)
                    } else {
                        Result(false, "File not found: ${args.filePath}")
                    }
                }
                "write" -> {
                    file.parentFile?.mkdirs()
                    file.writeText(args.content ?: "")
                    Result(true, "Content written to: ${args.filePath}")
                }
                "delete" -> {
                    val deleted = file.delete()
                    Result(deleted, if (deleted) "File deleted: ${args.filePath}" else "Failed to delete: ${args.filePath}")
                }
                else -> Result(false, "Unknown operation: ${args.operation}")
            }
        } catch (e: Exception) {
            Result(false, "Error: ${e.message}")
        }
    }
}
