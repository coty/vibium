package com.vibium.exceptions;

/**
 * Thrown when the browser process dies unexpectedly.
 */
public class BrowserCrashedException extends VibiumException {

    private final int exitCode;
    private final String output;

    /**
     * Create a browser crashed exception.
     *
     * @param exitCode The process exit code
     * @param output Process output (stdout/stderr)
     */
    public BrowserCrashedException(int exitCode, String output) {
        super(formatMessage(exitCode, output));
        this.exitCode = exitCode;
        this.output = output;
    }

    /**
     * Create a browser crashed exception without output.
     *
     * @param exitCode The process exit code
     */
    public BrowserCrashedException(int exitCode) {
        this(exitCode, null);
    }

    private static String formatMessage(int exitCode, String output) {
        if (output != null && !output.isEmpty()) {
            return String.format("Browser crashed with exit code %d: %s", exitCode, output);
        }
        return String.format("Browser crashed with exit code %d", exitCode);
    }

    /** @return The process exit code */
    public int getExitCode() {
        return exitCode;
    }

    /** @return Process output, or null */
    public String getOutput() {
        return output;
    }
}
