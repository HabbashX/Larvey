package com.habbashx.larvey.function;

import com.habbashx.larvey.semantic.Configuration;
import com.habbashx.larvey.semantic.LarveyValue;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PropertyFunction implements LarveyFunction {
    @Override
    public String name() {
        return "property";
    }

    @Override
    public LarveyValue invoke(List<LarveyValue> arguments) {
        throw new IllegalArgumentException("property() requires configuration context");
    }

    @Override
    public LarveyValue invoke(List<LarveyValue> arguments, FunctionContext context) {
        if (arguments.size() < 1 || arguments.size() > 2) {
            throw new IllegalArgumentException("property() expects 1 or 2 arguments");
        }
        LarveyValue first = arguments.get(0);
        if (!(first instanceof LarveyValue.StringValue stringValue)) {
            throw new IllegalArgumentException("property() key must be a string");
        }
        Optional<LarveyValue> found = context.root().getByPath(stringValue.value());
        if (found.isPresent() && !(found.get() instanceof LarveyValue.ObjectValue objectValue && objectValue.properties().isEmpty())) {
            return found.get();
        }
        Optional<Configuration> block = context.root().getBlockByPath(stringValue.value());
        if (block.isPresent()) {
            return fromConfiguration(block.get());
        }
        if (arguments.size() == 2) {
            return arguments.get(1);
        }
        return new LarveyValue.NullValue(first.location());
    }

    public static LarveyValue.ObjectValue fromConfiguration(Configuration config) {
        Map<String, LarveyValue> props = new LinkedHashMap<>(config.properties());
        for (Map.Entry<String, Configuration> entry : config.blocks().entrySet()) {
            props.put(entry.getKey(), fromConfiguration(entry.getValue()));
        }
        return new LarveyValue.ObjectValue(props, config.location());
    }
}
