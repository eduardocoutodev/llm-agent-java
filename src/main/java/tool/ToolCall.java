package tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;

import java.util.List;
import java.util.Map;

public record ToolCall(
        String toolName,
        String toolCallId,
        Map<String, String> arguments
) {

    public ChatCompletionMessageParam toChatCompletionMessageParam() throws JsonProcessingException {
        return ChatCompletionMessageParam.ofAssistant(
                ChatCompletionAssistantMessageParam.builder()
                        .toolCalls(List.of(
                                        ChatCompletionMessageToolCall.builder()
                                                .id(this.toolCallId())
                                                .function(
                                                        ChatCompletionMessageToolCall.Function.builder()
                                                                .name(this.toolName())
                                                                .arguments(new ObjectMapper().writeValueAsString(this.arguments()))

                                                                .build()
                                                )
                                                .build()
                                )
                        ).build());
    }
}
