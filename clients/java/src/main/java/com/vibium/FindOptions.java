package com.vibium;

import java.time.Duration;

/**
 * Options for finding elements.
 *
 * <pre>{@code
 * // Using milliseconds
 * Element el = vibe.find("button", FindOptions.withTimeout(5000));
 *
 * // Using Duration
 * Element el = vibe.find("button", FindOptions.withTimeout(Duration.ofSeconds(5)));
 * }</pre>
 */
public class FindOptions {

    private Integer timeout = null;

    /**
     * Create options with the specified timeout in milliseconds.
     *
     * @param timeoutMs Timeout in milliseconds
     * @return New FindOptions instance
     */
    public static FindOptions withTimeout(int timeoutMs) {
        return new FindOptions().timeout(timeoutMs);
    }

    /**
     * Create options with the specified timeout.
     *
     * @param timeout Timeout duration
     * @return New FindOptions instance
     */
    public static FindOptions withTimeout(Duration timeout) {
        return new FindOptions().timeout(timeout);
    }

    /**
     * Set timeout for finding element in milliseconds.
     *
     * @param timeoutMs Timeout in milliseconds
     * @return This instance for chaining
     */
    public FindOptions timeout(int timeoutMs) {
        this.timeout = timeoutMs;
        return this;
    }

    /**
     * Set timeout for finding element using Duration.
     *
     * @param timeout Timeout duration
     * @return This instance for chaining
     */
    public FindOptions timeout(Duration timeout) {
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
