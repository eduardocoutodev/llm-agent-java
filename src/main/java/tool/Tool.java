package tool;

import java.util.Map;

public sealed interface Tool permits ReadTool {
    void execute(Map<String, String> arguments);
}
