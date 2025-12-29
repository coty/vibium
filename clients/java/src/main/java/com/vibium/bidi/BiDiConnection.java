package com.vibium.bidi;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.vibium.exceptions.ConnectionException;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * WebSocket connection to the BiDi server.
 */
public class BiDiConnection {

    private static final Logger log = LoggerFactory.getLogger(BiDiConnection.class);
    private static final Gson gson = new Gson();

    private final WebSocketClient client;
    private volatile Consumer<JsonObject> messageHandler;
    private volatile boolean closed = false;

    private BiDiConnection(WebSocketClient client) {
        this.client = client;
    }

    /**
     * Connect to a BiDi server.
     *
     * @param url WebSocket URL (e.g., "ws://localhost:9222")
     * @return Connected BiDiConnection
     * @throws ConnectionException if connection fails
     */
    public static BiDiConnection connect(String url) throws ConnectionException {
        return connect(url, 10, TimeUnit.SECONDS);
    }

    /**
     * Connect to a BiDi server with custom timeout.
     */
    public static BiDiConnection connect(String url, long timeout, TimeUnit unit) throws ConnectionException {
        try {
            URI uri = new URI(url);
            CompletableFuture<Void> connectFuture = new CompletableFuture<>();
            CompletableFuture<Exception> errorFuture = new CompletableFuture<>();

            BiDiConnection[] connectionHolder = new BiDiConnection[1];

            WebSocketClient wsClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    log.debug("WebSocket connection opened to {}", url);
                    connectFuture.complete(null);
                }

                @Override
                public void onMessage(String message) {
                    if (connectionHolder[0] != null && connectionHolder[0].messageHandler != null) {
                        try {
                            JsonObject json = gson.fromJson(message, JsonObject.class);
                            connectionHolder[0].messageHandler.accept(json);
                        } catch (Exception e) {
                            log.error("Failed to parse BiDi message: {}", message, e);
                        }
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    log.debug("WebSocket connection closed: {} (code={})", reason, code);
                    if (connectionHolder[0] != null) {
                        connectionHolder[0].closed = true;
                    }
                }

                @Override
                public void onError(Exception ex) {
                    log.error("WebSocket error", ex);
                    if (!connectFuture.isDone()) {
                        errorFuture.complete(ex);
                    }
                }
            };

            BiDiConnection connection = new BiDiConnection(wsClient);
            connectionHolder[0] = connection;

            wsClient.connect();

            // Wait for connection or error
            CompletableFuture<Object> result = CompletableFuture.anyOf(connectFuture, errorFuture);
            result.get(timeout, unit);

            if (errorFuture.isDone()) {
                throw new ConnectionException(url, errorFuture.get());
            }

            return connection;

        } catch (ConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new ConnectionException(url, e);
        }
    }

    /**
     * Set the handler for incoming messages.
     */
    public void onMessage(Consumer<JsonObject> handler) {
        this.messageHandler = handler;
    }

    /**
     * Send a message to the server.
     */
    public void send(String message) {
        if (closed) {
            throw new IllegalStateException("Connection closed");
        }
        client.send(message);
    }

    /**
     * Check if the connection is closed.
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Close the connection.
     */
    public void close() {
        if (!closed) {
            closed = true;
            client.close();
        }
    }
}
