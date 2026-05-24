# llm-agent

A minimal agentic coding assistant built in Java. It accepts a natural language prompt, calls an LLM via the [OpenRouter](https://openrouter.ai) API, and autonomously executes tool calls in a loop until the task is complete.

## How it works

The agent runs a loop:
1. Sends the user prompt (plus conversation history) to the LLM
2. Receives a response — either a final answer or one or more tool calls
3. Executes the requested tools and feeds results back into the conversation
4. Repeats until the LLM returns a final answer (no more tool calls)

## Tools

| Tool | Description |
|------|-------------|
| `Read` | Read a file from disk |
| `Write` | Write content to a file |
| `Bash` | Execute a shell command |

## Requirements

- Java 25
- Maven
- An [OpenRouter](https://openrouter.ai) API key

## Setup

```sh
export OPENROUTER_API_KEY=your_key_here
```

Optionally override the base URL (defaults to `https://openrouter.ai/api/v1`):

```sh
export OPENROUTER_BASE_URL=https://openrouter.ai/api/v1
```

## Usage

```sh
./your_program.sh -p "your prompt here"
```

Example:

```sh
./your_program.sh -p "List all Java files in this directory and count the lines in each one"
```

## Model

Defaults to `anthropic/claude-haiku-4.5` via OpenRouter. Change the model in `Main.java` in the `invokeLLMApi` method.

## Build

```sh
mvn package -Ddir=/tmp/llm-agent-build
java --enable-preview --enable-native-access=ALL-UNNAMED -jar /tmp/llm-agent-build/llm-agent.jar -p "your prompt"
```
