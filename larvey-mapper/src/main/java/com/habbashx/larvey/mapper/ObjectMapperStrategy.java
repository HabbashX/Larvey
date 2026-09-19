package com.habbashx.larvey.mapper;

import com.habbashx.larvey.ast.ConfigurationNode;
import com.habbashx.larvey.semantic.Configuration;

public interface ObjectMapperStrategy {
    <T> T map(Configuration node, Class<T> type);
    <T> ConfigurationNode unmap(T value);
}
