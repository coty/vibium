package com.vibium.bidi.types;

import java.util.Map;

/**
 * A BiDi protocol command to send to the server.
 */
public record BiDiCommand(int id, String method, Map<String, Object> params) {
}
