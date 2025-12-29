# Java Client Sync Skill

Synchronize the Vibium Java client with the JavaScript/TypeScript client.

## Trigger

Invoke with: `/java-sync`

## Description

This skill analyzes the JavaScript client API and ensures the Java client has matching functionality. It detects gaps, generates Java code, and helps maintain API parity between the two client libraries.

## File Mapping

### Source Files

| JavaScript Source | Java Equivalent |
|-------------------|-----------------|
| `clients/javascript/src/index.ts` | Package exports |
| `clients/javascript/src/browser.ts` | `com/vibium/Browser.java` |
| `clients/javascript/src/vibe.ts` | `com/vibium/Vibe.java` |
| `clients/javascript/src/element.ts` | `com/vibium/Element.java` |
| `clients/javascript/src/bidi/types.ts` | `com/vibium/bidi/types/*.java` |
| `clients/javascript/src/bidi/client.ts` | `com/vibium/bidi/BiDiClient.java` |
| `clients/javascript/src/bidi/connection.ts` | `com/vibium/bidi/BiDiConnection.java` |
| `clients/javascript/src/clicker/process.ts` | `com/vibium/clicker/ClickerProcess.java` |
| `clients/javascript/src/clicker/binary.ts` | `com/vibium/clicker/BinaryResolver.java` |
| `clients/javascript/src/clicker/platform.ts` | `com/vibium/clicker/Platform.java` |
| `clients/javascript/src/utils/errors.ts` | `com/vibium/exceptions/*.java` |

### Test Files

| JavaScript Test | Java Test |
|-----------------|-----------|
| `tests/js/sync-api.test.js` | `tests/java/ApiTest.java` |
| `tests/js/async-api.test.js` | N/A (Java is sync by default) |
| `tests/js/browser-modes.test.js` | `tests/java/BrowserModesTest.java` |
| `tests/js/auto-wait.test.js` | `tests/java/AutoWaitTest.java` |
| `tests/js/process.test.js` | `tests/java/ProcessTest.java` |

## Instructions

### Step 1: Gather Current State

Read the JavaScript client to understand its public API:

```
clients/javascript/src/index.ts       # Exports
clients/javascript/src/browser.ts     # Browser.launch()
clients/javascript/src/vibe.ts        # Vibe class
clients/javascript/src/element.ts     # Element class
clients/javascript/src/utils/errors.ts # Error types
clients/javascript/src/bidi/types.ts  # BiDi protocol types
```

Read the Java client (if it exists):

```
clients/java/src/main/java/com/vibium/**/*.java
```

### Step 2: Build API Inventory

For the JavaScript client, extract:

1. **Exported classes/functions** from `index.ts`
2. **Public methods** for each class:
   - Method name
   - Parameters (name, type, optional?)
   - Return type
   - JSDoc description
3. **Type definitions** (interfaces, type aliases)
4. **Error classes** and their properties

Create a structured inventory like:

```
## JavaScript API

### browser
- launch(options?: LaunchOptions): Promise<Vibe>

### Vibe
- go(url: string): Promise<void>
- screenshot(): Promise<Buffer>
- evaluate<T>(script: string): Promise<T>
- find(selector: string, options?: FindOptions): Promise<Element>
- quit(): Promise<void>

### Element
- click(options?: ActionOptions): Promise<void>
- type(text: string, options?: ActionOptions): Promise<void>
- text(): Promise<string>
- getAttribute(name: string): Promise<string | null>
- boundingBox(): Promise<BoundingBox>
- info: ElementInfo (readonly)

### Types
- LaunchOptions: { headless?: boolean, port?: number, executablePath?: string }
- FindOptions: { timeout?: number }
- ActionOptions: { timeout?: number }
- BoundingBox: { x: number, y: number, width: number, height: number }
- ElementInfo: { tag: string, text: string, box: BoundingBox }

### Errors
- ConnectionError(url: string, cause?: Error)
- TimeoutError(selector: string, timeout: number, reason?: string)
- ElementNotFoundError(selector: string)
- BrowserCrashedError(exitCode: number, output?: string)
```

### Step 3: Compare and Identify Gaps

If the Java client exists, compare APIs and create a gap analysis:

