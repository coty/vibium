# Test Translation Reference

## Example

```javascript
// tests/js/sync-api.test.js
test('vibe.go() navigates to URL (sync)', () => {
  const vibe = browserSync.launch({ headless: true });
  try {
    vibe.go('https://the-internet.herokuapp.com/');
    assert.ok(true);
  } finally {
    vibe.quit();
  }
});
```

```java
// clients/java/src/test/java/com/vibium/ApiTest.java
@Test
void goNavigatesToUrl() {
    try (Vibe vibe = Browser.launch(new LaunchOptions().headless(true))) {
        vibe.go("https://the-internet.herokuapp.com/");
        // If we get here without exception, navigation worked
    }
}
```

## Java test skeleton

```java
package com.vibium;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class ApiTest {

    @Test
    void launchAndQuit() {
        try (Vibe vibe = Browser.launch(new LaunchOptions().headless(true))) {
            assertNotNull(vibe);
        }
    }

    @Test
    void goNavigatesToUrl() {
        try (Vibe vibe = Browser.launch(new LaunchOptions().headless(true))) {
            vibe.go("https://the-internet.herokuapp.com/");
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
            assertEquals((byte) 0x50, screenshot[1]);
            assertEquals((byte) 0x4E, screenshot[2]);
            assertEquals((byte) 0x47, screenshot[3]);
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
}
```
