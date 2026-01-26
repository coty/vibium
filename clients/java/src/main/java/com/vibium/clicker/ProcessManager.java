package com.vibium.clicker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages clicker processes and ensures cleanup on JVM shutdown.
 *
 * <p>This class maintains a registry of active processes and registers a JVM
 * shutdown hook to ensure all browser/driver processes are terminated when
 * the JVM exits.
 */
public class ProcessManager {

    private static final Logger log = LoggerFactory.getLogger(ProcessManager.class);
    private static final Set<ClickerProcess> activeProcesses = ConcurrentHashMap.newKeySet();
    private static volatile boolean shutdownHookRegistered = false;

    private ProcessManager() {
        // Static class
    }

    /**
     * Register a process for automatic cleanup on JVM shutdown.
     *
     * @param process The process to register
     */
    public static void register(ClickerProcess process) {
        ensureShutdownHook();
        activeProcesses.add(process);
    }

    /**
     * Unregister a process (called when process is manually stopped).
     *
     * @param process The process to unregister
     */
    public static void unregister(ClickerProcess process) {
        activeProcesses.remove(process);
    }

    /**
     * Get the number of active processes.
     *
     * @return Count of registered active processes
     */
    public static int getActiveCount() {
        return activeProcesses.size();
    }

    /**
     * Stop all registered processes immediately.
     * Called during JVM shutdown or for manual cleanup.
     */
    public static void stopAll() {
        if (activeProcesses.isEmpty()) {
            return;
        }

        log.info("Stopping {} active clicker process(es)", activeProcesses.size());
        for (ClickerProcess process : activeProcesses) {
            try {
                process.stop();
            } catch (Exception e) {
                log.warn("Error stopping process during shutdown", e);
            }
        }
        activeProcesses.clear();
    }

    private static synchronized void ensureShutdownHook() {
        if (shutdownHookRegistered) {
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.debug("JVM shutdown detected, cleaning up processes");
            stopAll();
        }, "vibium-process-cleanup"));

        shutdownHookRegistered = true;
    }
}
