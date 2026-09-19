package com.habbashx.larvey.mapper;

import com.habbashx.larvey.exception.LarveyMappingException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class FormatValidator {
    private FormatValidator() {
    }

    public static Object validate(Object converted, Class<?> target, String format, String path, Class<?> owner) {
        if (format == null || format.isEmpty() || converted == null) {
            return converted;
        }
        if (!(converted instanceof CharSequence)) {
            throw new LarveyMappingException("@LarveyFormat is only supported on String properties", path, owner);
        }
        String pattern = format.startsWith("regex:") ? format.substring(6) : format;
        Pattern compiled;
        try {
            compiled = Pattern.compile(pattern);
        } catch (PatternSyntaxException e) {
            throw new LarveyMappingException("Invalid @LarveyFormat regex '" + pattern + "'", path, owner, e);
        }
        if (!compiled.matcher((CharSequence) converted).matches()) {
            throw new LarveyMappingException("Value '" + converted + "' does not match format '" + pattern + "'", path, owner);
        }
        return converted;
    }
}
