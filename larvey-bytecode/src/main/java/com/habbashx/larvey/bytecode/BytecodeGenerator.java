package com.habbashx.larvey.bytecode;

import com.habbashx.larvey.exception.LarveyBytecodeException;
import com.habbashx.larvey.metadata.ClassMetadata;
import com.habbashx.larvey.metadata.PropertyMetadata;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class BytecodeGenerator {
    private BytecodeGenerator() {
    }

    public static <T> Class<? extends GeneratedMapper<T>> generate(Class<T> target) {
        if (!isAccessible(target)) {
            throw new LarveyBytecodeException("Bytecode mapper requires an accessible type: " + target.getName());
        }
        ClassMetadata<T> metadata = ClassMetadata.of(target);
        boolean useCreator = metadata.creator() != null;
        if (!useCreator && metadata.noArgConstructor() == null) {
            throw new LarveyBytecodeException("No suitable constructor for bytecode mapper: " + target.getName());
        }
        List<PropPlan> beanPlans = new ArrayList<>();
        List<ParamPlan> creatorPlans = new ArrayList<>();
        if (useCreator) {
            for (ClassMetadata.CreatorParameter param : metadata.creatorParameters()) {
                if (param.converter() != null) {
                    throw new LarveyBytecodeException("Custom converter not supported by bytecode mapper: " + param.name());
                }
                if (param.format() != null) {
                    throw new LarveyBytecodeException("Format validation not supported by bytecode mapper: " + param.name());
                }
                creatorPlans.add(planParam(param));
            }
        } else {
            for (PropertyMetadata property : metadata.properties()) {
                if (property.converter() != null) {
                    throw new LarveyBytecodeException("Custom converter not supported by bytecode mapper: " + property.name());
                }
                if (property.format() != null) {
                    throw new LarveyBytecodeException("Format validation not supported by bytecode mapper: " + property.name());
                }
                beanPlans.add(planProperty(property));
            }
        }
        String internalName = "com/habbashx/larvey/gen/" + target.getSimpleName() + "LarveyMapper" + Math.abs(target.getName().hashCode());
        String className = internalName.replace('/', '.');
        String targetInternal = org.objectweb.asm.Type.getInternalName(target);
        String targetDesc = "L" + targetInternal + ";";
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, internalName, null, "java/lang/Object", new String[]{org.objectweb.asm.Type.getInternalName(GeneratedMapper.class)});
        writer.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "owner", "Ljava/lang/Class;", null, null).visitEnd();
        writer.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "mhCtor", "Ljava/lang/invoke/MethodHandle;", null, null).visitEnd();
        for (int i = 0; i < beanPlans.size(); i++) {
            writer.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "mh" + i, "Ljava/lang/invoke/MethodHandle;", null, null).visitEnd();
        }
        MethodVisitor init = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(Ljava/lang/Class;)V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitVarInsn(Opcodes.ALOAD, 1);
        init.visitFieldInsn(Opcodes.PUTFIELD, internalName, "owner", "Ljava/lang/Class;");
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitVarInsn(Opcodes.ALOAD, 1);
        if (useCreator) {
            pushTypeArray(init, creatorParamTypes(metadata));
            init.visitMethodInsn(Opcodes.INVOKESTATIC, "com/habbashx/larvey/bytecode/BytecodeRuntimeHelper", "ctorHandle", "(Ljava/lang/Class;[Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;", false);
        } else {
            init.visitMethodInsn(Opcodes.INVOKESTATIC, "com/habbashx/larvey/bytecode/BytecodeRuntimeHelper", "noArgHandle", "(Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;", false);
        }
        init.visitFieldInsn(Opcodes.PUTFIELD, internalName, "mhCtor", "Ljava/lang/invoke/MethodHandle;");
        for (int i = 0; i < beanPlans.size(); i++) {
            PropPlan plan = beanPlans.get(i);
            init.visitVarInsn(Opcodes.ALOAD, 0);
            init.visitVarInsn(Opcodes.ALOAD, 1);
            init.visitLdcInsn(plan.memberName());
            if (plan.isSetter()) {
                pushClass(init, plan.accessType());
                init.visitMethodInsn(Opcodes.INVOKESTATIC, "com/habbashx/larvey/bytecode/BytecodeRuntimeHelper", "setterHandle", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;", false);
            } else {
                init.visitMethodInsn(Opcodes.INVOKESTATIC, "com/habbashx/larvey/bytecode/BytecodeRuntimeHelper", "fieldHandle", "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/invoke/MethodHandle;", false);
            }
            init.visitFieldInsn(Opcodes.PUTFIELD, internalName, "mh" + i, "Ljava/lang/invoke/MethodHandle;");
        }
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();
        if (useCreator) {
            emitCreatorMap(writer, internalName, targetDesc, creatorPlans);
        } else {
            emitBeanMap(writer, internalName, targetInternal, targetDesc, beanPlans);
        }
        writer.visitEnd();
        byte[] bytes = writer.toByteArray();
        try {
            DynamicLoader loader = new DynamicLoader(BytecodeGenerator.class.getClassLoader());
            @SuppressWarnings("unchecked")
            Class<? extends GeneratedMapper<T>> defined = (Class<? extends GeneratedMapper<T>>) loader.define(className, bytes);
            return defined;
        } catch (Exception e) {
            throw new LarveyBytecodeException("Failed to define generated mapper for " + target.getName(), e);
        }
    }

    private static void emitBeanMap(ClassWriter writer, String internalName, String targetInternal, String targetDesc, List<PropPlan> plans) {
        MethodVisitor map = writer.visitMethod(Opcodes.ACC_PUBLIC, "map", "(Lcom/habbashx/larvey/semantic/Configuration;)" + targetDesc, null, null);
        map.visitCode();
        Label start = new Label();
        Label end = new Label();
        Label handler = new Label();
        map.visitLabel(start);
        map.visitVarInsn(Opcodes.ALOAD, 0);
        map.visitFieldInsn(Opcodes.GETFIELD, internalName, "mhCtor", "Ljava/lang/invoke/MethodHandle;");
        map.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invoke", "()" + targetDesc, false);
        map.visitVarInsn(Opcodes.ASTORE, 2);
        for (int i = 0; i < plans.size(); i++) {
            emitBeanProperty(map, internalName, targetInternal, plans.get(i), i);
        }
        map.visitVarInsn(Opcodes.ALOAD, 2);
        map.visitInsn(Opcodes.ARETURN);
        map.visitLabel(end);
        map.visitLabel(handler);
        emitWrap(map, internalName, 5);
        map.visitTryCatchBlock(start, end, handler, "java/lang/Throwable");
        map.visitMaxs(0, 0);
        map.visitEnd();
        MethodVisitor bridge = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC, "map", "(Lcom/habbashx/larvey/semantic/Configuration;)Ljava/lang/Object;", null, null);
        bridge.visitCode();
        bridge.visitVarInsn(Opcodes.ALOAD, 0);
        bridge.visitVarInsn(Opcodes.ALOAD, 1);
        bridge.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internalName, "map", "(Lcom/habbashx/larvey/semantic/Configuration;)" + targetDesc, false);
        bridge.visitInsn(Opcodes.ARETURN);
        bridge.visitMaxs(0, 0);
        bridge.visitEnd();
    }

    private static void emitBeanProperty(MethodVisitor map, String internalName, String targetInternal, PropPlan plan, int index) {
        Label end = new Label();
        map.visitVarInsn(Opcodes.ALOAD, 1);
        map.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/habbashx/larvey/semantic/Configuration", "path", "()Ljava/lang/String;", false);
        map.visitLdcInsn(plan.name());
        map.visitMethodInsn(Opcodes.INVOKESTATIC, "com/habbashx/larvey/bytecode/BytecodeRuntime", "joinFull", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", false);
        map.visitVarInsn(Opcodes.ASTORE, 4);
        if (plan.kind() == BytecodeRuntime.KIND_BEAN || plan.kind() == BytecodeRuntime.KIND_MAP) {
            Label hasBlock = new Label();
            Label absent = new Label();
            map.visitVarInsn(Opcodes.ALOAD, 1);
            map.visitLdcInsn(plan.name());
            pushAliases(map, plan.aliases());
            map.visitMethodInsn(Opcodes.INVOKESTATIC, "com/habbashx/larvey/bytecode/BytecodeRuntime", "block", "(Lcom/habbashx/larvey/semantic/Configuration;Ljava/lang/String;[Ljava/lang/String;)Ljava/util/Optional;", false);
            map.visitVarInsn(Opcodes.ASTORE, 3);
            map.visitVarInsn(Opcodes.ALOAD, 3);
            map.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/Optional", "isPresent", "()Z", false);
            map.visitJumpInsn(Opcodes.IFNE, hasBlock);
            map.visitVarInsn(Opcodes.ALOAD, 1);
            map.visitLdcInsn(plan.name());
            pushAliases(map, plan.aliases());
            map.visitMethodInsn(Opcodes.INVOKESTATIC, "com/habbashx/larvey/bytecode/BytecodeRuntime", "prop", "(Lcom/habbashx/larvey/semantic/Configuration;Ljava/lang/String;[Ljava/lang/String;)Ljava/util/Optional;", false);
            map.visitVarInsn(Opcodes.ASTORE, 3);
            map.visitVarInsn(Opcodes.ALOAD, 3);
            map.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/Optional", "isEmpty", "()Z", false);
            map.visitJumpInsn(Opcodes.IFNE, absent);
            map.visitVarInsn(Opcodes.ALOAD, 3);
            map.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/Optional", "get", "()Ljava/lang/Object;", false);
            map.visitTypeInsn(Opcodes.CHECKCAST, "com/habbashx/larvey/semantic/LarveyValue");
            if (plan.kind() == BytecodeRuntime.KIND_BEAN) {
                pushClass(map, plan.type());
                map.visitVarInsn(Opcodes.ALOAD, 4);
                map.visitVarInsn(Opcodes.ALOAD, 0);
                map.visitFieldInsn(Opcodes.GETFIELD, internalName, "owner", "Ljava/lang/Class;");
                map.visitMethodInsn(Opcodes.INVOKESTATIC, "com/habbashx/larvey/bytecode/BytecodeRuntime", "beanFromValue", "(Lcom/habbashx/larvey/semantic/LarveyValue;Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;", false);
            } else {
                pushClass(map, plan.elem());
                pushClass(map, plan.elem2());
                map.visitVarInsn(Opcodes.ALOAD, 4);
                map.visitVarInsn(Opcodes.ALOAD, 0);
                map.visitFieldInsn(Opcodes.GETFIELD, internalName, "owner", "Ljava/lang/Class;");
                map.visitMethodInsn(Opcodes.INVOKESTATIC, "com/habbashx/larvey/bytecode/BytecodeRuntime", "mapFromValue", "(Lcom/habbashx/larvey/semantic/LarveyValue;Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/util/Map;", false);
            }
            map.visitVarInsn(Opcodes.ASTORE, 6);
            emitStore(map, internalName, targetInternal, index, plan);
            map.visitJumpInsn(Opcodes.GOTO, end);
            map.visitLabel(absent);
            emitAbsent(map, internalName, targetInternal, plan, index);
            map.visitJumpInsn(Opcodes.GOTO, end);
            map.visitLabel(hasBlock);
            map.visitVarInsn(Opcodes.ALOAD, 3);
            map.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/Optional", "get", "()Ljava/lang/Object;", false);
            map.visitTypeInsn(Opcodes.CHECKCAST, "com/habbashx/larvey/semantic/Configuration");
            if (plan.kind() == BytecodeRuntime.KIND_BEAN) {
                pushClass(map, plan.type());
                map.visitMethodInsn(Opcodes.INVOKESTATIC, "com/habbashx/larvey/bytecode/BytecodeRuntime", "mapOrGenerate", "(Lcom/habbashx/larvey/semantic/Configuration;Ljava/lang/Class;)Ljava/lang/Object;", false);
            } else {
                pushClass(map, plan.elem());
                pushClass(map, plan.elem2());
                map.visitVarInsn(Opcodes.ALOAD, 4);
                map.visitVarInsn(Opcodes.ALOAD, 0);
                map.visitFieldInsn(Opcodes.GETFIELD, internalName, "owner", "Ljava/lang/Class;");
                map.visitMethodInsn(Opcodes.INVOKESTATIC, "com/habbashx/larvey/bytecode/BytecodeRuntime", "mapFromConfig", "(Lcom/habbashx/larvey/semantic/Configuration;Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/util/Map;", false);
            }
            map.visitVarInsn(Opcodes.ASTORE, 6);
            emitStore(map, internalName, targetInternal, index, plan);
            map.visitLabel(end);
            return;
        }
        Label absent = new Label();
        map.visitVarInsn(Opcodes.ALOAD, 1);
        map.visitLdcInsn(plan.name());
        pushAliases(map, plan.aliases());
        map.visitMethodInsn(Opcodes.INVOKESTATIC, "com/habbashx/larvey/bytecode/BytecodeRuntime", "prop", "(Lcom/habbashx/larvey/semantic/Configuration;Ljava/lang/String;[Ljava/lang/String;)Ljava/util/Optional;", false);
        map.visitVarInsn(Opcodes.ASTORE, 3);
        if (plan.kind() == BytecodeRuntime.KIND_OPTIONAL) {
            map.visitVarInsn(Opcodes.ALOAD, 3);
            pushInt(map, plan.elemKind());
            pushClassOrNull(map, plan.elem());
            map.visitVarInsn(Opcodes.ALOAD, 4);
            map.visitVarInsn(Opcodes.ALOAD, 0);
            map.visitFieldInsn(Opcodes.GETFIELD, internalName, "owner", "Ljava/lang/Class;");
            map.visitMethodInsn(Opcodes.INVOKESTATIC, "com/habbashx/larvey/bytecode/BytecodeRuntime", "optionalFrom", "(Ljava/util/Optional;ILjava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/util/Optional;", false);
            map.visitVarInsn(Opcodes.ASTORE, 6);
            emitStore(map, internalName, targetInternal, index, plan);
            map.visitLabel(end);
            return;
        }
        map.visitVarInsn(Opcodes.ALOAD, 3);
        map.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/Optional", "isEmpty", "()Z", false);
        map.visitJumpInsn(Opcodes.IFNE, absent);
        map.visitVarInsn(Opcodes.ALOAD, 3);
        map.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/Optional", "get", "()Ljava/lang/Object;", false);
        map.visitTypeInsn(Opcodes.CHECKCAST, "com/habbashx/larvey/semantic/LarveyValue");
        emitConvertValue(map, internalName, plan);
        map.visitVarInsn(Opcodes.ASTORE, 6);
        if (plan.type().isPrimitive()) {
            map.visitVarInsn(Opcodes.ALOAD, 6);
            map.visitJumpInsn(Opcodes.IFNULL, end);
        }
        emitStore(map, internalName, targetInternal, index, plan);
        map.visitJumpInsn(Opcodes.GOTO, end);
        map.visitLabel(absent);
        emitAbsent(map, internalName, targetInternal, plan, index);
        map.visitLabel(end);
    }

    private static void emitStore(MethodVisitor map, String internalName, String targetInternal, int index, PropPlan plan) {
        Class<?> valueType = plan.isSetter() ? plan.accessType() : plan.type();
        map.visitVarInsn(Opcodes.ALOAD, 0);
        map.visitFieldInsn(Opcodes.GETFIELD, internalName, "mh" + index, "Ljava/lang/invoke/MethodHandle;");
        map.visitVarInsn(Opcodes.ALOAD, 2);
        map.visitVarInsn(Opcodes.ALOAD, 6);
        if (valueType.isPrimitive() || isAccessible(valueType)) {
            emitCastUnbox(map, valueType);
            String descriptor = "(L" + targetInternal + ";" + org.objectweb.asm.Type.getDescriptor(valueType) + ")V";
            map.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", descriptor, false);
        } else {
            map.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invoke", "(Ljava/lang/Object;Ljava/lang/Object;)V", false);
        }
    }

    private static void emitCastUnbox(MethodVisitor visitor, Class<?> type) {
        if (!type.isPrimitive()) {
            visitor.visitTypeInsn(Opcodes.CHECKCAST, org.objectweb.asm.Type.getInternalName(type));
            return;
        }
        String wrapper;
        String method;
        String descriptor;
        if (type == boolean.class) {
            wrapper = "java/lang/Boolean";
            method = "booleanValue";
            descriptor = "()Z";
        } else if (type == int.class) {
            wrapper = "java/lang/Integer";
            method = "intValue";
            descriptor = "()I";
        } else if (type == long.class) {
            wrapper = "java/lang/Long";
            method = "longValue";
            descriptor = "()J";
        } else if (type == double.class) {
            wrapper = "java/lang/Double";
            method = "doubleValue";
            descriptor = "()D";
        } else if (type == float.class) {
            wrapper = "java/lang/Float";
            method = "floatValue";
            descriptor = "()F";
        } else if (type == byte.class) {
            wrapper = "java/lang/Byte";
            method = "byteValue";
            descriptor = "()B";
        } else if (type == short.class) {
            wrapper = "java/lang/Short";
            method = "shortValue";
            descriptor = "()S";
        } else {
            wrapper = "java/lang/Character";
            method = "charValue";
            descriptor = "()C";
        }
        visitor.visitTypeInsn(Opcodes.CHECKCAST, wrapper);
        visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, wrapper, method, descriptor, false);
    }

    private static void emitConvertValue(MethodVisitor map, String internalName, PropPlan plan) {
        switch (plan.kind()) {
            case BytecodeRuntime.KIND_STRING -> {
                map.visitVarInsn(Opcodes.ALOAD, 4);
                map.visitVarInsn(Opcodes.ALOAD, 0);
                map.visitFieldInsn(Opcodes.GETFIELD, internalName, "owner", "Ljava/lang/Class;");
                map.visitMethodInsn(Opcodes.INVOKESTATIC, "com/habbashx/larvey/bytecode/BytecodeConvert", "toString", "(Lcom/habbashx/larvey/semantic/LarveyValue;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/String;", false);
            }
            case BytecodeRuntime.KIND_BOOLEAN -> {
                map.visitVarInsn(Opcodes.ALOAD, 4);
                map.visitVarInsn(Opcodes.ALOAD, 0);
                map.visitFieldInsn(Opcodes.GETFIELD, internalName, "owner", "Ljava/lang/Class;");
                map.visitMethodInsn(Opcodes.INVOKESTATIC, "com/habbashx/larvey/bytecode/BytecodeConvert", "toBooleanBoxed", "(Lcom/habbashx/larvey/semantic/LarveyValue;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Boolean;", false);
            }
            case BytecodeRuntime.KIND_INT -> {
                map.visitVarInsn(Opcodes.ALOAD, 4);
                map.visitVarInsn(Opcodes.ALOAD, 0);
                map.visitFieldInsn(Opcodes.GETFIELD, internalName, "owner", "Ljava/lang/Class;");
                map.visitMethodInsn(Opcodes.INVOKESTATIC, "com/habbashx/larvey/bytecode/BytecodeConvert", "toIntBoxed", "(Lcom/habbashx/larvey/semantic/LarveyValue;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Integer;", false);
            }
            case BytecodeRuntime.KIND_LONG -> {
                map.visitVarInsn(Opcodes.ALOAD, 4);
                map.visitVarInsn(Opcodes.ALOAD, 0);
                map.visitFieldInsn(Opcodes.GETFIELD, internalName, "owner", "Ljava/lang/Class;");
                map.visitMethodInsn(Opcodes.INVOKESTATIC, "com/habbashx/larvey/bytecode/BytecodeConvert", "toLongBoxed", "(Lcom/habbashx/larvey/semantic/LarveyValue;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Long;", false);
            }
            case BytecodeRuntime.KIND_DOUBLE -> {
                map.visitVarInsn(Opcodes.ALOAD, 4);
                map.visitVarInsn(Opcodes.ALOAD, 0);
                map.visitFieldInsn(Opcodes.GETFIELD, internalName, "owner", "Ljava/lang/Class;");
                map.visitMethodInsn(Opcodes.INVOKESTATIC, "com/habbashx/larvey/bytecode/BytecodeConvert", "toDoubleBoxed", "(Lcom/habbashx/larvey/semantic/LarveyValue;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Double;", false);
            }
            case BytecodeRuntime.KIND_LIST -> {
                pushClass(map, plan.elem());
                map.visitVarInsn(Opcodes.ALOAD, 4);
                map.visitVarInsn(Opcodes.ALOAD, 0);
                map.visitFieldInsn(Opcodes.GETFIELD, internalName, "owner", "Ljava/lang/Class;");
                map.visitMethodInsn(Opcodes.INVOKESTATIC, "com/habbashx/larvey/bytecode/BytecodeRuntime", "listFrom", "(Lcom/habbashx/larvey/semantic/LarveyValue;Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/util/List;", false);
            }
            case BytecodeRuntime.KIND_SET -> {
                pushClass(map, plan.elem());
                map.visitVarInsn(Opcodes.ALOAD, 4);
                map.visitVarInsn(Opcodes.ALOAD, 0);
                map.visitFieldInsn(Opcodes.GETFIELD, internalName, "owner", "Ljava/lang/Class;");
                map.visitMethodInsn(Opcodes.INVOKESTATIC, "com/habbashx/larvey/bytecode/BytecodeRuntime", "setFrom", "(Lcom/habbashx/larvey/semantic/LarveyValue;Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/util/Set;", false);
            }
            case BytecodeRuntime.KIND_ARRAY -> {
                pushClass(map, plan.elem());
                map.visitVarInsn(Opcodes.ALOAD, 4);
                map.visitVarInsn(Opcodes.ALOAD, 0);
                map.visitFieldInsn(Opcodes.GETFIELD, internalName, "owner", "Ljava/lang/Class;");
                map.visitMethodInsn(Opcodes.INVOKESTATIC, "com/habbashx/larvey/bytecode/BytecodeRuntime", "arrayFrom", "(Lcom/habbashx/larvey/semantic/LarveyValue;Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;", false);
            }
            default -> {
                pushClass(map, plan.type());
                map.visitVarInsn(Opcodes.ALOAD, 4);
                map.visitVarInsn(Opcodes.ALOAD, 0);
                map.visitFieldInsn(Opcodes.GETFIELD, internalName, "owner", "Ljava/lang/Class;");
                map.visitMethodInsn(Opcodes.INVOKESTATIC, "com/habbashx/larvey/bytecode/BytecodeRuntime", "convertOther", "(Lcom/habbashx/larvey/semantic/LarveyValue;Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;", false);
            }
        }
    }

    private static void emitAbsent(MethodVisitor map, String internalName, String targetInternal, PropPlan plan, int index) {
        if (plan.defaultValue() != null) {
            map.visitLdcInsn(plan.defaultValue());
            pushClass(map, plan.type());
            map.visitVarInsn(Opcodes.ALOAD, 4);
            map.visitVarInsn(Opcodes.ALOAD, 0);
            map.visitFieldInsn(Opcodes.GETFIELD, internalName, "owner", "Ljava/lang/Class;");
            map.visitMethodInsn(Opcodes.INVOKESTATIC, "com/habbashx/larvey/bytecode/BytecodeRuntime", "parseDefault", "(Ljava/lang/String;Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;", false);
            map.visitVarInsn(Opcodes.ASTORE, 6);
            emitStore(map, internalName, targetInternal, index, plan);
        } else if (plan.required()) {
            map.visitVarInsn(Opcodes.ALOAD, 4);
            map.visitVarInsn(Opcodes.ALOAD, 0);
            map.visitFieldInsn(Opcodes.GETFIELD, internalName, "owner", "Ljava/lang/Class;");
            map.visitInsn(Opcodes.ICONST_M1);
            map.visitInsn(Opcodes.ICONST_M1);
            map.visitMethodInsn(Opcodes.INVOKESTATIC, "com/habbashx/larvey/bytecode/BytecodeRuntime", "throwMissing", "(Ljava/lang/String;Ljava/lang/Class;II)V", false);
        }
    }

    private static void emitCreatorMap(ClassWriter writer, String internalName, String targetDesc, List<ParamPlan> plans) {
        MethodVisitor map = writer.visitMethod(Opcodes.ACC_PUBLIC, "map", "(Lcom/habbashx/larvey/semantic/Configuration;)" + targetDesc, null, null);
        map.visitCode();
        Label start = new Label();
        Label end = new Label();
        Label handler = new Label();
        map.visitLabel(start);
        int local = 3;
        int[] slots = new int[plans.size()];
        for (int i = 0; i < plans.size(); i++) {
            slots[i] = local;
            local += 1;
        }
        int exSlot = local;
        for (int i = 0; i < plans.size(); i++) {
            ParamPlan plan = plans.get(i);
            map.visitVarInsn(Opcodes.ALOAD, 1);
            map.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/habbashx/larvey/semantic/Configuration", "path", "()Ljava/lang/String;", false);
            map.visitLdcInsn(plan.name());
            map.visitMethodInsn(Opcodes.INVOKESTATIC, "com/habbashx/larvey/bytecode/BytecodeRuntime", "joinFull", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", false);
            map.visitVarInsn(Opcodes.ASTORE, 2);
            map.visitVarInsn(Opcodes.ALOAD, 1);
            map.visitLdcInsn(plan.name());
            pushAliases(map, plan.aliases());
            pushInt(map, plan.kind());
            pushClass(map, plan.type());
            pushInt(map, plan.elemKind());
            pushClassOrNull(map, plan.elem());
            pushClassOrNull(map, plan.elem2());
            if (plan.defaultValue() != null) {
                map.visitLdcInsn(plan.defaultValue());
            } else {
                map.visitInsn(Opcodes.ACONST_NULL);
            }
            map.visitInsn(plan.required() ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
            map.visitVarInsn(Opcodes.ALOAD, 0);
            map.visitFieldInsn(Opcodes.GETFIELD, internalName, "owner", "Ljava/lang/Class;");
            map.visitVarInsn(Opcodes.ALOAD, 2);
            map.visitMethodInsn(Opcodes.INVOKESTATIC, "com/habbashx/larvey/bytecode/BytecodeRuntime", "arg", "(Lcom/habbashx/larvey/semantic/Configuration;Ljava/lang/String;[Ljava/lang/String;ILjava/lang/Class;ILjava/lang/Class;Ljava/lang/Class;Ljava/lang/String;ZLjava/lang/Class;Ljava/lang/String;)Ljava/lang/Object;", false);
            map.visitVarInsn(Opcodes.ASTORE, slots[i]);
        }
        map.visitVarInsn(Opcodes.ALOAD, 0);
        map.visitFieldInsn(Opcodes.GETFIELD, internalName, "mhCtor", "Ljava/lang/invoke/MethodHandle;");
        boolean exact = true;
        for (ParamPlan plan : plans) {
            if (!plan.type().isPrimitive() && !isAccessible(plan.type())) {
                exact = false;
                break;
            }
        }
        for (int i = 0; i < plans.size(); i++) {
            map.visitVarInsn(Opcodes.ALOAD, slots[i]);
            if (exact) {
                emitCastUnbox(map, plans.get(i).type());
            }
        }
        StringBuilder descriptor = new StringBuilder("(");
        for (int i = 0; i < plans.size(); i++) {
            descriptor.append(exact ? org.objectweb.asm.Type.getDescriptor(plans.get(i).type()) : "Ljava/lang/Object;");
        }
        descriptor.append(")").append(targetDesc);
        map.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", exact ? "invokeExact" : "invoke", descriptor.toString(), false);
        map.visitInsn(Opcodes.ARETURN);
        map.visitLabel(end);
        map.visitLabel(handler);
        emitWrap(map, internalName, exSlot);
        map.visitTryCatchBlock(start, end, handler, "java/lang/Throwable");
        map.visitMaxs(0, 0);
        map.visitEnd();
        MethodVisitor bridge = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC, "map", "(Lcom/habbashx/larvey/semantic/Configuration;)Ljava/lang/Object;", null, null);
        bridge.visitCode();
        bridge.visitVarInsn(Opcodes.ALOAD, 0);
        bridge.visitVarInsn(Opcodes.ALOAD, 1);
        bridge.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internalName, "map", "(Lcom/habbashx/larvey/semantic/Configuration;)" + targetDesc, false);
        bridge.visitInsn(Opcodes.ARETURN);
        bridge.visitMaxs(0, 0);
        bridge.visitEnd();
    }

    private static void emitWrap(MethodVisitor map, String internalName, int exSlot) {
        map.visitVarInsn(Opcodes.ASTORE, exSlot);
        map.visitTypeInsn(Opcodes.NEW, "com/habbashx/larvey/exception/LarveyMappingException");
        map.visitInsn(Opcodes.DUP);
        map.visitLdcInsn("Generated mapper failed");
        map.visitVarInsn(Opcodes.ALOAD, 1);
        map.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/habbashx/larvey/semantic/Configuration", "path", "()Ljava/lang/String;", false);
        map.visitVarInsn(Opcodes.ALOAD, 0);
        map.visitFieldInsn(Opcodes.GETFIELD, internalName, "owner", "Ljava/lang/Class;");
        map.visitVarInsn(Opcodes.ALOAD, exSlot);
        map.visitMethodInsn(Opcodes.INVOKESPECIAL, "com/habbashx/larvey/exception/LarveyMappingException", "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Throwable;)V", false);
        map.visitInsn(Opcodes.ATHROW);
    }

    private static Class<?>[] creatorParamTypes(ClassMetadata<?> metadata) {
        List<Class<?>> types = new ArrayList<>();
        if (metadata.creator() != null) {
            for (java.lang.reflect.Parameter parameter : metadata.creator().getParameters()) {
                types.add(parameter.getType());
            }
        }
        return types.toArray(new Class<?>[0]);
    }

    private static void pushTypeArray(MethodVisitor visitor, Class<?>[] types) {
        pushInt(visitor, types.length);
        visitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Class");
        for (int i = 0; i < types.length; i++) {
            visitor.visitInsn(Opcodes.DUP);
            pushInt(visitor, i);
            pushClass(visitor, types[i]);
            visitor.visitInsn(Opcodes.AASTORE);
        }
    }

    private static void pushAliases(MethodVisitor visitor, List<String> aliases) {
        if (aliases == null || aliases.isEmpty()) {
            visitor.visitInsn(Opcodes.ACONST_NULL);
            return;
        }
        pushInt(visitor, aliases.size());
        visitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/String");
        for (int i = 0; i < aliases.size(); i++) {
            visitor.visitInsn(Opcodes.DUP);
            pushInt(visitor, i);
            visitor.visitLdcInsn(aliases.get(i));
            visitor.visitInsn(Opcodes.AASTORE);
        }
    }

    private static void pushInt(MethodVisitor visitor, int value) {
        if (value >= 0 && value <= 5) {
            visitor.visitInsn(Opcodes.ICONST_0 + value);
        } else if (value == -1) {
            visitor.visitInsn(Opcodes.ICONST_M1);
        } else if (value <= Byte.MAX_VALUE && value >= Byte.MIN_VALUE) {
            visitor.visitIntInsn(Opcodes.BIPUSH, value);
        } else if (value <= Short.MAX_VALUE && value >= Short.MIN_VALUE) {
            visitor.visitIntInsn(Opcodes.SIPUSH, value);
        } else {
            visitor.visitLdcInsn(value);
        }
    }

    private static void pushClass(MethodVisitor visitor, Class<?> clazz) {
        if (clazz.isPrimitive()) {
            String wrapper;
            if (clazz == boolean.class) {
                wrapper = "java/lang/Boolean";
            } else if (clazz == byte.class) {
                wrapper = "java/lang/Byte";
            } else if (clazz == short.class) {
                wrapper = "java/lang/Short";
            } else if (clazz == int.class) {
                wrapper = "java/lang/Integer";
            } else if (clazz == long.class) {
                wrapper = "java/lang/Long";
            } else if (clazz == float.class) {
                wrapper = "java/lang/Float";
            } else if (clazz == double.class) {
                wrapper = "java/lang/Double";
            } else if (clazz == char.class) {
                wrapper = "java/lang/Character";
            } else {
                wrapper = "java/lang/Void";
            }
            visitor.visitFieldInsn(Opcodes.GETSTATIC, wrapper, "TYPE", "Ljava/lang/Class;");
            return;
        }
        if (isAccessible(clazz)) {
            visitor.visitLdcInsn(org.objectweb.asm.Type.getType(clazz));
            return;
        }
        visitor.visitLdcInsn(clazz.getName());
        visitor.visitMethodInsn(Opcodes.INVOKESTATIC, "com/habbashx/larvey/bytecode/BytecodeRuntime", "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;", false);
    }

    private static boolean isAccessible(Class<?> clazz) {
        if (clazz.isArray()) {
            return isAccessible(clazz.getComponentType());
        }
        Class<?> current = clazz;
        while (current != null) {
            if (!java.lang.reflect.Modifier.isPublic(current.getModifiers())) {
                return false;
            }
            current = current.getEnclosingClass();
        }
        return true;
    }

    private static void pushClassOrNull(MethodVisitor visitor, Class<?> clazz) {
        if (clazz == null) {
            visitor.visitInsn(Opcodes.ACONST_NULL);
        } else {
            pushClass(visitor, clazz);
        }
    }

    static int kindOf(Class<?> type, Type generic) {
        if (type == String.class) {
            return BytecodeRuntime.KIND_STRING;
        }
        if (type == boolean.class || type == Boolean.class) {
            return BytecodeRuntime.KIND_BOOLEAN;
        }
        if (type == int.class || type == Integer.class) {
            return BytecodeRuntime.KIND_INT;
        }
        if (type == long.class || type == Long.class) {
            return BytecodeRuntime.KIND_LONG;
        }
        if (type == double.class || type == Double.class) {
            return BytecodeRuntime.KIND_DOUBLE;
        }
        if (type == Optional.class) {
            singleElement(generic, "Optional");
            return BytecodeRuntime.KIND_OPTIONAL;
        }
        if (type == List.class || type == ArrayList.class || type == Collection.class || type == Iterable.class) {
            singleElement(generic, "List");
            return BytecodeRuntime.KIND_LIST;
        }
        if (type == Set.class || type == HashSet.class || type == LinkedHashSet.class) {
            singleElement(generic, "Set");
            return BytecodeRuntime.KIND_SET;
        }
        if (type == Map.class || type == HashMap.class || type == LinkedHashMap.class) {
            mapElements(generic);
            return BytecodeRuntime.KIND_MAP;
        }
        if (type.isArray()) {
            return BytecodeRuntime.KIND_ARRAY;
        }
        if (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type)) {
            throw new LarveyBytecodeException("Unsupported collection type '" + type.getName() + "'");
        }
        if (type.isPrimitive() || type.isEnum() || type == Object.class || isJavaType(type)) {
            return BytecodeRuntime.KIND_OTHER;
        }
        if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
            throw new LarveyBytecodeException("Unsupported abstract type '" + type.getName() + "'");
        }
        return BytecodeRuntime.KIND_BEAN;
    }

    private static void singleElement(Type generic, String what) {
        if (!(generic instanceof ParameterizedType parameterizedType) || parameterizedType.getActualTypeArguments().length != 1) {
            return;
        }
        Type arg = parameterizedType.getActualTypeArguments()[0];
        if (arg instanceof ParameterizedType) {
            throw new LarveyBytecodeException("Nested generics in " + what + " are not supported by the bytecode mapper");
        }
    }

    private static void mapElements(Type generic) {
        if (!(generic instanceof ParameterizedType parameterizedType) || parameterizedType.getActualTypeArguments().length != 2) {
            return;
        }
        for (Type arg : parameterizedType.getActualTypeArguments()) {
            if (arg instanceof ParameterizedType) {
                throw new LarveyBytecodeException("Nested generics in Map are not supported by the bytecode mapper");
            }
        }
    }

    static Class<?> rawOf(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz;
        }
        if (type instanceof ParameterizedType parameterizedType) {
            return rawOf(parameterizedType.getRawType());
        }
        if (type instanceof GenericArrayType genericArrayType) {
            Class<?> component = rawOf(genericArrayType.getGenericComponentType());
            return Array.newInstance(component, 0).getClass();
        }
        return Object.class;
    }

    static Class<?> elementClass(Type generic, int index) {
        if (generic instanceof ParameterizedType parameterizedType && parameterizedType.getActualTypeArguments().length > index) {
            return rawOf(parameterizedType.getActualTypeArguments()[index]);
        }
        return Object.class;
    }

    private static boolean isJavaType(Class<?> type) {
        Package pack = type.getPackage();
        return pack != null && pack.getName().startsWith("java.");
    }

    private static PropPlan planProperty(PropertyMetadata property) {
        boolean useSetter = property.setter() != null;
        if (!useSetter && property.field() == null) {
            throw new LarveyBytecodeException("No accessor for property '" + property.name() + "'");
        }
        if (!useSetter && Modifier.isFinal(property.field().getModifiers())) {
            throw new LarveyBytecodeException("Final field without setter for property '" + property.name() + "'");
        }
        int kind = kindOf(property.type(), property.genericType());
        Class<?> elem = null;
        Class<?> elem2 = null;
        int elemKind = -1;
        if (kind == BytecodeRuntime.KIND_LIST || kind == BytecodeRuntime.KIND_SET) {
            elem = elementClass(property.genericType(), 0);
        } else if (kind == BytecodeRuntime.KIND_ARRAY) {
            elem = property.type().getComponentType();
        } else if (kind == BytecodeRuntime.KIND_MAP) {
            elem = elementClass(property.genericType(), 0);
            elem2 = elementClass(property.genericType(), 1);
        } else if (kind == BytecodeRuntime.KIND_OPTIONAL) {
            elem = elementClass(property.genericType(), 0);
            elemKind = kindOf(elem, elem);
        }
        String member = useSetter ? property.setter().getName() : property.field().getName();
        Class<?> accessType = useSetter ? property.setter().getParameterTypes()[0] : null;
        return new PropPlan(property.name(), new ArrayList<>(property.aliases()), property.type(), kind, elem, elem2, elemKind, property.defaultValue(), property.required(), member, useSetter, accessType);
    }

    private static ParamPlan planParam(ClassMetadata.CreatorParameter param) {
        int kind = kindOf(param.type(), param.genericType());
        Class<?> elem = null;
        Class<?> elem2 = null;
        int elemKind = -1;
        if (kind == BytecodeRuntime.KIND_LIST || kind == BytecodeRuntime.KIND_SET) {
            elem = elementClass(param.genericType(), 0);
        } else if (kind == BytecodeRuntime.KIND_ARRAY) {
            elem = param.type().getComponentType();
        } else if (kind == BytecodeRuntime.KIND_MAP) {
            elem = elementClass(param.genericType(), 0);
            elem2 = elementClass(param.genericType(), 1);
        } else if (kind == BytecodeRuntime.KIND_OPTIONAL) {
            elem = elementClass(param.genericType(), 0);
            elemKind = kindOf(elem, elem);
        }
        return new ParamPlan(param.name(), new ArrayList<>(param.aliases()), param.type(), kind, elemKind, elem, elem2, param.defaultValue(), param.required());
    }

    private record PropPlan(String name, List<String> aliases, Class<?> type, int kind, Class<?> elem, Class<?> elem2, int elemKind, String defaultValue, boolean required, String memberName, boolean isSetter, Class<?> accessType) {
    }

    private record ParamPlan(String name, List<String> aliases, Class<?> type, int kind, int elemKind, Class<?> elem, Class<?> elem2, String defaultValue, boolean required) {
    }

    private static final class DynamicLoader extends ClassLoader {
        DynamicLoader(ClassLoader parent) {
            super(parent);
        }

        Class<?> define(String name, byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}
