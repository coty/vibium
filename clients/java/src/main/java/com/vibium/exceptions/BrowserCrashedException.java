package com.vibium.exceptions;

/**
 * Thrown when the browser process dies unexpectedly.
 */
public class BrowserCrashedException extends VibiumException {

    private final int exitCode;
    private final String output;

    public BrowserCrashedException(int exitCode) {
        this(exitCode, null);
    }

    public BrowserCrashedException(int exitCode, String output) {
        super(formatMessage(exitCode, output));
        this.exitCode = exitCode;
        this.output = output;
    }

    private static String formatMessage(int exitCode, String output) {
        if (output != null && !output.isEmpty()) {
            return String.format("Browser crashed with exit code %d: %s", exitCode, output);
        }
        return String.format("Browser crashed with exit code %d", exitCode);
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getOutput() {
        return output;
    }
}
