# Parity Rules

Use these rules when mapping JS/TS (and Python) APIs to Java.

## Naming

- Keep public class/method names aligned with JS/TS.
- Java exceptions should use idiomatic Java names but preserve the JS error concept (e.g., `TimeoutError` -> `TimeoutException`).

## Nullability and optionals

- Default to nullable references + `@Nullable` annotations for JS optional fields and `T | null` unions.
- Use boxed types (`Integer`, `Boolean`, `Double`) for optional primitives in options/builders.
- Reserve `Optional<T>` for public return types where it clearly improves usage ergonomics; avoid `Optional` fields in data classes unless a downstream API requires it.
- Prefer `org.jetbrains.annotations.Nullable` when a nullable annotation is needed, and only use it if the dependency is already present or added as `compileOnly`.

## Records vs builders

- Use records for immutable data payloads coming from the protocol (e.g., bounding boxes, info structs).
- Use builders (fluent setters) for options objects passed by callers.

## Errors

- Preserve error message semantics and include key fields (selector, timeout, url, exit code).
- Store original cause when available and surface it in the Java exception constructor.

## Sync vs async

- Keep Java API synchronous by default; do not introduce CompletableFuture unless explicitly requested.
- If JS exposes both sync and async, map to sync in Java unless there is no equivalent.

## Tests

- Test names should reflect the JS test intent.
- Keep the same URLs/selectors unless the Java API differs and requires a safe substitution.
