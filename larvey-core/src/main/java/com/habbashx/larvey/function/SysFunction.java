package com.habbashx.larvey.function;

import com.habbashx.larvey.semantic.LarveyValue;
import java.util.List;

public final class SysFunction implements LarveyFunction {
    @Override
    public String name() {
        return "sys";
    }

    @Override
    public LarveyValue invoke(List<LarveyValue> arguments) {
        if (arguments.size() < 1 || arguments.size() > 2) {
            throw new IllegalArgumentException("sys() expects 1 or 2 arguments");
        }
        LarveyValue key = arguments.get(0);
        if (!(key instanceof LarveyValue.StringValue stringValue)) {
            throw new IllegalArgumentException("sys() key must be a string");
        }
        String value = System.getProperty(stringValue.value());
        if (value == null && arguments.size() == 2) {
            LarveyValue fallback = arguments.get(1);
            if (fallback instanceof LarveyValue.StringValue stringFallback) {
                value = stringFallback.value();
            } else {
                return fallback;
            }
        }
        if (value == null) {
            return new LarveyValue.NullValue(key.location());
        }
        return new LarveyValue.StringValue(value, key.location());
    }
}
