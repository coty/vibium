package com.vibium.bidi.types;

/**
 * Error details from a BiDi protocol response.
 */
public record BiDiError(String error, String message, String stacktrace) {
}
