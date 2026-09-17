package com.habbashx.larvey.ast;

import java.util.List;

public record ObjectNode(List<AssignmentNode> properties, SourceLocation location) implements ValueNode {
    public ObjectNode {
        properties = List.copyOf(properties);
    }

    @Override
    public SourceLocation sourceLocation() {
        return location;
    }
}
