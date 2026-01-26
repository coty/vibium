package com.vibium.bidi.types;

import java.util.Map;

/**
 * A BiDi protocol command sent to the server.
 *
 * @param id Unique command identifier
 * @param method The BiDi method name (e.g., "browsingContext.navigate")
 * @param params Command parameters
 */
public record BiDiCommand(int id, String method, Map<String, Object> params) {
}
