package com.vibium.bidi.types;

/**
 * A BiDi protocol response from the server.
 *
 * @param id The command ID this response corresponds to
 * @param type Response type: "success" or "error"
 * @param result The result payload (for success responses)
 * @param error Error details (for error responses)
 */
public record BiDiResponse(int id, String type, Object result, BiDiError error) {
}
