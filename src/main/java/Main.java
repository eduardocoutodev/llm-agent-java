import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.jetbrains.annotations.NotNull;
import tool.Tool;
import tool.ToolCallToInvoke;

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
    var tools = ToolDefinitions.getAvailableTools();
    var chatClientParams = ChatCompletionCreateParams.builder()
            .model("anthropic/claude-haiku-4.5")
            .addUserMessage(userPrompt)
            .tools(tools)
            .build();

    var response = client.chat().completions().create(
            chatClientParams
    );

    // Have this loop, to prevent recursive calls to llm from blowing up my money
    var allowedNumberOfLoops = 1;
    var currentInteraction = 0;
    var toolCallbacks = ToolDefinitions.getToolCallbacks();

    while (!isFinalAssistantMessage(response) && currentInteraction < allowedNumberOfLoops) {
        currentInteraction++;

        if (response.choices().isEmpty()) {
            throw new RuntimeException("no choices in response");
        }

        if (!containsToolCalls(response)) {
            IO.println("No tool call backs to invoke !");
            continue;
        }

        var toolCallsToInvoke = convertToolCallBacksToInvoke(response);
        invokeToolCallBacks(toolCallsToInvoke, toolCallbacks);
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

private static List<ToolCallToInvoke> convertToolCallBacksToInvoke(ChatCompletion response) {
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
                                                return new ToolCallToInvoke(
                                                        toolCall.function().name(),
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

private static void invokeToolCallBacks(List<ToolCallToInvoke> toolCallsToInvoke, Map<String, Tool> toolCallbacks) {
    // To introduce async processing if possible !
    for (var toolCallback : toolCallsToInvoke) {
        var tool = toolCallbacks.get(toolCallback.toolName());
        if (tool == null) {
            throw new IllegalStateException("Invalid tool invoked " + toolCallback.toolName());
        }

        tool.execute(toolCallback.arguments());
    }
}