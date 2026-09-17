package com.habbashx.larvey.ast;

import java.util.List;

public record BlockNode(String name, List<AstNode> members, SourceLocation location) implements AstNode {
    public BlockNode {
        members = List.copyOf(members);
    }

    @Override
    public SourceLocation sourceLocation() {
        return location;
    }
}
