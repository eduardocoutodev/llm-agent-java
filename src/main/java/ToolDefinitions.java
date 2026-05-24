import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.ChatCompletionTool;
import org.jetbrains.annotations.NotNull;
import tool.callbacks.BashToolCallback;
import tool.callbacks.ReadToolCallback;
import tool.callbacks.ToolCallback;
import tool.callbacks.WriteToolCallback;

import java.util.List;
import java.util.Map;

public class ToolDefinitions {

    public static List<ChatCompletionTool> getAvailableTools() {
        return List.of(
                readToolDefinition(),
                writeToolDefinition(),
                bashToolDefinition()
        );
    }

    public static Map<String, ToolCallback> getToolCallbacks(){
        return Map.of(
                "Read", new ReadToolCallback(),
                "Write", new WriteToolCallback(),
                "Bash", new BashToolCallback()
        );
    }

    private static ChatCompletionTool readToolDefinition() {
        return ChatCompletionTool.builder()
                .type(JsonValue.from("function"))
                .function(FunctionDefinition.builder()
                        .name("Read")
                        .description("Read and return the contents of a file")
                        .parameters(
                                FunctionParameters.builder()
                                        .putAdditionalProperty("type", JsonValue.from("object"))
                                        .putAdditionalProperty("properties", JsonValue.from(
                                                Map.of(
                                                        "file_path", JsonValue.from(
                                                                Map.of(
                                                                        "type", "string",
                                                                        "description", "The path to the file to read"
                                                                )
                                                        )
                                                )
                                        ))
                                        .putAdditionalProperty("required", JsonValue.from(List.of("file_path")))
                                        .build()
                        )
                        .build()
                )
                .build();
    }

    private static ChatCompletionTool writeToolDefinition() {
        return ChatCompletionTool.builder()
                .type(JsonValue.from("function"))
                .function(FunctionDefinition.builder()
                        .name("Write")
                        .description("Write content to a file")
                        .parameters(
                                FunctionParameters.builder()
                                        .putAdditionalProperty("type", JsonValue.from("object"))
                                        .putAdditionalProperty("properties", JsonValue.from(
                                                Map.of(
                                                        "file_path", JsonValue.from(
                                                                Map.of(
                                                                        "type", "string",
                                                                        "description", "The path to the file to write to"
                                                                )
                                                        ),
                                                        "content", JsonValue.from(
                                                                Map.of(
                                                                        "type", "string",
                                                                        "description", "The content to write to the file"
                                                                )
                                                        )
                                                )
                                        ))
                                        .putAdditionalProperty("required", JsonValue.from(List.of("file_path", "content")))
                                        .build()
                        )
                        .build()
                )
                .build();
    }

    private static ChatCompletionTool bashToolDefinition() {
        return ChatCompletionTool.builder()
                .type(JsonValue.from("function"))
                .function(FunctionDefinition.builder()
                        .name("Bash")
                        .description("Execute a shell command")
                        .parameters(
                                FunctionParameters.builder()
                                        .putAdditionalProperty("type", JsonValue.from("object"))
                                        .putAdditionalProperty("properties", JsonValue.from(
                                                Map.of(
                                                        "command", JsonValue.from(
                                                                Map.of(
                                                                        "type", "string",
                                                                        "description", "The command to execute"
                                                                )
                                                        )
                                                )
                                        ))
                                        .putAdditionalProperty("required", JsonValue.from(List.of("command")))
                                        .build()
                        )
                        .build()
                )
                .build();
    }
}
