package com.habbashx.larvey.exception;

import java.io.Serial;

public class LarveyException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 5960045305961528752L;

    public LarveyException(String message) {
        super(message);
    }

    public LarveyException(String message, Throwable cause) {
        super(message, cause);
    }
}
