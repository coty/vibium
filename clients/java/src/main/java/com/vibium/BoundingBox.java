package com.vibium;

/**
 * Represents the bounding box of an element on the page.
 *
 * @param x X coordinate of the top-left corner
 * @param y Y coordinate of the top-left corner
 * @param width Width of the element
 * @param height Height of the element
 */
public record BoundingBox(double x, double y, double width, double height) {
}
