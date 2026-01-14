---
name: java-sync
description: Synchronize the Vibium Java client with the JavaScript/TypeScript and Python clients for API parity. Use when syncing the Java API, aligning Java to JS/TS and Python, or when the user runs /java-sync.
---

# Java Client Sync Skill

Synchronize the Vibium Java client with the JavaScript/TypeScript client.

## Description

This skill analyzes the JavaScript client API and ensures the Java client has matching functionality. It detects gaps, generates Java code, and helps maintain API parity across all client libraries (JS, Python, Java).

## Reference Clients

When syncing, also check the Python client for implementation patterns:

| Python Source | Purpose |
|---------------|---------|
| `clients/python/src/vibium/browser.py` | Browser launch |
| `clients/python/src/vibium/vibe.py` | Vibe class |
| `clients/python/src/vibium/element.py` | Element class |
| `clients/python/src/vibium/client.py` | BiDi client |
| `clients/python/src/vibium/clicker.py` | Clicker process |

The Python client may have features not yet in JS, or vice versa. The goal is parity across all three.

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

Create a structured inventory like the template in `references/api-inventory.md`.

### Step 3: Decide Sync Direction

- If JS/TS changed most recently, use it as the baseline.
- If Python has features missing in JS/TS, use Python as the baseline and note any JS/TS gaps.
- When in doubt, prioritize the client with the newest public API change.

### Step 4: Compare and Identify Gaps

If the Java client exists, compare APIs and create a gap analysis:

| Feature | JS Client | Java Client | Action |
|---------|-----------|-------------|--------|
| Browser.launch() | ✅ | ? | Check/Create |
| Vibe.go() | ✅ | ? | Check/Create |
| Vibe.find() | ✅ | ? | Check/Create |
| Element.click() | ✅ | ? | Check/Create |

If the Java client doesn't exist, all features need to be created.

### Step 5: Generate Missing Code

For each gap, generate idiomatic Java code following these conventions:

**Naming:**
- Classes: PascalCase matching JS (Browser, Vibe, Element)
- Methods: camelCase matching JS (go, screenshot, find)
- Parameters: camelCase

**Patterns:**
- Use Java 17 records for immutable data types
- Use builder pattern for option classes with static factory methods (e.g., `FindOptions.withTimeout(5000)`)
- Implement AutoCloseable for Vibe
- Use CompletableFuture only if async API requested (default: sync)
- Use text blocks (triple quotes) for embedded JavaScript in Element.java
- BiDiClient should have type-safe `<T> T send(method, params, Class<T>)` method

**Example Translations:**

```typescript
// TypeScript
export interface FindOptions {
  timeout?: number;
}
```

```java
// Java - Builder pattern with static factory
public class FindOptions {
    private Integer timeout;

    public static FindOptions withTimeout(int timeout) {
        return new FindOptions().timeout(timeout);
    }

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

### Step 6: Generate Matching Tests

For each JS test in `tests/js/`, generate a corresponding Java JUnit 5 test.

See `references/test-translation.md` for example translations and a full Java test skeleton.

**Test conventions for Java:**
- Use JUnit 5 (`@Test`, `@BeforeAll`, `@AfterAll`)
- Use try-with-resources for automatic cleanup (Vibe implements AutoCloseable)
- Test method names: camelCase descriptive (e.g., `screenshotReturnsPngBuffer`)
- Assertions via `org.junit.jupiter.api.Assertions`
- Same test site: `https://the-internet.herokuapp.com/`

### Step 7: Apply Changes

Summarize proposed changes and confirm with the user before applying edits. Apply approved changes to:
- Source: `clients/java/src/main/java/com/vibium/`
- Tests: `tests/java/`

### Step 8: Verify

If the Java project has a `pom.xml`, run:

```bash
cd clients/java && mvn compile
```

Report any compilation errors for manual fixing.

### Step 9: Report

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
- Python also has both sync (`browser_sync`) and async (`browser`) APIs - Java mirrors the sync variant
- BiDi protocol layer changes require careful testing
- Check `CONTRIBUTING.md` for usage examples across all client libraries
- Common pitfalls: nullable JS fields that need Optional/nullable handling in Java, mismatch between JS error names and Java exception classes, and record vs builder usage for mutable options.
- See `references/parity-rules.md` for stricter mapping rules.
- See `references/packaging-checklist.md` for packaging steps once changes are complete.
