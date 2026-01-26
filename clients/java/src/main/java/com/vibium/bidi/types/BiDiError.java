package com.vibium.bidi.types;

/**
 * A BiDi protocol error.
 *
 * @param error Error code (e.g., "no such element", "timeout")
 * @param message Human-readable error message
 * @param stacktrace Server-side stacktrace (may be null)
 */
public record BiDiError(String error, String message, String stacktrace) {
}
