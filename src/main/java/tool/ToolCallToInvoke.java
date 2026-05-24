package tool;

import java.util.Map;

public record ToolCallToInvoke(
        String toolName,
        Map<String, String> arguments
) {
}
