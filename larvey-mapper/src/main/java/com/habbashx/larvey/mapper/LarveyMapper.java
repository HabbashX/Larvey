package com.habbashx.larvey.mapper;

import com.habbashx.larvey.ast.ConfigurationNode;
import com.habbashx.larvey.exception.LarveyMappingException;
import com.habbashx.larvey.function.FunctionRegistry;
import com.habbashx.larvey.function.LarveyFunction;
import com.habbashx.larvey.runtime.ConfigurationResolver;
import com.habbashx.larvey.semantic.Configuration;
import com.habbashx.larvey.serializer.LarveySerializer;
import java.util.ArrayList;
import java.util.List;

public final class LarveyMapper {
    private final MappingStrategy strategy;
    private final FunctionRegistry functions;
    private final ObjectMapperStrategy reflection = new ReflectionObjectMapper();
    private final ObjectMapperStrategy bytecode;

    private LarveyMapper(MappingStrategy strategy, FunctionRegistry functions, ObjectMapperStrategy bytecode) {
        this.strategy = strategy;
        this.functions = functions;
        this.bytecode = bytecode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static LarveyMapper defaultMapper() {
        return builder().build();
    }

    public <T> T map(ConfigurationNode ast, Class<T> type) {
        Configuration resolved = new ConfigurationResolver(functions).resolve(ast);
        return select().map(resolved, type);
    }

    public <T> T map(Configuration config, Class<T> type) {
        return select().map(config, type);
    }

    public <T> ConfigurationNode unmap(T value) {
        return select().unmap(value);
    }

    public String writeString(Object value) {
        return new LarveySerializer().serialize(value);
    }

    private ObjectMapperStrategy select() {
        if (strategy == MappingStrategy.BYTECODE && bytecode != null) {
            return bytecode;
        }
        return reflection;
    }

    public static final class Builder {
        private MappingStrategy strategy = MappingStrategy.REFLECTION;
        private final FunctionRegistry functions = new FunctionRegistry();
        private ObjectMapperStrategy bytecode;

        public Builder strategy(MappingStrategy strategy) {
            this.strategy = strategy;
            return this;
        }

        public Builder bytecodeMapper(ObjectMapperStrategy bytecode) {
            this.bytecode = bytecode;
            return this;
        }

        public Builder function(LarveyFunction function) {
            this.functions.register(function);
            return this;
        }

        public LarveyMapper build() {
            return new LarveyMapper(strategy, functions, bytecode);
        }
    }
}
