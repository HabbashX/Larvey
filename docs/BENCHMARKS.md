# Larvey Benchmarks

Run: `com.habbashx.larvey.benchmark.LarveyBenchmark` (in `larvey-tests`).

Config used: `name`, `version`, `debug`, plus a nested `server` block with `host` and `port`. 20,000 iterations per phase.

## Results

| Phase | Total (20,000 ops) | Per op |
|---|---|---|
| parse (lexer + parser + AST) | 185 ms | 9,287 ns |
| reflection-map | 930 ms | 46,538 ns |
| bytecode-map | 600 ms | 30,008 ns |
| serialize | 554 ms | 27,744 ns |

## Reflection vs Bytecode

The ASM-generated mapper performs the same mapping about **1.55x faster** than the reflection mapper on this configuration shape. Generated mappers use direct field/setter access through cached `MethodHandle`s, so repeated annotation inspection and reflective dispatch are eliminated from the hot path.

Bytecode generation is pay-as-you-go: the first mapping of a type generates and caches the mapper in a thread-safe cache, and types the generator cannot handle (custom converters, format validation, non-accessible classes) transparently fall back to the reflection mapper with identical results.

Machine: Windows, JDK 17+, single run, warm up included in the measured loop. Re-run on your own hardware before drawing conclusions.
