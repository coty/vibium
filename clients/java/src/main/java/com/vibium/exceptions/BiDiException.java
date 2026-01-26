package com.vibium.exceptions;

/**
 * Thrown when the BiDi protocol returns an error response.
 * Includes the error code from the protocol for programmatic handling.
 */
public class BiDiException extends VibiumException {

    private final String errorCode;
    private final String stacktrace;

    /**
     * Create a BiDi exception.
     *
     * @param errorCode Protocol error code (e.g., "no such element", "timeout")
     * @param message Human-readable error message
     * @param stacktrace Server-side stacktrace (may be null)
     */
    public BiDiException(String errorCode, String message, String stacktrace) {
        super(errorCode + ": " + message);
        this.errorCode = errorCode;
        this.stacktrace = stacktrace;
    }

    /**
     * Create a BiDi exception without a stacktrace.
     *
     * @param errorCode Protocol error code
     * @param message Human-readable error message
     */
    public BiDiException(String errorCode, String message) {
        this(errorCode, message, null);
    }

    /** @return The protocol error code (e.g., "no such element", "timeout") */
    public String getErrorCode() {
        return errorCode;
    }

    /** @return The server-side stacktrace, or null */
    public String getServerStacktrace() {
        return stacktrace;
    }

    /**
     * Check if this is a specific error type.
     *
     * @param code The error code to check
     * @return True if this exception has the specified error code
     */
    public boolean isError(String code) {
        return errorCode != null && errorCode.equals(code);
    }
}
