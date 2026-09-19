# Larvey — Java Configuration Language & Mapping Framework

Larvey is a configuration language and Java mapping framework with its own lexer, parser, AST, semantic model, annotations, reflection mapper, serializer, functions, interpolation, and an ASM bytecode-optimized runtime.

## Quick Start

```larvey
app {
    name = "GazaPay"
    version = "1.0.0"
    debug = true
    server {
        host = "0.0.0.0"
        port = 8080
    }
    database {
        driver = "mysql"
        host = "localhost"
        port = 3306
        database = "gazapay"
        username = env("DB_USERNAME", "root")
    }
}
```

```java
@LarveyConfig("app")
public record ApplicationConfig(
        String name,
        String version,
        boolean debug,
        ServerConfig server,
        DatabaseConfig database) {}

ApplicationConfig config = Larvey.load("application.larvey").map(ApplicationConfig.class);
System.out.println(config.server().port());
```

See `larvey-examples` for a runnable demo (`com.habbashx.larvey.example.Main` plus `application.larvey`).

## Modules

| Module | Contents |
|---|---|
| `larvey-annotations` | Mapping annotations and `LarveyConverter` |
| `larvey-core` | Lexer, parser, AST, semantic model, functions, runtime |
| `larvey-mapper` | Reflection mapper, metadata, serializer |
| `larvey-bytecode` | ASM mapper generator, cache |
| `larvey-api` | Public developer API (`Larvey`, `LarveyDocument`) |
| `larvey-processor` | Compile-time annotation processor |
| `larvey-tests` | Tests, golden files, benchmarks |
| `larvey-examples` | Runnable example application |

## Features

- Hand-written lexer with line/column errors, no regex parsing
- Recursive-descent parser producing an immutable AST (`BlockNode` vs `ObjectNode` distinguished, `InterpolatedStringNode` for `"${...}"`)
- Semantic model with duplicate detection and dotted-path lookup
- Reflection mapper: primitives, `BigInteger`/`BigDecimal`, enums, `List`/`Set`/`Map`, arrays, nested objects, `Optional`, records, `@LarveyCreator`
- Annotations: `@LarveyConfig`, `@LarveyProperty`, `@LarveyIgnore`, `@LarveyCreator`, `@LarveyDefault`, `@LarveyAlias`, `@LarveyFormat`, `@LarveyConverter`, `@LarveyRequired`
- Serialization: `Larvey.write(config)` produces deterministic readable output, including nested maps
- Functions: `env()`, `sys()`, `file()`, `property()`, `concat()`, `upper()`, `lower()`, `trim()`, plus custom `LarveyFunction` registration with configuration context
- Interpolation: `url = "${host}:${port}"` resolved against scope then root, with `$${` escape and cycle guard
- Type conversion: `String` to `UUID`, `Path`, `URI`, `Duration`, enums, numerics, custom converters
- Validation: `@LarveyRequired`, `@LarveyFormat("regex:...")`; mapping errors carry path, target type, line, and column
- Bytecode mapper (ASM): generated direct mappers for beans, records, creators, collections, and nested types, thread-safe cache, automatic reflection fallback with identical results
- Strategies: `Larvey.builder().strategy(MappingStrategy.BYTECODE).build()`

## Usage

```java
ApplicationConfig config = Larvey.parse(source).map(ApplicationConfig.class);
ConfigurationNode ast = Larvey.parseAst(source);
String output = Larvey.write(config);
Larvey.write(config, Path.of("application.larvey"));

LarveyMapper mapper = Larvey.builder()
        .strategy(MappingStrategy.BYTECODE)
        .function(new MyFunction())
        .build();
```

## Compile-Time Processor

`larvey-processor` validates `@LarveyConfig` classes during compilation (duplicate properties, multiple `@LarveyCreator` constructors) and generates a `<Type>LarveyMeta` class exposing `ROOT`, `TARGET`, and `PROPERTIES`. It is wired into `larvey-tests` via `annotationProcessorPaths`.

## Benchmarks

See `docs/BENCHMARKS.md`. The ASM mapper leads the reflection mapper on every measured shape (1.15–1.31x), and resolution reuse makes both mappers faster in absolute terms.

## Testing

```
mvn test
```

Covers lexer, parser, mapper, bytecode parity, serialization round-trips, functions, format validation, semantics, the annotation processor, and golden `.larvey` files with expected AST snapshots under `larvey-tests/src/test/resources/configs`.

## Requirements

Java 17+, Maven, ASM 9.7 (only runtime dependency besides tests)..
