package com.habbashx.larvey.bytecode;

import com.habbashx.larvey.exception.LarveyBytecodeException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class BytecodeRuntimeHelper {
    private BytecodeRuntimeHelper() {
    }

    public static MethodHandle noArgHandle(Class<?> target) {
        return ctorHandle(target);
    }

    public static MethodHandle ctorHandle(Class<?> target, Class<?>... paramTypes) {
        try {
            Constructor<?> constructor = target.getDeclaredConstructor(paramTypes);
            constructor.setAccessible(true);
            return MethodHandles.lookup().unreflectConstructor(constructor);
        } catch (Exception e) {
            throw new LarveyBytecodeException("Cannot access constructor of " + target.getName(), e);
        }
    }

    public static MethodHandle fieldHandle(Class<?> target, String fieldName) {
        try {
            Field field = target.getDeclaredField(fieldName);
            field.setAccessible(true);
            return MethodHandles.lookup().unreflectSetter(field);
        } catch (Exception e) {
            throw new LarveyBytecodeException("Cannot access field '" + fieldName + "' of " + target.getName(), e);
        }
    }

    public static MethodHandle setterHandle(Class<?> target, String methodName, Class<?> paramType) {
        try {
            Method method = target.getDeclaredMethod(methodName, paramType);
            method.setAccessible(true);
            return MethodHandles.lookup().unreflect(method);
        } catch (Exception e) {
            throw new LarveyBytecodeException("Cannot access setter '" + methodName + "' of " + target.getName(), e);
        }
    }
}
