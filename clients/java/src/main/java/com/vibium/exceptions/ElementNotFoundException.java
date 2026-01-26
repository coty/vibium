package com.vibium.exceptions;

/**
 * Thrown when a selector matches no elements.
 */
public class ElementNotFoundException extends VibiumException {

    private final String selector;

    /**
     * Create an element not found exception.
     *
     * @param selector The CSS selector that matched no elements
     */
    public ElementNotFoundException(String selector) {
        super("Element not found: " + selector);
        this.selector = selector;
    }

    /** @return The CSS selector that matched no elements */
    public String getSelector() {
        return selector;
    }
}
