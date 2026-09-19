package com.habbashx.larvey.function;

import com.habbashx.larvey.ast.SourceLocation;
import com.habbashx.larvey.semantic.Configuration;

public record FunctionContext(Configuration root, String path, SourceLocation location) {
}
