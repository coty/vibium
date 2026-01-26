package com.vibium.exceptions;

/**
 * Thrown when connecting to the browser fails.
 */
public class ConnectionException extends VibiumException {

    private final String url;

    /**
     * Create a connection exception.
     *
     * @param url The URL that failed to connect
     */
    public ConnectionException(String url) {
        super("Failed to connect to " + url);
        this.url = url;
    }

    /**
     * Create a connection exception with a cause.
     *
     * @param url The URL that failed to connect
     * @param cause The underlying cause
     */
    public ConnectionException(String url, Throwable cause) {
        super("Failed to connect to " + url + ": " + cause.getMessage(), cause);
        this.url = url;
    }

    /** @return The URL that failed to connect */
    public String getUrl() {
        return url;
    }
}
