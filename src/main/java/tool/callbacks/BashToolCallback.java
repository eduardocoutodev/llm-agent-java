package tool.callbacks;

import tool.model.ToolResult;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class BashToolCallback implements ToolCallback {
    private static final String COMMAND_ARGUMENT = "command";

    @Override
    public ToolResult execute(String toolCallId, Map<String, String> arguments) {
        var bashCommand = arguments.get(COMMAND_ARGUMENT);

        if(bashCommand == null){
            return new ToolResult(
                    toolCallId,
                    "Arguments provided are invalid, please provide bash_command",
                    true
            );
        }

        try {
            var process = new ProcessBuilder(bashCommand)
                    .redirectErrorStream(false)
                    .start();

            Future<String> stdoutFuture = Executors.newVirtualThreadPerTaskExecutor().submit(() ->
                    new String(process.getInputStream().readAllBytes()));
            Future<String> stderrFuture = Executors.newVirtualThreadPerTaskExecutor().submit(() ->
                    new String(process.getErrorStream().readAllBytes()));

            int exitCode = process.waitFor();
            String stdout = stdoutFuture.get();
            String stderr = stderrFuture.get();

            if(exitCode != 0){
                return new ToolResult(
                        toolCallId,
                        "Command resulted on non 0 exite code: " + exitCode
                        + " output of error : " + stderr,
                        true
                );
            }

            return new ToolResult(
                    toolCallId,
                    "Command resulted with success: " + exitCode
                            + " output of command : " + stdout,
                    false
            );

        } catch (Exception e) {
            IO.println("Error while processing bash command" + e);

            return new ToolResult(
                    toolCallId,
                    "Error while processing bash command",
                    true
            );
        }
    }
}
