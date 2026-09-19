package com.habbashx.larvey.ast;

import java.util.List;

public record ArrayNode(List<ValueNode> elements, SourceLocation location) implements ValueNode {
    public ArrayNode {
        elements = List.copyOf(elements);
    }

    @Override
    public SourceLocation sourceLocation() {
        return location;
    }
}
