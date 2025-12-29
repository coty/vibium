package com.vibium;

/**
 * Options for finding elements.
 */
public class FindOptions {

    private Integer timeout = null;

    /**
     * Create options with a custom timeout.
     *
     * @param timeout Timeout in milliseconds
     * @return FindOptions with the specified timeout
     */
    public static FindOptions withTimeout(int timeout) {
        return new FindOptions().timeout(timeout);
    }

    /**
     * Timeout in milliseconds to wait for element.
     * Default: 30000 (30 seconds).
     */
    public FindOptions timeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public Integer getTimeout() {
        return timeout;
    }
}
