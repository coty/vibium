# Vibium Java Client

Java client library for Vibium browser automation.

## Status

Implemented. Synced with JavaScript client.

## Versioning

- Keep `-SNAPSHOT` suffix during development
- On release, align version with JavaScript client (`clients/javascript/package.json`)
- Example: If JS is `0.1.2`, Java releases as `0.1.2`

## Sync with JavaScript Client

The Java client should maintain API parity with the JavaScript client at `clients/javascript/`.

**To sync:** Run `/java-sync` to analyze the JS client and generate/update Java code.

## Tech Stack

- Java 17+ (LTS)
- Maven
- Java-WebSocket (WebSocket client)
- Gson (JSON)
- SLF4J (Logging)

## API Design

Sync-first (Java's natural model). Example usage:

```java
import com.vibium.Browser;
import com.vibium.Vibe;
import com.vibium.Element;

try (Vibe vibe = Browser.launch()) {
    vibe.go("https://example.com");

    Element el = vibe.find("button.submit");
    el.click();

    byte[] screenshot = vibe.screenshot();
} // auto-closes via AutoCloseable
```

## File Mapping to JS Client

| JavaScript | Java |
|------------|------|
| `browser.ts` | `Browser.java` |
| `vibe.ts` | `Vibe.java` |
| `element.ts` | `Element.java` |
| `bidi/*.ts` | `bidi/*.java` |
| `clicker/*.ts` | `clicker/*.java` |
| `utils/errors.ts` | `exceptions/*.java` |

## Tests

Tests live in `tests/java/` (parallel to `tests/js/`).

| JS Test | Java Test |
|---------|-----------|
| `tests/js/sync-api.test.js` | `tests/java/ApiTest.java` |
| `tests/js/browser-modes.test.js` | `tests/java/BrowserModesTest.java` |
| `tests/js/auto-wait.test.js` | `tests/java/AutoWaitTest.java` |

Run tests: `mvn test -f clients/java/pom.xml`

## Implementation Order

1. Project setup (`pom.xml`, package structure)
2. BiDi protocol layer (`bidi/`)
3. Clicker process management (`clicker/`)
4. Public API (`Browser`, `Vibe`, `Element`)
5. Tests (matching `tests/js/` structure)
6. Documentation
