package com.habbashx.larvey.exception;

import java.io.Serial;

public class LarveyLexerException extends LarveyException {
    @Serial
    private static final long serialVersionUID = -5748294208139096855L;

    private final int line;
    private final int column;

    public LarveyLexerException(String message, int line, int column) {
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
