package com.habbashx.larvey.api;

import com.habbashx.larvey.ast.ConfigurationNode;
import com.habbashx.larvey.function.LarveyFunction;
import com.habbashx.larvey.lexer.Lexer;
import com.habbashx.larvey.mapper.LarveyMapper;
import com.habbashx.larvey.mapper.MappingStrategy;
import com.habbashx.larvey.parser.Parser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Larvey {
    private Larvey() {
    }

    public static LarveyDocument parse(String source) {
        ConfigurationNode ast = parseAst(source);
        return new LarveyDocument(ast, LarveyMapper.defaultMapper());
    }

    public static ConfigurationNode parseAst(String source) {
        return new Parser(new Lexer(source).tokenize()).parse();
    }

    public static LarveyDocument load(String path) {
        return load(Path.of(path));
    }

    public static LarveyDocument load(Path path) {
        try {
            return parse(Files.readString(path));
        } catch (IOException e) {
            throw new com.habbashx.larvey.exception.LarveyException("Cannot read file '" + path + "'", e);
        }
    }

    public static String write(Object value) {
        return LarveyMapper.defaultMapper().writeString(value);
    }

    public static void write(Object value, Path path) {
        try {
            Files.writeString(path, write(value));
        } catch (IOException e) {
            throw new com.habbashx.larvey.exception.LarveyException("Cannot write file '" + path + "'", e);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final LarveyMapper.Builder mapper = LarveyMapper.builder();
        private MappingStrategy strategy = MappingStrategy.REFLECTION;

        public Builder strategy(MappingStrategy strategy) {
            this.strategy = strategy;
            mapper.strategy(strategy);
            if (strategy == MappingStrategy.BYTECODE) {
                mapper.bytecodeMapper(new com.habbashx.larvey.bytecode.BytecodeObjectMapper(new com.habbashx.larvey.mapper.ReflectionObjectMapper()));
            }
            return this;
        }

        public Builder function(LarveyFunction function) {
            mapper.function(function);
            return this;
        }

        public LarveyDocument parse(String source) {
            return new LarveyDocument(parseAst(source), mapper.build());
        }

        public LarveyDocument load(Path path) {
            try {
                return parse(Files.readString(path));
            } catch (IOException e) {
                throw new com.habbashx.larvey.exception.LarveyException("Cannot read file '" + path + "'", e);
            }
        }

        public LarveyMapper build() {
            return mapper.build();
        }
    }
}
