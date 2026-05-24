package tool.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionToolMessageParam;

public record ToolResult(
        String toolCallId,
        String content,
        boolean isError
) {

    public ChatCompletionMessageParam toChatCompletionMessageParam() throws JsonProcessingException {
        return ChatCompletionMessageParam.ofTool(
                ChatCompletionToolMessageParam.builder()
                        .toolCallId(this.toolCallId())
                        .content(new ObjectMapper().writeValueAsString(this.content()))
                        .build()
        );
    }
}
