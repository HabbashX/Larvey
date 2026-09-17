package com.habbashx.larvey.ast;

public record BooleanNode(boolean value, SourceLocation location) implements ValueNode {
    @Override
    public SourceLocation sourceLocation() {
        return location;
    }
}
