package com.habbashx.larvey.semantic;

import com.habbashx.larvey.ast.ArrayNode;
import com.habbashx.larvey.ast.AssignmentNode;
import com.habbashx.larvey.ast.AstNode;
import com.habbashx.larvey.ast.BlockNode;
import com.habbashx.larvey.ast.BooleanNode;
import com.habbashx.larvey.ast.ConfigurationNode;
import com.habbashx.larvey.ast.DecimalNode;
import com.habbashx.larvey.ast.FunctionCallNode;
import com.habbashx.larvey.ast.IntegerNode;
import com.habbashx.larvey.ast.NullNode;
import com.habbashx.larvey.ast.ObjectNode;
import com.habbashx.larvey.ast.SourceLocation;
import com.habbashx.larvey.ast.StringNode;
import com.habbashx.larvey.ast.ValueNode;
import com.habbashx.larvey.exception.LarveySemanticException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class Configuration {
    private final Map<String, LarveyValue> properties;
    private final Map<String, Configuration> blocks;
    private final SourceLocation location;
    private final String path;

    private Configuration(Map<String, LarveyValue> properties, Map<String, Configuration> blocks, SourceLocation location, String path) {
        this.properties = Map.copyOf(properties);
        this.blocks = Map.copyOf(blocks);
        this.location = location;
        this.path = path;
    }

    public static Configuration from(ConfigurationNode node) {
        return fromMembers(node.members(), node.sourceLocation(), "");
    }

    private static Configuration fromMembers(List<AstNode> members, SourceLocation location, String path) {
        Map<String, LarveyValue> properties = new LinkedHashMap<>();
        Map<String, List<AstNode>> blockGroups = new LinkedHashMap<>();
        Map<String, SourceLocation> firstSeen = new LinkedHashMap<>();
        for (AstNode member : members) {
            if (member instanceof AssignmentNode assignment) {
                String name = assignment.name();
                SourceLocation loc = assignment.sourceLocation();
                if (properties.containsKey(name) || blockGroups.containsKey(name)) {
                    SourceLocation prev = firstSeen.get(name);
                    throw new LarveySemanticException("Duplicate property '" + fullPath(path, name) + "'", fullPath(path, name), loc.line(), loc.column());
                }
                properties.put(name, toValue(assignment.value(), fullPath(path, name)));
                firstSeen.put(name, loc);
            } else if (member instanceof BlockNode block) {
                blockGroups.computeIfAbsent(block.name(), k -> new ArrayList<>()).add(block);
                firstSeen.putIfAbsent(block.name(), block.sourceLocation());
            } else {
                SourceLocation loc = member.sourceLocation();
                throw new LarveySemanticException("Invalid member", path, loc.line(), loc.column());
            }
        }
        Map<String, Configuration> blocks = new LinkedHashMap<>();
        for (Map.Entry<String, List<AstNode>> entry : blockGroups.entrySet()) {
            String name = entry.getKey();
            if (properties.containsKey(name)) {
                SourceLocation loc = firstSeen.get(name);
                throw new LarveySemanticException("Duplicate property '" + fullPath(path, name) + "'", fullPath(path, name), loc.line(), loc.column());
            }
            List<AstNode> merged = new ArrayList<>();
            for (AstNode node : entry.getValue()) {
                merged.addAll(((BlockNode) node).members());
            }
            BlockNode first = (BlockNode) entry.getValue().get(0);
            blocks.put(name, fromMembers(merged, first.sourceLocation(), fullPath(path, name)));
        }
        return new Configuration(properties, blocks, location, path);
    }

    private static LarveyValue toValue(ValueNode node, String path) {
        if (node instanceof StringNode stringNode) {
            return new LarveyValue.StringValue(stringNode.value(), stringNode.sourceLocation());
        }
        if (node instanceof IntegerNode integerNode) {
            return new LarveyValue.IntegerValue(integerNode.value(), integerNode.sourceLocation());
        }
        if (node instanceof DecimalNode decimalNode) {
            return new LarveyValue.DecimalValue(decimalNode.value(), decimalNode.sourceLocation());
        }
        if (node instanceof BooleanNode booleanNode) {
            return new LarveyValue.BooleanValue(booleanNode.value(), booleanNode.sourceLocation());
        }
        if (node instanceof NullNode nullNode) {
            return new LarveyValue.NullValue(nullNode.sourceLocation());
        }
        if (node instanceof ArrayNode arrayNode) {
            List<LarveyValue> elements = new ArrayList<>();
            for (ValueNode element : arrayNode.elements()) {
                elements.add(toValue(element, path));
            }
            return new LarveyValue.ArrayValue(elements, arrayNode.sourceLocation());
        }
        if (node instanceof ObjectNode objectNode) {
            Map<String, LarveyValue> props = new LinkedHashMap<>();
            for (AssignmentNode prop : objectNode.properties()) {
                if (props.containsKey(prop.name())) {
                    SourceLocation loc = prop.sourceLocation();
                    throw new LarveySemanticException("Duplicate property '" + fullPath(path, prop.name()) + "'", fullPath(path, prop.name()), loc.line(), loc.column());
                }
                props.put(prop.name(), toValue(prop.value(), fullPath(path, prop.name())));
            }
            return new LarveyValue.ObjectValue(props, objectNode.sourceLocation());
        }
        if (node instanceof FunctionCallNode functionCallNode) {
            List<LarveyValue> args = new ArrayList<>();
            for (ValueNode arg : functionCallNode.arguments()) {
                args.add(toValue(arg, path));
            }
            return new LarveyValue.FunctionCallValue(functionCallNode.name(), args, functionCallNode.sourceLocation());
        }
        SourceLocation loc = node.sourceLocation();
        throw new LarveySemanticException("Invalid value type", path, loc.line(), loc.column());
    }

    private static String fullPath(String parent, String name) {
        return parent.isEmpty() ? name : parent + "." + name;
    }

    public Map<String, LarveyValue> properties() {
        return properties;
    }

    public Map<String, Configuration> blocks() {
        return blocks;
    }

    public SourceLocation location() {
        return location;
    }

    public String path() {
        return path;
    }

    public boolean hasProperty(String name) {
        return properties.containsKey(name);
    }

    public boolean hasBlock(String name) {
        return blocks.containsKey(name);
    }

    public Set<String> propertyNames() {
        return properties.keySet();
    }

    public Set<String> blockNames() {
        return blocks.keySet();
    }

    public Optional<LarveyValue> getProperty(String name) {
        return Optional.ofNullable(properties.get(name));
    }

    public Optional<Configuration> getBlock(String name) {
        return Optional.ofNullable(blocks.get(name));
    }

    public Optional<LarveyValue> getByPath(String dottedPath) {
        String[] parts = dottedPath.split("\\.", -1);
        Configuration current = this;
        for (int i = 0; i < parts.length - 1; i++) {
            current = current.blocks.get(parts[i]);
            if (current == null) {
                return Optional.empty();
            }
        }
        String last = parts[parts.length - 1];
        LarveyValue value = current.properties.get(last);
        if (value != null) {
            return Optional.of(value);
        }
        Configuration block = current.blocks.get(last);
        if (block == null) {
            return Optional.empty();
        }
        return Optional.of(new LarveyValue.ObjectValue(new LinkedHashMap<>(), block.location));
    }

    public Optional<Configuration> getBlockByPath(String dottedPath) {
        String[] parts = dottedPath.split("\\.", -1);
        Configuration current = this;
        for (String part : parts) {
            current = current.blocks.get(part);
            if (current == null) {
                return Optional.empty();
            }
        }
        return Optional.of(current);
    }
}
