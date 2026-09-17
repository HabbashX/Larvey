package com.habbashx.larvey.lexer;

import com.habbashx.larvey.exception.LarveyLexerException;

import java.util.ArrayList;
import java.util.List;

public final class Lexer {

    private final String source;
    private int position;
    private int line = 1;
    private int column = 1;

    public Lexer(String source) {
        this.source = source;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (true) {
            skipWhitespaceAndComments();
            if (isAtEnd()) {
                tokens.add(new Token(TokenType.EOF, "", line, column));
                break;
            }

            int startLine = line;
            int startColumn = column;
            char c = advance();

            switch (c) {
                case '=' -> tokens.add(new Token(TokenType.EQUALS, "=", startLine, startColumn));
                case '{' -> tokens.add(new Token(TokenType.LEFT_BRACE, "{", startLine, startColumn));
                case '}' -> tokens.add(new Token(TokenType.RIGHT_BRACE, "}", startLine, startColumn));
                case '[' -> tokens.add(new Token(TokenType.LEFT_BRACKET, "[", startLine, startColumn));
                case ']' -> tokens.add(new Token(TokenType.RIGHT_BRACKET, "]", startLine, startColumn));
                case '(' -> tokens.add(new Token(TokenType.LEFT_PAREN, "(", startLine, startColumn));
                case ')' -> tokens.add(new Token(TokenType.RIGHT_PAREN, ")", startLine, startColumn));
                case ',' -> tokens.add(new Token(TokenType.COMMA, ",", startLine, startColumn));
                case '.' -> tokens.add(new Token(TokenType.DOT, ".", startLine, startColumn));
                case '"' -> tokens.add(scanString(startLine, startColumn));
                default -> {
                    if (isDigit(c) || (c == '-' && isDigit(peek()))) {
                        tokens.add(scanNumber(c, startLine, startColumn));
                    } else if (isIdentifierStart(c)) {
                        tokens.add(scanIdentifierOrKeyword(c, startLine, startColumn));
                    } else {
                        throw new LarveyLexerException("Unexpected character '" + c + "'", startLine, startColumn);
                    }
                }
            }
        }
        return tokens;
    }

    private Token scanString(int startLine, int startColumn) {
        StringBuilder builder = new StringBuilder();
        while (!isAtEnd() && peek() != '"') {
            char c = advance();
            if (c == '\n') {
                throw new LarveyLexerException("Unterminated string", startLine, startColumn);
            }
            if (c == '\\' && !isAtEnd()) {
                char escaped = advance();
                builder.append(unescape(escaped, startLine, startColumn));
            } else {
                builder.append(c);
            }
        }
        if (isAtEnd()) {
            throw new LarveyLexerException("Unterminated string", startLine, startColumn);
        }
        advance();
        return new Token(TokenType.STRING, builder.toString(), startLine, startColumn);
    }

    private char unescape(char escaped, int line, int column) {
        return switch (escaped) {
            case 'n' -> '\n';
            case 't' -> '\t';
            case 'r' -> '\r';
            case '"' -> '"';
            case '\\' -> '\\';
            default -> throw new LarveyLexerException("Invalid escape sequence '\\" + escaped + "'", line, column);
        };
    }

    private Token scanNumber(char first, int startLine, int startColumn) {
        StringBuilder builder = new StringBuilder();
        builder.append(first);
        boolean isDecimal = false;

        while (!isAtEnd() && isDigit(peek())) {
            builder.append(advance());
        }

        if (!isAtEnd() && peek() == '.' && isDigit(peekNext())) {
            isDecimal = true;
            builder.append(advance());
            while (!isAtEnd() && isDigit(peek())) {
                builder.append(advance());
            }
        }

        if (!isAtEnd() && (peek() == 'e' || peek() == 'E')) {
            int mark = position;
            int markLine = line;
            int markColumn = column;
            StringBuilder exponent = new StringBuilder();
            exponent.append(advance());
            if (!isAtEnd() && (peek() == '+' || peek() == '-')) {
                exponent.append(advance());
            }
            if (!isAtEnd() && isDigit(peek())) {
                isDecimal = true;
                while (!isAtEnd() && isDigit(peek())) {
                    exponent.append(advance());
                }
                builder.append(exponent);
            } else {
                position = mark;
                line = markLine;
                column = markColumn;
            }
        }

        if (!isAtEnd() && (isIdentifierStart(peek()))) {
            throw new LarveyLexerException("Invalid number '" + builder + peek() + "'", startLine, startColumn);
        }

        return new Token(isDecimal ? TokenType.DECIMAL : TokenType.INTEGER, builder.toString(), startLine, startColumn);
    }

    private Token scanIdentifierOrKeyword(char first, int startLine, int startColumn) {
        StringBuilder builder = new StringBuilder();
        builder.append(first);
        while (!isAtEnd() && isIdentifierPart(peek())) {
            builder.append(advance());
        }
        String text = builder.toString();
        TokenType type = switch (text) {
            case "true" -> TokenType.TRUE;
            case "false" -> TokenType.FALSE;
            case "null" -> TokenType.NULL;
            default -> TokenType.IDENTIFIER;
        };
        return new Token(type, text, startLine, startColumn);
    }

    private void skipWhitespaceAndComments() {
        while (!isAtEnd()) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\r') {
                advance();
            } else if (c == '\n') {
                advance();
            } else if (c == '/' && peekNext() == '/') {
                while (!isAtEnd() && peek() != '\n') {
                    advance();
                }
            } else if (c == '/' && peekNext() == '*') {
                advance();
                advance();
                while (!isAtEnd() && !(peek() == '*' && peekNext() == '/')) {
                    advance();
                }
                if (isAtEnd()) {
                    throw new LarveyLexerException("Unterminated block comment", line, column);
                }
                advance();
                advance();
            } else {
                break;
            }
        }
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-';
    }

    private boolean isAtEnd() {
        return position >= source.length();
    }

    private char peek() {
        if (isAtEnd()) {
            return '\0';
        }
        return source.charAt(position);
    }

    private char peekNext() {
        if (position + 1 >= source.length()) {
            return '\0';
        }
        return source.charAt(position + 1);
    }

    private char advance() {
        char c = source.charAt(position++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }
}