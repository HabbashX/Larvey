package com.habbashx.larvey.ast;

public sealed interface AstNode permits ConfigurationNode, AssignmentNode, BlockNode, ValueNode {
    SourceLocation sourceLocation();
}
