package com.habbashx.larvey.bytecode;

import com.habbashx.larvey.exception.LarveyMappingException;
import com.habbashx.larvey.semantic.LarveyValue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.UUID;

public final class BytecodeConvert {
    private BytecodeConvert() {
    }

    public static String toString(LarveyValue value, String path, Class<?> owner) {
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
            return null;
        }
        throw new LarveyMappingException("Cannot convert value to String", path, owner);
    }

    public static boolean toBoolean(LarveyValue value, String path, Class<?> owner) {
        if (value instanceof LarveyValue.BooleanValue booleanValue) {
            return booleanValue.value();
        }
        if (value instanceof LarveyValue.StringValue stringValue) {
            if (stringValue.value().equalsIgnoreCase("true")) {
                return true;
            }
            if (stringValue.value().equalsIgnoreCase("false")) {
                return false;
            }
        }
        throw new LarveyMappingException("Cannot convert value to boolean", path, owner);
    }

    public static Boolean toBooleanBoxed(LarveyValue value, String path, Class<?> owner) {
        if (value instanceof LarveyValue.NullValue) {
            return null;
        }
        return toBoolean(value, path, owner);
    }

    public static int toInt(LarveyValue value, String path, Class<?> owner) {
        if (value instanceof LarveyValue.IntegerValue integerValue) {
            long raw = integerValue.value();
            if (raw < Integer.MIN_VALUE || raw > Integer.MAX_VALUE) {
                throw new LarveyMappingException("Value out of int range", path, owner);
            }
            return (int) raw;
        }
        if (value instanceof LarveyValue.StringValue stringValue) {
            try {
                return Integer.parseInt(stringValue.value().trim());
            } catch (NumberFormatException e) {
                throw new LarveyMappingException("Cannot convert to int", path, owner, e);
            }
        }
        throw new LarveyMappingException("Cannot convert value to int", path, owner);
    }

    public static Integer toIntBoxed(LarveyValue value, String path, Class<?> owner) {
        if (value instanceof LarveyValue.NullValue) {
            return null;
        }
        return toInt(value, path, owner);
    }

    public static long toLong(LarveyValue value, String path, Class<?> owner) {
        if (value instanceof LarveyValue.IntegerValue integerValue) {
            return integerValue.value();
        }
        if (value instanceof LarveyValue.StringValue stringValue) {
            try {
                return Long.parseLong(stringValue.value().trim());
            } catch (NumberFormatException e) {
                throw new LarveyMappingException("Cannot convert to long", path, owner, e);
            }
        }
        throw new LarveyMappingException("Cannot convert value to long", path, owner);
    }

    public static Long toLongBoxed(LarveyValue value, String path, Class<?> owner) {
        if (value instanceof LarveyValue.NullValue) {
            return null;
        }
        return toLong(value, path, owner);
    }

    public static double toDouble(LarveyValue value, String path, Class<?> owner) {
        if (value instanceof LarveyValue.DecimalValue decimalValue) {
            return decimalValue.value();
        }
        if (value instanceof LarveyValue.IntegerValue integerValue) {
            return (double) integerValue.value();
        }
        if (value instanceof LarveyValue.StringValue stringValue) {
            try {
                return Double.parseDouble(stringValue.value().trim());
            } catch (NumberFormatException e) {
                throw new LarveyMappingException("Cannot convert to double", path, owner, e);
            }
        }
        throw new LarveyMappingException("Cannot convert value to double", path, owner);
    }

    public static Double toDoubleBoxed(LarveyValue value, String path, Class<?> owner) {
        if (value instanceof LarveyValue.NullValue) {
            return null;
        }
        return toDouble(value, path, owner);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object toOther(LarveyValue value, Class<?> target, String path, Class<?> owner) {
        if (value instanceof LarveyValue.NullValue) {
            return null;
        }
        if (target.isEnum()) {
            if (!(value instanceof LarveyValue.StringValue stringValue)) {
                throw new LarveyMappingException("Cannot convert value to enum '" + target.getSimpleName() + "'", path, owner);
            }
            String raw = stringValue.value();
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
        if (value instanceof LarveyValue.IntegerValue integerValue) {
            if (target == BigInteger.class) {
                return BigInteger.valueOf(integerValue.value());
            }
            if (target == BigDecimal.class) {
                return BigDecimal.valueOf(integerValue.value());
            }
        }
        if (value instanceof LarveyValue.DecimalValue decimalValue && target == BigDecimal.class) {
            return BigDecimal.valueOf(decimalValue.value());
        }
        if (!(value instanceof LarveyValue.StringValue stringValue)) {
            throw new LarveyMappingException("Cannot convert value to '" + target.getSimpleName() + "'", path, owner);
        }
        String raw = stringValue.value();
        try {
            if (target == UUID.class) {
                return UUID.fromString(raw.trim());
            }
            if (target == BigInteger.class) {
                return new BigInteger(raw.trim());
            }
            if (target == BigDecimal.class) {
                return new BigDecimal(raw.trim());
            }
            if (target == Float.class || target == float.class) {
                return Float.parseFloat(raw.trim());
            }
            if (target == Byte.class || target == byte.class) {
                return Byte.parseByte(raw.trim());
            }
            if (target == Short.class || target == short.class) {
                return Short.parseShort(raw.trim());
            }
        } catch (IllegalArgumentException e) {
            throw new LarveyMappingException("Cannot convert '" + raw + "' to '" + target.getSimpleName() + "'", path, owner, e);
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
        if ((target == Character.class || target == char.class) && raw.length() == 1) {
            return raw.charAt(0);
        }
        throw new LarveyMappingException("Cannot convert string to '" + target.getSimpleName() + "'", path, owner);
    }
}
