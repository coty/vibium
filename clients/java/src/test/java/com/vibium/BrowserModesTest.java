package com.vibium;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Browser Modes Tests - mirrors tests/js/browser-modes.test.js
 * Tests headless, visible, and default launch options
 */
class BrowserModesTest {

    @Test
    void headlessModeWorks() {
        try (Vibe vibe = Browser.launch(new LaunchOptions().headless(true))) {
            vibe.go("https://the-internet.herokuapp.com/");
            byte[] screenshot = vibe.screenshot();
            assertTrue(screenshot.length > 1000, "Should capture screenshot in headless mode");
        }
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "CI", matches = "true")
    @DisabledIfEnvironmentVariable(named = "GITHUB_ACTIONS", matches = "true")
    void headedModeWorks() {
        try (Vibe vibe = Browser.launch(new LaunchOptions().headless(false))) {
            vibe.go("https://the-internet.herokuapp.com/");
            byte[] screenshot = vibe.screenshot();
            assertTrue(screenshot.length > 1000, "Should capture screenshot in headed mode");
        }
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "CI", matches = "true")
    @DisabledIfEnvironmentVariable(named = "GITHUB_ACTIONS", matches = "true")
    void defaultIsVisible() {
        // Browser.launch() without headless option should default to visible
        try (Vibe vibe = Browser.launch()) {
            vibe.go("https://the-internet.herokuapp.com/");
            String title = vibe.evaluate("return document.title");
            assertTrue(title.toLowerCase().contains("internet"), "Should work with default options");
        }
    }
}
