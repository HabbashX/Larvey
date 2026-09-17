package com.habbashx.larvey.ast;

import java.util.List;

public record InterpolatedStringNode(String raw, List<Part> parts, SourceLocation location) implements ValueNode {
    public InterpolatedStringNode {
        parts = List.copyOf(parts);
    }

    @Override
    public SourceLocation sourceLocation() {
        return location;
    }

    public sealed interface Part permits TextPart, ExpressionPart {
    }

    public record TextPart(String text) implements Part {
    }

    public record ExpressionPart(String expression) implements Part {
    }
}
