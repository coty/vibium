package com.vibium.bidi;

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
 * Low-level WebSocket connection to the BiDi server.
 *
 * <p>Handles WebSocket lifecycle and message passing. Message handlers
 * must be set BEFORE connecting to avoid race conditions where messages
 * are received before the handler is registered.
 */
public class BiDiConnection extends WebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(BiDiConnection.class);
    private static final long CONNECT_TIMEOUT_MS = 10000;

    private final CompletableFuture<Void> openFuture = new CompletableFuture<>();
    private final CompletableFuture<Void> closeFuture = new CompletableFuture<>();
    private Consumer<String> messageHandler;
    private volatile boolean closed = false;

    private BiDiConnection(URI serverUri) {
        super(serverUri);
    }

    /**
     * Connect to a BiDi server with a message handler.
     *
     * <p><strong>Important:</strong> The message handler is set BEFORE the
     * WebSocket connection opens to avoid a race condition where messages
     * are received before the handler is registered.
     *
     * @param url The WebSocket URL (e.g., "ws://localhost:9515")
     * @param messageHandler Handler for incoming messages
     * @return A connected BiDiConnection
     * @throws ConnectionException If connection fails
     */
    public static BiDiConnection connect(String url, Consumer<String> messageHandler) {
        try {
            URI uri = new URI(url);
            BiDiConnection connection = new BiDiConnection(uri);

            // Set handler BEFORE connecting to avoid race condition
            connection.messageHandler = messageHandler;
            connection.connect();

            // Wait for connection to open
            connection.openFuture.get(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            log.debug("WebSocket connected to {}", url);
            return connection;
        } catch (Exception e) {
            throw new ConnectionException(url, e);
        }
    }

    /**
     * Connect to a BiDi server without an initial message handler.
     *
     * @param url The WebSocket URL
     * @return A connected BiDiConnection
     * @throws ConnectionException If connection fails
     */
    public static BiDiConnection connect(String url) {
        return connect(url, null);
    }

    /**
     * Set the message handler for incoming messages.
     *
     * @param handler The message handler
     */
    public void setMessageHandler(Consumer<String> handler) {
        this.messageHandler = handler;
    }

    /**
     * Check if the connection is closed.
     *
     * @return True if closed
     */
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        log.debug("WebSocket opened, status: {}", handshake.getHttpStatus());
        openFuture.complete(null);
    }

    @Override
    public void onMessage(String message) {
        log.trace("Received: {}", message);
        if (messageHandler != null) {
            messageHandler.accept(message);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.debug("WebSocket closed, code: {}, reason: {}, remote: {}", code, reason, remote);
        closed = true;
        closeFuture.complete(null);
        if (!openFuture.isDone()) {
            openFuture.completeExceptionally(
                    new ConnectionException(getURI().toString(),
                            new RuntimeException("Connection closed: " + reason)));
        }
    }

    @Override
    public void onError(Exception ex) {
        log.debug("WebSocket error: {}", ex.getMessage());
        if (!openFuture.isDone()) {
            openFuture.completeExceptionally(ex);
        }
    }

    @Override
    public void send(String text) {
        log.trace("Sending: {}", text);
        super.send(text);
    }
}
