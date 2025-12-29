package com.vibium;

/**
 * Options for finding elements.
 */
public class FindOptions {

    private Integer timeout = null;

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
