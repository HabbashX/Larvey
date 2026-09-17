package com.habbashx.larvey.ast;

public record NullNode(SourceLocation location) implements ValueNode {
    @Override
    public SourceLocation sourceLocation() {
        return location;
    }
}
