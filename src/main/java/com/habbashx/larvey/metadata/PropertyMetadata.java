package com.habbashx.larvey.metadata;

import com.habbashx.larvey.annotations.LarveyAlias;
import com.habbashx.larvey.annotations.LarveyConverter;
import com.habbashx.larvey.annotations.LarveyDefault;
import com.habbashx.larvey.annotations.LarveyFormat;
import com.habbashx.larvey.annotations.LarveyIgnore;
import com.habbashx.larvey.annotations.LarveyProperty;
import com.habbashx.larvey.annotations.LarveyRequired;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Set;

public final class PropertyMetadata {
    private final String name;
    private final Set<String> aliases;
    private final Field field;
    private final Method setter;
    private final Method getter;
    private final RecordComponent recordComponent;
    private final Class<?> type;
    private final Type genericType;
    private final boolean required;
    private final String defaultValue;
    private final String format;
    private final Class<? extends com.habbashx.larvey.convert.LarveyConverter<?, ?>> converter;

    public PropertyMetadata(String name, Set<String> aliases, Field field, Method setter, Method getter, RecordComponent recordComponent, Class<?> type, Type genericType, boolean required, String defaultValue, String format, Class<? extends com.habbashx.larvey.convert.LarveyConverter<?, ?>> converter) {
        this.name = name;
        this.aliases = Set.copyOf(aliases);
        this.field = field;
        this.setter = setter;
        this.getter = getter;
        this.recordComponent = recordComponent;
        this.type = type;
        this.genericType = genericType;
        this.required = required;
        this.defaultValue = defaultValue;
        this.format = format;
        this.converter = converter;
    }

    static PropertyMetadata fromField(Field field, Method setter, Method getter) {
        if (field.isAnnotationPresent(LarveyIgnore.class)) {
            return null;
        }
        if (setter != null && setter.isAnnotationPresent(LarveyIgnore.class)) {
            return null;
        }
        String name = field.getName();
        LarveyProperty prop = field.getAnnotation(LarveyProperty.class);
        if (prop != null && !prop.value().isEmpty()) {
            name = prop.value();
        } else if (setter != null) {
            LarveyProperty sp = setter.getAnnotation(LarveyProperty.class);
            if (sp != null && !sp.value().isEmpty()) {
                name = sp.value();
            }
        }
        Set<String> aliases = new LinkedHashSet<>();
        LarveyAlias alias = field.getAnnotation(LarveyAlias.class);
        if (alias != null) {
            for (String a : alias.value()) {
                aliases.add(a);
            }
        }
        if (setter != null) {
            LarveyAlias sa = setter.getAnnotation(LarveyAlias.class);
            if (sa != null) {
                for (String a : sa.value()) {
                    aliases.add(a);
                }
            }
        }
        boolean required = field.isAnnotationPresent(LarveyRequired.class);
        String defaultValue = null;
        LarveyDefault def = field.getAnnotation(LarveyDefault.class);
        if (def != null) {
            defaultValue = def.value();
        }
        String format = null;
        LarveyFormat fmt = field.getAnnotation(LarveyFormat.class);
        if (fmt != null) {
            format = fmt.value();
        }
        Class<? extends com.habbashx.larvey.convert.LarveyConverter<?, ?>> converter = null;
        LarveyConverter conv = field.getAnnotation(LarveyConverter.class);
        if (conv != null) {
            converter = conv.value();
        }
        if (setter != null) {
            setter.setAccessible(true);
        }
        field.setAccessible(true);
        if (getter != null) {
            getter.setAccessible(true);
        }
        return new PropertyMetadata(name, aliases, field, setter, getter, null, field.getType(), field.getGenericType(), required, defaultValue, format, converter);
    }

    static PropertyMetadata fromRecordComponent(RecordComponent component) {
        if (component.isAnnotationPresent(LarveyIgnore.class)) {
            return null;
        }
        String name = component.getName();
        LarveyProperty prop = component.getAnnotation(LarveyProperty.class);
        if (prop != null && !prop.value().isEmpty()) {
            name = prop.value();
        }
        Set<String> aliases = new LinkedHashSet<>();
        LarveyAlias alias = component.getAnnotation(LarveyAlias.class);
        if (alias != null) {
            for (String a : alias.value()) {
                aliases.add(a);
            }
        }
        boolean required = component.isAnnotationPresent(LarveyRequired.class);
        String defaultValue = null;
        LarveyDefault def = component.getAnnotation(LarveyDefault.class);
        if (def != null) {
            defaultValue = def.value();
        }
        String format = null;
        LarveyFormat fmt = component.getAnnotation(LarveyFormat.class);
        if (fmt != null) {
            format = fmt.value();
        }
        Class<? extends com.habbashx.larvey.convert.LarveyConverter<?, ?>> converter = null;
        LarveyConverter conv = component.getAnnotation(LarveyConverter.class);
        if (conv != null) {
            converter = conv.value();
        }
        return new PropertyMetadata(name, aliases, null, null, component.getAccessor(), component, component.getType(), component.getGenericType(), required, defaultValue, format, converter);
    }

    public String name() {
        return name;
    }

    public Set<String> aliases() {
        return aliases;
    }

    public Field field() {
        return field;
    }

    public Method setter() {
        return setter;
    }

    public Method getter() {
        return getter;
    }

    public RecordComponent recordComponent() {
        return recordComponent;
    }

    public Class<?> type() {
        return type;
    }

    public Type genericType() {
        return genericType;
    }

    public boolean required() {
        return required;
    }

    public String defaultValue() {
        return defaultValue;
    }

    public String format() {
        return format;
    }

    public Class<? extends com.habbashx.larvey.convert.LarveyConverter<?, ?>> converter() {
        return converter;
    }

    public boolean matches(String key) {
        if (name.equals(key)) {
            return true;
        }
        return aliases.contains(key);
    }
}
