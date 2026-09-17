package com.habbashx.larvey.runtime;

import com.habbashx.larvey.ast.ConfigurationNode;
import com.habbashx.larvey.function.FunctionRegistry;
import com.habbashx.larvey.semantic.Configuration;
import com.habbashx.larvey.semantic.LarveyValue;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ConfigurationResolver {
    private final FunctionRegistry functions;

    public ConfigurationResolver(FunctionRegistry functions) {
        this.functions = functions;
    }

    public Configuration resolve(ConfigurationNode ast) {
        Configuration raw = Configuration.from(ast);
        Configuration withFunctions = resolveFunctions(raw, raw, "");
        return resolveInterpolation(withFunctions, withFunctions);
    }

    private Configuration resolveFunctions(Configuration node, Configuration root, String path) {
        Map<String, LarveyValue> props = new LinkedHashMap<>();
        for (Map.Entry<String, LarveyValue> entry : node.properties().entrySet()) {
            props.put(entry.getKey(), resolveValue(entry.getValue(), root, join(path, entry.getKey())));
        }
        List<com.habbashx.larvey.ast.AstNode> members = new ArrayList<>();
        for (Map.Entry<String, LarveyValue> entry : props.entrySet()) {
            members.add(new com.habbashx.larvey.ast.AssignmentNode(entry.getKey(), toValueNode(entry.getValue()), entry.getValue().location()));
        }
        for (Map.Entry<String, Configuration> entry : node.blocks().entrySet()) {
            Configuration resolved = resolveFunctions(entry.getValue(), root, join(path, entry.getKey()));
            for (com.habbashx.larvey.ast.AstNode m : toMembers(resolved)) {
                members.add(m);
            }
            members.add(blockMarker(entry.getKey(), resolved, members));
            members.remove(members.size() - 1);
        }
        return rebuild(props, node.blocks(), root, path, node);
    }

    private Configuration rebuild(Map<String, LarveyValue> props, Map<String, Configuration> originalBlocks, Configuration root, String path, Configuration node) {
        try {
            Constructor<Configuration> c = Configuration.class.getDeclaredConstructor(Map.class, Map.class, com.habbashx.larvey.ast.SourceLocation.class, String.class);
            c.setAccessible(true);
            Map<String, Configuration> blocks = new LinkedHashMap<>();
            for (Map.Entry<String, Configuration> entry : originalBlocks.entrySet()) {
                blocks.put(entry.getKey(), resolveFunctions(entry.getValue(), root, join(path, entry.getKey())));
            }
            return c.newInstance(props, blocks, node.location(), node.path());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private List<com.habbashx.larvey.ast.AstNode> toMembers(Configuration config) {
        List<com.habbashx.larvey.ast.AstNode> members = new ArrayList<>();
        for (Map.Entry<String, LarveyValue> entry : config.properties().entrySet()) {
            members.add(new com.habbashx.larvey.ast.AssignmentNode(entry.getKey(), toValueNode(entry.getValue()), entry.getValue().location()));
        }
        for (Map.Entry<String, Configuration> entry : config.blocks().entrySet()) {
            members.add(new com.habbashx.larvey.ast.BlockNode(entry.getKey(), toMembers(entry.getValue()), entry.getValue().location()));
        }
        return members;
    }

    private com.habbashx.larvey.ast.AstNode blockMarker(String name, Configuration resolved, List<com.habbashx.larvey.ast.AstNode> members) {
        return new com.habbashx.larvey.ast.BlockNode(name, toMembers(resolved), resolved.location());
    }

    private LarveyValue resolveValue(LarveyValue value, Configuration root, String path) {
        if (value instanceof LarveyValue.FunctionCallValue fn) {
            List<LarveyValue> args = new ArrayList<>();
            for (LarveyValue arg : fn.arguments()) {
                args.add(resolveValue(arg, root, path));
            }
            LarveyValue result = functions.invoke(fn.name(), args, fn.location(), null, path);
            return resolveValue(result, root, path);
        }
        if (value instanceof LarveyValue.ArrayValue arrayValue) {
            List<LarveyValue> elements = new ArrayList<>();
            for (LarveyValue element : arrayValue.elements()) {
                elements.add(resolveValue(element, root, path));
            }
            return new LarveyValue.ArrayValue(elements, arrayValue.location());
        }
        if (value instanceof LarveyValue.ObjectValue objectValue) {
            Map<String, LarveyValue> props = new LinkedHashMap<>();
            for (Map.Entry<String, LarveyValue> entry : objectValue.properties().entrySet()) {
                props.put(entry.getKey(), resolveValue(entry.getValue(), root, join(path, entry.getKey())));
            }
            return new LarveyValue.ObjectValue(props, objectValue.location());
        }
        return value;
    }

    private Configuration resolveInterpolation(Configuration node, Configuration root) {
        Map<String, LarveyValue> props = new LinkedHashMap<>();
        for (Map.Entry<String, LarveyValue> entry : node.properties().entrySet()) {
            props.put(entry.getKey(), interpolate(entry.getValue(), node, root, join(node.path(), entry.getKey())));
        }
        Map<String, Configuration> blocks = new LinkedHashMap<>();
        for (Map.Entry<String, Configuration> entry : node.blocks().entrySet()) {
            blocks.put(entry.getKey(), resolveInterpolation(entry.getValue(), root));
        }
        try {
            Constructor<Configuration> c = Configuration.class.getDeclaredConstructor(Map.class, Map.class, com.habbashx.larvey.ast.SourceLocation.class, String.class);
            c.setAccessible(true);
            return c.newInstance(props, blocks, node.location(), node.path());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private LarveyValue interpolate(LarveyValue value, Configuration scope, Configuration root, String path) {
        if (value instanceof LarveyValue.StringValue stringValue) {
            String raw = stringValue.value();
            if (!raw.contains("${")) {
                return value;
            }
            String resolved = interpolateString(raw, scope, root, path, 0);
            return new LarveyValue.StringValue(resolved, stringValue.location());
        }
        if (value instanceof LarveyValue.ArrayValue arrayValue) {
            List<LarveyValue> elements = new ArrayList<>();
            for (LarveyValue element : arrayValue.elements()) {
                elements.add(interpolate(element, scope, root, path));
            }
            return new LarveyValue.ArrayValue(elements, arrayValue.location());
        }
        if (value instanceof LarveyValue.ObjectValue objectValue) {
            Map<String, LarveyValue> props = new LinkedHashMap<>();
            for (Map.Entry<String, LarveyValue> entry : objectValue.properties().entrySet()) {
                props.put(entry.getKey(), interpolate(entry.getValue(), scope, root, join(path, entry.getKey())));
            }
            return new LarveyValue.ObjectValue(props, objectValue.location());
        }
        return value;
    }

    private String interpolateString(String raw, Configuration scope, Configuration root, String path, int depth) {
        if (depth > 10) {
            return raw;
        }
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < raw.length()) {
            int start = raw.indexOf("${", i);
            if (start < 0) {
                out.append(raw.substring(i));
                break;
            }
            out.append(raw, i, start);
            int end = raw.indexOf('}', start + 2);
            if (end < 0) {
                out.append(raw.substring(start));
                break;
            }
            String key = raw.substring(start + 2, end).trim();
            out.append(lookup(key, scope, root));
            i = end + 1;
        }
        String result = out.toString();
        if (result.contains("${") && !result.equals(raw)) {
            return interpolateString(result, scope, root, path, depth + 1);
        }
        return result;
    }

    private String lookup(String key, Configuration scope, Configuration root) {
        java.util.Optional<LarveyValue> found = scope.getProperty(key);
        if (found.isPresent()) {
            return display(found.get());
        }
        found = scope.getByPath(key);
        if (found.isPresent() && !(found.get() instanceof LarveyValue.ObjectValue)) {
            return display(found.get());
        }
        java.util.Optional<LarveyValue> fromRoot = root.getByPath(key);
        if (fromRoot.isPresent() && !(fromRoot.get() instanceof LarveyValue.ObjectValue)) {
            return display(fromRoot.get());
        }
        return "${" + key + "}";
    }

    private String display(LarveyValue value) {
        if (value instanceof LarveyValue.StringValue stringValue) {
            return stringValue.value();
        }
        if (value instanceof LarveyValue.IntegerValue integerValue) {
            return Long.toString(integerValue.value());
        }
        if (value instanceof LarveyValue.DecimalValue decimalValue) {
            return Double.toString(decimalValue.value());
        }
        if (value instanceof LarveyValue.BooleanValue booleanValue) {
            return Boolean.toString(booleanValue.value());
        }
        if (value instanceof LarveyValue.NullValue) {
            return "";
        }
        return "";
    }

    private com.habbashx.larvey.ast.ValueNode toValueNode(LarveyValue value) {
        if (value instanceof LarveyValue.StringValue stringValue) {
            return new com.habbashx.larvey.ast.StringNode(stringValue.value(), stringValue.location());
        }
        if (value instanceof LarveyValue.IntegerValue integerValue) {
            return new com.habbashx.larvey.ast.IntegerNode(integerValue.value(), integerValue.location());
        }
        if (value instanceof LarveyValue.DecimalValue decimalValue) {
            return new com.habbashx.larvey.ast.DecimalNode(decimalValue.value(), decimalValue.location());
        }
        if (value instanceof LarveyValue.BooleanValue booleanValue) {
            return new com.habbashx.larvey.ast.BooleanNode(booleanValue.value(), booleanValue.location());
        }
        if (value instanceof LarveyValue.NullValue nullValue) {
            return new com.habbashx.larvey.ast.NullNode(nullValue.location());
        }
        if (value instanceof LarveyValue.ArrayValue arrayValue) {
            List<com.habbashx.larvey.ast.ValueNode> elements = new ArrayList<>();
            for (LarveyValue element : arrayValue.elements()) {
                elements.add(toValueNode(element));
            }
            return new com.habbashx.larvey.ast.ArrayNode(elements, arrayValue.location());
        }
        if (value instanceof LarveyValue.ObjectValue objectValue) {
            List<com.habbashx.larvey.ast.AssignmentNode> props = new ArrayList<>();
            for (Map.Entry<String, LarveyValue> entry : objectValue.properties().entrySet()) {
                props.add(new com.habbashx.larvey.ast.AssignmentNode(entry.getKey(), toValueNode(entry.getValue()), entry.getValue().location()));
            }
            return new com.habbashx.larvey.ast.ObjectNode(props, objectValue.location());
        }
        if (value instanceof LarveyValue.FunctionCallValue fn) {
            List<com.habbashx.larvey.ast.ValueNode> args = new ArrayList<>();
            for (LarveyValue arg : fn.arguments()) {
                args.add(toValueNode(arg));
            }
            return new com.habbashx.larvey.ast.FunctionCallNode(fn.name(), args, fn.location());
        }
        throw new IllegalStateException("Unknown value");
    }

    private String join(String parent, String name) {
        return parent == null || parent.isEmpty() ? name : parent + "." + name;
    }
}
