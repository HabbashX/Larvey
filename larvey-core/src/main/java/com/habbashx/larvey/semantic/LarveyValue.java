package com.habbashx.larvey.semantic;

import com.habbashx.larvey.ast.SourceLocation;
import java.util.List;
import java.util.Map;

public sealed interface LarveyValue permits LarveyValue.StringValue, LarveyValue.IntegerValue, LarveyValue.DecimalValue, LarveyValue.BooleanValue, LarveyValue.NullValue, LarveyValue.ArrayValue, LarveyValue.ObjectValue, LarveyValue.FunctionCallValue, LarveyValue.InterpolatedValue {
    SourceLocation location();

    record StringValue(String value, SourceLocation location) implements LarveyValue {
    }

    record IntegerValue(long value, SourceLocation location) implements LarveyValue {
    }

    record DecimalValue(double value, SourceLocation location) implements LarveyValue {
    }

    record BooleanValue(boolean value, SourceLocation location) implements LarveyValue {
    }

    record NullValue(SourceLocation location) implements LarveyValue {
    }

    record ArrayValue(List<LarveyValue> elements, SourceLocation location) implements LarveyValue {
        public ArrayValue {
            elements = List.copyOf(elements);
        }
    }

    record ObjectValue(Map<String, LarveyValue> properties, SourceLocation location) implements LarveyValue {
        public ObjectValue {
            properties = Map.copyOf(properties);
        }
    }

    record FunctionCallValue(String name, List<LarveyValue> arguments, SourceLocation location) implements LarveyValue {
        public FunctionCallValue {
            arguments = List.copyOf(arguments);
        }
    }

    record InterpolatedValue(String raw, List<Part> parts, SourceLocation location) implements LarveyValue {
        public InterpolatedValue {
            parts = List.copyOf(parts);
        }

        public sealed interface Part permits Text, Expression {
        }

        public record Text(String text) implements Part {
        }

        public record Expression(String expression) implements Part {
        }
    }
}
