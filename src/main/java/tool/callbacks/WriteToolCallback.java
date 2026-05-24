package tool.callbacks;

import tool.model.ToolResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class WriteToolCallback implements ToolCallback {
    private static final String FILE_PATH_ARGUMENT = "file_path";
    private static final String CONTENT_ARGUMENT = "content";

    @Override
    public ToolResult execute(String toolCallId, Map<String, String> arguments) {
        var filePathToWrite = arguments.get(FILE_PATH_ARGUMENT);
        var content = arguments.get(CONTENT_ARGUMENT);

        if (filePathToWrite == null || content == null) {
            return new ToolResult(
                    toolCallId,
                    "Arguments provided are invalid, please provide file_path and content",
                    true
            );
        }

        try {
            var path = Path.of(filePathToWrite);

            if (!Files.exists(path)) {
                Files.createFile(path);
            }

            Files.writeString(path, content);

            return new ToolResult(
                    toolCallId,
                    "Wrote with success on the file",
                    false
            );
        } catch (IOException e) {
            IO.println("Error while processing write tool" + e);
            return new ToolResult(
                    toolCallId,
                    "Error while writing to file",
                    true
            );
        }
    }
}
