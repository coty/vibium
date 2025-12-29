package com.vibium;

import com.vibium.exceptions.VibiumException;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Auto-Wait Tests - mirrors tests/js/auto-wait.test.js
 * Tests that actions wait for elements to be actionable
 */
class AutoWaitTest {

    @Test
    void findWaitsForElementToAppear() {
        try (Vibe vibe = Browser.launch(new LaunchOptions().headless(true))) {
            vibe.go("https://the-internet.herokuapp.com/dynamic_loading/1");

            // Click the start button to trigger dynamic loading
            Element startBtn = vibe.find("#start button", new FindOptions().timeout(5000));
            startBtn.click();

            // find() should wait for the dynamically loaded element
            Element result = vibe.find("#finish h4", new FindOptions().timeout(10000));
            assertNotNull(result, "Should find the dynamically loaded element");
            assertEquals("Hello World!", result.getInfo().text(), "Should have correct text");
        }
    }

    @Test
    void clickWaitsForElementToBeActionable() {
        try (Vibe vibe = Browser.launch(new LaunchOptions().headless(true))) {
            vibe.go("https://the-internet.herokuapp.com/add_remove_elements/");

            // Click the "Add Element" button
            Element addBtn = vibe.find("button[onclick=\"addElement()\"]", new FindOptions().timeout(5000));
            addBtn.click(new ActionOptions().timeout(5000));

            // Verify the delete button appeared
            Element deleteBtn = vibe.find(".added-manually", new FindOptions().timeout(5000));
            assertNotNull(deleteBtn, "Delete button should have appeared after click");
        }
    }

    @Test
    void findTimesOutForNonExistentElement() {
        try (Vibe vibe = Browser.launch(new LaunchOptions().headless(true))) {
            vibe.go("https://the-internet.herokuapp.com/");

            Exception exception = assertThrows(VibiumException.class, () -> {
                vibe.find("#does-not-exist", new FindOptions().timeout(1000));
            });

            assertTrue(
                exception.getMessage().toLowerCase().contains("timeout") ||
                exception.getMessage().contains("does-not-exist"),
                "Should throw timeout-related error"
            );
        }
    }

    @Test
    void timeoutErrorMessageIsClear() {
        try (Vibe vibe = Browser.launch(new LaunchOptions().headless(true))) {
            vibe.go("https://the-internet.herokuapp.com/");

            try {
                vibe.find("#nonexistent-element-xyz", new FindOptions().timeout(1000));
                fail("Should have thrown");
            } catch (Exception err) {
                // Error should mention the selector or timeout
                assertTrue(
                    err.getMessage().toLowerCase().contains("timeout") ||
                    err.getMessage().contains("#nonexistent-element-xyz"),
                    "Error message should be clear: " + err.getMessage()
                );
            }
        }
    }
}
