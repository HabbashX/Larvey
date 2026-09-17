package com.habbashx.larvey.bytecode;

import java.util.concurrent.ConcurrentHashMap;

public final class MapperCache {
    private final ConcurrentHashMap<Class<?>, GeneratedMapper<?>> cache = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> GeneratedMapper<T> get(Class<T> type) {
        return (GeneratedMapper<T>) cache.get(type);
    }

    public <T> void put(Class<T> type, GeneratedMapper<T> mapper) {
        cache.put(type, mapper);
    }

    @SuppressWarnings("unchecked")
    public <T> GeneratedMapper<T> computeIfAbsent(Class<T> type, java.util.function.Function<Class<T>, GeneratedMapper<T>> factory) {
        return (GeneratedMapper<T>) cache.computeIfAbsent(type, k -> factory.apply(type));
    }

    public void clear() {
        cache.clear();
    }

    public int size() {
        return cache.size();
    }
}
