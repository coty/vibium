package com.vibium.bidi.types;

import java.util.Map;

/**
 * A BiDi protocol event (unsolicited message from server).
 *
 * @param method The event method name
 * @param params Event parameters
 */
public record BiDiEvent(String method, Map<String, Object> params) {
}
