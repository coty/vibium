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
 * Manages a clicker subprocess.
 *
 * <p>Handles starting the clicker binary, waiting for it to be ready,
 * and stopping it cleanly with proper descendant process cleanup.
 */
public class ClickerProcess {

    private static final Logger log = LoggerFactory.getLogger(ClickerProcess.class);
    private static final long START_TIMEOUT_MS = 10000;
    private static final long STOP_TIMEOUT_MS = 3000;
    private static final Pattern PORT_PATTERN = Pattern.compile("Server listening on ws://localhost:(\\d+)");

    private final Process process;
    private final int port;
    private volatile boolean stopped = false;

    private ClickerProcess(Process process, int port) {
        this.process = process;
        this.port = port;
    }

    /**
     * Get the WebSocket port the clicker is listening on.
     *
     * @return The port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Check if the process is still running.
     *
     * @return True if running, false if stopped or crashed
     */
    public boolean isRunning() {
        return process.isAlive() && !stopped;
    }

    /**
     * Start a clicker process.
     *
     * @param headless Run browser in headless mode
     * @param port WebSocket port (null for auto-select)
     * @param executablePath Path to clicker binary (null for auto-detect)
     * @return A running ClickerProcess instance
     */
    public static ClickerProcess start(boolean headless, Integer port, String executablePath) {
        String binaryPath = BinaryResolver.resolve(executablePath);
        log.debug("Starting clicker from: {}", binaryPath);

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

        ProcessBuilder pb = new ProcessBuilder(args);
        pb.redirectErrorStream(true);

        Process proc;
        try {
            proc = pb.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start clicker process", e);
        }

        int actualPort = waitForPort(proc);

        ClickerProcess clickerProcess = new ClickerProcess(proc, actualPort);
        ProcessManager.register(clickerProcess);

        log.info("Clicker started on port {}", actualPort);
        return clickerProcess;
    }

    /**
     * Start a clicker process with default options.
     *
     * @return A running ClickerProcess instance
     */
    public static ClickerProcess start() {
        return start(false, null, null);
    }

    /**
     * Stop the clicker process with proper descendant cleanup.
     *
     * <p>This method kills descendant processes (ChromeDriver, Chrome) first,
     * which is necessary on Windows where child processes aren't automatically
     * killed when the parent exits.
     */
    public void stop() {
        if (stopped) {
            return;
        }
        stopped = true;
        ProcessManager.unregister(this);

        if (!process.isAlive()) {
            return;
        }

        log.debug("Stopping clicker process on port {}", port);

        // Kill descendant processes first (ChromeDriver, Chrome)
        try {
            ProcessHandle processHandle = process.toHandle();
            processHandle.descendants().forEach(ph -> {
                log.debug("Killing descendant process: {} ({})", ph.pid(),
                        ph.info().command().orElse("unknown"));
                ph.destroyForcibly();
            });
        } catch (Exception e) {
            log.debug("Error killing descendant processes: {}", e.getMessage());
        }

        // Now stop the clicker process itself
        process.destroy();

        try {
            if (!process.waitFor(STOP_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                log.debug("Force killing clicker process");
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Wait for the clicker process to output its listening port.
     */
    private static int waitForPort(Process proc) {
        CompletableFuture<Integer> portFuture = new CompletableFuture<>();
        StringBuilder output = new StringBuilder();

        // Output reader thread
        Thread reader = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    output.append(line).append("\n");
                    log.trace("clicker: {}", line);

                    Matcher matcher = PORT_PATTERN.matcher(line);
                    if (matcher.find() && !portFuture.isDone()) {
                        portFuture.complete(Integer.parseInt(matcher.group(1)));
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

        // Exit watcher thread - detect crashes
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
            throw new TimeoutException("clicker", (int) START_TIMEOUT_MS,
                    "waiting for clicker to start");
        } catch (Exception e) {
            proc.destroyForcibly();
            if (e.getCause() instanceof BrowserCrashedException) {
                throw (BrowserCrashedException) e.getCause();
            }
            throw new RuntimeException("Failed to start clicker", e);
        }
    }
}
