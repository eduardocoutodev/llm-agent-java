package tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class ReadTool implements Tool{
    private static final String FILE_PATH_ARGUMENT = "file_path";

    @Override
    public ToolResult execute(String toolCallId, Map<String, String> arguments) {
        var filePathToRead = arguments.get(FILE_PATH_ARGUMENT);

        try {
            var fileContent = Files.readString(Path.of(filePathToRead));
            return new ToolResult(
                    toolCallId,
                    fileContent,
                    false
            );
        } catch (IOException e) {
            // TODO, should return toolResultError !!
            IO.println("Error while procesing read tool" + e);
            return new ToolResult(
                    toolCallId,
                    "Error while reading the file",
                    true
            );
        }
    }
}
