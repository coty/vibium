package com.vibium.clicker;

import com.vibium.exceptions.BrowserCrashedException;
import com.vibium.exceptions.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages the clicker subprocess.
 */
public class ClickerProcess {

    private static final Logger log = LoggerFactory.getLogger(ClickerProcess.class);
    private static final Pattern PORT_PATTERN = Pattern.compile("Server listening on ws://localhost:(\\d+)");
    private static final long START_TIMEOUT_MS = 10000;
    private static final long STOP_TIMEOUT_MS = 3000;

    private final Process process;
    private final int port;
    private volatile boolean stopped = false;

    private ClickerProcess(Process process, int port) {
        this.process = process;
        this.port = port;
    }

    /**
     * Start the clicker process.
     *
     * @param headless Run browser in headless mode
     * @param port Port to use (0 for auto-select)
     * @param executablePath Path to clicker binary (null for auto-detect)
     * @return Started ClickerProcess
     */
    public static ClickerProcess start(boolean headless, Integer port, String executablePath) {
        String binaryPath = BinaryResolver.resolve(executablePath);
        log.debug("Starting clicker: {}", binaryPath);

        List<String> args = new ArrayList<>();
        args.add(binaryPath);
        args.add("serve");

        if (port != null && port > 0) {
            args.add("--port");
            args.add(port.toString());
        }

        if (headless) {
            args.add("--headless");
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.redirectErrorStream(true);
            Process proc = pb.start();

            // Wait for the server to start and extract the port
            int actualPort = waitForPort(proc);
            log.info("Clicker started on port {}", actualPort);

            return new ClickerProcess(proc, actualPort);

        } catch (IOException e) {
            throw new RuntimeException("Failed to start clicker process", e);
        }
    }

    private static int waitForPort(Process proc) {
        CompletableFuture<Integer> portFuture = new CompletableFuture<>();
        StringBuilder output = new StringBuilder();

        Thread reader = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    output.append(line).append("\n");
                    log.debug("clicker: {}", line);

                    Matcher matcher = PORT_PATTERN.matcher(line);
                    if (matcher.find() && !portFuture.isDone()) {
                        int port = Integer.parseInt(matcher.group(1));
                        portFuture.complete(port);
                    }
                }
            } catch (IOException e) {
                if (!portFuture.isDone()) {
                    portFuture.completeExceptionally(e);
                }
            }
        });
        reader.setDaemon(true);
        reader.start();

        // Also check for process exit
        Thread exitWatcher = new Thread(() -> {
            try {
                int exitCode = proc.waitFor();
                if (!portFuture.isDone()) {
                    portFuture.completeExceptionally(
                        new BrowserCrashedException(exitCode, output.toString())
                    );
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        exitWatcher.setDaemon(true);
        exitWatcher.start();

        try {
            return portFuture.get(START_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            proc.destroyForcibly();
            throw new TimeoutException("clicker", (int) START_TIMEOUT_MS, "waiting for clicker to start");
        } catch (Exception e) {
            proc.destroyForcibly();
            if (e.getCause() instanceof BrowserCrashedException) {
                throw (BrowserCrashedException) e.getCause();
            }
            throw new RuntimeException("Failed to start clicker", e);
        }
    }

    /**
     * Get the port the server is listening on.
     */
    public int getPort() {
        return port;
    }

    /**
     * Check if the process is still running.
     */
    public boolean isRunning() {
        return process.isAlive() && !stopped;
    }

    /**
     * Stop the clicker process.
     */
    public void stop() {
        if (stopped) {
            return;
        }
        stopped = true;

        log.debug("Stopping clicker process");

        // Try graceful shutdown first
        process.destroy();

        try {
            boolean exited = process.waitFor(STOP_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!exited) {
                log.debug("Force killing clicker process");
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
        }

        log.debug("Clicker process stopped");
    }
}
