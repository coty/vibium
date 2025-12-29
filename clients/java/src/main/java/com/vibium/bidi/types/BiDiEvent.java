package com.vibium.bidi.types;

import java.util.Map;

/**
 * A BiDi protocol event from the server.
 */
public record BiDiEvent(String method, Map<String, Object> params) {
}
