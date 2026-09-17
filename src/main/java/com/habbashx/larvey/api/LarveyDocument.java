package com.habbashx.larvey.api;

import com.habbashx.larvey.ast.ConfigurationNode;
import com.habbashx.larvey.mapper.LarveyMapper;
import java.nio.file.Path;

public final class LarveyDocument {
    private final ConfigurationNode ast;
    private final LarveyMapper mapper;

    public LarveyDocument(ConfigurationNode ast, LarveyMapper mapper) {
        this.ast = ast;
        this.mapper = mapper;
    }

    public <T> T map(Class<T> type) {
        return mapper.map(ast, type);
    }

    public ConfigurationNode ast() {
        return ast;
    }
}
