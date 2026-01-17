# Parity Rules

Use these rules when mapping JS/TS (and Python) APIs to Java.

## API Completeness

**Preserve full API parity, even for unused methods.** If the JavaScript client exposes a public method or property, the Java client must include it—even if it's not currently called anywhere in the codebase.

Rationale:
- Users may depend on these APIs
- Future features may require them
- API surface should match across all client libraries

Examples of APIs to include even if unused:
- Event handlers (`onEvent()`, `onMessage()`)
- Optional callback setters
- Utility methods on public classes

## Naming

- Keep public class/method names aligned with JS/TS.
- Java exceptions should use idiomatic Java names but preserve the JS error concept (e.g., `TimeoutError` -> `TimeoutException`).
- Constants: Use `UPPER_SNAKE_CASE` with `long` type for timeouts (e.g., `private static final long START_TIMEOUT_MS = 10000;`).
- Logger variable: Always name it `log` (not `logger`).

## Type Safety

**Preserve type safety in translation.** If TypeScript uses specific types, Java should use equivalent typed classes - don't downgrade to raw types (`JsonObject`, `Object`, `Map<String, Object>`) when TypeScript has concrete types.

This applies to:
- **API responses** → use typed records, not `JsonObject`
- **Method return types** → use specific types, not `Object`
- **Generic methods** → use `Class<T>` parameter (idiomatic Java pattern)
- **Collections** → use typed collections, not raw

```typescript
// TypeScript - typed response
const tree = await client.send<BrowsingContextTree>('browsingContext.getTree');
const result = await client.send<NavigationResult>('browsingContext.navigate', params);
```

```java
// Java - WRONG (loses type safety)
JsonObject result = client.send("browsingContext.getTree");
String context = result.getAsJsonArray("contexts").get(0)...  // manual extraction

// Java - CORRECT (preserves type safety, idiomatic pattern)
public <T> T send(String method, Map<String, Object> params, Class<T> responseType) {
    JsonObject raw = sendRaw(method, params);
    return gson.fromJson(raw, responseType);
}

BrowsingContextTree tree = client.send("browsingContext.getTree", params, BrowsingContextTree.class);
String context = tree.contexts().get(0).context();  // type-safe access
```

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

## TypeScript String Literal Types → Java Enums

When TypeScript uses string literal union types, translate to Java enums with `getIdentifier()`:

```typescript
// TypeScript
export type Platform = 'linux' | 'darwin' | 'win32';
export type Arch = 'x64' | 'arm64';
```

```java
// Java - Enum with identifier for string representation
public enum OS {
    LINUX("linux"),
    DARWIN("darwin"),
    WINDOWS("win32");

    private final String identifier;

    OS(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}

public enum Arch {
    X64("x64"),
    ARM64("arm64");

    private final String identifier;

    Arch(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}
```

This pattern:
- Preserves type safety (enum vs raw string)
- Provides the exact string value via `getIdentifier()` for serialization/comparison
- Matches the TypeScript string values exactly

**Important:** When detecting platform/architecture, throw exceptions for unsupported values (matching JS behavior):

```typescript
// JavaScript - throws for unsupported
export function getPlatform(): Platform {
  const platform = os.platform();
  if (platform === 'linux' || platform === 'darwin' || platform === 'win32') {
    return platform;
  }
  throw new Error(`Unsupported platform: ${platform}`);
}
```

```java
// Java - also throws for unsupported (DO NOT default to a fallback)
public static OS getOS() {
    String os = System.getProperty("os.name").toLowerCase();
    if (os.contains("linux")) {
        return OS.LINUX;
    } else if (os.contains("mac") || os.contains("darwin")) {
        return OS.DARWIN;
    } else if (os.contains("win")) {
        return OS.WINDOWS;
    }
    throw new UnsupportedOperationException("Unsupported operating system: " + os);
}

public static Arch getArch() {
    String arch = System.getProperty("os.arch").toLowerCase();
    if (arch.contains("amd64") || arch.contains("x86_64")) {
        return Arch.X64;
    } else if (arch.contains("aarch64") || arch.contains("arm64")) {
        return Arch.ARM64;
    }
    throw new UnsupportedOperationException("Unsupported architecture: " + arch);
}
```

## BiDi Protocol Types

Always create ALL of these types, even if not all are directly used:

| Type | Purpose |
|------|---------|
| `BiDiCommand` | Command sent to server: `record BiDiCommand(int id, String method, Map<String, Object> params)` |
| `BiDiResponse` | Response from server: `record BiDiResponse(int id, String type, Object result, BiDiError error)` |
| `BiDiEvent` | Unsolicited event: `record BiDiEvent(String method, Map<String, Object> params)` |
| `BiDiError` | Error details: `record BiDiError(String error, String message, String stacktrace)` |

## Error Handling

See `ts-java-translation.md` for general TS→Java exception mapping rules. Key points:

