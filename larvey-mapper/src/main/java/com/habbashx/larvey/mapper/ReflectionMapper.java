package com.habbashx.larvey.mapper;

import com.habbashx.larvey.ast.ConfigurationNode;
import com.habbashx.larvey.convert.LarveyConverter;
import com.habbashx.larvey.exception.LarveyMappingException;
import com.habbashx.larvey.metadata.ClassMetadata;
import com.habbashx.larvey.metadata.PropertyMetadata;
import com.habbashx.larvey.semantic.Configuration;
import com.habbashx.larvey.semantic.LarveyValue;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ReflectionMapper {
    private final ConcurrentHashMap<Class<?>, LarveyConverter<?, ?>> converterCache = new ConcurrentHashMap<>();

    public <T> T map(Configuration config, Class<T> type) {
        Configuration effective = config;
        ClassMetadata<T> metadata = ClassMetadata.of(type);
        if (!metadata.root().isEmpty()) {
            Optional<Configuration> block = config.getBlockByPath(metadata.root());
            if (block.isEmpty()) {
                throw new LarveyMappingException("Missing required block '" + metadata.root() + "'", metadata.root(), type);
            }
            effective = block.get();
        }
        return mapObject(effective, metadata, effective.path());
    }

    public <T> T map(ConfigurationNode node, Class<T> type) {
        return map(Configuration.from(node), type);
    }

    private <T> T mapObject(Configuration config, ClassMetadata<T> metadata, String path) {
        try {
            if (metadata.creator() != null) {
                Object[] args = new Object[metadata.creatorParameters().size()];
                for (ClassMetadata.CreatorParameter param : metadata.creatorParameters()) {
                    args[param.index()] = resolveCreatorArg(config, param, path, metadata.type());
                }
                Constructor<T> creator = metadata.creator();
                return creator.newInstance(args);
            }
            Constructor<T> noArg = metadata.noArgConstructor();
            if (noArg == null) {
                throw new LarveyMappingException("No suitable constructor for '" + metadata.type().getSimpleName() + "'", path, metadata.type());
            }
            T instance = noArg.newInstance();
            for (PropertyMetadata property : metadata.properties()) {
                Object value = resolveProperty(config, property, path, metadata.type());
                if (value == null && property.required()) {
                    throw new LarveyMappingException("Missing required property '" + join(path, property.name()) + "'", join(path, property.name()), metadata.type(), config.location().line(), config.location().column());
                }
                if (value == null) {
                    continue;
                }
                if (property.setter() != null) {
                    property.setter().invoke(instance, value);
                } else if (property.field() != null) {
                    property.field().set(instance, value);
                }
            }
            return instance;
        } catch (LarveyMappingException e) {
            throw e;
        } catch (Exception e) {
            throw new LarveyMappingException("Failed to construct '" + metadata.type().getSimpleName() + "': " + e.getMessage(), path, metadata.type(), e);
        }
    }

    private Object resolveCreatorArg(Configuration config, ClassMetadata.CreatorParameter param, String path, Class<?> owner) {
        String fullPath = join(path, param.name());
        Optional<LarveyValue> prop = findValue(config, param.name(), param.aliases());
        Optional<Configuration> block = findBlock(config, param.name(), param.aliases());
        if (prop.isPresent()) {
            try {
                Object converted = convert(prop.get(), param.type(), param.genericType(), fullPath, owner, param.converter(), param.format());
                return FormatValidator.validate(converted, param.type(), param.format(), fullPath, owner);
            } catch (LarveyMappingException e) {
                throw e.withLocation(prop.get().location().line(), prop.get().location().column());
            }
        }
        if (block.isPresent()) {
            return convertBlock(block.get(), param.type(), param.genericType(), fullPath, owner, param.converter());
        }
        if (param.defaultValue() != null) {
            return convertString(param.defaultValue(), param.type(), param.genericType(), fullPath, owner, param.converter());
        }
        if (param.required()) {
            throw new LarveyMappingException("Missing required property '" + fullPath + "'", fullPath, owner, config.location().line(), config.location().column());
        }
        if (param.type() == Optional.class) {
            return Optional.empty();
        }
        if (param.type().isPrimitive()) {
            return primitiveDefault(param.type());
        }
        return null;
    }

    private Object resolveProperty(Configuration config, PropertyMetadata property, String path, Class<?> owner) {
        String fullPath = join(path, property.name());
        Optional<LarveyValue> prop = findValue(config, property.name(), new ArrayList<>(property.aliases()));
        Optional<Configuration> block = findBlock(config, property.name(), new ArrayList<>(property.aliases()));
        if (prop.isPresent()) {
            try {
                Object converted = convert(prop.get(), property.type(), property.genericType(), fullPath, owner, property.converter(), property.format());
                return FormatValidator.validate(converted, property.type(), property.format(), fullPath, owner);
            } catch (LarveyMappingException e) {
                throw e.withLocation(prop.get().location().line(), prop.get().location().column());
            }
        }
        if (block.isPresent()) {
            return convertBlock(block.get(), property.type(), property.genericType(), fullPath, owner, property.converter());
        }
        if (property.defaultValue() != null) {
            return convertString(property.defaultValue(), property.type(), property.genericType(), fullPath, owner, property.converter());
        }
        if (property.type() == Optional.class) {
            return Optional.empty();
        }
        return null;
    }

    private Optional<LarveyValue> findValue(Configuration config, String name, List<String> aliases) {
        Optional<LarveyValue> direct = config.getProperty(name);
        if (direct.isPresent()) {
            return direct;
        }
        for (String alias : aliases) {
            Optional<LarveyValue> found = config.getProperty(alias);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private Optional<Configuration> findBlock(Configuration config, String name, List<String> aliases) {
        Optional<Configuration> direct = config.getBlock(name);
        if (direct.isPresent()) {
            return direct;
        }
        for (String alias : aliases) {
            Optional<Configuration> found = config.getBlock(alias);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    public Object convertExternal(LarveyValue value, Class<?> target, Type generic, String path) {
        return convert(value, target, generic, path, target, null, null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object convert(LarveyValue value, Class<?> target, Type generic, String path, Class<?> owner, Class<? extends LarveyConverter<?, ?>> converterClass, String format) {
        try {
            return convertInner(value, target, generic, path, owner, converterClass, format);
        } catch (LarveyMappingException e) {
            if (value.location() != null) {
                throw e.withLocation(value.location().line(), value.location().column());
            }
            throw e;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object convertInner(LarveyValue value, Class<?> target, Type generic, String path, Class<?> owner, Class<? extends LarveyConverter<?, ?>> converterClass, String format) {
        if (value instanceof LarveyValue.NullValue) {
            if (target == Optional.class) {
                return Optional.empty();
            }
            if (target.isPrimitive()) {
                return primitiveDefault(target);
            }
            return null;
        }
        if (converterClass != null) {
            LarveyConverter<?, ?> converter = converterCache.computeIfAbsent(converterClass, k -> {
                try {
                    return (LarveyConverter<?, ?>) k.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new LarveyMappingException("Cannot instantiate converter '" + k.getName() + "'", path, owner, e);
                }
            });
            Object raw = toRaw(value);
            return ((LarveyConverter<Object, Object>) converter).convert(raw);
        }
        if (target == Optional.class) {
            Class<?> inner = Object.class;
            Type innerType = Object.class;
            if (generic instanceof ParameterizedType parameterizedType && parameterizedType.getActualTypeArguments().length == 1) {
                innerType = parameterizedType.getActualTypeArguments()[0];
                inner = rawClass(innerType);
            }
            Object converted = convert(value, inner, innerType, path, owner, null, format);
            return Optional.ofNullable(converted);
        }
        if (value instanceof LarveyValue.StringValue stringValue) {
            return convertString(stringValue.value(), target, generic, path, owner, null);
        }
        if (value instanceof LarveyValue.IntegerValue integerValue) {
            return convertLong(integerValue.value(), target, path, owner);
        }
        if (value instanceof LarveyValue.DecimalValue decimalValue) {
            return convertDouble(decimalValue.value(), target, path, owner);
        }
        if (value instanceof LarveyValue.BooleanValue booleanValue) {
            return convertBoolean(booleanValue.value(), target, path, owner);
        }
        if (value instanceof LarveyValue.ArrayValue arrayValue) {
            return convertArray(arrayValue, target, generic, path, owner);
        }
        if (value instanceof LarveyValue.ObjectValue objectValue) {
            return convertObjectValue(objectValue, target, generic, path, owner);
        }
        if (value instanceof LarveyValue.FunctionCallValue functionCallValue) {
            throw new LarveyMappingException("Unresolved function '" + functionCallValue.name() + "'", path, owner);
        }
        if (value instanceof LarveyValue.InterpolatedValue interpolatedValue) {
            throw new LarveyMappingException("Unresolved interpolation '" + interpolatedValue.raw() + "'", path, owner);
        }
        throw new LarveyMappingException("Cannot convert value to '" + target.getSimpleName() + "'", path, owner);
    }

    private Object convertBlock(Configuration block, Class<?> target, Type generic, String path, Class<?> owner, Class<? extends LarveyConverter<?, ?>> converterClass) {
        if (converterClass != null) {
            throw new LarveyMappingException("Converter on nested block is not supported", path, owner);
        }
        if (target == Map.class || target.isAssignableFrom(LinkedHashMap.class)) {
            Type[] args = mapArgs(generic);
            Map<Object, Object> result = new LinkedHashMap<>();
            for (Map.Entry<String, LarveyValue> entry : block.properties().entrySet()) {
                result.put(convertKey(entry.getKey(), args[0], path, owner), convert(entry.getValue(), rawClass(args[1]), args[1], join(path, entry.getKey()), owner, null, null));
            }
            for (Map.Entry<String, Configuration> entry : block.blocks().entrySet()) {
                result.put(convertKey(entry.getKey(), args[0], path, owner), convertBlock(entry.getValue(), rawClass(args[1]), args[1], join(path, entry.getKey()), owner, null));
            }
            return result;
        }
        if (target == Object.class) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<String, LarveyValue> entry : block.properties().entrySet()) {
                result.put(entry.getKey(), toRaw(entry.getValue()));
            }
            return result;
        }
        return mapObject(block, ClassMetadata.of(target), path);
    }

    private Object convertObjectValue(LarveyValue.ObjectValue objectValue, Class<?> target, Type generic, String path, Class<?> owner) {
        if (target == Map.class || target.isAssignableFrom(LinkedHashMap.class)) {
            Type[] args = mapArgs(generic);
            Map<Object, Object> result = new LinkedHashMap<>();
            for (Map.Entry<String, LarveyValue> entry : objectValue.properties().entrySet()) {
                result.put(convertKey(entry.getKey(), args[0], path, owner), convert(entry.getValue(), rawClass(args[1]), args[1], join(path, entry.getKey()), owner, null, null));
            }
            return result;
        }
        if (target == Object.class) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<String, LarveyValue> entry : objectValue.properties().entrySet()) {
                result.put(entry.getKey(), toRaw(entry.getValue()));
            }
            return result;
        }
        Configuration synthetic = syntheticConfig(objectValue, path);
        return mapObject(synthetic, ClassMetadata.of(target), path);
    }

    private Configuration syntheticConfig(LarveyValue.ObjectValue objectValue, String path) {
        List<com.habbashx.larvey.ast.AstNode> members = new ArrayList<>();
        for (Map.Entry<String, LarveyValue> entry : objectValue.properties().entrySet()) {
            members.add(new com.habbashx.larvey.ast.AssignmentNode(entry.getKey(), rawToValueNode(entry.getValue()), entry.getValue().location()));
        }
        return Configuration.from(new ConfigurationNode(members, objectValue.location()));
    }

    private com.habbashx.larvey.ast.ValueNode rawToValueNode(LarveyValue value) {
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
                elements.add(rawToValueNode(element));
            }
            return new com.habbashx.larvey.ast.ArrayNode(elements, arrayValue.location());
        }
        if (value instanceof LarveyValue.ObjectValue objectValue) {
            List<com.habbashx.larvey.ast.AssignmentNode> props = new ArrayList<>();
            for (Map.Entry<String, LarveyValue> entry : objectValue.properties().entrySet()) {
                props.add(new com.habbashx.larvey.ast.AssignmentNode(entry.getKey(), rawToValueNode(entry.getValue()), entry.getValue().location()));
            }
            return new com.habbashx.larvey.ast.ObjectNode(props, objectValue.location());
        }
        if (value instanceof LarveyValue.FunctionCallValue functionCallValue) {
            List<com.habbashx.larvey.ast.ValueNode> args = new ArrayList<>();
            for (LarveyValue arg : functionCallValue.arguments()) {
                args.add(rawToValueNode(arg));
            }
            return new com.habbashx.larvey.ast.FunctionCallNode(functionCallValue.name(), args, functionCallValue.location());
        }
        if (value instanceof LarveyValue.InterpolatedValue interpolatedValue) {
            List<com.habbashx.larvey.ast.InterpolatedStringNode.Part> parts = new ArrayList<>();
            for (LarveyValue.InterpolatedValue.Part part : interpolatedValue.parts()) {
                if (part instanceof LarveyValue.InterpolatedValue.Text text) {
                    parts.add(new com.habbashx.larvey.ast.InterpolatedStringNode.TextPart(text.text()));
                } else if (part instanceof LarveyValue.InterpolatedValue.Expression expression) {
                    parts.add(new com.habbashx.larvey.ast.InterpolatedStringNode.ExpressionPart(expression.expression()));
                }
            }
            return new com.habbashx.larvey.ast.InterpolatedStringNode(interpolatedValue.raw(), parts, interpolatedValue.location());
        }
        throw new IllegalStateException("Unknown value");
    }

    private Object convertArray(LarveyValue.ArrayValue arrayValue, Class<?> target, Type generic, String path, Class<?> owner) {
        List<LarveyValue> elements = arrayValue.elements();
        if (target.isArray()) {
            Class<?> component = target.getComponentType();
            Object array = Array.newInstance(component, elements.size());
            for (int i = 0; i < elements.size(); i++) {
                Array.set(array, i, convert(elements.get(i), component, component, path + "[" + i + "]", owner, null, null));
            }
            return array;
        }
        Class<?> elementClass = Object.class;
        Type elementType = Object.class;
        if (generic instanceof ParameterizedType parameterizedType && parameterizedType.getActualTypeArguments().length == 1) {
            elementType = parameterizedType.getActualTypeArguments()[0];
            elementClass = rawClass(elementType);
        }
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < elements.size(); i++) {
            list.add(convert(elements.get(i), elementClass, elementType, path + "[" + i + "]", owner, null, null));
        }
        if (target == Set.class || target.isAssignableFrom(HashSet.class)) {
            return new HashSet<>(list);
        }
        if (target == List.class || target.isAssignableFrom(ArrayList.class) || target == Collection.class || target == Object.class) {
            return list;
        }
        throw new LarveyMappingException("Cannot convert array to '" + target.getSimpleName() + "'", path, owner);
    }

    private Object convertLong(long raw, Class<?> target, String path, Class<?> owner) {
        if (target == long.class || target == Long.class) {
            return raw;
        }
        if (target == int.class || target == Integer.class) {
            if (raw < Integer.MIN_VALUE || raw > Integer.MAX_VALUE) {
                throw new LarveyMappingException("Value " + raw + " out of int range", path, owner);
            }
            return (int) raw;
        }
        if (target == short.class || target == Short.class) {
            if (raw < Short.MIN_VALUE || raw > Short.MAX_VALUE) {
                throw new LarveyMappingException("Value " + raw + " out of short range", path, owner);
            }
            return (short) raw;
        }
        if (target == byte.class || target == Byte.class) {
            if (raw < Byte.MIN_VALUE || raw > Byte.MAX_VALUE) {
                throw new LarveyMappingException("Value " + raw + " out of byte range", path, owner);
            }
            return (byte) raw;
        }
        if (target == double.class || target == Double.class) {
            return (double) raw;
        }
        if (target == float.class || target == Float.class) {
            return (float) raw;
        }
        if (target == String.class) {
            return Long.toString(raw);
        }
        if (target == BigInteger.class) {
            return BigInteger.valueOf(raw);
        }
        if (target == BigDecimal.class) {
            return BigDecimal.valueOf(raw);
        }
        if (target == Object.class) {
            return raw;
        }
        throw new LarveyMappingException("Cannot convert integer to '" + target.getSimpleName() + "'", path, owner);
    }

    private Object convertDouble(double raw, Class<?> target, String path, Class<?> owner) {
        if (target == double.class || target == Double.class) {
            return raw;
        }
        if (target == float.class || target == Float.class) {
            return (float) raw;
        }
        if (target == String.class) {
            return Double.toString(raw);
        }
        if (target == BigDecimal.class) {
            return BigDecimal.valueOf(raw);
        }
        if (target == Object.class) {
            return raw;
        }
        if (target == long.class || target == Long.class || target == int.class || target == Integer.class) {
            throw new LarveyMappingException("Cannot convert decimal to '" + target.getSimpleName() + "'", path, owner);
        }
        throw new LarveyMappingException("Cannot convert decimal to '" + target.getSimpleName() + "'", path, owner);
    }

    private Object convertBoolean(boolean raw, Class<?> target, String path, Class<?> owner) {
        if (target == boolean.class || target == Boolean.class) {
            return raw;
        }
        if (target == String.class) {
            return Boolean.toString(raw);
        }
        if (target == Object.class) {
            return raw;
        }
        throw new LarveyMappingException("Cannot convert boolean to '" + target.getSimpleName() + "'", path, owner);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object convertString(String raw, Class<?> target, Type generic, String path, Class<?> owner, Class<? extends LarveyConverter<?, ?>> converterClass) {
        if (converterClass != null) {
            LarveyConverter<?, ?> converter = converterCache.computeIfAbsent(converterClass, k -> {
                try {
                    return (LarveyConverter<?, ?>) k.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new LarveyMappingException("Cannot instantiate converter '" + k.getName() + "'", path, owner, e);
                }
            });
            return ((LarveyConverter<Object, Object>) converter).convert(raw);
        }
        if (target == String.class) {
            return raw;
        }
        if (target == Object.class) {
            return raw;
        }
        if (target == boolean.class || target == Boolean.class) {
            if (raw.equalsIgnoreCase("true")) {
                return true;
            }
            if (raw.equalsIgnoreCase("false")) {
                return false;
            }
            throw new LarveyMappingException("Cannot convert '" + raw + "' to boolean", path, owner);
        }
        if (target == byte.class || target == Byte.class) {
            try {
                return Byte.parseByte(raw.trim());
            } catch (NumberFormatException e) {
                throw new LarveyMappingException("Cannot convert '" + raw + "' to byte", path, owner, e);
            }
        }
        if (target == short.class || target == Short.class) {
            try {
                return Short.parseShort(raw.trim());
            } catch (NumberFormatException e) {
                throw new LarveyMappingException("Cannot convert '" + raw + "' to short", path, owner, e);
            }
        }
        if (target == int.class || target == Integer.class) {
            try {
                return Integer.parseInt(raw.trim());
            } catch (NumberFormatException e) {
                throw new LarveyMappingException("Cannot convert '" + raw + "' to int", path, owner, e);
            }
        }
        if (target == long.class || target == Long.class) {
            try {
                return Long.parseLong(raw.trim());
            } catch (NumberFormatException e) {
                throw new LarveyMappingException("Cannot convert '" + raw + "' to long", path, owner, e);
            }
        }
        if (target == float.class || target == Float.class) {
            try {
                return Float.parseFloat(raw.trim());
            } catch (NumberFormatException e) {
                throw new LarveyMappingException("Cannot convert '" + raw + "' to float", path, owner, e);
            }
        }
        if (target == double.class || target == Double.class) {
            try {
                return Double.parseDouble(raw.trim());
            } catch (NumberFormatException e) {
                throw new LarveyMappingException("Cannot convert '" + raw + "' to double", path, owner, e);
            }
        }
        if (target == BigInteger.class) {
            try {
                return new BigInteger(raw.trim());
            } catch (NumberFormatException e) {
                throw new LarveyMappingException("Cannot convert '" + raw + "' to BigInteger", path, owner, e);
            }
        }
        if (target == BigDecimal.class) {
            try {
                return new BigDecimal(raw.trim());
            } catch (NumberFormatException e) {
                throw new LarveyMappingException("Cannot convert '" + raw + "' to BigDecimal", path, owner, e);
            }
        }
        if (target.isEnum()) {
            try {
                return Enum.valueOf((Class<Enum>) target, raw);
            } catch (IllegalArgumentException e) {
                for (Object constant : target.getEnumConstants()) {
                    if (((Enum<?>) constant).name().equalsIgnoreCase(raw)) {
                        return constant;
                    }
                }
                throw new LarveyMappingException("Cannot convert '" + raw + "' to enum '" + target.getSimpleName() + "'", path, owner, e);
            }
        }
        if (target == UUID.class) {
            try {
                return UUID.fromString(raw.trim());
            } catch (IllegalArgumentException e) {
                throw new LarveyMappingException("Cannot convert '" + raw + "' to UUID", path, owner, e);
            }
        }
        if (target == Path.class) {
            return Paths.get(raw);
        }
        if (target == URI.class) {
            try {
                return URI.create(raw.trim());
            } catch (IllegalArgumentException e) {
                throw new LarveyMappingException("Cannot convert '" + raw + "' to URI", path, owner, e);
            }
        }
        if (target == Duration.class) {
            try {
                return Duration.parse(raw.trim());
            } catch (Exception e) {
                throw new LarveyMappingException("Cannot convert '" + raw + "' to Duration", path, owner, e);
            }
        }
        if (target == char.class || target == Character.class) {
            if (raw.length() == 1) {
                return raw.charAt(0);
            }
            throw new LarveyMappingException("Cannot convert '" + raw + "' to char", path, owner);
        }
        if (target == Optional.class) {
            Class<?> inner = Object.class;
            Type innerType = Object.class;
            if (generic instanceof ParameterizedType parameterizedType && parameterizedType.getActualTypeArguments().length == 1) {
                innerType = parameterizedType.getActualTypeArguments()[0];
                inner = rawClass(innerType);
            }
            return Optional.ofNullable(convertString(raw, inner, innerType, path, owner, null));
        }
        throw new LarveyMappingException("Cannot convert string to '" + target.getSimpleName() + "'", path, owner);
    }

    private Object convertKey(String key, Type keyType, String path, Class<?> owner) {
        Class<?> raw = rawClass(keyType);
        if (raw == String.class || raw == Object.class) {
            return key;
        }
        return convertString(key, raw, keyType, path, owner, null);
    }

    private Type[] mapArgs(Type generic) {
        if (generic instanceof ParameterizedType parameterizedType && parameterizedType.getActualTypeArguments().length == 2) {
            return parameterizedType.getActualTypeArguments();
        }
        return new Type[]{String.class, Object.class};
    }

    private Class<?> rawClass(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz;
        }
        if (type instanceof ParameterizedType parameterizedType) {
            return rawClass(parameterizedType.getRawType());
        }
        if (type instanceof GenericArrayType genericArrayType) {
            Class<?> component = rawClass(genericArrayType.getGenericComponentType());
            return Array.newInstance(component, 0).getClass();
        }
        return Object.class;
    }

    private Object primitiveDefault(Class<?> type) {
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

    private Object toRaw(LarveyValue value) {
        if (value instanceof LarveyValue.StringValue stringValue) {
            return stringValue.value();
        }
        if (value instanceof LarveyValue.IntegerValue integerValue) {
            return integerValue.value();
        }
        if (value instanceof LarveyValue.DecimalValue decimalValue) {
            return decimalValue.value();
        }
        if (value instanceof LarveyValue.BooleanValue booleanValue) {
            return booleanValue.value();
        }
        if (value instanceof LarveyValue.NullValue) {
            return null;
        }
        if (value instanceof LarveyValue.ArrayValue arrayValue) {
            List<Object> list = new ArrayList<>();
            for (LarveyValue element : arrayValue.elements()) {
                list.add(toRaw(element));
            }
            return list;
        }
        if (value instanceof LarveyValue.ObjectValue objectValue) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Map.Entry<String, LarveyValue> entry : objectValue.properties().entrySet()) {
                map.put(entry.getKey(), toRaw(entry.getValue()));
            }
            return map;
        }
        if (value instanceof LarveyValue.FunctionCallValue functionCallValue) {
            Map<String, Object> map = new HashMap<>();
            map.put("$fn", functionCallValue.name());
            return map;
        }
        if (value instanceof LarveyValue.InterpolatedValue interpolatedValue) {
            return interpolatedValue.raw();
        }
        return null;
    }

    private String join(String parent, String name) {
        return parent == null || parent.isEmpty() ? name : parent + "." + name;
    }
}
