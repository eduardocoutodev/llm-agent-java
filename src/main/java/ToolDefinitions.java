import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.ChatCompletionTool;
import org.jetbrains.annotations.NotNull;
import tool.ReadTool;
import tool.Tool;

import java.util.List;
import java.util.Map;

public class ToolDefinitions {

    public static List<ChatCompletionTool> getAvailableTools() {
        return List.of(
                readToolDefinition()
        );
    }

    public static Map<String, Tool> getToolCallbacks(){
        return Map.of(
                "Read", new ReadTool()
        );
    }

    @NotNull
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
}
