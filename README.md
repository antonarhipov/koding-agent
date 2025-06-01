# Kagent - Kotlin Coding Agent with Koog Framework

Kagent is an AI-powered coding assistant that helps users generate, compile, and test Kotlin code based on natural language requests. Built on the Koog Agents framework, it provides an interactive interface for code generation and testing.

## Features

- **Code Generation**: Create Kotlin classes, data classes, and utility functions based on requirements
- **Test Creation**: Generate comprehensive unit tests for the created code
- **Code Compilation**: Compile Kotlin code and handle errors
- **Test Execution**: Run tests and report results
- **Project Setup**: Create proper project structures

## Setup

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/kagent.git
   cd kagent
   ```

2. Set your OpenAI API key as an environment variable:
   ```
   export OPENAI_API_KEY=your_api_key_here
   ```

3. Build the project:
   ```
   ./gradlew build
   ```

4. Run the application:
   ```
   ./gradlew run
   ```

## Usage

Once the application is running, you'll be presented with an interactive prompt where you can enter your coding requests. For example:

```
Enter your coding request (or 'quit' to exit): Create a simple calculator class with add, subtract, multiply, and divide operations, including comprehensive tests
```

The agent will:
1. Create a proper project structure
2. Generate the requested Kotlin code
3. Create comprehensive tests
4. Compile the code and tests
5. Run the tests and report results

## Project Structure

- `src/main/kotlin/org/example/kagent/` - Main source code
  - `Agent.kt` - Agent configuration and strategy
  - `Main.kt` - Application entry point
  - `tools/` - Tools used by the agent
    - `FileOperationsTool.kt` - File manipulation operations
    - `KotlinCompilerTool.kt` - Kotlin code compilation
    - `TestRunnerTool.kt` - Test execution
    - `ProjectStructureTool.kt` - Project structure creation

## Branches

- **main**: The primary branch with the core functionality using custom tools for code generation, compilation, and testing.
- **mcp**: Integrates JetBrains MCP (Model Code Processing) server support with fallback to custom tools. This branch enhances the agent's capabilities by leveraging JetBrains' code processing services when available.

## Dependencies

- Kotlin 2.2.0-RC
- Koog Agents Framework 0.1.0

## License

This project is licensed under the Apache License, Version 2.0 - see the [LICENSE](LICENSE) file for details.

## Contributing

[Add contribution guidelines here]
