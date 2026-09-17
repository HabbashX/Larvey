package com.habbashx.larvey.metadata;

import com.habbashx.larvey.annotations.LarveyAlias;
import com.habbashx.larvey.annotations.LarveyConfig;
import com.habbashx.larvey.annotations.LarveyConverter;
import com.habbashx.larvey.annotations.LarveyCreator;
import com.habbashx.larvey.annotations.LarveyDefault;
import com.habbashx.larvey.annotations.LarveyFormat;
import com.habbashx.larvey.annotations.LarveyIgnore;
import com.habbashx.larvey.annotations.LarveyProperty;
import com.habbashx.larvey.annotations.LarveyRequired;
import com.habbashx.larvey.exception.LarveyMappingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClassMetadata<T> {
    private static final ConcurrentHashMap<Class<?>, ClassMetadata<?>> CACHE = new ConcurrentHashMap<>();

    private final Class<T> type;
    private final String root;
    private final boolean record;
    private final Constructor<T> creator;
    private final List<CreatorParameter> creatorParameters;
    private final List<PropertyMetadata> properties;
    private final Map<String, PropertyMetadata> lookup;
    private final Constructor<T> noArgConstructor;

    private ClassMetadata(Class<T> type, String root, boolean record, Constructor<T> creator, List<CreatorParameter> creatorParameters, List<PropertyMetadata> properties, Constructor<T> noArgConstructor) {
        this.type = type;
        this.root = root;
        this.record = record;
        this.creator = creator;
        this.creatorParameters = List.copyOf(creatorParameters);
        this.properties = List.copyOf(properties);
        Map<String, PropertyMetadata> map = new LinkedHashMap<>();
        for (PropertyMetadata p : properties) {
            map.put(p.name(), p);
            for (String a : p.aliases()) {
                map.putIfAbsent(a, p);
            }
        }
        this.lookup = Map.copyOf(map);
        this.noArgConstructor = noArgConstructor;
    }

    @SuppressWarnings("unchecked")
    public static <T> ClassMetadata<T> of(Class<T> type) {
        return (ClassMetadata<T>) CACHE.computeIfAbsent(type, ClassMetadata::build);
    }

    private static <T> ClassMetadata<T> build(Class<T> type) {
        String root = "";
        LarveyConfig config = type.getAnnotation(LarveyConfig.class);
        if (config != null) {
            root = config.value();
        }
        boolean isRecord = type.isRecord();
        Constructor<T> creator = null;
        List<CreatorParameter> creatorParameters = new ArrayList<>();
        Constructor<T> noArg = null;
        try {
            Constructor<T> c = type.getDeclaredConstructor();
            c.setAccessible(true);
            noArg = c;
        } catch (NoSuchMethodException ignored) {
        }
        if (isRecord) {
            RecordComponent[] components = type.getRecordComponents();
            Class<?>[] paramTypes = new Class<?>[components.length];
            for (int i = 0; i < components.length; i++) {
                paramTypes[i] = components[i].getType();
            }
            Constructor<T> canonical;
            try {
                canonical = type.getDeclaredConstructor(paramTypes);
                canonical.setAccessible(true);
                creator = canonical;
            } catch (NoSuchMethodException e) {
                throw new LarveyMappingException("Record has no canonical constructor", "", type);
            }
            for (int i = 0; i < components.length; i++) {
                creatorParameters.add(CreatorParameter.from(i, canonical.getParameters()[i], components[i]));
            }
        } else {
            Constructor<?>[] constructors = type.getDeclaredConstructors();
            Constructor<T> annotated = null;
            for (Constructor<?> c : constructors) {
                if (c.isAnnotationPresent(LarveyCreator.class)) {
                    if (annotated != null) {
                        throw new LarveyMappingException("Multiple @LarveyCreator constructors", "", type);
                    }
                    @SuppressWarnings("unchecked")
                    Constructor<T> found = (Constructor<T>) c;
                    annotated = found;
                }
            }
            if (annotated != null) {
                annotated.setAccessible(true);
                creator = annotated;
                java.lang.reflect.Parameter[] params = annotated.getParameters();
                for (int i = 0; i < params.length; i++) {
                    creatorParameters.add(CreatorParameter.from(i, params[i], null));
                }
            } else if (noArg == null && constructors.length == 1) {
                Constructor<T> single = (Constructor<T>) constructors[0];
                single.setAccessible(true);
                creator = single;
                java.lang.reflect.Parameter[] params = single.getParameters();
                for (int i = 0; i < params.length; i++) {
                    creatorParameters.add(CreatorParameter.from(i, params[i], null));
                }
            }
        }
        List<PropertyMetadata> properties = new ArrayList<>();
        if (isRecord) {
            for (RecordComponent component : type.getRecordComponents()) {
                PropertyMetadata pm = PropertyMetadata.fromRecordComponent(component);
                if (pm != null) {
                    properties.add(pm);
                }
            }
        } else if (creator == null) {
            Map<String, Method> setters = new LinkedHashMap<>();
            Map<String, Method> getters = new LinkedHashMap<>();
            for (Method m : type.getMethods()) {
                if (m.isAnnotationPresent(LarveyIgnore.class)) {
                    continue;
                }
                String name = m.getName();
                if (name.startsWith("set") && name.length() > 3 && m.getParameterCount() == 1 && m.getReturnType() == void.class) {
                    String prop = Character.toLowerCase(name.charAt(3)) + name.substring(4);
                    LarveyProperty lp = m.getAnnotation(LarveyProperty.class);
                    if (lp != null && !lp.value().isEmpty()) {
                        prop = lp.value();
                    }
                    setters.put(prop, m);
                }
                if ((name.startsWith("get") && name.length() > 3 && m.getParameterCount() == 0) || (name.startsWith("is") && name.length() > 2 && m.getParameterCount() == 0)) {
                    String prop = name.startsWith("get") ? Character.toLowerCase(name.charAt(3)) + name.substring(4) : Character.toLowerCase(name.charAt(2)) + name.substring(3);
                    getters.put(prop, m);
                }
            }
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (field.isSynthetic()) {
                    continue;
                }
                Method setter = setters.get(field.getName());
                if (setter == null) {
                    for (Method m : setters.values()) {
                        LarveyProperty lp = m.getAnnotation(LarveyProperty.class);
                        if (lp != null && !lp.value().isEmpty()) {
                            try {
                                if (type.getDeclaredField(m.getName().substring(3, 4).toLowerCase() + m.getName().substring(4)).equals(field)) {
                                    setter = m;
                                    break;
                                }
                            } catch (NoSuchFieldException ignored) {
                            }
                        }
                    }
                }
                PropertyMetadata pm = PropertyMetadata.fromField(field, setter, getters.get(field.getName()));
                if (pm != null) {
                    properties.add(pm);
                }
            }
        } else {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (field.isSynthetic()) {
                    continue;
                }
                PropertyMetadata pm = PropertyMetadata.fromField(field, null, null);
                if (pm != null) {
                    properties.add(pm);
                }
            }
        }
        return new ClassMetadata<>(type, root, isRecord, creator, creatorParameters, properties, noArg);
    }

    public Class<T> type() {
        return type;
    }

    public String root() {
        return root;
    }

    public boolean isRecord() {
        return record;
    }

    public Constructor<T> creator() {
        return creator;
    }

    public List<CreatorParameter> creatorParameters() {
        return creatorParameters;
    }

    public List<PropertyMetadata> properties() {
        return properties;
    }

    public PropertyMetadata find(String key) {
        return lookup.get(key);
    }

    public Constructor<T> noArgConstructor() {
        return noArgConstructor;
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public record CreatorParameter(int index, String name, List<String> aliases, Class<?> type, java.lang.reflect.Type genericType, boolean required, String defaultValue, String format, Class<? extends com.habbashx.larvey.convert.LarveyConverter<?, ?>> converter) {
        static CreatorParameter from(int index, java.lang.reflect.Parameter parameter, RecordComponent component) {
            String name = parameter.getName();
            List<String> aliases = new ArrayList<>();
            boolean required = false;
            String defaultValue = null;
            String format = null;
            Class<? extends com.habbashx.larvey.convert.LarveyConverter<?, ?>> converter = null;
            LarveyProperty lp = parameter.getAnnotation(LarveyProperty.class);
            if (lp != null && !lp.value().isEmpty()) {
                name = lp.value();
            }
            LarveyAlias al = parameter.getAnnotation(LarveyAlias.class);
            if (al != null) {
                for (String a : al.value()) {
                    aliases.add(a);
                }
            }
            if (parameter.isAnnotationPresent(LarveyRequired.class)) {
                required = true;
            }
            LarveyDefault def = parameter.getAnnotation(LarveyDefault.class);
            if (def != null) {
                defaultValue = def.value();
            }
            LarveyConverter conv = parameter.getAnnotation(LarveyConverter.class);
            if (conv != null) {
                converter = conv.value();
            }
                LarveyFormat fmt = parameter.getAnnotation(LarveyFormat.class);
                if (fmt != null) {
                    format = fmt.value();
                }
                if (component != null) {
                if ((lp == null || lp.value().isEmpty())) {
                    LarveyProperty clp = component.getAnnotation(LarveyProperty.class);
                    if (clp != null && !clp.value().isEmpty()) {
                        name = clp.value();
                    } else {
                        name = component.getName();
                    }
                }
                LarveyAlias cal = component.getAnnotation(LarveyAlias.class);
                if (cal != null) {
                    for (String a : cal.value()) {
                        if (!aliases.contains(a)) {
                            aliases.add(a);
                        }
                    }
                }
                if (component.isAnnotationPresent(LarveyRequired.class)) {
                    required = true;
                }
                LarveyDefault cdef = component.getAnnotation(LarveyDefault.class);
                if (cdef != null && defaultValue == null) {
                    defaultValue = cdef.value();
                }
                LarveyConverter cconv = component.getAnnotation(LarveyConverter.class);
                if (cconv != null && converter == null) {
                    converter = cconv.value();
                }
                LarveyFormat cfmt = component.getAnnotation(LarveyFormat.class);
                if (cfmt != null && format == null) {
                    format = cfmt.value();
                }
            }
            return new CreatorParameter(index, name, List.copyOf(aliases), parameter.getType(), parameter.getParameterizedType(), required, defaultValue, format, converter);
        }

        public boolean matches(String key) {
            if (name.equals(key)) {
                return true;
            }
            return aliases.contains(key);
        }
    }
}
