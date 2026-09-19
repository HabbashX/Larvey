package com.habbashx.larvey.exception;

import java.io.Serial;

public class LarveySemanticException extends LarveyException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int line;
    private final int column;
    private final String path;

    public LarveySemanticException(String message, String path, int line, int column) {
        super(message + " at " + path + " (" + line + ":" + column + ")");
        this.path = path;
        this.line = line;
        this.column = column;
    }

    public LarveySemanticException(String message, int line, int column) {
        super(message + " (" + line + ":" + column + ")");
        this.path = "";
        this.line = line;
        this.column = column;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getPath() {
        return path;
    }
}
