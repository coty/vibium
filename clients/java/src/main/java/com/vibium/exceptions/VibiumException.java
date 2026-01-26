package com.vibium.exceptions;

/**
 * Base exception for all Vibium-related errors.
 */
public class VibiumException extends RuntimeException {

    /**
     * Create a new VibiumException with a message.
     *
     * @param message Error message
     */
    public VibiumException(String message) {
        super(message);
    }

    /**
     * Create a new VibiumException with a message and cause.
     *
     * @param message Error message
     * @param cause The underlying cause
     */
    public VibiumException(String message, Throwable cause) {
        super(message, cause);
    }
}
