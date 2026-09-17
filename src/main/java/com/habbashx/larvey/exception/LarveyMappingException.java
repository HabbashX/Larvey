package com.habbashx.larvey.exception;

import java.io.Serial;

public class LarveyMappingException extends LarveyException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String path;
    private final Class<?> targetType;

    public LarveyMappingException(String message, String path, Class<?> targetType) {
        super(message + " [path=" + path + ", target=" + targetType.getName() + "]");
        this.path = path;
        this.targetType = targetType;
    }

    public LarveyMappingException(String message, String path, Class<?> targetType, Throwable cause) {
        super(message + " [path=" + path + ", target=" + targetType.getName() + "]", cause);
        this.path = path;
        this.targetType = targetType;
    }

    public String getPath() {
        return path;
    }

    public Class<?> getTargetType() {
        return targetType;
    }
}
