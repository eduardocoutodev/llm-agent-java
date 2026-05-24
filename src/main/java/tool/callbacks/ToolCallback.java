package tool.callbacks;

import tool.model.ToolResult;

import java.util.Map;

public sealed interface ToolCallback permits ReadToolCallback, WriteToolCallback, BashToolCallback {
    ToolResult execute(String toolCallId, Map<String, String> arguments);
}
