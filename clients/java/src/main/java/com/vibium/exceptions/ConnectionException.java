package com.vibium.exceptions;

/**
 * Thrown when connecting to the browser fails.
 */
public class ConnectionException extends VibiumException {

    private final String url;

    public ConnectionException(String url) {
        super("Failed to connect to " + url);
        this.url = url;
    }

    public ConnectionException(String url, Throwable cause) {
        super("Failed to connect to " + url + ": " + cause.getMessage(), cause);
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
