package com.habbashx.larvey.ast;

public record AssignmentNode(String name, ValueNode value, SourceLocation location) implements AstNode {
    @Override
    public SourceLocation sourceLocation() {
        return location;
    }
}
