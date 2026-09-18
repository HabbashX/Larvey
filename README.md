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

## Features

- Hand-written lexer with line/column errors, no regex parsing
- Recursive-descent parser producing an immutable AST (`BlockNode` vs `ObjectNode` distinguished)
- Semantic model with duplicate detection and dotted-path lookup
- Reflection mapper: primitives, `BigInteger`/`BigDecimal`, enums, `List`/`Set`/`Map`, arrays, nested objects, `Optional`, records, `@LarveyCreator`
- Annotations: `@LarveyConfig`, `@LarveyProperty`, `@LarveyIgnore`, `@LarveyCreator`, `@LarveyDefault`, `@LarveyAlias`, `@LarveyFormat`, `@LarveyConverter`, `@LarveyRequired`
- Serialization: `Larvey.write(config)` produces deterministic readable output
- Functions: `env()`, `sys()`, plus custom `LarveyFunction` registration
- Interpolation: `url = "${host}:${port}"`
- Type conversion: `String` to `UUID`, `Path`, `URI`, `Duration`, enums, numerics, custom converters
- Bytecode mapper (ASM): generates direct field/setter mappers, thread-safe cache, identical results to reflection, automatic fallback
- Strategies: `LarveyMapper.builder().strategy(MappingStrategy.BYTECODE).build()`

## Usage

```java
ApplicationConfig config = Larvey.parse(source).map(ApplicationConfig.class);
ConfigurationNode ast = Larvey.parseAst(source);
String output = Larvey.write(config);
Larvey.write(config, Path.of("application.larvey"));

LarveyMapper mapper = Larvey.builder()
        .strategy(MappingStrategy.BYTECODE)
        .function(new MyFunction())
        .build()
        .map(ast, ApplicationConfig.class);
```

## Benchmarks

Run `com.habbashx.larvey.benchmark.LarveyBenchmark`. It measures parsing, AST creation, reflection mapping, bytecode mapping, and serialization.

## Testing

```
mvn test
```

44 tests: lexer, parser, mapper, bytecode parity, serialization round-trips, golden `.larvey` files under `src/test/resources/configs`.

## Requirements

Java 17+, Maven, ASM 9.7 (only runtime dependency besides tests)..
