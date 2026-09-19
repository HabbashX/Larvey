package com.habbashx.larvey.function;

import com.habbashx.larvey.semantic.LarveyValue;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public final class FileFunction implements LarveyFunction {
    @Override
    public String name() {
        return "file";
    }

    @Override
    public LarveyValue invoke(List<LarveyValue> arguments) {
        if (arguments.size() < 1 || arguments.size() > 2) {
            throw new IllegalArgumentException("file() expects 1 or 2 arguments");
        }
        LarveyValue first = arguments.get(0);
        if (!(first instanceof LarveyValue.StringValue stringValue)) {
            throw new IllegalArgumentException("file() path must be a string");
        }
        Path path = Paths.get(stringValue.value());
        try {
            return new LarveyValue.StringValue(Files.readString(path), first.location());
        } catch (Exception e) {
            if (arguments.size() == 2) {
                LarveyValue fallback = arguments.get(1);
                if (fallback instanceof LarveyValue.StringValue stringFallback) {
                    return new LarveyValue.StringValue(stringFallback.value(), first.location());
                }
                return fallback;
            }
            throw new IllegalArgumentException("file() cannot read '" + path + "'", e);
        }
    }
}
