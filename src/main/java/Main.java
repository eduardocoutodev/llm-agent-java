import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

void main(String[] args) {
    if (args.length < 2 || !"-p".equals(args[0])) {
        System.err.println("Usage: program -p <prompt>");
        System.exit(1);
    }

    String prompt = args[1];

    var client = buildOpenAIClient();

    var tools = ToolDefinitions.getAvailableTools();
    ChatCompletion response = client.chat().completions().create(
            ChatCompletionCreateParams.builder()
                    .model("anthropic/claude-haiku-4.5")
                    .addUserMessage(prompt)
                    .tools(tools)
                    .build()
    );

    if (response.choices().isEmpty()) {
        throw new RuntimeException("no choices in response");
    }

    IO.print(response.choices().get(0).message().content().orElse(""));
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
