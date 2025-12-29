package com.vibium;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

/**
 * Process Cleanup Tests - mirrors tests/js/process.test.js
 * Tests that browser processes are cleaned up properly
 */
class ProcessTest {

    /**
     * Get PIDs of Chrome for Testing processes spawned by clicker.
     */
    private static Set<Long> getClickerChromePids() {
        Set<Long> pids = new HashSet<>();
        String os = System.getProperty("os.name").toLowerCase();

        try {
            String cmd;
            if (os.contains("mac") || os.contains("darwin")) {
                cmd = "pgrep -f 'Chrome for Testing.*--remote-debugging-port'";
            } else if (os.contains("linux")) {
                cmd = "pgrep -f 'chrome.*--remote-debugging-port'";
            } else {
                return pids; // Unsupported OS for this test
            }

            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        pids.add(Long.parseLong(line.trim()));
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors - pgrep returns non-zero if no matches
        }
        return pids;
    }

    /**
     * Get new PIDs that appeared between two sets.
     */
    private static Set<Long> getNewPids(Set<Long> before, Set<Long> after) {
        Set<Long> newPids = new HashSet<>(after);
        newPids.removeAll(before);
        return newPids;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void cleansUpChromeOnQuit() {
        Set<Long> pidsBefore = getClickerChromePids();

        try (Vibe vibe = Browser.launch(new LaunchOptions().headless(true))) {
            vibe.go("https://the-internet.herokuapp.com/");
        }

        sleep(2000);

        Set<Long> pidsAfter = getClickerChromePids();
        Set<Long> newPids = getNewPids(pidsBefore, pidsAfter);

        assertEquals(
            0,
            newPids.size(),
            "Chrome processes should be cleaned up. New PIDs remaining: " + newPids
        );
    }

    @Test
    void cleansUpChromeOnClose() {
        Set<Long> pidsBefore = getClickerChromePids();

        Vibe vibe = Browser.launch(new LaunchOptions().headless(true));
        vibe.go("https://the-internet.herokuapp.com/");
        vibe.quit();

        sleep(2000);

        Set<Long> pidsAfter = getClickerChromePids();
        Set<Long> newPids = getNewPids(pidsBefore, pidsAfter);

        assertEquals(
            0,
            newPids.size(),
            "Chrome processes should be cleaned up. New PIDs remaining: " + newPids
        );
    }

    @Test
    void multipleSequentialSessionsCleanUpProperly() {
        Set<Long> pidsBefore = getClickerChromePids();

        // Run 3 sessions sequentially
        for (int i = 0; i < 3; i++) {
            try (Vibe vibe = Browser.launch(new LaunchOptions().headless(true))) {
                vibe.go("https://the-internet.herokuapp.com/");
            }
        }

        sleep(2000);

        Set<Long> pidsAfter = getClickerChromePids();
        Set<Long> newPids = getNewPids(pidsBefore, pidsAfter);

        assertEquals(
            0,
            newPids.size(),
            "All Chrome processes should be cleaned up after 3 sessions. New PIDs remaining: " + newPids
        );
    }
}
