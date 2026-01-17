# TypeScript to Java Translation Guide

A reference for translating idiomatic TypeScript to idiomatic Java (17+).

## Approach

No mature TS→Java transpiler exists. Recommended approach:

- **LLM-assisted translation**: Translate module-by-module with human review
- **Optional**: Use [jsii](https://github.com/aws/jsii) to generate type scaffolding, then implement with LLM assistance
- Feed full `.ts` source files (not `.d.ts`) when translating behavioral code

## Types & Nullability

| TypeScript | Java |
|------------|------|
| `string \| null` | `Optional<String>` or `@Nullable String` |
| `T \| undefined` | `Optional<T>` |
| Union types `A \| B` | Sealed interfaces with record implementations |
| `unknown` | `Object` |
| `any` | `Object` (lose type safety) |
| Type aliases | Interfaces or records |
| Literal types `"foo" \| "bar"` | Enums |

## Functions & Lambdas

| TypeScript | Java |
|------------|------|
| `(x: T) => R` | `Function<T, R>` |
| `(x: T) => void` | `Consumer<T>` |
| `() => T` | `Supplier<T>` |
| `(x: T) => boolean` | `Predicate<T>` |
| Arrow functions | Lambdas |
| Default parameters | Overloaded methods |
| Rest parameters `...args` | Varargs `T... args` |

## Collections & Iteration

| TypeScript | Java |
|------------|------|
| `array.map().filter().reduce()` | Streams: `list.stream().map().filter().reduce()` |
| `for (const x of items)` | `for (var x : items)` |
| `Object.entries(obj)` | `map.entrySet()` |
| `[...arr1, ...arr2]` | `Stream.concat()` or list constructor |
| Destructuring `const {a, b} = obj` | Explicit: `var a = obj.a(); var b = obj.b();` |

## Async

| TypeScript | Java |
|------------|------|
| `async/await` | `CompletableFuture` with `.thenApply()` chains |
| `Promise<T>` | `CompletableFuture<T>` |
| `Promise.all()` | `CompletableFuture.allOf()` |

### Async close() / cleanup methods

JavaScript's single-threaded model requires `async/await` to sequence operations after I/O:

```typescript
// JS - must await because it can't block
async close(): Promise<void> {
    this.ws.close();
    await this.closePromise;  // Wait for close event
}
```

Java can block, so **use simple non-blocking close** unless ordering matters:

```java
// Java - idiomatic, fire-and-forget
public void close() {
    connection.close();  // Initiate close, return immediately
}

// NOT this (over-engineered for most cases):
public void close() {
    try {
        connection.closeBlocking();
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
```

**Rationale:**
- JS awaits because it *can't* block (single-threaded)
- Java *can* block but usually doesn't need to for cleanup
- Standard Java `close()` methods (Socket, Stream) are non-blocking
- Blocking is available (`closeBlocking()`) if ordering truly matters

## Objects & Classes

| TypeScript | Java |
|------------|------|
| Interface (structural) | Interface (nominal) |
| `class` | `class` |
| Object literals `{a: 1, b: 2}` | Records, builders, or POJOs |
| `readonly` properties | `final` fields or records |
| Optional properties `a?: string` | `Optional<String>` or `@Nullable` |
| Getters/setters | Explicit methods or Lombok |

## Error Handling

| TypeScript | Java |
|------------|------|
| `throw new Error()` | `throw new RuntimeException()` |
| Try/catch | Try/catch |
| `Result<T, E>` pattern | `Either<L, R>` (Vavr) or sealed `Result` type |

### Typed Exceptions

**Generate typed exceptions when:**

- TS code has explicit error classes (`class ValidationError extends Error`)
- Discriminated error patterns exist (`type Result = {ok: true, value: T} | {ok: false, error: E}`)
- Error represents a recoverable, domain-specific condition
- API boundary is public/external

**Use RuntimeException when:**

- TS throws generic `Error` with a message
- Programming error (invalid state, assertion failure)
- Unrecoverable condition (config missing, service unavailable)

**Checked vs unchecked:**

Prefer unchecked (`extends RuntimeException`) unless at a clear API boundary where callers must handle it. Checked exceptions are viral.

**Example mapping:**

```typescript
// TypeScript
class OrderNotFoundError extends Error {
  constructor(public orderId: string) {
    super(`Order not found: ${orderId}`);
  }
}

class InsufficientStockError extends Error {
  constructor(public available: number, public requested: number) {
    super(`Insufficient stock: ${available} available, ${requested} requested`);
  }
}
```

```java
// Java (unchecked)
public class OrderNotFoundException extends RuntimeException {
    private final String orderId;

    public OrderNotFoundException(String orderId) {
        super("Order not found: " + orderId);
        this.orderId = orderId;
    }

    public String getOrderId() { return orderId; }
}

public class InsufficientStockException extends RuntimeException {
    private final int available;
    private final int requested;

    public InsufficientStockException(int available, int requested) {
        super("Insufficient stock: " + available + " available, " + requested + " requested");
        this.available = available;
        this.requested = requested;
    }

    public int getAvailable() { return available; }
    public int getRequested() { return requested; }
}
```

**Alternative: Preserve Result pattern:**

```java
sealed interface Result<T> permits Success, Failure {}
record Success<T>(T value) implements Result<T> {}
record Failure<T>(DomainError error) implements Result<T> {}
```

## Patterns Requiring Restructuring

| TypeScript Pattern | Java Approach |
|--------------------|---------------|
| Mixins | Composition or default interface methods |
| Duck typing | Extract explicit interfaces |
| Index signatures `[key: string]: T` | `Map<String, T>` |
| Discriminated unions | Sealed interfaces with pattern matching |
| Closures capturing mutable state | Restructure to explicit state objects |

## Discriminated Union Example

```typescript
// TypeScript
type Shape =
  | { kind: "circle"; radius: number }
  | { kind: "rectangle"; width: number; height: number };

function area(shape: Shape): number {
  switch (shape.kind) {
    case "circle":
      return Math.PI * shape.radius ** 2;
    case "rectangle":
      return shape.width * shape.height;
  }
}
```

```java
// Java 17+
sealed interface Shape permits Circle, Rectangle {}

record Circle(double radius) implements Shape {}
record Rectangle(double width, double height) implements Shape {}

double area(Shape shape) {
    return switch (shape) {
        case Circle c -> Math.PI * c.radius() * c.radius();
        case Rectangle r -> r.width() * r.height();
    };
}
```
