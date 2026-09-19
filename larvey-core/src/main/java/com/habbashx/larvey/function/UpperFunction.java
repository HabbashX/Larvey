package com.habbashx.larvey.function;

import com.habbashx.larvey.semantic.LarveyValue;
import java.util.List;
import java.util.Locale;

public final class UpperFunction implements LarveyFunction {
    @Override
    public String name() {
        return "upper";
    }

    @Override
    public LarveyValue invoke(List<LarveyValue> arguments) {
        if (arguments.size() != 1) {
            throw new IllegalArgumentException("upper() expects exactly 1 argument");
        }
        LarveyValue argument = arguments.get(0);
        if (!(argument instanceof LarveyValue.StringValue stringValue)) {
            throw new IllegalArgumentException("upper() argument must be a string");
        }
        return new LarveyValue.StringValue(stringValue.value().toUpperCase(Locale.ROOT), argument.location());
    }
}
