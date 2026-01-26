package com.vibium;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * API tests for the Vibium Java client.
 * Tests mirror the JavaScript sync-api tests.
 */
class ApiTest {

    @Test
    void launchAndQuit() {
        try (Vibe vibe = Browser.launch(new LaunchOptions().headless(true))) {
            assertNotNull(vibe);
            assertTrue(vibe.isConnected());
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

            assertNotNull(screenshot);
            assertTrue(screenshot.length > 1000, "Screenshot should have reasonable size");

            // Check PNG magic bytes
            assertEquals((byte) 0x89, screenshot[0]);
            assertEquals((byte) 0x50, screenshot[1]);  // P
            assertEquals((byte) 0x4E, screenshot[2]);  // N
            assertEquals((byte) 0x47, screenshot[3]);  // G
        }
    }

    @Test
    void findLocatesElement() {
        try (Vibe vibe = Browser.launch(new LaunchOptions().headless(true))) {
            vibe.go("https://the-internet.herokuapp.com/");
            Element heading = vibe.find("h1.heading");

            assertNotNull(heading);
            assertNotNull(heading.getInfo());
            assertTrue(heading.getInfo().tag().equalsIgnoreCase("h1"));
        }
    }

    @Test
    void clickWorks() {
        try (Vibe vibe = Browser.launch(new LaunchOptions().headless(true))) {
            vibe.go("https://the-internet.herokuapp.com/");
            Element link = vibe.find("a[href=\"/add_remove_elements/\"]");
            link.click();

            Element heading = vibe.find("h3");
            assertTrue(heading.getInfo().text().contains("Add/Remove Elements"));
        }
    }

    @Test
    void typeEntersText() {
        try (Vibe vibe = Browser.launch(new LaunchOptions().headless(true))) {
            vibe.go("https://the-internet.herokuapp.com/inputs");
            Element input = vibe.find("input");
            input.type("12345");

            String value = vibe.evaluate("return document.querySelector('input').value;");
            assertEquals("12345", value);
        }
    }

    @Test
    void evaluateReturnsValue() {
        try (Vibe vibe = Browser.launch(new LaunchOptions().headless(true))) {
            vibe.go("https://the-internet.herokuapp.com/");
            String title = vibe.evaluate("return document.title;");
            assertNotNull(title);
            assertTrue(title.contains("Internet"));
        }
    }

    @Test
    void elementTextReturnsContent() {
        try (Vibe vibe = Browser.launch(new LaunchOptions().headless(true))) {
            vibe.go("https://the-internet.herokuapp.com/");
            Element heading = vibe.find("h1.heading");
            String text = heading.text();

            assertNotNull(text);
            assertTrue(text.contains("Welcome"));
        }
    }

    @Test
    void elementBoundingBoxWorks() {
        try (Vibe vibe = Browser.launch(new LaunchOptions().headless(true))) {
            vibe.go("https://the-internet.herokuapp.com/");
            Element heading = vibe.find("h1.heading");
            BoundingBox box = heading.boundingBox();

            assertNotNull(box);
            assertTrue(box.width() > 0, "Width should be positive");
            assertTrue(box.height() > 0, "Height should be positive");
        }
    }

    @Test
    void getAttributeReturnsValue() {
        try (Vibe vibe = Browser.launch(new LaunchOptions().headless(true))) {
            vibe.go("https://the-internet.herokuapp.com/");
            Element link = vibe.find("a[href=\"/add_remove_elements/\"]");
            String href = link.getAttribute("href");

            assertNotNull(href);
            assertEquals("/add_remove_elements/", href);
        }
    }
}