| Feature | JS Client | Java Client | Action |
|---------|-----------|-------------|--------|
| Browser.launch() | ✅ | ? | Check/Create |
| Vibe.go() | ✅ | ? | Check/Create |
| Vibe.find() | ✅ | ? | Check/Create |
| Element.click() | ✅ | ? | Check/Create |
| ... | ... | ... | ... |

If the Java client doesn't exist, all features need to be created.

### Step 4: Generate Missing Code

For each gap, generate idiomatic Java code following these conventions:

**Naming:**
- Classes: PascalCase matching JS (Browser, Vibe, Element)
- Methods: camelCase matching JS (go, screenshot, find)
- Parameters: camelCase

**Patterns:**
- Use Java 17 records for immutable data types
- Use builder pattern for option classes
- Implement AutoCloseable for Vibe
- Use CompletableFuture only if async API requested (default: sync)

**Example Translations:**

```typescript
// TypeScript
export interface FindOptions {
  timeout?: number;
}
```

```java
// Java - Builder pattern
public class FindOptions {
    private Integer timeout;

    public FindOptions timeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public Integer getTimeout() {
        return timeout;
    }
}
```

```typescript
// TypeScript
export interface BoundingBox {
  x: number;
  y: number;
  width: number;
  height: number;
}
```

```java
// Java - Record (immutable)
public record BoundingBox(double x, double y, double width, double height) {}
```

```typescript
// TypeScript
export class TimeoutError extends Error {
  constructor(
    public selector: string,
    public timeout: number,
    public reason?: string
  ) {
    super(...);
  }
}
```

```java
// Java - Exception class
public class TimeoutException extends VibiumException {
    private final String selector;
    private final int timeout;
    private final String reason;

    public TimeoutException(String selector, int timeout, String reason) {
        super(formatMessage(selector, timeout, reason));
        this.selector = selector;
        this.timeout = timeout;
        this.reason = reason;
    }

    // Getters...
}
```

### Step 5: Generate Matching Tests

For each JS test in `tests/js/`, generate a corresponding Java JUnit 5 test.

**Test Translation Example:**

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
// tests/java/ApiTest.java
@Test
void goNavigatesToUrl() {
    try (Vibe vibe = Browser.launch(new LaunchOptions().headless(true))) {
        vibe.go("https://the-internet.herokuapp.com/");
        // If we get here without exception, navigation worked
    }
}
```

**Test conventions for Java:**
- Use JUnit 5 (`@Test`, `@BeforeAll`, `@AfterAll`)
- Use try-with-resources for automatic cleanup (Vibe implements AutoCloseable)
- Test method names: camelCase descriptive (e.g., `screenshotReturnsPngBuffer`)
- Assertions via `org.junit.jupiter.api.Assertions`
- Same test site: `https://the-internet.herokuapp.com/`

**Java test structure:**

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

### Step 6: Apply Changes

1. Show the user a summary of proposed changes
2. For each file to be created/modified, show the diff
3. Ask for approval before applying
4. Apply approved changes to:
   - Source: `clients/java/src/main/java/com/vibium/`
   - Tests: `tests/java/`

### Step 7: Verify

If the Java project has a `pom.xml`, run:

```bash
cd clients/java && mvn compile
```

Report any compilation errors for manual fixing.

### Step 7: Report

Provide a summary:

```
## Java Client Sync Summary

### Files Created
- com/vibium/Browser.java
- com/vibium/Vibe.java
- ...

### Files Modified
- (none)

### Verification
- Compilation: PASS/FAIL

### Manual Steps Needed
- (list any items requiring human attention)
```

## Versioning

- Java client version should align with JS client on release
- Check `clients/javascript/package.json` for current JS version
- Update `clients/java/pom.xml` version to match (without `-SNAPSHOT` suffix for releases)
- Keep `-SNAPSHOT` suffix during development

## Notes

- This skill does NOT handle async Java API (CompletableFuture) - that's a future enhancement
- Complex architectural changes may need manual intervention
- The sync API in JS (`browserSync`, `VibeSync`) maps to the default Java API (Java is sync by default)
- BiDi protocol layer changes require careful testing
