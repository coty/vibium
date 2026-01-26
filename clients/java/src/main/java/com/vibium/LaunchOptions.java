package com.vibium;

/**
 * Options for launching a browser.
 *
 * <pre>{@code
 * Vibe vibe = Browser.launch(new LaunchOptions()
 *     .headless(true)
 *     .port(9515));
 * }</pre>
 */
public class LaunchOptions {

    private boolean headless = false;
    private Integer port = null;
    private String executablePath = null;

    /**
     * Run browser in headless mode (no visible window).
     * Default: false (browser is visible).
     *
     * @param headless True for headless mode
     * @return This instance for chaining
     */
    public LaunchOptions headless(boolean headless) {
        this.headless = headless;
        return this;
    }

    /**
     * Port for the WebSocket server.
     * Default: auto-select available port.
     *
     * @param port The port number
     * @return This instance for chaining
     */
    public LaunchOptions port(int port) {
        this.port = port;
        return this;
    }

    /**
     * Path to the clicker binary.
     * Default: auto-detect from PATH or environment.
     *
     * @param path Path to the clicker binary
     * @return This instance for chaining
     */
    public LaunchOptions executablePath(String path) {
        this.executablePath = path;
        return this;
    }

    /**
     * Check if headless mode is enabled.
     *
     * @return True if headless
     */
    public boolean isHeadless() {
        return headless;
    }

    /**
     * Get the configured port.
     *
     * @return Port number, or null for auto-select
     */
    public Integer getPort() {
        return port;
    }

    /**
     * Get the configured executable path.
     *
     * @return Path to clicker binary, or null for auto-detect
     */
    public String getExecutablePath() {
        return executablePath;
    }
}
