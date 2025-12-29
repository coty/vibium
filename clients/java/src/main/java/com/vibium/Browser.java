package com.vibium;

import com.vibium.bidi.BiDiClient;
import com.vibium.clicker.ClickerProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for launching a browser.
 *
 * <pre>{@code
 * try (Vibe vibe = Browser.launch()) {
 *     vibe.go("https://example.com");
 *     Element el = vibe.find("h1");
 *     System.out.println(el.text());
 * }
 * }</pre>
 */
public class Browser {

    private static final Logger log = LoggerFactory.getLogger(Browser.class);

    private Browser() {
        // Static utility class
    }

    /**
     * Launch a browser with default options.
     * Browser will be visible (not headless).
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
        log.debug("Launching browser: headless={}, port={}", options.isHeadless(), options.getPort());

        // Start the clicker process
        ClickerProcess process = ClickerProcess.start(
            options.isHeadless(),
            options.getPort(),
            options.getExecutablePath()
        );

        try {
            // Connect to the BiDi server
            String url = "ws://localhost:" + process.getPort();
            BiDiClient client = BiDiClient.connect(url);

            log.info("Browser launched on port {}", process.getPort());
            return new Vibe(client, process);

        } catch (Exception e) {
            // Clean up process if connection fails
            process.stop();
            throw e;
        }
    }
}
