package com.vibium;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.vibium.bidi.BiDiClient;
import com.vibium.exceptions.ElementNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a DOM element.
 * Provides methods for interacting with the element.
 */
public class Element {

    private static final Logger log = LoggerFactory.getLogger(Element.class);
    private static final Gson gson = new Gson();

    private final BiDiClient client;
    private final String context;
    private final String selector;
    private final ElementInfo info;

    Element(BiDiClient client, String context, String selector, ElementInfo info) {
        this.client = client;
        this.context = context;
        this.selector = selector;
        this.info = info;
    }

    /**
     * Get element information (tag, text, bounding box).
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
     */
    public void click(ActionOptions options) {
        log.debug("Clicking element: {}", selector);

        Map<String, Object> params = new HashMap<>();
        params.put("context", context);
        params.put("selector", selector);
        if (options != null && options.getTimeout() != null) {
            params.put("timeout", options.getTimeout());
        }

        client.send("vibium:click", params);
    }

    /**
     * Type text into the element.
     * Waits for element to be visible, stable, receive events, enabled, and editable.
     *
     * @param text Text to type
     */
    public void type(String text) {
        type(text, null);
    }

    /**
     * Type text into the element with options.
     */
    public void type(String text, ActionOptions options) {
        log.debug("Typing into element: {}", selector);

        Map<String, Object> params = new HashMap<>();
        params.put("context", context);
        params.put("selector", selector);
        params.put("text", text);
        if (options != null && options.getTimeout() != null) {
            params.put("timeout", options.getTimeout());
        }

        client.send("vibium:type", params);
    }

    /**
     * Get the text content of the element.
     *
     * @return Text content (trimmed)
     */
    public String text() {
        String script = String.format(
            "(selector) => { " +
            "  const el = document.querySelector(selector); " +
            "  return el ? (el.textContent || '').trim() : null; " +
            "}",
            selector
        );

        Map<String, Object> params = new HashMap<>();
        params.put("functionDeclaration", script);
        params.put("target", Map.of("context", context));
        params.put("arguments", List.of(Map.of("type", "string", "value", selector)));
        params.put("awaitPromise", false);
        params.put("resultOwnership", "root");

        JsonObject result = client.send("script.callFunction", params);
        JsonObject resultValue = result.getAsJsonObject("result");

        if ("null".equals(resultValue.get("type").getAsString())) {
            throw new ElementNotFoundException(selector);
        }

        return resultValue.get("value").getAsString();
    }

    /**
     * Get an attribute value from the element.
     *
     * @param name Attribute name
     * @return Attribute value, or null if not present
     */
    public String getAttribute(String name) {
        String script =
            "(selector, attrName) => { " +
            "  const el = document.querySelector(selector); " +
            "  return el ? el.getAttribute(attrName) : null; " +
            "}";

        Map<String, Object> params = new HashMap<>();
        params.put("functionDeclaration", script);
        params.put("target", Map.of("context", context));
        params.put("arguments", List.of(
            Map.of("type", "string", "value", selector),
            Map.of("type", "string", "value", name)
        ));
        params.put("awaitPromise", false);
        params.put("resultOwnership", "root");

        JsonObject result = client.send("script.callFunction", params);
        JsonObject resultValue = result.getAsJsonObject("result");

        if ("null".equals(resultValue.get("type").getAsString())) {
            return null;
        }

        return resultValue.get("value").getAsString();
    }

    /**
     * Get the bounding box of the element.
     *
     * @return Bounding box with x, y, width, height
     */
    public BoundingBox boundingBox() {
        String script =
            "(selector) => { " +
            "  const el = document.querySelector(selector); " +
            "  if (!el) return null; " +
            "  const rect = el.getBoundingClientRect(); " +
            "  return JSON.stringify({ " +
            "    x: rect.x, " +
            "    y: rect.y, " +
            "    width: rect.width, " +
            "    height: rect.height " +
            "  }); " +
            "}";

        Map<String, Object> params = new HashMap<>();
        params.put("functionDeclaration", script);
        params.put("target", Map.of("context", context));
        params.put("arguments", List.of(Map.of("type", "string", "value", selector)));
        params.put("awaitPromise", false);
        params.put("resultOwnership", "root");

        JsonObject result = client.send("script.callFunction", params);
        JsonObject resultValue = result.getAsJsonObject("result");

        if ("null".equals(resultValue.get("type").getAsString())) {
            throw new ElementNotFoundException(selector);
        }

        String json = resultValue.get("value").getAsString();
        return gson.fromJson(json, BoundingBox.class);
    }
}
