package com.vibium;

import java.time.Duration;

/**
 * Options for element actions (click, type, etc.).
 *
 * <pre>{@code
 * // Using milliseconds
 * element.click(ActionOptions.withTimeout(5000));
 *
 * // Using Duration
 * element.click(ActionOptions.withTimeout(Duration.ofSeconds(5)));
 * }</pre>
 */
public class ActionOptions {

    private Integer timeout = null;

    /**
     * Create options with the specified timeout in milliseconds.
     *
     * @param timeoutMs Timeout in milliseconds
     * @return New ActionOptions instance
     */
    public static ActionOptions withTimeout(int timeoutMs) {
        return new ActionOptions().timeout(timeoutMs);
    }

    /**
     * Create options with the specified timeout.
     *
     * @param timeout Timeout duration
     * @return New ActionOptions instance
     */
    public static ActionOptions withTimeout(Duration timeout) {
        return new ActionOptions().timeout(timeout);
    }

    /**
     * Set timeout for actionability checks in milliseconds.
     *
     * @param timeoutMs Timeout in milliseconds
     * @return This instance for chaining
     */
    public ActionOptions timeout(int timeoutMs) {
        this.timeout = timeoutMs;
        return this;
    }

    /**
     * Set timeout for actionability checks using Duration.
     *
     * @param timeout Timeout duration
     * @return This instance for chaining
     */
    public ActionOptions timeout(Duration timeout) {
        this.timeout = (int) timeout.toMillis();
        return this;
    }

    /**
     * Get the timeout value.
     *
     * @return Timeout in milliseconds, or null if not set
     */
    public Integer getTimeout() {
        return timeout;
    }
}
