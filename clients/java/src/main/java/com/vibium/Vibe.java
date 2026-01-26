package com.vibium;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.vibium.bidi.BiDiClient;
import com.vibium.bidi.types.BrowsingContextTree;
import com.vibium.bidi.types.NavigationResult;
import com.vibium.bidi.types.ScreenshotResult;
import com.vibium.clicker.ClickerProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * Main interface for browser automation.
 *
 * <p>Provides methods for navigation, element interaction, and screenshots.
 * Implements {@link AutoCloseable} for use with try-with-resources.
 *
 * <pre>{@code
 * try (Vibe vibe = Browser.launch()) {
 *     vibe.go("https://example.com");
 *     Element heading = vibe.find("h1");
 *     System.out.println(heading.text());
 * }
 * }</pre>
 */
public class Vibe implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(Vibe.class);
    private final Gson gson = new Gson();

    private final BiDiClient client;
    private final ClickerProcess process;
    private String context = null;

    /**
     * Create a Vibe instance.
     *
     * @param client The BiDi client
     * @param process The clicker process (may be null for external connections)
     */
    Vibe(BiDiClient client, ClickerProcess process) {
        this.client = client;
        this.process = process;
    }

    /**
     * Check if the browser connection is active.
     *
     * @return True if connected
     */
    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    /**
     * Navigate to a URL.
     *
     * @param url The URL to navigate to
     */
    public void go(String url) {
        log.debug("Navigating to {}", url);
        String ctx = getContext();
        client.send("browsingContext.navigate", Map.of(
                "context", ctx,
                "url", url,
                "wait", "complete"
        ), NavigationResult.class);
        log.debug("Navigation complete: {}", url);
    }

    /**
     * Take a screenshot of the current page.
     *
     * @return PNG image data as a byte array
     */
    public byte[] screenshot() {
        String ctx = getContext();
        ScreenshotResult result = client.send("browsingContext.captureScreenshot", Map.of(
                "context", ctx
        ), ScreenshotResult.class);
        return Base64.getDecoder().decode(result.data());
    }

    /**
     * Execute JavaScript in the page context.
     *
     * @param script The JavaScript code to execute
     * @param <T> The expected return type
     * @return The result of the script execution
     */
    @SuppressWarnings("unchecked")
    public <T> T evaluate(String script) {
        String ctx = getContext();
        JsonObject result = client.sendRaw("script.callFunction", Map.of(
                "functionDeclaration", "() => { " + script + " }",
                "target", Map.of("context", ctx),
                "arguments", java.util.List.of(),
                "awaitPromise", true,
                "resultOwnership", "root"
        ));

        JsonObject resultObj = result.getAsJsonObject("result");
        String type = resultObj.get("type").getAsString();

        if ("undefined".equals(type) || "null".equals(type)) {
            return null;
        }

        if (resultObj.has("value")) {
            return (T) gson.fromJson(resultObj.get("value"), Object.class);
        }

        return null;
    }

    /**
     * Find an element by CSS selector.
     * Waits for element to exist before returning.
     *
     * @param selector The CSS selector
     * @return The found element
     */
    public Element find(String selector) {
        return find(selector, (FindOptions) null);
    }

    /**
     * Find an element by CSS selector with options.
     *
     * @param selector The CSS selector
     * @param options Find options (timeout, etc.)
     * @return The found element
     */
    public Element find(String selector, FindOptions options) {
        log.debug("Finding element: {}", selector);
        String ctx = getContext();

        Map<String, Object> params = new java.util.HashMap<>();
        params.put("context", ctx);
        params.put("selector", selector);
        if (options != null && options.getTimeout() != null) {
            params.put("timeout", options.getTimeout());
        }

        JsonObject result = client.sendRaw("vibium:find", params);

        ElementInfo info = new ElementInfo(
                result.get("tag").getAsString(),
                result.get("text").getAsString(),
                gson.fromJson(result.getAsJsonObject("box"), BoundingBox.class)
        );

        log.debug("Element found: {} <{}>", selector, info.tag());
        return new Element(client, ctx, selector, info);
    }

    /**
     * Find an element by CSS selector with a timeout.
     *
     * @param selector The CSS selector
     * @param timeout Timeout duration
     * @return The found element
     */
    public Element find(String selector, Duration timeout) {
        return find(selector, FindOptions.withTimeout(timeout));
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
     * Alias for {@link #quit()} to support try-with-resources.
     */
    @Override
    public void close() {
        quit();
    }

    /**
     * Get the current browsing context ID.
     */
    private String getContext() {
        if (context != null) {
            return context;
        }

        BrowsingContextTree tree = client.send("browsingContext.getTree", Map.of(),
                BrowsingContextTree.class);

        if (tree.contexts() == null || tree.contexts().isEmpty()) {
            throw new RuntimeException("No browsing context available");
        }

        context = tree.contexts().get(0).context();
        return context;
    }
}
