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

import ai.koog.agents.core.tools.ToolResult
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import kotlinx.serialization.Serializable
import java.io.File

enum class FileOperationType { CREATE, READ, WRITE, DELETE }

@Serializable
data class FileOperationResult(
    val success: Boolean,
    val message: String,
    val content: String? = null
) : ToolResult {
    override fun toStringDefault(): String = message
}

@Tool
@LLMDescription("Perform file operations: create, read, write, delete files")
fun fileOperations(
    @LLMDescription("Operation type")
    operation: FileOperationType, // "create", "read", "write", "delete"
    @LLMDescription("Path to the file")
    filePath: String,
    @LLMDescription("File content for write operations (optional)")
    content: String? = null
): FileOperationResult {
    return try {
        val file = File(filePath)
        when (operation.name.lowercase()) {
            "create" -> {
                file.parentFile?.mkdirs()
                file.createNewFile()
                FileOperationResult(true, "File created: ${filePath}")
            }

            "read" -> {
                if (file.exists()) {
                    val content = file.readText()
                    FileOperationResult(true, "File read successfully", content)
                } else {
                    FileOperationResult(false, "File not found: ${filePath}")
                }
            }

            "write" -> {
                file.parentFile?.mkdirs()
                file.writeText(content ?: "")
                FileOperationResult(true, "Content written to: ${filePath}")
            }

            "delete" -> {
                val deleted = file.delete()
                FileOperationResult(
                    deleted,
                    if (deleted) "File deleted: ${filePath}" else "Failed to delete: ${filePath}"
                )
            }

            else -> FileOperationResult(false, "Unknown operation: ${operation}")
        }
    } catch (e: Exception) {
        FileOperationResult(false, "Error: ${e.message}")
    }
}
