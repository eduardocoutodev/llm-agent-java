import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonValue;
import com.openai.models.chat.completions.*;
import kotlin.Pair;
import tool.Tool;
import tool.ToolCall;
import tool.ToolResult;

import static com.openai.models.chat.completions.ChatCompletion.Choice.FinishReason.STOP;
import static com.openai.models.chat.completions.ChatCompletion.Choice.FinishReason.TOOL_CALLS;


void main(String[] args) {
    if (args.length < 2 || !"-p".equals(args[0])) {
        System.err.println("Usage: program -p <prompt>");
        System.exit(1);
    }

    String prompt = args[1];

    var client = buildOpenAIClient();

    agenticLoop(client, prompt);
}

private static OpenAIClient buildOpenAIClient() {
    String apiKey = System.getenv("OPENROUTER_API_KEY");
    String baseUrl = System.getenv("OPENROUTER_BASE_URL");
    if (baseUrl == null || baseUrl.isEmpty()) {
        baseUrl = "https://openrouter.ai/api/v1";
    }

    if (apiKey == null || apiKey.isEmpty()) {
        throw new RuntimeException("OPENROUTER_API_KEY is not set");
    }

    return OpenAIOkHttpClient.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .build();
}

private static void agenticLoop(
        OpenAIClient client,
        String userPrompt
) {
    var messages = List.of(
            ChatCompletionMessageParam.ofUser(
                    ChatCompletionUserMessageParam.builder()
                            .content(userPrompt)
                            .build()
            )
    );

    var response = invokeLLMApi(client, messages);

    // Have this loop, to prevent recursive calls to llm from blowing up my money
    var allowedNumberOfLoops = 5;
    var currentInteraction = 0;
    var toolCallbacks = ToolDefinitions.getToolCallbacks();

    while (!isFinalAssistantMessage(response) && currentInteraction < allowedNumberOfLoops) {
        IO.println("Starting agentic loop, interaction n:" + currentInteraction);
        currentInteraction++;

        if (response.choices().isEmpty()) {
            throw new RuntimeException("no choices in response");
        }

        if (!containsToolCalls(response)) {
            IO.println("No tool call backs to invoke !");
            continue;
        }

        var toolCallsToInvoke = convertToolCallBacksToInvoke(response);
        var toolResults = invokeToolCallBacks(toolCallsToInvoke, toolCallbacks);

        messages = appendToolResultsToChatContext(messages, toolCallsToInvoke, toolResults);
        response = invokeLLMApi(client, messages);
    }

    IO.print(response.choices().get(0).message().content().orElse(""));
}

private static boolean containsToolCalls(ChatCompletion chatCompletion) {
    return chatCompletion.choices()
            .stream()
            .anyMatch(choice -> choice.finishReason().equals(TOOL_CALLS));
}

private static boolean isFinalAssistantMessage(ChatCompletion chatCompletion) {
    return chatCompletion.choices()
            .stream()
            .allMatch(choice -> choice.finishReason().equals(STOP));
}

private static ChatCompletion invokeLLMApi(OpenAIClient client, List<ChatCompletionMessageParam> messages) {
    var tools = ToolDefinitions.getAvailableTools();
    var chatClientParams = ChatCompletionCreateParams.builder()
            .model("anthropic/claude-haiku-4.5")
            .messages(messages)
            .tools(tools)
            .build();

    return client.chat().completions().create(
            chatClientParams
    );
}

private static List<ToolCall> convertToolCallBacksToInvoke(ChatCompletion response) {
    var objectMapper = new ObjectMapper();
    TypeReference<Map<String, String>> argumentsTypeRef = new TypeReference<>() {
    };

    return response.choices()
            .stream()
            .filter(choice -> choice.finishReason().equals(TOOL_CALLS))
            .flatMap(choice -> {
                        var toolCalls = choice.message().toolCalls().orElseThrow(() -> new IllegalStateException("Expected Tool call"));
                        return toolCalls.stream()
                                .map(
                                        toolCall -> {
                                            try {
                                                return new ToolCall(
                                                        toolCall.function().name(),
                                                        toolCall.id(),
                                                        objectMapper.readValue(toolCall.function().arguments(), argumentsTypeRef)
                                                );
                                            } catch (JsonProcessingException e) {
                                                IO.println("Error while converting arguments");
                                                throw new RuntimeException(e);
                                            }
                                        }
                                );
                    }
            ).toList();
}

private static List<ToolResult> invokeToolCallBacks(List<ToolCall> toolCallsToInvoke, Map<String, Tool> toolCallbacks) {
    // To introduce async processing if possible !
    return toolCallsToInvoke.stream()
            .map(toolCallback -> {
                var tool = toolCallbacks.get(toolCallback.toolName());
                if (tool == null) {
                    throw new IllegalStateException("Invalid tool invoked " + toolCallback.toolName());
                }

                return tool.execute(toolCallback.toolCallId(), toolCallback.arguments());
            })
            .toList();
}


private static List<ChatCompletionMessageParam> appendToolResultsToChatContext(
        List<ChatCompletionMessageParam> messages,
        List<ToolCall> toolCallsToInvoke,
        List<ToolResult> toolResults
) {
    Map<ToolCall, ToolResult> toolCallsToToolResultsMap = toolCallsToInvoke.stream()
            .map(toolCall -> {
                var toolResultAssociated = toolResults.stream()
                        .filter(toolResult -> toolCall.toolCallId().equalsIgnoreCase(toolResult.toolCallId()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Corresponding tool result not found"));

                return new Pair<>(toolCall, toolResultAssociated);
            }).collect(Collectors.toMap(
                    Pair::getFirst, Pair::getSecond
            ));

    List<ChatCompletionMessageParam> toolMessagesToAppend = new ArrayList<>();
    for (var entry : toolCallsToToolResultsMap.entrySet()) {
        var toolCall = entry.getKey();
        var toolResult = entry.getValue();

        try {
            toolMessagesToAppend.add(toolCall.toChatCompletionMessageParam());
            toolMessagesToAppend.add(toolResult.toChatCompletionMessageParam());
        } catch (JsonProcessingException e) {
            IO.println("Failure while processing Json");
            throw new RuntimeException(e);
        }
    }


    return Stream.of(
                    messages,
                    toolMessagesToAppend
            )
            .flatMap(Collection::stream)
            .toList();
}