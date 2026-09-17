package com.habbashx.larvey.function;

import com.habbashx.larvey.ast.SourceLocation;
import com.habbashx.larvey.exception.LarveyMappingException;
import com.habbashx.larvey.semantic.LarveyValue;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class FunctionRegistry {
    private final Map<String, LarveyFunction> functions = new ConcurrentHashMap<>();

    public FunctionRegistry() {
        register(new EnvFunction());
        register(new SysFunction());
    }

    public void register(LarveyFunction function) {
        functions.put(function.name(), function);
    }

    public boolean has(String name) {
        return functions.containsKey(name);
    }

    public LarveyValue invoke(String name, List<LarveyValue> args, SourceLocation location, Class<?> owner, String path) {
        LarveyFunction fn = functions.get(name);
        if (fn == null) {
            throw new LarveyMappingException("Unknown function '" + name + "'", path, owner == null ? Object.class : owner);
        }
        return fn.invoke(List.copyOf(args));
    }
}
