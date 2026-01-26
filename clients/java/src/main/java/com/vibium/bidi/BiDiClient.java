package com.vibium.bidi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.vibium.bidi.types.BiDiCommand;
import com.vibium.bidi.types.BiDiEvent;
import com.vibium.exceptions.BiDiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * High-level BiDi protocol client.
 *
 * <p>Provides type-safe command sending and response handling.
 *
 * <pre>{@code
 * BiDiClient client = BiDiClient.connect("ws://localhost:9515");
 * BrowsingContextTree tree = client.send("browsingContext.getTree",
 *     Map.of(), BrowsingContextTree.class);
 * client.close();
 * }</pre>
 */
public class BiDiClient {

    private static final Logger log = LoggerFactory.getLogger(BiDiClient.class);
    private static final long DEFAULT_TIMEOUT_MS = 30000;

    private final Gson gson = new GsonBuilder().create();
    private final AtomicInteger nextId = new AtomicInteger(1);
    private final ConcurrentHashMap<Integer, CompletableFuture<JsonObject>> pendingCommands = new ConcurrentHashMap<>();
    private BiDiConnection connection;
    private Consumer<BiDiEvent> eventHandler;

    private BiDiClient() {
        // Use connect() factory method
    }

    /**
     * Connect to a BiDi server.
     *
     * @param url The WebSocket URL (e.g., "ws://localhost:9515")
     * @return A connected BiDiClient
     */
    public static BiDiClient connect(String url) {
        BiDiClient client = new BiDiClient();
        // Pass handler to connect() to avoid race condition
        client.connection = BiDiConnection.connect(url, client::handleMessage);
        log.debug("BiDiClient connected to {}", url);
        return client;
    }

    /**
     * Check if the client is connected.
     *
     * @return True if connected
     */
    public boolean isConnected() {
        return connection != null && !connection.isClosed();
    }

    /**
     * Set the event handler for unsolicited events from the server.
     *
     * @param handler The event handler
     */
    public void onEvent(Consumer<BiDiEvent> handler) {
        this.eventHandler = handler;
    }

    /**
     * Send a command and return the result.
     *
     * @param method The BiDi method name
     * @param params Command parameters
     * @param responseType The expected response type class
     * @param <T> The response type
     * @return The typed response
     */
    public <T> T send(String method, Map<String, Object> params, Class<T> responseType) {
        return send(method, params, responseType, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Send a command with a custom timeout.
     *
     * @param method The BiDi method name
     * @param params Command parameters
     * @param responseType The expected response type class
     * @param timeoutMs Timeout in milliseconds
     * @param <T> The response type
     * @return The typed response
     */
    public <T> T send(String method, Map<String, Object> params, Class<T> responseType, long timeoutMs) {
        JsonObject result = sendRaw(method, params, timeoutMs);
        return gson.fromJson(result, responseType);
    }

    /**
     * Send a command and return raw JSON result.
     *
     * @param method The BiDi method name
     * @param params Command parameters
     * @return The raw JSON result
     */
    public JsonObject sendRaw(String method, Map<String, Object> params) {
        return sendRaw(method, params, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Send a command and return raw JSON result with custom timeout.
     *
     * @param method The BiDi method name
     * @param params Command parameters
     * @param timeoutMs Timeout in milliseconds
     * @return The raw JSON result
     */
    public JsonObject sendRaw(String method, Map<String, Object> params, long timeoutMs) {
        int id = nextId.getAndIncrement();
        BiDiCommand command = new BiDiCommand(id, method, params);

        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        pendingCommands.put(id, future);

        String json = gson.toJson(command);
        log.debug("Sending command: {} (id={})", method, id);

        try {
            connection.send(json);
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            pendingCommands.remove(id);
            throw new com.vibium.exceptions.TimeoutException(method, (int) timeoutMs,
                    "command timed out");
        } catch (Exception e) {
            pendingCommands.remove(id);
            if (e.getCause() instanceof BiDiException) {
                throw (BiDiException) e.getCause();
            }
            throw new RuntimeException("Command failed: " + method, e);
        }
    }

    /**
     * Close the connection.
     */
    public void close() {
        // Reject all pending commands
        for (Map.Entry<Integer, CompletableFuture<JsonObject>> entry : pendingCommands.entrySet()) {
            entry.getValue().completeExceptionally(new RuntimeException("Connection closed"));
        }
        pendingCommands.clear();

        if (connection != null) {
            connection.close();
        }
    }

    /**
     * Handle incoming WebSocket messages.
     */
    private void handleMessage(String message) {
        try {
            JsonObject json = gson.fromJson(message, JsonObject.class);

            // Check if this is a response (has id) or event (has method, no id)
            if (json.has("id")) {
                handleResponse(json);
            } else if (json.has("method")) {
                handleEvent(json);
            } else {
                log.warn("Unknown message format: {}", message);
            }
        } catch (Exception e) {
            log.error("Failed to handle message: {}", e.getMessage());
        }
    }

    /**
     * Handle a response message.
     */
    private void handleResponse(JsonObject json) {
        int id = json.get("id").getAsInt();
        CompletableFuture<JsonObject> future = pendingCommands.remove(id);

        if (future == null) {
            log.warn("Received response for unknown command: {}", id);
            return;
        }

        String type = json.has("type") ? json.get("type").getAsString() : null;

        if ("error".equals(type)) {
            // Handle error response
            String errorCode = "unknown error";
            String errorMsg = "Unknown error";
            String stacktrace = null;

            if (json.has("error")) {
                errorCode = json.get("error").getAsString();
            }
            if (json.has("message")) {
                errorMsg = json.get("message").getAsString();
            }
            if (json.has("stacktrace")) {
                stacktrace = json.get("stacktrace").getAsString();
            }

            future.completeExceptionally(new BiDiException(errorCode, errorMsg, stacktrace));
        } else {
            // Success response
            JsonObject result = json.has("result") ? json.getAsJsonObject("result") : new JsonObject();
            future.complete(result);
        }
    }

    /**
     * Handle an event message.
     */
    private void handleEvent(JsonObject json) {
        String method = json.get("method").getAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> params = json.has("params")
                ? gson.fromJson(json.get("params"), Map.class)
                : Map.of();

        BiDiEvent event = new BiDiEvent(method, params);
        log.debug("Received event: {}", method);

        if (eventHandler != null) {
            eventHandler.accept(event);
        }
    }
}
