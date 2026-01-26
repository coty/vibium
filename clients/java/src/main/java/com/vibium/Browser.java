package com.vibium;

import com.vibium.bidi.BiDiClient;
import com.vibium.clicker.ClickerProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for launching a browser.
 *
 * <pre>{@code
 * // Launch with default options (visible browser)
 * try (Vibe vibe = Browser.launch()) {
 *     vibe.go("https://example.com");
 *     Element el = vibe.find("h1");
 *     System.out.println(el.text());
 * }
 *
 * // Launch in headless mode
 * try (Vibe vibe = Browser.launch(new LaunchOptions().headless(true))) {
 *     vibe.go("https://example.com");
 *     byte[] screenshot = vibe.screenshot();
 * }
 *
 * // Connect to existing browser
 * Vibe vibe = Browser.connect("ws://localhost:9515");
 * }</pre>
 */
public class Browser {

    private static final Logger log = LoggerFactory.getLogger(Browser.class);

    private Browser() {
        // Static factory methods only
    }

    /**
     * Launch a browser with default options.
     *
     * @return Vibe instance for browser automation
     */
    public static Vibe launch() {
        return launch(new LaunchOptions());
    }

    /**
     * Launch a browser with custom options.
     *
     * @param options Launch configuration
     * @return Vibe instance for browser automation
     */
    public static Vibe launch(LaunchOptions options) {
        log.debug("Launching browser, headless={}", options.isHeadless());

        // Start the clicker process
        ClickerProcess process = ClickerProcess.start(
                options.isHeadless(),
                options.getPort(),
                options.getExecutablePath()
        );

        // Connect to the proxy - cleanup process on failure
        String url = "ws://localhost:" + process.getPort();
        try {
            BiDiClient client = BiDiClient.connect(url);
            log.info("Browser launched on port {}", process.getPort());
            return new Vibe(client, process);
        } catch (Exception e) {
            // Critical: don't leak processes on connection failure
            log.debug("Connection failed, stopping process");
            process.stop();
            throw e;
        }
    }

    /**
     * Connect to an existing browser at the specified WebSocket URL.
     * Use this when you want to connect to a browser that was started externally.
     *
     * @param wsUrl The WebSocket URL (e.g., "ws://localhost:9222")
     * @return Vibe instance connected to the browser
     */
    public static Vibe connect(String wsUrl) {
        log.debug("Connecting to browser at {}", wsUrl);
        BiDiClient client = BiDiClient.connect(wsUrl);
        log.info("Connected to browser at {}", wsUrl);
        return new Vibe(client, null);  // null process - not managed by us
    }
}
