package com.habbashx.larvey.bytecode;

import com.habbashx.larvey.mapper.LarveyMapper;
import com.habbashx.larvey.mapper.MappingStrategy;
import com.habbashx.larvey.mapper.ReflectionObjectMapper;

public final class BytecodeMappers {
    private BytecodeMappers() {
    }

    public static LarveyMapper.Builder builder() {
        return LarveyMapper.builder().strategy(MappingStrategy.BYTECODE).bytecodeMapper(new BytecodeObjectMapper(new ReflectionObjectMapper()));
    }

    public static LarveyMapper create() {
        return builder().build();
    }
}
