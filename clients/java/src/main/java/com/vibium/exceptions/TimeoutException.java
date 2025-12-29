package com.vibium.exceptions;

/**
 * Thrown when a wait operation times out.
 */
public class TimeoutException extends VibiumException {

    private final String selector;
    private final int timeout;
    private final String reason;

    public TimeoutException(String selector, int timeout) {
        this(selector, timeout, null);
    }

    public TimeoutException(String selector, int timeout, String reason) {
        super(formatMessage(selector, timeout, reason));
        this.selector = selector;
        this.timeout = timeout;
        this.reason = reason;
    }

    private static String formatMessage(String selector, int timeout, String reason) {
        if (reason != null) {
            return String.format("Timeout after %dms waiting for '%s': %s", timeout, selector, reason);
        }
        return String.format("Timeout after %dms waiting for '%s'", timeout, selector);
    }

    public String getSelector() {
        return selector;
    }

    public int getTimeout() {
        return timeout;
    }

    public String getReason() {
        return reason;
    }
}
