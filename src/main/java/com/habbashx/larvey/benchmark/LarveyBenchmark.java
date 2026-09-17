package com.habbashx.larvey.benchmark;

import com.habbashx.larvey.api.Larvey;
import com.habbashx.larvey.ast.ConfigurationNode;
import com.habbashx.larvey.mapper.LarveyMapper;
import com.habbashx.larvey.mapper.MappingStrategy;

public final class LarveyBenchmark {
    public static class ServerConfig {
        public String host;
        public int port;
    }

    public static class AppConfig {
        public String name;
        public String version;
        public boolean debug;
        public ServerConfig server;
    }

    public static void main(String[] args) {
        String source = "name = \"GazaPay\"\nversion = \"1.0.0\"\ndebug = true\nserver { host = \"0.0.0.0\" port = 8080 }\n";
        ConfigurationNode ast = Larvey.parseAst(source);
        LarveyMapper reflection = LarveyMapper.builder().strategy(MappingStrategy.REFLECTION).build();
        LarveyMapper bytecode = LarveyMapper.builder().strategy(MappingStrategy.BYTECODE).build();
        AppConfig warm = reflection.map(ast, AppConfig.class);
        warm = bytecode.map(ast, AppConfig.class);
        int iterations = 20000;
        benchmark("parse", iterations, () -> Larvey.parseAst(source));
        benchmark("reflection-map", iterations, () -> reflection.map(ast, AppConfig.class));
        benchmark("bytecode-map", iterations, () -> bytecode.map(ast, AppConfig.class));
        AppConfig config = reflection.map(ast, AppConfig.class);
        benchmark("serialize", iterations, () -> Larvey.write(config));
    }

    private static void benchmark(String name, int iterations, Runnable task) {
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            task.run();
        }
        long elapsed = System.nanoTime() - start;
        System.out.printf("%-15s %,d ops in %,d ms (%,d ns/op)%n", name, iterations, elapsed / 1_000_000, elapsed / iterations);
    }
}
