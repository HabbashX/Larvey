package com.habbashx.larvey.processor;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

@SupportedAnnotationTypes("com.habbashx.larvey.annotations.LarveyConfig")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public final class LarveyConfigProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends javax.lang.model.element.TypeElement> annotations, RoundEnvironment roundEnv) {
        javax.lang.model.element.TypeElement larveyConfig = processingEnv.getElementUtils().getTypeElement("com.habbashx.larvey.annotations.LarveyConfig");
        if (larveyConfig == null) {
            return false;
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(larveyConfig)) {
            processConfig(element);
        }
        return false;
    }

    private void processConfig(Element element) {
        if (element.getKind() != ElementKind.CLASS && element.getKind() != ElementKind.RECORD) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "@LarveyConfig is only supported on classes and records", element);
            return;
        }
        TypeElement type = (TypeElement) element;
        String root = rootValue(type);
        List<String> properties = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        boolean isRecord = element.getKind() == ElementKind.RECORD;
        if (isRecord) {
            for (Element enclosed : type.getEnclosedElements()) {
                if (enclosed instanceof RecordComponentElement component) {
                    if (hasIgnore(component)) {
                        continue;
                    }
                    String name = propertyName(component.getSimpleName().toString(), component);
                    checkDuplicate(name, seen, component);
                    properties.add(name);
                }
            }
        } else {
            int creators = 0;
            for (Element enclosed : type.getEnclosedElements()) {
                if (enclosed.getKind() == ElementKind.CONSTRUCTOR && hasCreator(enclosed)) {
                    creators++;
                    if (creators > 1) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Multiple @LarveyCreator constructors", enclosed);
                    }
                    for (VariableElement parameter : ((ExecutableElement) enclosed).getParameters()) {
                        String name = propertyName(parameter.getSimpleName().toString(), parameter);
                        checkDuplicate(name, seen, parameter);
                        properties.add(name);
                    }
                }
            }
            if (creators == 0) {
                for (Element enclosed : type.getEnclosedElements()) {
                    if (enclosed.getKind() == ElementKind.FIELD && !enclosed.getModifiers().contains(javax.lang.model.element.Modifier.STATIC)) {
                        if (hasIgnore(enclosed)) {
                            continue;
                        }
                        String name = propertyName(enclosed.getSimpleName().toString(), enclosed);
                        checkDuplicate(name, seen, enclosed);
                        properties.add(name);
                    }
                }
            }
        }
        generateMeta(type, root, properties);
    }

    private void checkDuplicate(String name, Set<String> seen, Element element) {
        if (!seen.add(name)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Duplicate Larvey property '" + name + "'", element);
        }
    }

    private String rootValue(TypeElement type) {
        for (javax.lang.model.element.AnnotationMirror mirror : type.getAnnotationMirrors()) {
            if (mirror.getAnnotationType().toString().equals("com.habbashx.larvey.annotations.LarveyConfig")) {
                for (java.util.Map.Entry<? extends ExecutableElement, ? extends javax.lang.model.element.AnnotationValue> entry : mirror.getElementValues().entrySet()) {
                    if (entry.getKey().getSimpleName().toString().equals("value")) {
                        return entry.getValue().getValue().toString();
                    }
                }
            }
        }
        return "";
    }

    private String propertyName(String fallback, Element element) {
        for (javax.lang.model.element.AnnotationMirror mirror : element.getAnnotationMirrors()) {
            if (mirror.getAnnotationType().toString().equals("com.habbashx.larvey.annotations.LarveyProperty")) {
                for (java.util.Map.Entry<? extends ExecutableElement, ? extends javax.lang.model.element.AnnotationValue> entry : mirror.getElementValues().entrySet()) {
                    if (entry.getKey().getSimpleName().toString().equals("value")) {
                        String value = entry.getValue().getValue().toString();
                        if (!value.isEmpty()) {
                            return value;
                        }
                    }
                }
            }
        }
        return fallback;
    }

    private boolean hasIgnore(Element element) {
        return hasAnnotation(element, "com.habbashx.larvey.annotations.LarveyIgnore");
    }

    private boolean hasCreator(Element element) {
        return hasAnnotation(element, "com.habbashx.larvey.annotations.LarveyCreator");
    }

    private boolean hasAnnotation(Element element, String name) {
        for (javax.lang.model.element.AnnotationMirror mirror : element.getAnnotationMirrors()) {
            if (mirror.getAnnotationType().toString().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private void generateMeta(TypeElement type, String root, List<String> properties) {
        String packageName = processingEnv.getElementUtils().getPackageOf(type).getQualifiedName().toString();
        String simpleName = type.getSimpleName().toString();
        Element enclosing = type.getEnclosingElement();
        while (enclosing != null && (enclosing.getKind() == ElementKind.CLASS || enclosing.getKind() == ElementKind.RECORD)) {
            simpleName = enclosing.getSimpleName().toString() + "_" + simpleName;
            enclosing = enclosing.getEnclosingElement();
        }
        String metaName = simpleName + "LarveyMeta";
        try {
            String qualified = packageName.isEmpty() ? metaName : packageName + "." + metaName;
            JavaFileObject file = processingEnv.getFiler().createSourceFile(qualified, type);
            try (PrintWriter writer = new PrintWriter(file.openWriter())) {
                if (!packageName.isEmpty()) {
                    writer.println("package " + packageName + ";");
                    writer.println();
                }
                writer.println("public final class " + metaName + " {");
                writer.println("    private " + metaName + "() {");
                writer.println("    }");
                writer.println();
                writer.println("    public static final String ROOT = \"" + escape(root) + "\";");
                writer.println("    public static final String TARGET = \"" + escape(type.getQualifiedName().toString()) + "\";");
                writer.println("    public static final java.util.List<String> PROPERTIES = java.util.List.of(");
                for (int i = 0; i < properties.size(); i++) {
                    writer.print("            \"" + escape(properties.get(i)) + "\"");
                    if (i < properties.size() - 1) {
                        writer.print(",");
                    }
                    writer.println();
                }
                writer.println("    );");
                writer.println("}");
            }
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Cannot generate Larvey metadata: " + e.getMessage(), type);
        }
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
