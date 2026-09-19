package com.habbashx.larvey.exception;

import java.io.Serial;

public class LarveyParseException extends LarveyException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int line;
    private final int column;

    public LarveyParseException(String message, int line, int column) {
        super(message + " at " + line + ":" + column);
        this.line = line;
        this.column = column;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }
}
