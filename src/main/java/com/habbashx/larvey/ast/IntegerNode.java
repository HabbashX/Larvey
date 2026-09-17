package com.habbashx.larvey.ast;

public record IntegerNode(long value, SourceLocation location) implements ValueNode {
    @Override
    public SourceLocation sourceLocation() {
        return location;
    }
}
