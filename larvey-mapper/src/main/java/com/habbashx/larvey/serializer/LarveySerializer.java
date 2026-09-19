package com.habbashx.larvey.serializer;

import com.habbashx.larvey.annotations.LarveyIgnore;
import com.habbashx.larvey.annotations.LarveyProperty;
import com.habbashx.larvey.exception.LarveySerializationException;
import com.habbashx.larvey.metadata.ClassMetadata;
import com.habbashx.larvey.metadata.PropertyMetadata;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public final class LarveySerializer {
    public String serialize(Object value) {
        if (value == null) {
            throw new LarveySerializationException("Cannot serialize null root");
        }
        StringBuilder out = new StringBuilder();
        writeValue(value, null, out, 0, true);
        return out.toString();
    }

    private void writeValue(Object value, String name, StringBuilder out, int indent, boolean isRoot) {
        if (value == null) {
            emitKey(name, out, indent);
            out.append("null\n");
            return;
        }
        if (value instanceof String stringValue) {
            emitKey(name, out, indent);
            out.append(quote(stringValue)).append("\n");
            return;
        }
        if (value instanceof Number || value instanceof Boolean) {
            emitKey(name, out, indent);
            out.append(value).append("\n");
            return;
        }
        if (value instanceof Character character) {
            emitKey(name, out, indent);
            out.append(quote(character.toString())).append("\n");
            return;
        }
        if (value instanceof Enum<?> enumValue) {
            emitKey(name, out, indent);
            out.append(quote(enumValue.name())).append("\n");
            return;
        }
        if (value instanceof java.util.Optional<?> optional) {
            if (optional.isEmpty()) {
                if (!isRoot) {
                    emitKey(name, out, indent);
                    out.append("null\n");
                }
                return;
            }
            writeValue(optional.get(), name, out, indent, isRoot);
            return;
        }
        if (value instanceof Collection<?> collection) {
            emitKey(name, out, indent);
            out.append("[");
            boolean first = true;
            for (Object element : collection) {
                if (!first) {
                    out.append(", ");
                }
                out.append(inline(element));
                first = false;
            }
            out.append("]\n");
            return;
        }
        if (value != null && value.getClass().isArray()) {
            emitKey(name, out, indent);
            out.append("[");
            int len = Array.getLength(value);
            for (int i = 0; i < len; i++) {
                if (i > 0) {
                    out.append(", ");
                }
                out.append(inline(Array.get(value, i)));
            }
            out.append("]\n");
            return;
        }
        if (value instanceof Map<?, ?> map) {
            if (isRoot) {
                Map<String, Object> sorted = new TreeMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    sorted.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                for (Map.Entry<String, Object> entry : sorted.entrySet()) {
                    writeValue(entry.getValue(), entry.getKey(), out, indent, false);
                }
                return;
            }
            if (isSimpleMap(map)) {
                emitKey(name, out, indent);
                out.append("{\n");
                Map<String, Object> sorted = new TreeMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    sorted.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                for (Map.Entry<String, Object> entry : sorted.entrySet()) {
                    writeValue(entry.getValue(), entry.getKey(), out, indent + 1, false);
                }
                indent(out, indent);
                out.append("}\n");
                return;
            }
            if (name != null) {
                indent(out, indent);
                out.append(name).append(" {\n");
                writeMapEntries(map, out, indent + 1);
                indent(out, indent);
                out.append("}\n");
                return;
            }
            writeMapEntries(map, out, indent);
            return;
        }
        if (isRoot || name != null) {
            if (!isRoot) {
                indent(out, indent);
                out.append(name).append(" {\n");
            }
            writeBean(value, out, isRoot ? indent : indent + 1);
            if (!isRoot) {
                indent(out, indent);
                out.append("}\n");
            }
            return;
        }
        writeBean(value, out, indent);
    }

    private void writeBean(Object bean, StringBuilder out, int indent) {
        ClassMetadata<?> metadata = ClassMetadata.of(bean.getClass());
        for (PropertyMetadata property : metadata.properties()) {
            Object fieldValue = readProperty(bean, property);
            if (fieldValue == null) {
                continue;
            }
            if (fieldValue instanceof java.util.Optional<?> optional && optional.isEmpty()) {
                continue;
            }
            if (isBean(fieldValue)) {
                writeValue(fieldValue, property.name(), out, indent, false);
            } else {
                writeValue(fieldValue, property.name(), out, indent, false);
            }
        }
    }

    private Object readProperty(Object bean, PropertyMetadata property) {
        try {
            if (property.getter() != null) {
                return property.getter().invoke(bean);
            }
            if (property.field() != null) {
                return property.field().get(bean);
            }
            if (property.recordComponent() != null) {
                return property.recordComponent().getAccessor().invoke(bean);
            }
            return null;
        } catch (Exception e) {
            throw new LarveySerializationException("Cannot read property '" + property.name() + "'", e);
        }
    }

    private void writeMapEntries(Map<?, ?> map, StringBuilder out, int indent) {
        Map<String, Object> sorted = new TreeMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            sorted.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        for (Map.Entry<String, Object> entry : sorted.entrySet()) {
            writeValue(entry.getValue(), entry.getKey(), out, indent, false);
        }
    }

    private boolean isBean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Character || value instanceof Enum || value instanceof Collection || value instanceof Map || value instanceof java.util.Optional) {
            return false;
        }
        return !value.getClass().isArray();
    }

    private boolean isSimpleMap(Map<?, ?> map) {
        for (Object v : map.values()) {
            if (v != null && !(v instanceof String) && !(v instanceof Number) && !(v instanceof Boolean) && !(v instanceof Character) && !(v instanceof Enum) && !(v instanceof Collection) && v.getClass().isArray()) {
                return false;
            }
        }
        return true;
    }

    private String inline(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String stringValue) {
            return quote(stringValue);
        }
        if (value instanceof Enum<?> enumValue) {
            return quote(enumValue.name());
        }
        if (value instanceof Character character) {
            return quote(character.toString());
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof java.util.Optional<?> optional) {
            return optional.map(this::inline).orElse("null");
        }
        return quote(String.valueOf(value));
    }

    private void emitKey(String name, StringBuilder out, int indent) {
        if (name != null) {
            indent(out, indent);
            out.append(name).append(" = ");
        } else {
            indent(out, indent);
        }
    }

    private void indent(StringBuilder out, int level) {
        for (int i = 0; i < level; i++) {
            out.append("    ");
        }
    }

    private String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r") + "\"";
    }
}
