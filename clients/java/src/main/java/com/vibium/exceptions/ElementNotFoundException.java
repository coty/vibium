package com.vibium.exceptions;

/**
 * Thrown when a selector matches no elements.
 */
public class ElementNotFoundException extends VibiumException {

    private final String selector;

    public ElementNotFoundException(String selector) {
        super("Element not found: " + selector);
        this.selector = selector;
    }

    public String getSelector() {
        return selector;
    }
}
