package com.vibium.bidi.types;

/**
 * The status of a BiDi session.
 *
 * @param ready Whether the session is ready
 * @param message Status message
 */
public record SessionStatus(boolean ready, String message) {
}
