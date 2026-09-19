package com.habbashx.larvey.benchmark;

import com.habbashx.larvey.api.Larvey;
import com.habbashx.larvey.ast.ConfigurationNode;
import com.habbashx.larvey.bytecode.BytecodeMappers;
import com.habbashx.larvey.mapper.LarveyMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class LarveyBenchmark {
    public static class Flat {
        public String a;
        public String b;
        public String c;
        public String d;
        public int e;
        public int f;
        public boolean g;
        public double h;
    }

    public static class Server {
        public String host;
        public int port;
    }

    public static class Db {
        public String driver;
        public String host;
        public int port;
        public String database;
    }

    public static class Nested {
        public String name;
        public String version;
        public boolean debug;
        public Server server;
        public Db database;
    }

    public static class WithCollections {
        public List<String> hosts;
        public List<Integer> ports;
        public Set<String> tags;
        public Map<String, String> labels;
        public int[] codes;
        public Optional<String> note;
    }

    public record Rec(String name, int port, List<String> hosts) {
    }

    public enum Level { LOW, HIGH }

    public static class WithOther {
        public Level level;
        public java.util.UUID id;
        public java.time.Duration timeout;
        public java.math.BigDecimal ratio;
    }

    private static final String FLAT_SOURCE = "a = \"1\"\nb = \"2\"\nc = \"3\"\nd = \"4\"\ne = 5\nf = 6\ng = true\nh = 1.5\n";
    private static final String NESTED_SOURCE = "name = \"GazaPay\"\nversion = \"1.0.0\"\ndebug = true\nserver { host = \"0.0.0.0\" port = 8080 }\ndatabase { driver = \"mysql\" host = \"localhost\" port = 3306 database = \"gazapay\" }\n";
    private static final String COLLECTIONS_SOURCE = "hosts = [\"a\", \"b\", \"c\"]\nports = [1, 2, 3]\ntags = [\"x\", \"y\"]\nlabels = { env = \"prod\" team = \"pay\" }\ncodes = [7, 8]\nnote = \"hi\"\n";
    private static final String RECORD_SOURCE = "name = \"app\"\nport = 8080\nhosts = [\"a\", \"b\"]\n";
    private static final String OTHER_SOURCE = "level = \"HIGH\"\nid = \"550e8400-e29b-41d4-a716-446655440000\"\ntimeout = \"PT30S\"\nratio = \"3.14\"\n";
    private static final String INLINE_SOURCE = "name = \"GazaPay\"\nversion = \"1.0.0\"\ndebug = true\nserver = { host = \"0.0.0.0\" port = 8080 }\ndatabase = { driver = \"mysql\" host = \"localhost\" port = 3306 database = \"gazapay\" }\n";

    public static void main(String[] args) {
        LarveyMapper reflection = LarveyMapper.builder().build();
        LarveyMapper bytecode = BytecodeMappers.create();
        com.habbashx.larvey.runtime.ConfigurationResolver resolver =
                new com.habbashx.larvey.runtime.ConfigurationResolver(new com.habbashx.larvey.function.FunctionRegistry());
        warmup(reflection, bytecode);
        int iterations = 100000;
        shape("flat", FLAT_SOURCE, Flat.class, reflection, bytecode, resolver, iterations);
        shape("nested", NESTED_SOURCE, Nested.class, reflection, bytecode, resolver, iterations);
        shape("collections", COLLECTIONS_SOURCE, WithCollections.class, reflection, bytecode, resolver, iterations);
        shape("record", RECORD_SOURCE, Rec.class, reflection, bytecode, resolver, iterations);
        shape("other", OTHER_SOURCE, WithOther.class, reflection, bytecode, resolver, iterations);
        shape("inline", INLINE_SOURCE, Nested.class, reflection, bytecode, resolver, iterations);
        benchmark("parse", 20000, () -> Larvey.parseAst(NESTED_SOURCE));
        Nested config = reflection.map(Larvey.parseAst(NESTED_SOURCE), Nested.class);
        benchmark("serialize", 20000, () -> Larvey.write(config));
    }

    private static void warmup(LarveyMapper reflection, LarveyMapper bytecode) {
        Object[][] cases = {
            {FLAT_SOURCE, Flat.class},
            {NESTED_SOURCE, Nested.class},
            {COLLECTIONS_SOURCE, WithCollections.class},
            {RECORD_SOURCE, Rec.class},
            {OTHER_SOURCE, WithOther.class},
            {INLINE_SOURCE, Nested.class},
        };
        for (int i = 0; i < 3000; i++) {
            for (Object[] c : cases) {
                ConfigurationNode ast = Larvey.parseAst((String) c[0]);
                reflection.map(ast, (Class<?>) c[1]);
                bytecode.map(ast, (Class<?>) c[1]);
            }
        }
    }

    private static <T> void shape(String name, String source, Class<T> type, LarveyMapper reflection, LarveyMapper bytecode, com.habbashx.larvey.runtime.ConfigurationResolver resolver, int iterations) {
        ConfigurationNode ast = Larvey.parseAst(source);
        T first = reflection.map(ast, type);
        T second = bytecode.map(ast, type);
        if (!first.toString().isEmpty() && second.toString().isEmpty()) {
            throw new IllegalStateException("Parity failure for " + name);
        }
        double[] speedups = new double[5];
        long[] reflectMeds = new long[5];
        long[] bytecodeMeds = new long[5];
        long[] resolveMeds = new long[5];
        for (int fork = 0; fork < 5; fork++) {
            long reflectNs = 0;
            long bytecodeNs = 0;
            long resolveNs = 0;
            for (int i = 0; i < iterations; i++) {
                long t = System.nanoTime();
                reflection.map(ast, type);
                reflectNs += System.nanoTime() - t;
                t = System.nanoTime();
                bytecode.map(ast, type);
                bytecodeNs += System.nanoTime() - t;
                t = System.nanoTime();
                resolver.resolve(ast);
                resolveNs += System.nanoTime() - t;
            }
            reflectMeds[fork] = reflectNs / iterations;
            bytecodeMeds[fork] = bytecodeNs / iterations;
            resolveMeds[fork] = resolveNs / iterations;
            speedups[fork] = (double) reflectNs / bytecodeNs;
        }
        java.util.Arrays.sort(reflectMeds);
        java.util.Arrays.sort(bytecodeMeds);
        java.util.Arrays.sort(resolveMeds);
        java.util.Arrays.sort(speedups);
        System.out.printf("%-12s reflection %,d ns | bytecode %,d ns | resolve %,d ns | speedup %.2fx%n",
                name, reflectMeds[2], bytecodeMeds[2], resolveMeds[2], speedups[2]);
    }

    private static void benchmark(String name, int iterations, Runnable task) {
        for (int i = 0; i < 2000; i++) {
            task.run();
        }
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            task.run();
        }
        long elapsed = System.nanoTime() - start;
        System.out.printf("%-22s %,d ops in %,d ms (%,d ns/op)%n", name, iterations, elapsed / 1_000_000, elapsed / iterations);
    }
}
