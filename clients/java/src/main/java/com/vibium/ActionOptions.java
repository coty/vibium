package com.vibium;

/**
 * Options for element actions (click, type, etc.).
 */
public class ActionOptions {

    private Integer timeout = null;

    /**
     * Create options with a custom timeout.
     *
     * @param timeout Timeout in milliseconds
     * @return ActionOptions with the specified timeout
     */
    public static ActionOptions withTimeout(int timeout) {
        return new ActionOptions().timeout(timeout);
    }

    /**
     * Timeout in milliseconds for actionability checks.
     * Default: 30000 (30 seconds).
     */
    public ActionOptions timeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public Integer getTimeout() {
        return timeout;
    }
}
