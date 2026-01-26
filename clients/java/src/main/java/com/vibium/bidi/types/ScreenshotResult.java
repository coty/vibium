package com.vibium.bidi.types;

/**
 * The result of a screenshot command.
 *
 * @param data Base64-encoded PNG image data
 */
public record ScreenshotResult(String data) {
}
