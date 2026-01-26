package com.vibium.exceptions;

/**
 * Thrown when a wait operation times out.
 */
public class TimeoutException extends VibiumException {

    private final String selector;
    private final int timeout;
    private final String reason;

    /**
     * Create a timeout exception.
     *
     * @param selector The CSS selector that timed out
     * @param timeout The timeout value in milliseconds
     * @param reason Optional reason for the timeout
     */
    public TimeoutException(String selector, int timeout, String reason) {
        super(formatMessage(selector, timeout, reason));
        this.selector = selector;
        this.timeout = timeout;
        this.reason = reason;
    }

    /**
     * Create a timeout exception without a reason.
     *
     * @param selector The CSS selector that timed out
     * @param timeout The timeout value in milliseconds
     */
    public TimeoutException(String selector, int timeout) {
        this(selector, timeout, null);
    }

    private static String formatMessage(String selector, int timeout, String reason) {
        if (reason != null) {
            return String.format("Timeout after %dms waiting for '%s': %s", timeout, selector, reason);
        }
        return String.format("Timeout after %dms waiting for '%s'", timeout, selector);
    }

    /** @return The CSS selector that timed out */
    public String getSelector() {
        return selector;
    }

    /** @return The timeout value in milliseconds */
    public int getTimeout() {
        return timeout;
    }

    /** @return The reason for the timeout, or null */
    public String getReason() {
        return reason;
    }
}
