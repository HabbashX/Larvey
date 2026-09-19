package com.habbashx.larvey.ast;

import java.util.List;

public record FunctionCallNode(String name, List<ValueNode> arguments, SourceLocation location) implements ValueNode {
    public FunctionCallNode {
        arguments = List.copyOf(arguments);
    }

    @Override
    public SourceLocation sourceLocation() {
        return location;
    }
}
