package com.habbashx.larvey.exception;

import java.io.Serial;

public class LarveySerializationException extends LarveyException {
    @Serial
    private static final long serialVersionUID = 1L;

    public LarveySerializationException(String message) {
        super(message);
    }

    public LarveySerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
