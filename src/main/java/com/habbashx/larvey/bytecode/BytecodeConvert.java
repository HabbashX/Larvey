package com.habbashx.larvey.bytecode;

import com.habbashx.larvey.exception.LarveyMappingException;
import com.habbashx.larvey.semantic.LarveyValue;

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
}
