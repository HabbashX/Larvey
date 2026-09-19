package com.habbashx.larvey.parser;

import com.habbashx.larvey.ast.ArrayNode;
import com.habbashx.larvey.ast.AssignmentNode;
import com.habbashx.larvey.ast.AstNode;
import com.habbashx.larvey.ast.BlockNode;
import com.habbashx.larvey.ast.BooleanNode;
import com.habbashx.larvey.ast.ConfigurationNode;
import com.habbashx.larvey.ast.DecimalNode;
import com.habbashx.larvey.ast.FunctionCallNode;
import com.habbashx.larvey.ast.IntegerNode;
import com.habbashx.larvey.ast.NullNode;
import com.habbashx.larvey.ast.ObjectNode;
import com.habbashx.larvey.ast.SourceLocation;
import com.habbashx.larvey.ast.StringNode;
import com.habbashx.larvey.ast.ValueNode;
import com.habbashx.larvey.exception.LarveyParseException;
import com.habbashx.larvey.lexer.Token;
import com.habbashx.larvey.lexer.TokenType;
import java.util.ArrayList;
import java.util.List;

public final class Parser {
    private final List<Token> tokens;
    private int position;

    public Parser(List<Token> tokens) {
        this.tokens = List.copyOf(tokens);
    }

    public ConfigurationNode parse() {
        List<AstNode> members = new ArrayList<>();
        while (!check(TokenType.EOF)) {
            members.add(parseMember());
        }
        Token eof = advance();
        return new ConfigurationNode(members, new SourceLocation(eof.line(), eof.column()));
    }

    private AstNode parseMember() {
        Token name = expect(TokenType.IDENTIFIER, "Expected identifier");
        if (match(TokenType.EQUALS)) {
            ValueNode value = parseValue();
            return new AssignmentNode(name.lexeme(), value, loc(name));
        }
        if (check(TokenType.LEFT_BRACE)) {
            advance();
            List<AstNode> members = new ArrayList<>();
            while (!check(TokenType.RIGHT_BRACE)) {
                if (check(TokenType.EOF)) {
                    Token eof = peek();
                    throw new LarveyParseException("Unexpected end of input, expected '}'", eof.line(), eof.column());
                }
                members.add(parseMember());
            }
            advance();
            return new BlockNode(name.lexeme(), members, loc(name));
        }
        throw new LarveyParseException("Expected '=' or '{' after '" + name.lexeme() + "'", peek().line(), peek().column());
    }

    private ValueNode parseValue() {
        Token token = peek();
        return switch (token.type()) {
            case STRING -> {
                advance();
                yield buildString(token);
            }
            case INTEGER -> {
                advance();
                try {
                    yield new IntegerNode(Long.parseLong(token.lexeme()), loc(token));
                } catch (NumberFormatException e) {
                    throw new LarveyParseException("Invalid integer '" + token.lexeme() + "'", token.line(), token.column());
                }
            }
            case DECIMAL -> {
                advance();
                try {
                    yield new DecimalNode(Double.parseDouble(token.lexeme()), loc(token));
                } catch (NumberFormatException e) {
                    throw new LarveyParseException("Invalid decimal '" + token.lexeme() + "'", token.line(), token.column());
                }
            }
            case TRUE -> {
                advance();
                yield new BooleanNode(true, loc(token));
            }
            case FALSE -> {
                advance();
                yield new BooleanNode(false, loc(token));
            }
            case NULL -> {
                advance();
                yield new NullNode(loc(token));
            }
            case LEFT_BRACKET -> parseArray();
            case LEFT_BRACE -> parseObject();
            case IDENTIFIER -> parseFunctionCall();
            default -> throw new LarveyParseException("Expected value but found " + token.type(), token.line(), token.column());
        };
    }

