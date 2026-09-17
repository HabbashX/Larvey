package com.habbashx.larvey.ast;

public record DecimalNode(double value, SourceLocation location) implements ValueNode {
    @Override
    public SourceLocation sourceLocation() {
        return location;
    }
}
