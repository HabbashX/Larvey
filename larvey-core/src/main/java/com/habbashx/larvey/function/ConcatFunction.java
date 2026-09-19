package com.habbashx.larvey.function;

import com.habbashx.larvey.semantic.LarveyValue;
import java.util.List;

public final class ConcatFunction implements LarveyFunction {
    @Override
    public String name() {
        return "concat";
    }

    @Override
    public LarveyValue invoke(List<LarveyValue> arguments) {
        if (arguments.isEmpty()) {
            throw new IllegalArgumentException("concat() expects at least 1 argument");
        }
        StringBuilder out = new StringBuilder();
        for (LarveyValue argument : arguments) {
            if (!(argument instanceof LarveyValue.StringValue stringValue)) {
                throw new IllegalArgumentException("concat() arguments must be strings");
            }
            out.append(stringValue.value());
        }
        return new LarveyValue.StringValue(out.toString(), arguments.get(0).location());
    }
}
