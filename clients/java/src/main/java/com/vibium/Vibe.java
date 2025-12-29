package com.vibium;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.vibium.bidi.BiDiClient;
import com.vibium.clicker.ClickerProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Main interface for browser automation.
 * Implements AutoCloseable for use with try-with-resources.
 *
 * <pre>{@code
 * try (Vibe vibe = Browser.launch()) {
 *     vibe.go("https://example.com");
 *     byte[] screenshot = vibe.screenshot();
 * }
 * }</pre>
 */
public class Vibe implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(Vibe.class);
    private static final Gson gson = new Gson();

    private final BiDiClient client;
    private final ClickerProcess process;
    private String context = null;

    Vibe(BiDiClient client, ClickerProcess process) {
        this.client = client;
        this.process = process;
    }

    /**
     * Get the browsing context ID.
     */
    private String getContext() {
        if (context != null) {
            return context;
        }

        JsonObject result = client.send("browsingContext.getTree");
        var contexts = result.getAsJsonArray("contexts");
        if (contexts == null || contexts.isEmpty()) {
            throw new IllegalStateException("No browsing context available");
        }

        context = contexts.get(0).getAsJsonObject().get("context").getAsString();
        return context;
    }

    /**
     * Navigate to a URL.
     *
     * @param url The URL to navigate to
     */
    public void go(String url) {
        log.debug("Navigating to: {}", url);

        Map<String, Object> params = new HashMap<>();
        params.put("context", getContext());
        params.put("url", url);
        params.put("wait", "complete");

        client.send("browsingContext.navigate", params);
        log.debug("Navigation complete: {}", url);
    }

    /**
     * Take a screenshot of the current page.
     *
     * @return PNG image data
     */
    public byte[] screenshot() {
        Map<String, Object> params = new HashMap<>();
        params.put("context", getContext());

        JsonObject result = client.send("browsingContext.captureScreenshot", params);
        String base64Data = result.get("data").getAsString();
        return Base64.getDecoder().decode(base64Data);
    }

    /**
     * Execute JavaScript in the page context.
     *
     * @param script JavaScript code to execute (should include 'return' for a value)
     * @param <T> Expected return type
     * @return Result of the script execution
     */
    @SuppressWarnings("unchecked")
    public <T> T evaluate(String script) {
        Map<String, Object> params = new HashMap<>();
        params.put("functionDeclaration", "() => { " + script + " }");
        params.put("target", Map.of("context", getContext()));
        params.put("arguments", new Object[0]);
        params.put("awaitPromise", true);
        params.put("resultOwnership", "root");

        JsonObject result = client.send("script.callFunction", params);
        JsonObject resultValue = result.getAsJsonObject("result");

        if (resultValue.has("value")) {
            return (T) gson.fromJson(resultValue.get("value"), Object.class);
        }
        return null;
    }

    /**
     * Find an element by CSS selector.
     * Waits for the element to exist before returning.
     *
     * @param selector CSS selector
     * @return Element wrapper
     */
    public Element find(String selector) {
        return find(selector, null);
    }

    /**
     * Find an element by CSS selector with options.
     *
     * @param selector CSS selector
     * @param options Find options (timeout, etc.)
     * @return Element wrapper
     */
    public Element find(String selector, FindOptions options) {
        log.debug("Finding element: {}", selector);

        Map<String, Object> params = new HashMap<>();
        params.put("context", getContext());
        params.put("selector", selector);
        if (options != null && options.getTimeout() != null) {
            params.put("timeout", options.getTimeout());
        }

        JsonObject result = client.send("vibium:find", params);

        String tag = result.get("tag").getAsString();
        String text = result.get("text").getAsString();
        JsonObject boxJson = result.getAsJsonObject("box");
        BoundingBox box = new BoundingBox(
            boxJson.get("x").getAsDouble(),
            boxJson.get("y").getAsDouble(),
            boxJson.get("width").getAsDouble(),
            boxJson.get("height").getAsDouble()
        );

        ElementInfo info = new ElementInfo(tag, text, box);
        log.debug("Element found: {} ({})", selector, tag);

        return new Element(client, getContext(), selector, info);
    }

    /**
     * Close the browser and clean up resources.
     */
    public void quit() {
        log.debug("Quitting browser");
        client.close();
        if (process != null) {
            process.stop();
        }
    }

    /**
     * AutoCloseable implementation - calls quit().
     */
    @Override
    public void close() {
        quit();
    }
}
