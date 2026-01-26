package com.vibium;

/**
 * Information about a found element.
 *
 * @param tag HTML tag name (e.g., "div", "button")
 * @param text Text content of the element
 * @param box Bounding box of the element
 */
public record ElementInfo(String tag, String text, BoundingBox box) {
}
