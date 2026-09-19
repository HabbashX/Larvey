package com.habbashx.larvey.runtime;

import com.habbashx.larvey.ast.ConfigurationNode;
import com.habbashx.larvey.function.FunctionContext;
import com.habbashx.larvey.function.FunctionRegistry;
import com.habbashx.larvey.semantic.Configuration;
import com.habbashx.larvey.semantic.LarveyValue;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ConfigurationResolver {
    private static final int MAX_DEPTH = 50;
    private final FunctionRegistry functions;

    public ConfigurationResolver(FunctionRegistry functions) {
        this.functions = functions;
    }

    public Configuration resolve(ConfigurationNode ast) {
        Configuration raw = Configuration.from(ast);
        Configuration withFunctions = resolveFunctions(raw, raw, "", 0);
        return resolveInterpolation(withFunctions, withFunctions);
    }

    private Configuration resolveFunctions(Configuration node, Configuration root, String path, int depth) {
        if (depth > MAX_DEPTH) {
            throw new com.habbashx.larvey.exception.LarveySemanticException("Cyclic function or property reference", path, node.location().line(), node.location().column());
        }
        Map<String, LarveyValue> props = new LinkedHashMap<>();
        for (Map.Entry<String, LarveyValue> entry : node.properties().entrySet()) {
            props.put(entry.getKey(), resolveValue(entry.getValue(), root, join(path, entry.getKey()), depth));
        }
        Map<String, Configuration> blocks = new LinkedHashMap<>();
        for (Map.Entry<String, Configuration> entry : node.blocks().entrySet()) {
            blocks.put(entry.getKey(), resolveFunctions(entry.getValue(), root, join(path, entry.getKey()), depth + 1));
        }
        try {
            Constructor<Configuration> c = Configuration.class.getDeclaredConstructor(Map.class, Map.class, com.habbashx.larvey.ast.SourceLocation.class, String.class);
            c.setAccessible(true);
            return c.newInstance(props, blocks, node.location(), node.path());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private LarveyValue resolveValue(LarveyValue value, Configuration root, String path, int depth) {
        if (depth > MAX_DEPTH) {
            throw new com.habbashx.larvey.exception.LarveySemanticException("Cyclic function or property reference", path, value.location().line(), value.location().column());
        }
        if (value instanceof LarveyValue.FunctionCallValue fn) {
            List<LarveyValue> args = new ArrayList<>();
            for (LarveyValue arg : fn.arguments()) {
                args.add(resolveValue(arg, root, path, depth + 1));
            }
            LarveyValue result = functions.invoke(fn.name(), args, new FunctionContext(root, path, fn.location()), null);
            return resolveValue(result, root, path, depth + 1);
        }
        if (value instanceof LarveyValue.ArrayValue arrayValue) {
            List<LarveyValue> elements = new ArrayList<>();
            for (LarveyValue element : arrayValue.elements()) {
                elements.add(resolveValue(element, root, path, depth + 1));
            }
            return new LarveyValue.ArrayValue(elements, arrayValue.location());
        }
        if (value instanceof LarveyValue.ObjectValue objectValue) {
            Map<String, LarveyValue> props = new LinkedHashMap<>();
            for (Map.Entry<String, LarveyValue> entry : objectValue.properties().entrySet()) {
                props.put(entry.getKey(), resolveValue(entry.getValue(), root, join(path, entry.getKey()), depth + 1));
            }
            return new LarveyValue.ObjectValue(props, objectValue.location());
        }
        return value;
    }

    private Configuration resolveInterpolation(Configuration node, Configuration root) {
        Map<String, LarveyValue> props = new LinkedHashMap<>();
        for (Map.Entry<String, LarveyValue> entry : node.properties().entrySet()) {
            props.put(entry.getKey(), interpolate(entry.getValue(), node, root, join(node.path(), entry.getKey()), 0));
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

    private LarveyValue interpolate(LarveyValue value, Configuration scope, Configuration root, String path, int depth) {
        if (value instanceof LarveyValue.InterpolatedValue interpolated) {
            String resolved = interpolateParts(interpolated.parts(), scope, root, path, depth);
            return new LarveyValue.StringValue(resolved, interpolated.location());
        }
        if (value instanceof LarveyValue.StringValue stringValue) {
            String raw = stringValue.value();
            if (!raw.contains("${")) {
                return value;
            }
            String resolved = interpolateString(raw, scope, root, path, depth);
            return new LarveyValue.StringValue(resolved, stringValue.location());
        }
        if (value instanceof LarveyValue.ArrayValue arrayValue) {
            List<LarveyValue> elements = new ArrayList<>();
            for (LarveyValue element : arrayValue.elements()) {
                elements.add(interpolate(element, scope, root, path, depth));
            }
            return new LarveyValue.ArrayValue(elements, arrayValue.location());
        }
        if (value instanceof LarveyValue.ObjectValue objectValue) {
            Map<String, LarveyValue> props = new LinkedHashMap<>();
            for (Map.Entry<String, LarveyValue> entry : objectValue.properties().entrySet()) {
                props.put(entry.getKey(), interpolate(entry.getValue(), scope, root, join(path, entry.getKey()), depth));
            }
            return new LarveyValue.ObjectValue(props, objectValue.location());
        }
        return value;
    }

    private String interpolateParts(List<LarveyValue.InterpolatedValue.Part> parts, Configuration scope, Configuration root, String path, int depth) {
        StringBuilder out = new StringBuilder();
        for (LarveyValue.InterpolatedValue.Part part : parts) {
            if (part instanceof LarveyValue.InterpolatedValue.Text text) {
                out.append(text.text());
            } else if (part instanceof LarveyValue.InterpolatedValue.Expression expression) {
                out.append(lookup(expression.expression(), scope, root, path, depth));
            }
        }
        return out.toString();
    }

    private String interpolateString(String raw, Configuration scope, Configuration root, String path, int depth) {
        if (depth > MAX_DEPTH) {
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
            out.append(lookup(key, scope, root, path, depth));
            i = end + 1;
        }
        String result = out.toString();
        if (result.contains("${") && !result.equals(raw)) {
            return interpolateString(result, scope, root, path, depth + 1);
        }
        return result;
    }

    private String lookup(String key, Configuration scope, Configuration root, String path, int depth) {
        LarveyValue found = find(key, scope, root);
        if (found == null) {
            return "${" + key + "}";
        }
        if (found instanceof LarveyValue.InterpolatedValue interpolated) {
            return interpolateParts(interpolated.parts(), scope, root, path, depth + 1);
        }
        if (found instanceof LarveyValue.StringValue stringValue && stringValue.value().contains("${")) {
            return interpolateString(stringValue.value(), scope, root, path, depth + 1);
        }
        return display(found);
    }

    private LarveyValue find(String key, Configuration scope, Configuration root) {
        java.util.Optional<LarveyValue> found = scope.getProperty(key);
        if (found.isPresent()) {
            return unwrap(found.get());
        }
        found = scope.getByPath(key);
        if (found.isPresent() && unwrap(found.get()) != null) {
            return unwrap(found.get());
        }
        java.util.Optional<LarveyValue> fromRoot = root.getByPath(key);
        if (fromRoot.isPresent()) {
            return unwrap(fromRoot.get());
        }
        return null;
    }

    private LarveyValue unwrap(LarveyValue value) {
        if (value instanceof LarveyValue.ObjectValue objectValue && objectValue.properties().isEmpty()) {
            return null;
        }
        return value;
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
        if (value instanceof LarveyValue.InterpolatedValue interpolated) {
            return interpolated.raw();
        }
        if (value instanceof LarveyValue.NullValue) {
            return "";
        }
        return "";
    }

    private String join(String parent, String name) {
        return parent == null || parent.isEmpty() ? name : parent + "." + name;
    }
}
