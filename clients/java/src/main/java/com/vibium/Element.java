package com.vibium;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.vibium.bidi.BiDiClient;
import com.vibium.exceptions.ElementNotFoundException;

import java.util.Map;

/**
 * Represents a DOM element on the page.
 *
 * <p>Provides methods to interact with and query the element.
 *
 * <pre>{@code
 * Element button = vibe.find("button.submit");
 * button.click();
 * String text = button.text();
 * }</pre>
 */
public class Element {

    private final BiDiClient client;
    private final String context;
    private final String selector;
    private final ElementInfo info;
    private final Gson gson = new Gson();

    /**
     * Create an Element instance.
     *
     * @param client The BiDi client
     * @param context The browsing context ID
     * @param selector The CSS selector used to find this element
     * @param info Information about the element
     */
    Element(BiDiClient client, String context, String selector, ElementInfo info) {
        this.client = client;
        this.context = context;
        this.selector = selector;
        this.info = info;
    }

    /**
     * Get information about the element (tag, text, bounding box).
     *
     * @return Element information
     */
    public ElementInfo getInfo() {
        return info;
    }

    /**
     * Click the element.
     * Waits for element to be visible, stable, receive events, and enabled.
     */
    public void click() {
        click(null);
    }

    /**
     * Click the element with options.
     *
     * @param options Action options (timeout, etc.)
     */
    public void click(ActionOptions options) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("context", context);
        params.put("selector", selector);
        if (options != null && options.getTimeout() != null) {
            params.put("timeout", options.getTimeout());
        }
        client.sendRaw("vibium:click", params);
    }

    /**
     * Type text into the element.
     * Waits for element to be visible, stable, receive events, enabled, and editable.
     *
     * @param text The text to type
     */
    public void type(String text) {
        type(text, null);
    }

    /**
     * Type text into the element with options.
     *
     * @param text The text to type
     * @param options Action options (timeout, etc.)
     */
    public void type(String text, ActionOptions options) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("context", context);
        params.put("selector", selector);
        params.put("text", text);
        if (options != null && options.getTimeout() != null) {
            params.put("timeout", options.getTimeout());
        }
        client.sendRaw("vibium:type", params);
    }

    /**
     * Get the text content of the element.
     *
     * @return The text content
     * @throws ElementNotFoundException If the element no longer exists
     */
    public String text() {
        String script = """
            (selector) => {
                const el = document.querySelector(selector);
                return el ? (el.textContent || '').trim() : null;
            }
            """;

        JsonObject result = executeScript(script);
        JsonObject resultObj = result.getAsJsonObject("result");

        if ("null".equals(resultObj.get("type").getAsString())) {
            throw new ElementNotFoundException(selector);
        }

        return resultObj.get("value").getAsString();
    }

    /**
     * Get an attribute value from the element.
     *
     * @param name The attribute name
     * @return The attribute value, or null if not present
     */
    public String getAttribute(String name) {
        String script = """
            (selector, attrName) => {
                const el = document.querySelector(selector);
                return el ? el.getAttribute(attrName) : null;
            }
            """;

        JsonObject result = client.sendRaw("script.callFunction", Map.of(
                "functionDeclaration", script,
                "target", Map.of("context", context),
                "arguments", java.util.List.of(
                        Map.of("type", "string", "value", selector),
                        Map.of("type", "string", "value", name)
                ),
                "awaitPromise", false,
                "resultOwnership", "root"
        ));

        JsonObject resultObj = result.getAsJsonObject("result");
        if ("null".equals(resultObj.get("type").getAsString())) {
            return null;
        }

        return resultObj.get("value").getAsString();
    }

    /**
     * Get the current bounding box of the element.
     *
     * @return The bounding box
     * @throws ElementNotFoundException If the element no longer exists
     */
    public BoundingBox boundingBox() {
        String script = """
            (selector) => {
                const el = document.querySelector(selector);
                if (!el) return null;
                const rect = el.getBoundingClientRect();
                return JSON.stringify({
                    x: rect.x,
                    y: rect.y,
                    width: rect.width,
                    height: rect.height
                });
            }
            """;

        JsonObject result = executeScript(script);
        JsonObject resultObj = result.getAsJsonObject("result");

        if ("null".equals(resultObj.get("type").getAsString())) {
            throw new ElementNotFoundException(selector);
        }

        String json = resultObj.get("value").getAsString();
        return gson.fromJson(json, BoundingBox.class);
    }

    /**
     * Execute a script with the selector as an argument.
     */
    private JsonObject executeScript(String script) {
        return client.sendRaw("script.callFunction", Map.of(
                "functionDeclaration", script,
                "target", Map.of("context", context),
                "arguments", java.util.List.of(
                        Map.of("type", "string", "value", selector)
                ),
                "awaitPromise", false,
                "resultOwnership", "root"
        ));
    }
}
