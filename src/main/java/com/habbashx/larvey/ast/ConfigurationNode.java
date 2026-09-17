package com.habbashx.larvey.ast;

import java.util.List;

public record ConfigurationNode(List<AstNode> members, SourceLocation location) implements AstNode {
    public ConfigurationNode {
        members = List.copyOf(members);
    }

    @Override
    public SourceLocation sourceLocation() {
        return location;
    }
}
