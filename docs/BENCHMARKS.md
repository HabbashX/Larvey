# Larvey Benchmarks

Run: `com.habbashx.larvey.benchmark.LarveyBenchmark` (in `larvey-tests`).

Methodology: every shape is fully warmed up first (3,000 mixed iterations), then measured in 5 interleaved forks where reflection, bytecode, and resolution run back-to-back in the same loop under identical CPU/GC conditions. Reported values are medians. Single-fork timings without warmup showed up to 10x noise and are not trusted.

## Results (ns/op, lower is better)

| Shape | Reflection | Bytecode | Resolve only | Speedup |
|---|---|---|---|---|
| flat (8 scalars) | 1,362 | 1,057 | 770 | 1.25x |
| nested (blocks) | 4,071 | 3,482 | 2,664 | 1.17x |
| collections | 3,394 | 2,613 | 1,770 | 1.31x |
| record | 1,098 | 971 | 656 | 1.16x |
| enums + UUID/Duration/BigDecimal | 1,338 | 1,152 | 487 | 1.15x |
| inline objects | 2,580 | 2,256 | 1,633 | 1.16x |
| parse (lexer + parser + AST) | 10,913 | — | — | — |
| serialize | 4,505 | — | — | — |

## Where the time goes

Resolution (AST to semantic model, function evaluation, interpolation) is 55–65% of every mapping call and is shared by both strategies. The resolver reuses untouched subtrees by identity instead of rebuilding them, and builds configurations directly instead of through reflection, which made resolution 1.5–2.7x faster and lifted both mappers with it.

The remaining mapping delta comes from the generated mapper: direct `MethodHandle` stores with exact signatures (no lenient `invoke` conversions, no `Field.set`), direct conversion of enums and JDK scalar types with no reflective dispatch, and inline objects mapped through generated mappers instead of an AST round-trip.

## Is reflection still needed?

Not on the hot path. For standard configuration shapes the generated mapper performs zero reflective calls per mapping. Reflection remains in two deliberate roles: one-time metadata analysis at generation time (`ClassMetadata`), and a transparent fallback for exotic types (custom converters, `@LarveyFormat` validation, non-accessible classes), which produce results identical to the reflection mapper.

Machine: Windows, JDK 17+, medians of 5 interleaved forks. Re-run on your own hardware before drawing conclusions.
