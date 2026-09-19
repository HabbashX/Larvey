package com.habbashx.larvey.exception;

import java.io.Serial;

public class LarveyBytecodeException extends LarveyException {
    @Serial
    private static final long serialVersionUID = 1L;

    public LarveyBytecodeException(String message) {
        super(message);
    }

    public LarveyBytecodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
