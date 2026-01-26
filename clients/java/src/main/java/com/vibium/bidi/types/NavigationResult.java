package com.vibium.bidi.types;

/**
 * The result of a navigation command.
 *
 * @param navigation Navigation ID
 * @param url The URL navigated to
 */
public record NavigationResult(String navigation, String url) {
}
