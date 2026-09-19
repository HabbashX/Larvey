package com.habbashx.larvey.function;

import com.habbashx.larvey.semantic.LarveyValue;
import java.util.List;

public final class TrimFunction implements LarveyFunction {
    @Override
    public String name() {
        return "trim";
    }

    @Override
    public LarveyValue invoke(List<LarveyValue> arguments) {
        if (arguments.size() != 1) {
            throw new IllegalArgumentException("trim() expects exactly 1 argument");
        }
        LarveyValue argument = arguments.get(0);
        if (!(argument instanceof LarveyValue.StringValue stringValue)) {
            throw new IllegalArgumentException("trim() argument must be a string");
        }
        return new LarveyValue.StringValue(stringValue.value().strip(), argument.location());
    }
}
