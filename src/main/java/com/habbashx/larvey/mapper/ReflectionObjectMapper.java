package com.habbashx.larvey.mapper;

import com.habbashx.larvey.ast.ConfigurationNode;
import com.habbashx.larvey.semantic.Configuration;
import com.habbashx.larvey.serializer.LarveySerializer;

public final class ReflectionObjectMapper implements ObjectMapperStrategy {
    private final ReflectionMapper mapper = new ReflectionMapper();
    private final LarveySerializer serializer = new LarveySerializer();

    @Override
    public <T> T map(Configuration node, Class<T> type) {
        return mapper.map(node, type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> ConfigurationNode unmap(T value) {
        String source = serializer.serialize(value);
        return com.habbashx.larvey.api.Larvey.parseAst(source);
    }
}
