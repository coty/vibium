# Parity Rules

Use these rules when mapping JS/TS (and Python) APIs to Java.

## Naming

- Keep public class/method names aligned with JS/TS.
- Java exceptions should use idiomatic Java names but preserve the JS error concept (e.g., `TimeoutError` -> `TimeoutException`).
- Constants: Use `UPPER_SNAKE_CASE` with `long` type for timeouts (e.g., `private static final long START_TIMEOUT_MS = 10000;`).
- Logger variable: Always name it `log` (not `logger`).

## Nullability and Optionals

- For **boolean options**: Use primitive `boolean` with a default value, not `Boolean`:
  ```java
  private boolean headless = false;  // CORRECT
  private Boolean headless;          // WRONG
  ```
- For **numeric options**: Use boxed `Integer`/`Long` only when the value is truly optional (null means "use default"):
  ```java
  private Integer port = null;       // null = auto-select
  ```
- Getter naming for booleans: Use `isXxx()` not `getXxx()`:
  ```java
  public boolean isHeadless() { ... }  // CORRECT
  public Boolean getHeadless() { ... } // WRONG
  ```

## Records vs Builders

- Use **records** for immutable data payloads coming from the protocol:
  - `BoundingBox`, `ElementInfo`, `NavigationResult`, `ScreenshotResult`
  - ALL BiDi protocol types: `BiDiCommand`, `BiDiResponse`, `BiDiEvent`, `BiDiError`
- Use **builders** (fluent setters returning `this`) for options objects passed by callers:
  - `LaunchOptions`, `FindOptions`, `ActionOptions`

## BiDi Protocol Types

Always create ALL of these types, even if not all are directly used:

| Type | Purpose |
|------|---------|
| `BiDiCommand` | Command sent to server: `record BiDiCommand(int id, String method, Map<String, Object> params)` |
| `BiDiResponse` | Response from server: `record BiDiResponse(int id, String type, Object result, BiDiError error)` |
| `BiDiEvent` | Unsolicited event: `record BiDiEvent(String method, Map<String, Object> params)` |
| `BiDiError` | Error details: `record BiDiError(String error, String message, String stacktrace)` |

## Error Handling

- **Always clean up resources on failure** - especially in `Browser.launch()`:
  ```java
  ClickerProcess process = ClickerProcess.start(...);
  try {
      BiDiClient client = BiDiClient.connect(url);
      return new Vibe(client, process);
  } catch (Exception e) {
      process.stop();  // CRITICAL: don't leak processes
      throw e;
  }
  ```
- Preserve error message semantics and include key fields (selector, timeout, url, exit code).
- Store original cause when available via constructor chaining.

## Documentation

Every public class and method **must** have JavaDoc:

**Class-level:**
```java
/**
 * Entry point for launching a browser.
 *
 * <pre>{@code
 * try (Vibe vibe = Browser.launch()) {
 *     vibe.go("https://example.com");
 *     Element el = vibe.find("h1");
 *     System.out.println(el.text());
 * }
 * }</pre>
 */
public class Browser {
```

**Method-level:**
```java
/**
 * Launch a browser with custom options.
 *
 * @param options Launch configuration
 * @return Vibe instance for browser automation
 */
public static Vibe launch(LaunchOptions options) {
```

**Single-line getters:**
```java
/** @return The CSS selector that timed out */
public String getSelector() {
    return selector;
}
```

## Code Organization

1. Blank line after package declaration
2. Blank line after imports
3. **Blank line after class declaration before fields**
4. Group fields together, then constructors, then public methods, then private methods
5. Extract complex logic into private helper methods

Example structure:
```java
package com.vibium;

import ...;

/**
 * Class description.
 */
public class Example {

    private static final Logger log = LoggerFactory.getLogger(Example.class);
    private static final long TIMEOUT_MS = 10000;

    private final SomeField field;

    public Example(...) { ... }

    // Public methods

    // Private helper methods
}
```

## Sync vs Async

- Keep Java API **synchronous by default**; do not introduce CompletableFuture unless explicitly requested.
- If JS exposes both sync and async, map to sync in Java unless there is no equivalent.
- Use `CompletableFuture.get()` with timeout internally to implement blocking calls:
  ```java
  JsonObject response = future.get(timeoutMs, TimeUnit.MILLISECONDS);
  ```

## Tests

- Test names should reflect the JS test intent using camelCase (e.g., `screenshotReturnsPngBuffer`).
- Keep the same URLs/selectors unless the Java API differs.
- Use `try-with-resources` for automatic cleanup.
- Skip CI-incompatible tests with `@DisabledIfEnvironmentVariable`.

## Constructor Visibility

- **Package-private constructors** for classes instantiated by the library (Vibe, Element):
  ```java
  Vibe(BiDiClient client, ClickerProcess process) { ... }  // No public modifier
  ```
- **Public constructors** only for classes users instantiate directly (exceptions, options).
