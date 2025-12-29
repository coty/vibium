package com.vibium.bidi;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.vibium.bidi.types.BiDiCommand;
import com.vibium.bidi.types.BiDiError;
import com.vibium.exceptions.ConnectionException;
import com.vibium.exceptions.VibiumException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * BiDi protocol client for sending commands and receiving responses.
 */
public class BiDiClient {

    private static final Logger log = LoggerFactory.getLogger(BiDiClient.class);
    private static final Gson gson = new Gson();
    private static final long DEFAULT_TIMEOUT_MS = 30000;

    private final BiDiConnection connection;
    private final AtomicInteger nextId = new AtomicInteger(1);
    private final Map<Integer, CompletableFuture<JsonObject>> pendingCommands = new ConcurrentHashMap<>();
    private Consumer<JsonObject> eventHandler;

    private BiDiClient(BiDiConnection connection) {
        this.connection = connection;
        connection.onMessage(this::handleMessage);
    }

    /**
     * Connect to a BiDi server.
     */
    public static BiDiClient connect(String url) throws ConnectionException {
        BiDiConnection connection = BiDiConnection.connect(url);
        return new BiDiClient(connection);
    }

    private void handleMessage(JsonObject message) {
        if (message.has("id")) {
            // This is a response
            int id = message.get("id").getAsInt();
            CompletableFuture<JsonObject> future = pendingCommands.remove(id);
            if (future != null) {
                future.complete(message);
            } else {
                log.warn("Received response for unknown command: {}", id);
            }
        } else if (message.has("method")) {
            // This is an event
            if (eventHandler != null) {
                eventHandler.accept(message);
            }
        }
    }

    /**
     * Set handler for BiDi events.
     */
    public void onEvent(Consumer<JsonObject> handler) {
        this.eventHandler = handler;
    }

    /**
     * Send a command and wait for response.
     *
     * @param method BiDi method name
     * @param params Command parameters
     * @return Result from the response
     */
    public JsonObject send(String method, Map<String, Object> params) {
        return send(method, params, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Send a command with custom timeout.
     */
    public JsonObject send(String method, Map<String, Object> params, long timeoutMs) {
        int id = nextId.getAndIncrement();
        BiDiCommand command = new BiDiCommand(id, method, params != null ? params : new HashMap<>());

        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        pendingCommands.put(id, future);

        try {
            String json = gson.toJson(command);
            log.debug("Sending command: {}", json);
            connection.send(json);

            JsonObject response = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            log.debug("Received response for command {}: {}", id, response);

            // Check for error
            String type = response.has("type") ? response.get("type").getAsString() : null;
            if ("error".equals(type) && response.has("error")) {
                JsonObject errorObj = response.getAsJsonObject("error");
                BiDiError error = gson.fromJson(errorObj, BiDiError.class);
                throw new VibiumException(error.error() + ": " + error.message());
            }

            // Return the result
            if (response.has("result")) {
                return response.getAsJsonObject("result");
            }
            return new JsonObject();

        } catch (VibiumException e) {
            throw e;
        } catch (Exception e) {
            pendingCommands.remove(id);
            throw new VibiumException("Command failed: " + method, e);
        }
    }

    /**
     * Send a command with no parameters.
     */
    public JsonObject send(String method) {
        return send(method, null);
    }

    /**
     * Send a command and convert the result to a specific type.
     *
     * @param method BiDi method name
     * @param params Command parameters
     * @param resultType The class to convert the result to
     * @return The result converted to the specified type
     */
    public <T> T send(String method, Map<String, Object> params, Class<T> resultType) {
        JsonObject result = send(method, params);
        return gson.fromJson(result, resultType);
    }

    /**
     * Close the connection.
     */
    public void close() {
        // Reject all pending commands
        for (Map.Entry<Integer, CompletableFuture<JsonObject>> entry : pendingCommands.entrySet()) {
            entry.getValue().completeExceptionally(new VibiumException("Connection closed"));
        }
        pendingCommands.clear();
        connection.close();
    }
}
