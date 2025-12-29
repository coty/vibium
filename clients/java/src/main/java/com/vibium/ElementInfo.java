package com.vibium;

/**
 * Information about an element returned from find().
 */
public record ElementInfo(String tag, String text, BoundingBox box) {
}
