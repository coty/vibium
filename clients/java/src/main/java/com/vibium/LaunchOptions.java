package com.vibium;

/**
 * Options for launching a browser.
 */
public class LaunchOptions {

    private boolean headless = false;
    private Integer port = null;
    private String executablePath = null;

    /**
     * Run browser in headless mode (no visible window).
     * Default: false (browser is visible).
     */
    public LaunchOptions headless(boolean headless) {
        this.headless = headless;
        return this;
    }

    /**
     * Port for the WebSocket server.
     * Default: auto-select available port.
     */
    public LaunchOptions port(int port) {
        this.port = port;
        return this;
    }

    /**
     * Path to the clicker binary.
     * Default: auto-detect from PATH or environment.
     */
    public LaunchOptions executablePath(String path) {
        this.executablePath = path;
        return this;
    }

    public boolean isHeadless() {
        return headless;
    }

    public Integer getPort() {
        return port;
    }

    public String getExecutablePath() {
        return executablePath;
    }
}
