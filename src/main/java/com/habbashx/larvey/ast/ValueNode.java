package com.habbashx.larvey.ast;

public sealed interface ValueNode extends AstNode permits ObjectNode, ArrayNode, StringNode, IntegerNode, DecimalNode, BooleanNode, NullNode, FunctionCallNode, InterpolatedStringNode {
}
