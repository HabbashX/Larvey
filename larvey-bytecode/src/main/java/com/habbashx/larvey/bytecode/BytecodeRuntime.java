package com.habbashx.larvey.bytecode;

import com.habbashx.larvey.exception.LarveyMappingException;
import com.habbashx.larvey.mapper.ReflectionMapper;
import com.habbashx.larvey.metadata.ClassMetadata;
import com.habbashx.larvey.semantic.Configuration;
import com.habbashx.larvey.semantic.LarveyValue;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class BytecodeRuntime {
    public static final int KIND_STRING = 0;
    public static final int KIND_BOOLEAN = 1;
    public static final int KIND_INT = 2;
    public static final int KIND_LONG = 3;
    public static final int KIND_DOUBLE = 4;
    public static final int KIND_BEAN = 5;
    public static final int KIND_LIST = 6;
    public static final int KIND_SET = 7;
    public static final int KIND_ARRAY = 8;
    public static final int KIND_MAP = 9;
    public static final int KIND_OPTIONAL = 10;
    public static final int KIND_OTHER = 11;

    private static final ReflectionMapper DELEGATE = new ReflectionMapper();
    private static final MapperCache CACHE = new MapperCache();

    private BytecodeRuntime() {
    }

    public static String joinFull(String parent, String name) {
        return parent == null || parent.isEmpty() ? name : parent + "." + name;
    }

    public static Class<?> loadClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new com.habbashx.larvey.exception.LarveyBytecodeException("Cannot load class '" + name + "'", e);
        }
    }

    public static Optional<LarveyValue> prop(Configuration config, String name, String[] aliases) {
        Optional<LarveyValue> direct = config.getProperty(name);
        if (direct.isPresent()) {
            return direct;
        }
        if (aliases != null) {
            for (String alias : aliases) {
                Optional<LarveyValue> found = config.getProperty(alias);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<Configuration> block(Configuration config, String name, String[] aliases) {
        Optional<Configuration> direct = config.getBlock(name);
        if (direct.isPresent()) {
            return direct;
        }
        if (aliases != null) {
            for (String alias : aliases) {
                Optional<Configuration> found = config.getBlock(alias);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }

    public static Object mapOrGenerate(Configuration config, Class<?> type) {
        ClassMetadata<?> metadata = ClassMetadata.of(type);
        if (!metadata.root().isEmpty()) {
            return DELEGATE.map(config, type);
        }
        try {
            GeneratedMapper<?> existing = CACHE.getRaw(type);
            if (existing == null) {
                existing = create(type);
                CACHE.putRaw(type, existing);
            }
            return existing.map(config);
        } catch (com.habbashx.larvey.exception.LarveyBytecodeException e) {
            return DELEGATE.map(config, type);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static GeneratedMapper<?> create(Class<?> type) {
        try {
            Class<? extends GeneratedMapper<?>> clazz = BytecodeGenerator.generate((Class) type);
            return clazz.getDeclaredConstructor(Class.class).newInstance(type);
        } catch (com.habbashx.larvey.exception.LarveyBytecodeException e) {
            throw e;
        } catch (Exception e) {
            throw new com.habbashx.larvey.exception.LarveyBytecodeException("Cannot instantiate generated mapper for " + type.getName(), e);
        }
    }

    public static Object beanFromValue(LarveyValue value, Class<?> beanClass, String path, Class<?> owner) {
        if (value instanceof LarveyValue.ObjectValue objectValue) {
            if (ClassMetadata.of(beanClass).root().isEmpty()) {
                return mapOrGenerate(Configuration.of(objectValue.properties(), Map.of(), objectValue.location(), path), beanClass);
            }
            return DELEGATE.convertExternal(value, beanClass, beanClass, path);
        }
        throw new LarveyMappingException("Cannot convert value to '" + beanClass.getSimpleName() + "'", path, owner);
    }

    public static Object convertOther(LarveyValue value, Class<?> target, String path, Class<?> owner) {
        if (target.isEnum() || target == java.util.UUID.class || target == java.nio.file.Path.class
                || target == java.net.URI.class || target == java.time.Duration.class
                || target == java.math.BigInteger.class || target == java.math.BigDecimal.class
                || target == Character.class || target == char.class || target == Float.class || target == float.class
                || target == Byte.class || target == byte.class || target == Short.class || target == short.class) {
            return BytecodeConvert.toOther(value, target, path, owner);
        }
        return DELEGATE.convertExternal(value, target, target, path);
    }

    public static Object elemConvert(LarveyValue element, Class<?> elem, String path, Class<?> owner) {
        if (elem == String.class) {
            return BytecodeConvert.toString(element, path, owner);
        }
        if (elem == Boolean.class) {
            return BytecodeConvert.toBooleanBoxed(element, path, owner);
        }
        if (elem == Integer.class) {
            return BytecodeConvert.toIntBoxed(element, path, owner);
        }
        if (elem == Long.class) {
            return BytecodeConvert.toLongBoxed(element, path, owner);
        }
        if (elem == Double.class) {
            return BytecodeConvert.toDoubleBoxed(element, path, owner);
        }
        return DELEGATE.convertExternal(element, elem, elem, path);
    }

    public static List<Object> listFrom(LarveyValue value, Class<?> elem, String path, Class<?> owner) {
        if (!(value instanceof LarveyValue.ArrayValue arrayValue)) {
            throw new LarveyMappingException("Cannot convert value to List", path, owner);
        }
        List<Object> result = new ArrayList<>();
        List<LarveyValue> elements = arrayValue.elements();
        for (int i = 0; i < elements.size(); i++) {
            result.add(elemConvert(elements.get(i), elem, path + "[" + i + "]", owner));
        }
        return result;
    }

    public static Set<Object> setFrom(LarveyValue value, Class<?> elem, String path, Class<?> owner) {
        return new LinkedHashSet<>(listFrom(value, elem, path, owner));
    }

    public static Object arrayFrom(LarveyValue value, Class<?> component, String path, Class<?> owner) {
        if (!(value instanceof LarveyValue.ArrayValue arrayValue)) {
            throw new LarveyMappingException("Cannot convert value to array", path, owner);
        }
        List<LarveyValue> elements = arrayValue.elements();
        if (component == int.class) {
            int[] result = new int[elements.size()];
            for (int i = 0; i < elements.size(); i++) {
                result[i] = BytecodeConvert.toInt(elements.get(i), path + "[" + i + "]", owner);
            }
            return result;
        }
        if (component == long.class) {
            long[] result = new long[elements.size()];
            for (int i = 0; i < elements.size(); i++) {
                result[i] = BytecodeConvert.toLong(elements.get(i), path + "[" + i + "]", owner);
            }
            return result;
        }
        if (component == double.class) {
            double[] result = new double[elements.size()];
            for (int i = 0; i < elements.size(); i++) {
                result[i] = BytecodeConvert.toDouble(elements.get(i), path + "[" + i + "]", owner);
            }
            return result;
        }
        if (component == boolean.class) {
            boolean[] result = new boolean[elements.size()];
            for (int i = 0; i < elements.size(); i++) {
                result[i] = BytecodeConvert.toBoolean(elements.get(i), path + "[" + i + "]", owner);
            }
            return result;
        }
        Object result = Array.newInstance(component, elements.size());
        for (int i = 0; i < elements.size(); i++) {
            Array.set(result, i, elemConvert(elements.get(i), component, path + "[" + i + "]", owner));
        }
        return result;
    }

    public static Object keyConvert(String key, Class<?> keyClass, String path, Class<?> owner) {
        if (keyClass == String.class || keyClass == Object.class) {
            return key;
        }
        return DELEGATE.convertExternal(new LarveyValue.StringValue(key, new com.habbashx.larvey.ast.SourceLocation(0, 0)), keyClass, keyClass, path);
    }

    public static Map<Object, Object> mapFromValue(LarveyValue value, Class<?> keyClass, Class<?> valClass, String path, Class<?> owner) {
        if (!(value instanceof LarveyValue.ObjectValue objectValue)) {
            throw new LarveyMappingException("Cannot convert value to Map", path, owner);
        }
        Map<Object, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, LarveyValue> entry : objectValue.properties().entrySet()) {
            result.put(keyConvert(entry.getKey(), keyClass, path, owner), elemConvert(entry.getValue(), valClass, path + "." + entry.getKey(), owner));
        }
        return result;
    }

    public static Map<Object, Object> mapFromConfig(Configuration config, Class<?> keyClass, Class<?> valClass, String path, Class<?> owner) {
        Map<Object, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, LarveyValue> entry : config.properties().entrySet()) {
            result.put(keyConvert(entry.getKey(), keyClass, path, owner), elemConvert(entry.getValue(), valClass, path + "." + entry.getKey(), owner));
        }
        for (Map.Entry<String, Configuration> entry : config.blocks().entrySet()) {
            Object nested;
            if (valClass.getName().startsWith("java.") || valClass.isEnum()) {
                throw new LarveyMappingException("Cannot convert block to '" + valClass.getSimpleName() + "'", path + "." + entry.getKey(), owner);
            } else if (valClass == Object.class) {
                nested = com.habbashx.larvey.function.PropertyFunction.fromConfiguration(entry.getValue());
            } else {
                nested = mapOrGenerate(entry.getValue(), valClass);
            }
            result.put(keyConvert(entry.getKey(), keyClass, path, owner), nested);
        }
        return result;
    }

    public static Optional<Object> optionalFrom(Optional<LarveyValue> value, int elemKind, Class<?> elem, String path, Class<?> owner) {
        if (value.isEmpty()) {
            return Optional.empty();
        }
        LarveyValue present = value.get();
        Object converted = switch (elemKind) {
            case KIND_STRING -> BytecodeConvert.toString(present, path, owner);
            case KIND_BOOLEAN -> BytecodeConvert.toBooleanBoxed(present, path, owner);
            case KIND_INT -> BytecodeConvert.toIntBoxed(present, path, owner);
            case KIND_LONG -> BytecodeConvert.toLongBoxed(present, path, owner);
            case KIND_DOUBLE -> BytecodeConvert.toDoubleBoxed(present, path, owner);
            case KIND_BEAN -> beanFromValue(present, elem, path, owner);
            default -> convertOther(present, elem, path, owner);
        };
        return Optional.ofNullable(converted);
    }

    public static Object arg(Configuration config, String name, String[] aliases, int kind, Class<?> type, int elemKind, Class<?> elem, Class<?> elem2, String def, boolean required, Class<?> owner, String path) {
        Optional<LarveyValue> value = prop(config, name, aliases);
        if (value.isPresent()) {
            LarveyValue present = value.get();
            Object converted = switch (kind) {
                case KIND_STRING -> BytecodeConvert.toString(present, path, owner);
                case KIND_BOOLEAN -> BytecodeConvert.toBooleanBoxed(present, path, owner);
                case KIND_INT -> BytecodeConvert.toIntBoxed(present, path, owner);
                case KIND_LONG -> BytecodeConvert.toLongBoxed(present, path, owner);
                case KIND_DOUBLE -> BytecodeConvert.toDoubleBoxed(present, path, owner);
                case KIND_LIST -> listFrom(present, elem, path, owner);
                case KIND_SET -> setFrom(present, elem, path, owner);
                case KIND_ARRAY -> arrayFrom(present, elem, path, owner);
                case KIND_MAP -> mapFromValue(present, elem, elem2, path, owner);
                case KIND_OPTIONAL -> optionalFrom(value, elemKind, elem, path, owner);
                case KIND_BEAN -> beanFromValue(present, type, path, owner);
                default -> convertOther(present, type, path, owner);
            };
            if (converted == null && type.isPrimitive()) {
                return primitiveDefault(type);
            }
            return converted;
        }
        Optional<Configuration> blk = block(config, name, aliases);
        if (blk.isPresent()) {
            if (kind == KIND_BEAN) {
                return mapOrGenerate(blk.get(), type);
            }
            if (kind == KIND_MAP) {
                return mapFromConfig(blk.get(), elem, elem2, path, owner);
            }
            if (kind == KIND_OPTIONAL && elemKind == KIND_BEAN) {
                return Optional.of(mapOrGenerate(blk.get(), elem));
            }
            throw new LarveyMappingException("Cannot convert block to '" + type.getSimpleName() + "'", path, owner);
        }
        if (def != null) {
            return parseDefault(def, type, path, owner);
        }
        if (required) {
            throwMissing(path, owner, config.location().line(), config.location().column());
        }
        if (kind == KIND_OPTIONAL) {
            return Optional.empty();
        }
        if (type.isPrimitive()) {
            return primitiveDefault(type);
        }
        return null;
    }

    public static void throwMissing(String fullPath, Class<?> owner, int line, int column) {
        throw new LarveyMappingException("Missing required property '" + fullPath + "'", fullPath, owner, line, column);
    }

    public static Object parseDefault(String def, Class<?> type, String path, Class<?> owner) {
        if (type == String.class) {
            return def;
        }
        if (type == boolean.class || type == Boolean.class) {
            if (def.equalsIgnoreCase("true")) {
                return true;
            }
            if (def.equalsIgnoreCase("false")) {
                return false;
            }
            throw new LarveyMappingException("Cannot convert default '" + def + "' to boolean", path, owner);
        }
        try {
            if (type == int.class || type == Integer.class) {
                return Integer.parseInt(def.trim());
            }
            if (type == long.class || type == Long.class) {
                return Long.parseLong(def.trim());
            }
            if (type == double.class || type == Double.class) {
                return Double.parseDouble(def.trim());
            }
        } catch (NumberFormatException e) {
            throw new LarveyMappingException("Cannot convert default '" + def + "'", path, owner, e);
        }
        if (type.isEnum()) {
            try {
                @SuppressWarnings({"unchecked", "rawtypes"})
                Object constant = Enum.valueOf((Class<Enum>) type, def);
                return constant;
            } catch (IllegalArgumentException e) {
                for (Object constant : type.getEnumConstants()) {
                    if (((Enum<?>) constant).name().equalsIgnoreCase(def)) {
                        return constant;
                    }
                }
                throw new LarveyMappingException("Cannot convert default '" + def + "' to enum", path, owner, e);
            }
        }
        throw new LarveyMappingException("Unsupported default for '" + type.getSimpleName() + "'", path, owner);
    }

    static Object primitiveDefault(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0f;
        }
        if (type == double.class) {
            return 0d;
        }
        if (type == char.class) {
            return '\0';
        }
        return null;
    }
}
