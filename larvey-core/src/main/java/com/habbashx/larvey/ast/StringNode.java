package com.habbashx.larvey.ast;

public record StringNode(String value, SourceLocation location) implements ValueNode {
    @Override
    public SourceLocation sourceLocation() {
        return location;
    }
}