    private ValueNode buildString(Token token) {
        String raw = token.lexeme();
        if (!raw.contains("${") && !raw.contains("$${")) {
            return new StringNode(raw, loc(token));
        }
        List<com.habbashx.larvey.ast.InterpolatedStringNode.Part> parts = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        int i = 0;
        while (i < raw.length()) {
            if (raw.startsWith("$${", i)) {
                text.append("${");
                i += 3;
            } else if (raw.startsWith("${", i)) {
                int end = raw.indexOf('}', i + 2);
                if (end < 0) {
                    throw new LarveyParseException("Unterminated interpolation in string", token.line(), token.column());
                }
                String expression = raw.substring(i + 2, end).trim();
                if (expression.isEmpty()) {
                    throw new LarveyParseException("Empty interpolation in string", token.line(), token.column());
                }
                if (text.length() > 0) {
                    parts.add(new com.habbashx.larvey.ast.InterpolatedStringNode.TextPart(text.toString()));
                    text.setLength(0);
                }
                parts.add(new com.habbashx.larvey.ast.InterpolatedStringNode.ExpressionPart(expression));
                i = end + 1;
            } else {
                text.append(raw.charAt(i));
                i++;
            }
        }
        if (text.length() > 0) {
            parts.add(new com.habbashx.larvey.ast.InterpolatedStringNode.TextPart(text.toString()));
        }
        boolean hasExpression = false;
        for (com.habbashx.larvey.ast.InterpolatedStringNode.Part part : parts) {
            if (part instanceof com.habbashx.larvey.ast.InterpolatedStringNode.ExpressionPart) {
                hasExpression = true;
                break;
            }
        }
        if (!hasExpression) {
            StringBuilder plain = new StringBuilder();
            for (com.habbashx.larvey.ast.InterpolatedStringNode.Part part : parts) {
                plain.append(((com.habbashx.larvey.ast.InterpolatedStringNode.TextPart) part).text());
            }
            return new StringNode(plain.toString(), loc(token));
        }
        return new com.habbashx.larvey.ast.InterpolatedStringNode(raw, parts, loc(token));
    }

    private ArrayNode parseArray() {
        Token start = expect(TokenType.LEFT_BRACKET, "Expected '['");
        List<ValueNode> elements = new ArrayList<>();
        if (match(TokenType.RIGHT_BRACKET)) {
            return new ArrayNode(elements, loc(start));
        }
        elements.add(parseValue());
        while (match(TokenType.COMMA)) {
            if (check(TokenType.RIGHT_BRACKET)) {
                break;
            }
            if (check(TokenType.EOF)) {
                break;
            }
            elements.add(parseValue());
        }
        expect(TokenType.RIGHT_BRACKET, "Expected ']'");
        return new ArrayNode(elements, loc(start));
    }

    private ObjectNode parseObject() {
        Token start = expect(TokenType.LEFT_BRACE, "Expected '{'");
        List<AssignmentNode> properties = new ArrayList<>();
        if (match(TokenType.RIGHT_BRACE)) {
            return new ObjectNode(properties, loc(start));
        }
        properties.add(parseInlineAssignment());
        while (match(TokenType.COMMA)) {
            if (check(TokenType.RIGHT_BRACE)) {
                break;
            }
            properties.add(parseInlineAssignment());
        }
        while (check(TokenType.IDENTIFIER)) {
            properties.add(parseInlineAssignment());
            match(TokenType.COMMA);
        }
        expect(TokenType.RIGHT_BRACE, "Expected '}'");
        return new ObjectNode(properties, loc(start));
    }

    private AssignmentNode parseInlineAssignment() {
        Token name = expect(TokenType.IDENTIFIER, "Expected identifier");
        expect(TokenType.EQUALS, "Expected '=' after '" + name.lexeme() + "'");
        ValueNode value = parseValue();
        return new AssignmentNode(name.lexeme(), value, loc(name));
    }

    private FunctionCallNode parseFunctionCall() {
        Token name = expect(TokenType.IDENTIFIER, "Expected function name");
        expect(TokenType.LEFT_PAREN, "Expected '(' after function '" + name.lexeme() + "'");
        List<ValueNode> arguments = new ArrayList<>();
        if (match(TokenType.RIGHT_PAREN)) {
            return new FunctionCallNode(name.lexeme(), arguments, loc(name));
        }
        arguments.add(parseValue());
        while (match(TokenType.COMMA)) {
            if (check(TokenType.RIGHT_PAREN)) {
                break;
            }
            arguments.add(parseValue());
        }
        expect(TokenType.RIGHT_PAREN, "Expected ')'");
        return new FunctionCallNode(name.lexeme(), arguments, loc(name));
    }

    private Token expect(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }
        Token token = peek();
        throw new LarveyParseException(message + ", found " + token.type(), token.line(), token.column());
    }

    private boolean match(TokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    private boolean check(TokenType type) {
        return peek().type() == type;
    }

    private Token peek() {
        return tokens.get(position);
    }

    private Token advance() {
        if (position < tokens.size() - 1) {
            return tokens.get(position++);
        }
        return tokens.get(tokens.size() - 1);
    }

    private SourceLocation loc(Token token) {
        return new SourceLocation(token.line(), token.column());
    }
}
