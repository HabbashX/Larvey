package com.habbashx.larvey.bytecode;

import com.habbashx.larvey.exception.LarveyBytecodeException;
import com.habbashx.larvey.metadata.ClassMetadata;
import com.habbashx.larvey.metadata.PropertyMetadata;
import com.habbashx.larvey.semantic.Configuration;
import com.habbashx.larvey.semantic.LarveyValue;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public final class BytecodeGenerator {
    private BytecodeGenerator() {
    }

    public static <T> Class<? extends GeneratedMapper<T>> generate(Class<T> target) {
        ClassMetadata<T> metadata = ClassMetadata.of(target);
        if (metadata.creator() != null) {
            throw new LarveyBytecodeException("Bytecode mapper supports only no-arg bean types: " + target.getName());
        }
        if (metadata.noArgConstructor() == null) {
            throw new LarveyBytecodeException("No no-arg constructor for bytecode mapper: " + target.getName());
        }
        for (PropertyMetadata property : metadata.properties()) {
            if (!isSupported(property.type())) {
                throw new LarveyBytecodeException("Unsupported property type '" + property.type().getName() + "' for bytecode mapper");
            }
            if (property.setter() == null && property.field() == null) {
                throw new LarveyBytecodeException("No accessor for property '" + property.name() + "'");
            }
        }
        String internalName = "com/habbashx/larvey/gen/" + target.getSimpleName() + "LarveyMapper" + Math.abs(target.getName().hashCode());
        String className = internalName.replace('/', '.');
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, internalName, null, "java/lang/Object", new String[]{Type.getInternalName(GeneratedMapper.class)});
        writer.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "owner", "Ljava/lang/Class;", null, null).visitEnd();
        MethodVisitor init = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(Ljava/lang/Class;)V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitVarInsn(Opcodes.ALOAD, 1);
        init.visitFieldInsn(Opcodes.PUTFIELD, internalName, "owner", "Ljava/lang/Class;");
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();
        String targetInternal = Type.getInternalName(target);
        MethodVisitor map = writer.visitMethod(Opcodes.ACC_PUBLIC, "map", "(Lcom/habbashx/larvey/semantic/Configuration;)L" + targetInternal + ";", null, null);
        map.visitCode();
        map.visitTypeInsn(Opcodes.NEW, targetInternal);
        map.visitInsn(Opcodes.DUP);
        map.visitMethodInsn(Opcodes.INVOKESPECIAL, targetInternal, "<init>", "()V", false);
        map.visitVarInsn(Opcodes.ASTORE, 2);
        for (PropertyMetadata property : metadata.properties()) {
            emitProperty(map, internalName, targetInternal, target, property);
        }
        map.visitVarInsn(Opcodes.ALOAD, 2);
        map.visitInsn(Opcodes.ARETURN);
        map.visitMaxs(0, 0);
        map.visitEnd();
        MethodVisitor bridge = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC, "map", "(Lcom/habbashx/larvey/semantic/Configuration;)Ljava/lang/Object;", null, null);
        bridge.visitCode();
        bridge.visitVarInsn(Opcodes.ALOAD, 0);
        bridge.visitVarInsn(Opcodes.ALOAD, 1);
        bridge.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internalName, "map", "(Lcom/habbashx/larvey/semantic/Configuration;)L" + targetInternal + ";", false);
        bridge.visitInsn(Opcodes.ARETURN);
        bridge.visitMaxs(0, 0);
        bridge.visitEnd();
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

    private static void emitProperty(MethodVisitor map, String genInternal, String targetInternal, Class<?> target, PropertyMetadata property) {
        String name = property.name();
        Class<?> type = property.type();
        Label skip = new Label();
        map.visitVarInsn(Opcodes.ALOAD, 1);
        map.visitLdcInsn(name);
        map.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/habbashx/larvey/semantic/Configuration", "getProperty", "(Ljava/lang/String;)Ljava/util/Optional;", false);
        map.visitVarInsn(Opcodes.ASTORE, 3);
        map.visitVarInsn(Opcodes.ALOAD, 3);
        map.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/Optional", "isEmpty", "()Z", false);
        map.visitJumpInsn(Opcodes.IFNE, skip);
        map.visitVarInsn(Opcodes.ALOAD, 2);
        map.visitVarInsn(Opcodes.ALOAD, 3);
        map.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/Optional", "get", "()Ljava/lang/Object;", false);
        map.visitTypeInsn(Opcodes.CHECKCAST, "com/habbashx/larvey/semantic/LarveyValue");
        map.visitLdcInsn(name);
        map.visitVarInsn(Opcodes.ALOAD, 0);
        map.visitFieldInsn(Opcodes.GETFIELD, genInternal, "owner", "Ljava/lang/Class;");
        map.visitMethodInsn(Opcodes.INVOKESTATIC, "com/habbashx/larvey/bytecode/BytecodeConvert", convertMethod(type), "(Lcom/habbashx/larvey/semantic/LarveyValue;Ljava/lang/String;Ljava/lang/Class;)" + Type.getDescriptor(type), false);
        Method setter = property.setter();
        Field field = property.field();
        if (setter != null) {
            map.visitMethodInsn(Opcodes.INVOKEVIRTUAL, targetInternal, setter.getName(), Type.getMethodDescriptor(setter), false);
        } else {
            map.visitFieldInsn(Opcodes.PUTFIELD, targetInternal, field.getName(), Type.getDescriptor(type));
        }
        map.visitLabel(skip);
    }

    private static String convertMethod(Class<?> type) {
        if (type == String.class) {
            return "toString";
        }
        if (type == boolean.class) {
            return "toBoolean";
        }
        if (type == Boolean.class) {
            return "toBooleanBoxed";
        }
        if (type == int.class) {
            return "toInt";
        }
        if (type == Integer.class) {
            return "toIntBoxed";
        }
        if (type == long.class) {
            return "toLong";
        }
        if (type == Long.class) {
            return "toLongBoxed";
        }
        if (type == double.class) {
            return "toDouble";
        }
        if (type == Double.class) {
            return "toDoubleBoxed";
        }
        throw new LarveyBytecodeException("Unsupported type " + type.getName());
    }

    private static boolean isSupported(Class<?> type) {
        return type == String.class || type == boolean.class || type == Boolean.class || type == int.class || type == Integer.class || type == long.class || type == Long.class || type == double.class || type == Double.class;
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
