package com.vibium;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * API Tests - mirrors tests/js/sync-api.test.js
 */
class ApiTest {

    @Test
    void launchAndQuit() {
        try (Vibe vibe = Browser.launch(new LaunchOptions().headless(true))) {
            assertNotNull(vibe, "Should return a Vibe instance");
        }
    }

    @Test
    void goNavigatesToUrl() {
        try (Vibe vibe = Browser.launch(new LaunchOptions().headless(true))) {
            vibe.go("https://the-internet.herokuapp.com/");
            // If we get here without exception, navigation worked
        }
    }

    @Test
    void screenshotReturnsPngBuffer() {
        try (Vibe vibe = Browser.launch(new LaunchOptions().headless(true))) {
            vibe.go("https://the-internet.herokuapp.com/");
            byte[] screenshot = vibe.screenshot();

            assertNotNull(screenshot, "Should return screenshot data");
            assertTrue(screenshot.length > 1000, "Screenshot should have reasonable size");

            // Check PNG magic bytes
            assertEquals((byte) 0x89, screenshot[0], "Should be valid PNG");
            assertEquals((byte) 0x50, screenshot[1], "Should be valid PNG");
            assertEquals((byte) 0x4E, screenshot[2], "Should be valid PNG");
            assertEquals((byte) 0x47, screenshot[3], "Should be valid PNG");
        }
    }

    @Test
    void evaluateExecutesJavaScript() {
        try (Vibe vibe = Browser.launch(new LaunchOptions().headless(true))) {
            vibe.go("https://the-internet.herokuapp.com/");
            String title = vibe.evaluate("return document.title");
            assertTrue(title.toLowerCase().contains("internet"), "Should return page title");
        }
    }

    @Test
    void findLocatesElement() {
        try (Vibe vibe = Browser.launch(new LaunchOptions().headless(true))) {
            vibe.go("https://the-internet.herokuapp.com/");
            Element heading = vibe.find("h1.heading");

            assertNotNull(heading, "Should return an Element");
            assertNotNull(heading.getInfo(), "Element should have info");
            assertTrue(heading.getInfo().tag().equalsIgnoreCase("h1"), "Should be an h1 tag");
            assertTrue(heading.getInfo().text().toLowerCase().contains("welcome"), "Should have heading text");
        }
    }

    @Test
    void clickWorks() {
        try (Vibe vibe = Browser.launch(new LaunchOptions().headless(true))) {
            vibe.go("https://the-internet.herokuapp.com/");
            Element link = vibe.find("a[href=\"/add_remove_elements/\"]");
            link.click();

            // After click, should have navigated
            Element heading = vibe.find("h3");
            assertTrue(heading.getInfo().text().contains("Add/Remove Elements"), "Should have navigated");
        }
    }

    @Test
    void typeEntersText() {
        try (Vibe vibe = Browser.launch(new LaunchOptions().headless(true))) {
            vibe.go("https://the-internet.herokuapp.com/inputs");
            Element input = vibe.find("input");
            input.type("12345");

            // Verify the value was entered
            String value = vibe.evaluate("return document.querySelector('input').value;");
            assertEquals("12345", value, "Input should have typed value");
        }
    }

    @Test
    void textReturnsElementText() {
        try (Vibe vibe = Browser.launch(new LaunchOptions().headless(true))) {
            vibe.go("https://the-internet.herokuapp.com/");
            Element heading = vibe.find("h1.heading");
            String text = heading.text();
            assertTrue(text.toLowerCase().contains("welcome"), "Should return heading text");
        }
    }
}
