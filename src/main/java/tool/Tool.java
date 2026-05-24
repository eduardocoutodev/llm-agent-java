package tool;

import java.util.Map;

public sealed interface Tool permits ReadTool {
    ToolResult execute(String toolCallId, Map<String, String> arguments);
}
