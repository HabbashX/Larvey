package com.habbashx.larvey.exception;

import java.io.Serial;

public class LarveyMappingException extends LarveyException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String path;
    private final Class<?> targetType;
    private final int line;
    private final int column;

    public LarveyMappingException(String message, String path, Class<?> targetType) {
        this(message, path, targetType, -1, -1, null);
    }

    public LarveyMappingException(String message, String path, Class<?> targetType, Throwable cause) {
        this(message, path, targetType, -1, -1, cause);
    }

    public LarveyMappingException(String message, String path, Class<?> targetType, int line, int column) {
        this(message, path, targetType, line, column, null);
    }

    public LarveyMappingException(String message, String path, Class<?> targetType, int line, int column, Throwable cause) {
        super(message + " [path=" + path + ", target=" + targetType.getName() + (line >= 0 ? ", at " + line + ":" + column : "") + "]", cause);
        this.path = path;
        this.targetType = targetType;
        this.line = line;
        this.column = column;
    }

    public LarveyMappingException withLocation(int line, int column) {
        if (this.line >= 0 || line < 0) {
            return this;
        }
        return new LarveyMappingException(stripSuffix(getMessage()), path, targetType, line, column, getCause());
    }

    private static String stripSuffix(String message) {
        int index = message.lastIndexOf(" [path=");
        if (index >= 0) {
            return message.substring(0, index);
        }
        return message;
    }

    public String getPath() {
        return path;
    }

    public Class<?> getTargetType() {
        return targetType;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }
}