- **`throw new Error(msg)`** → **`throw new RuntimeException(msg)`**
- **Typed TS errors** (e.g., `class ValidationError extends Error`) → **Typed Java exceptions** (e.g., `ValidationException extends RuntimeException`)
- Prefer unchecked exceptions (`extends RuntimeException`) unless at a clear API boundary

**Vibium-specific rules:**

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

## JavaDoc Style

- Use **Title Case** for @param and @return descriptions:
  ```java
  /**
   * @param options Launch configuration       // CORRECT: "Launch" capitalized
   * @return Vibe instance for automation      // CORRECT: "Vibe" capitalized
   */

  // NOT:
  /**
   * @param options launch configuration       // WRONG: lowercase
   * @return vibe instance for automation      // WRONG: lowercase
   */
  ```

## Record Formatting

- Use multi-line format for records with closing brace on its own line:
  ```java
  public record BiDiCommand(int id, String method, Map<String, Object> params) {
  }

  // NOT:
  public record BiDiCommand(int id, String method, Map<String, Object> params) {}
  ```

## Production-Ready Process Management (ClickerProcess)

Use `CompletableFuture` for async port detection with a dedicated exit watcher:

```java
public class ClickerProcess {
    private static final long START_TIMEOUT_MS = 10000;
    private static final long STOP_TIMEOUT_MS = 3000;

    // Use CompletableFuture for cleaner async handling
    private static int waitForPort(Process proc) {
        CompletableFuture<Integer> portFuture = new CompletableFuture<>();
        StringBuilder output = new StringBuilder();

        // Output reader thread
        Thread reader = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    output.append(line).append("\n");
                    log.debug("clicker: {}", line);

                    Matcher matcher = PORT_PATTERN.matcher(line);
                    if (matcher.find() && !portFuture.isDone()) {
                        portFuture.complete(Integer.parseInt(matcher.group(1)));
                    }
                }
            } catch (IOException e) {
                if (!portFuture.isDone()) {
                    portFuture.completeExceptionally(e);
                }
            }
        });
        reader.setDaemon(true);
        reader.start();

        // Exit watcher thread - detect crashes
        Thread exitWatcher = new Thread(() -> {
            try {
                int exitCode = proc.waitFor();
                if (!portFuture.isDone()) {
                    portFuture.completeExceptionally(
                        new BrowserCrashedException(exitCode, output.toString())
                    );
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        exitWatcher.setDaemon(true);
        exitWatcher.start();

        try {
            return portFuture.get(START_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            proc.destroyForcibly();
            throw new TimeoutException("clicker", (int) START_TIMEOUT_MS,
                "waiting for clicker to start");
        } catch (Exception e) {
            proc.destroyForcibly();
            if (e.getCause() instanceof BrowserCrashedException) {
                throw (BrowserCrashedException) e.getCause();
            }
            throw new RuntimeException("Failed to start clicker", e);
        }
    }

    /**
     * Check if the process is still running.
     */
    public boolean isRunning() {
        return process.isAlive() && !stopped;
    }
}
```

## Production-Ready Binary Resolution (BinaryResolver)

Implement a robust binary search with 6 locations:

