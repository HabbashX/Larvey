package com.habbashx.larvey.ast;

import java.util.List;

public final class AstDump {
    private AstDump() {
    }

    public static String dump(ConfigurationNode node) {
        StringBuilder out = new StringBuilder();
        dumpMembers(node.members(), out, 0);
        return out.toString();
    }

    private static void dumpMembers(List<AstNode> members, StringBuilder out, int indent) {
        for (AstNode member : members) {
            if (member instanceof AssignmentNode assignment) {
                indent(out, indent);
                out.append("prop ").append(assignment.name()).append(" = ");
                dumpValue(assignment.value(), out);
                out.append(" @").append(at(assignment.sourceLocation())).append("\n");
            } else if (member instanceof BlockNode block) {
                indent(out, indent);
                out.append("block ").append(block.name()).append(" @").append(at(block.sourceLocation())).append("\n");
                dumpMembers(block.members(), out, indent + 1);
                indent(out, indent);
                out.append("end ").append(block.name()).append("\n");
            }
        }
    }

    private static void dumpValue(ValueNode value, StringBuilder out) {
        if (value instanceof StringNode stringNode) {
            out.append(quote(stringNode.value()));
        } else if (value instanceof IntegerNode integerNode) {
            out.append(integerNode.value());
        } else if (value instanceof DecimalNode decimalNode) {
            out.append(decimalNode.value());
        } else if (value instanceof BooleanNode booleanNode) {
            out.append(booleanNode.value());
        } else if (value instanceof NullNode) {
            out.append("null");
        } else if (value instanceof ArrayNode arrayNode) {
            out.append("[");
            boolean first = true;
            for (ValueNode element : arrayNode.elements()) {
                if (!first) {
                    out.append(", ");
                }
                dumpValue(element, out);
                first = false;
            }
            out.append("]");
        } else if (value instanceof ObjectNode objectNode) {
            out.append("{");
            boolean first = true;
            for (AssignmentNode prop : objectNode.properties()) {
                if (!first) {
                    out.append(", ");
                }
                out.append(prop.name()).append(" = ");
                dumpValue(prop.value(), out);
                first = false;
            }
            out.append("}");
        } else if (value instanceof FunctionCallNode functionCallNode) {
            out.append(functionCallNode.name()).append("(");
            boolean first = true;
            for (ValueNode arg : functionCallNode.arguments()) {
                if (!first) {
                    out.append(", ");
                }
                dumpValue(arg, out);
                first = false;
            }
            out.append(")");
        } else if (value instanceof InterpolatedStringNode interpolated) {
            out.append("interp(").append(quote(interpolated.raw())).append(")");
        }
    }

    private static void indent(StringBuilder out, int level) {
        for (int i = 0; i < level; i++) {
            out.append("  ");
        }
    }

    private static String at(SourceLocation location) {
        if (location == null) {
            return "?:?";
        }
        return location.line() + ":" + location.column();
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r") + "\"";
    }
}
