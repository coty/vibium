package com.vibium.bidi.types;

/**
 * A BiDi protocol response from the server.
 */
public record BiDiResponse(int id, String type, Object result, BiDiError error) {

    public boolean isSuccess() {
        return "success".equals(type);
    }

    public boolean isError() {
        return "error".equals(type);
    }
}