```java
public class BinaryResolver {
    private static final String VERSION = "0.1.2";  // Match JS client version

    /**
     * Resolve the clicker binary path.
     *
     * Search order:
     * 1. Explicit path (if provided)
     * 2. VIBIUM_CLICKER_PATH or CLICKER_PATH environment variable
     * 3. Bundled binary in JAR resources (extracted to cache)
     * 4. System PATH
     * 5. Cache directory
     * 6. Local development paths
     *
     * @param explicitPath Optional explicit path (highest priority)
     * @return Path to the clicker binary
     * @throws IllegalStateException if binary cannot be found
     */
    public static String resolve(String explicitPath) {
        String binaryName = Platform.getBinaryName();

        // 1. Explicit path
        if (explicitPath != null && !explicitPath.isEmpty()) {
            File file = new File(explicitPath);
            if (file.exists() && file.canExecute()) {
                return explicitPath;
            }
            throw new IllegalStateException(
                "Clicker binary not found at explicit path: " + explicitPath);
        }

        // 2. Environment variables
        String envPath = System.getenv("VIBIUM_CLICKER_PATH");
        if (envPath == null || envPath.isEmpty()) {
            envPath = System.getenv("CLICKER_PATH");
        }
        if (envPath != null && !envPath.isEmpty()) {
            File file = new File(envPath);
            if (file.exists() && file.canExecute()) {
                return envPath;
            }
        }

        // 3. Bundled binary (extract from JAR)
        String bundledPath = extractBundledBinary();
        if (bundledPath != null) {
            return bundledPath;
        }

        // 4. System PATH
        String pathBinary = findInPath(binaryName);
        if (pathBinary != null) {
            return pathBinary;
        }

        // 5. Cache directory
        Path cacheBinary = Platform.getCacheDir().resolve(binaryName);
        if (Files.isExecutable(cacheBinary)) {
            return cacheBinary.toString();
        }

        // 6. Local development paths
        // ... (same as before)

        throw new IllegalStateException(
            "Could not find clicker binary. Options:\n" +
            "  1. Set VIBIUM_CLICKER_PATH environment variable\n" +
            "  2. Add clicker to your PATH\n" +
            "  3. Build from source: make build-go");
    }

    public static String resolve() {
        return resolve(null);
    }

    /**
     * Extract bundled binary from JAR resources to cache directory.
     */
    private static String extractBundledBinary() {
        String platform = Platform.getPlatformIdentifier();
        String binaryName = Platform.getBinaryName();
        String resourcePath = "/" + platform + "/bin/" + binaryName;

        try (InputStream is = BinaryResolver.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                return null;
            }

            Path cacheDir = Platform.getCacheDir().resolve("clicker").resolve(VERSION);
            Files.createDirectories(cacheDir);
            Path targetPath = cacheDir.resolve(binaryName);

            if (Files.isExecutable(targetPath)) {
                return targetPath.toString();
            }

            // Extract to temp, then atomic move
            Path tempFile = cacheDir.resolve(binaryName + ".tmp." + System.currentTimeMillis());
            try {
                Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);

                // Make executable on Unix
                if (Platform.getOS() != Platform.OS.WINDOWS) {
                    Set<PosixFilePermission> perms = Files.getPosixFilePermissions(tempFile);
                    perms.add(PosixFilePermission.OWNER_EXECUTE);
                    perms.add(PosixFilePermission.GROUP_EXECUTE);
                    perms.add(PosixFilePermission.OTHERS_EXECUTE);
                    Files.setPosixFilePermissions(tempFile, perms);
                }

                Files.move(tempFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
                return targetPath.toString();
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } catch (IOException e) {
            log.warn("Failed to extract bundled binary: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Find binary in system PATH.
     */
    private static String findInPath(String binaryName) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) return null;

        for (String dir : pathEnv.split(File.pathSeparator)) {
            File file = new File(dir, binaryName);
            if (file.exists() && file.canExecute()) {
                return file.getAbsolutePath();
            }
        }
        return null;
    }
}
```

## Platform Utilities

Platform.java must include these utility methods. **Important:** Throw exceptions for unsupported platforms/architectures (see "TypeScript String Literal Types → Java Enums" section above).

```java
public class Platform {

    public enum OS {
        LINUX("linux"),
        DARWIN("darwin"),
        WINDOWS("win32");

        private final String identifier;

        OS(String identifier) {
            this.identifier = identifier;
        }

        public String getIdentifier() {
            return identifier;
        }
    }

    public enum Arch {
        X64("x64"),
        ARM64("arm64");

        private final String identifier;

        Arch(String identifier) {
            this.identifier = identifier;
        }

        public String getIdentifier() {
            return identifier;
        }
    }

    /**
     * Get the current operating system.
     */
    public static OS getOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("linux")) {
            return OS.LINUX;
        } else if (os.contains("mac") || os.contains("darwin")) {
            return OS.DARWIN;
        } else if (os.contains("win")) {
            return OS.WINDOWS;
        }
        throw new UnsupportedOperationException("Unsupported operating system: " + os);
    }

    /**
     * Get the current architecture.
     */
    public static Arch getArch() {
        String arch = System.getProperty("os.arch").toLowerCase();
        if (arch.contains("amd64") || arch.contains("x86_64")) {
            return Arch.X64;
        } else if (arch.contains("aarch64") || arch.contains("arm64")) {
            return Arch.ARM64;
        }
        throw new UnsupportedOperationException("Unsupported architecture: " + arch);
    }

    /**
     * Get platform identifier for binary paths (e.g., "darwin-arm64").
     */
    public static String getPlatformIdentifier() {
        return getOS().getIdentifier() + "-" + getArch().getIdentifier();
    }

    /**
     * Get the binary name for the current platform.
     */
    public static String getBinaryName() {
        return getOS() == OS.WINDOWS ? "clicker.exe" : "clicker";
    }

    /**
     * Get the cache directory for extracted binaries.
     *
     * - macOS: ~/Library/Caches/vibium
     * - Linux: ~/.cache/vibium (or XDG_CACHE_HOME/vibium)
     * - Windows: %LOCALAPPDATA%/vibium
     */
    public static Path getCacheDir() {
        String home = System.getProperty("user.home");
        switch (getOS()) {
            case DARWIN:
                return Paths.get(home, "Library", "Caches", "vibium");
            case WINDOWS:
                String localAppData = System.getenv("LOCALAPPDATA");
                if (localAppData != null && !localAppData.isEmpty()) {
                    return Paths.get(localAppData, "vibium");
                }
                return Paths.get(home, "AppData", "Local", "vibium");
            default: // LINUX
                String xdgCache = System.getenv("XDG_CACHE_HOME");
                if (xdgCache != null && !xdgCache.isEmpty()) {
                    return Paths.get(xdgCache, "vibium");
                }
                return Paths.get(home, ".cache", "vibium");
        }
    }
}
```
