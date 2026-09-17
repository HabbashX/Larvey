package com.habbashx.larvey.bytecode;

import com.habbashx.larvey.ast.ConfigurationNode;
import com.habbashx.larvey.exception.LarveyBytecodeException;
import com.habbashx.larvey.mapper.ObjectMapperStrategy;
import com.habbashx.larvey.metadata.ClassMetadata;
import com.habbashx.larvey.semantic.Configuration;

public final class BytecodeObjectMapper implements ObjectMapperStrategy {
    private final MapperCache cache = new MapperCache();
    private final ObjectMapperStrategy fallback;

    public BytecodeObjectMapper(ObjectMapperStrategy fallback) {
        this.fallback = fallback;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T map(Configuration node, Class<T> type) {
        Configuration effective = node;
        ClassMetadata<T> metadata = ClassMetadata.of(type);
        if (!metadata.root().isEmpty()) {
            effective = node.getBlockByPath(metadata.root()).orElse(null);
            if (effective == null) {
                return fallback.map(node, type);
            }
        }
        Configuration target = effective;
        try {
            GeneratedMapper<T> mapper = cache.computeIfAbsent(type, this::create);
            return mapper.map(target);
        } catch (LarveyBytecodeException e) {
            return fallback.map(node, type);
        }
    }

    @Override
    public <T> ConfigurationNode unmap(T value) {
        return fallback.unmap(value);
    }

    private <T> GeneratedMapper<T> create(Class<T> type) {
        try {
            Class<? extends GeneratedMapper<T>> clazz = BytecodeGenerator.generate(type);
            return clazz.getDeclaredConstructor(Class.class).newInstance(type);
        } catch (LarveyBytecodeException e) {
            throw e;
        } catch (Exception e) {
            throw new LarveyBytecodeException("Cannot instantiate generated mapper for " + type.getName(), e);
        }
    }

    public int cachedCount() {
        return cache.size();
    }
}
