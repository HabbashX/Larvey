

import com.habbashx.larvey.exception.LarveyLexerException;
import com.habbashx.larvey.lexer.Lexer;
import com.habbashx.larvey.lexer.Token;
import com.habbashx.larvey.lexer.TokenType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LexerTest {

    private List<Token> tokenize(String source) {
        return new Lexer(source).tokenize();
    }

    @Test
    void tokenizesString() {
        List<Token> tokens = tokenize("\"hello\"");
        assertEquals(TokenType.STRING, tokens.get(0).type());
        assertEquals("hello", tokens.get(0).lexeme());
    }

    @Test
    void tokenizesEscapedString() {
        List<Token> tokens = tokenize("\"line\\ntab\\t\"");
        assertEquals("line\ntab\t", tokens.get(0).lexeme());
    }

    @Test
    void tokenizesInteger() {
        List<Token> tokens = tokenize("42");
        assertEquals(TokenType.INTEGER, tokens.get(0).type());
        assertEquals("42", tokens.get(0).lexeme());
    }

    @Test
    void tokenizesNegativeInteger() {
        List<Token> tokens = tokenize("-42");
        assertEquals(TokenType.INTEGER, tokens.get(0).type());
        assertEquals("-42", tokens.get(0).lexeme());
    }

    @Test
    void tokenizesDecimal() {
        List<Token> tokens = tokenize("185.5");
        assertEquals(TokenType.DECIMAL, tokens.get(0).type());
        assertEquals("185.5", tokens.get(0).lexeme());
    }

    @Test
    void tokenizesDecimalWithExponent() {
        List<Token> tokens = tokenize("1.5e10");
        assertEquals(TokenType.DECIMAL, tokens.get(0).type());
        assertEquals("1.5e10", tokens.get(0).lexeme());
    }

    @Test
    void tokenizesBooleansAndNull() {
        List<Token> tokens = tokenize("true false null");
        assertEquals(TokenType.TRUE, tokens.get(0).type());
        assertEquals(TokenType.FALSE, tokens.get(1).type());
        assertEquals(TokenType.NULL, tokens.get(2).type());
    }

    @Test
    void tokenizesIdentifiersWithDashAndUnderscore() {
        List<Token> tokens = tokenize("server-port database_url _private");
        assertEquals("server-port", tokens.get(0).lexeme());
        assertEquals("database_url", tokens.get(1).lexeme());
        assertEquals("_private", tokens.get(2).lexeme());
    }

    @Test
    void tokenizesSymbols() {
        List<Token> tokens = tokenize("{}[](),.=");
        assertEquals(TokenType.LEFT_BRACE, tokens.get(0).type());
        assertEquals(TokenType.RIGHT_BRACE, tokens.get(1).type());
        assertEquals(TokenType.LEFT_BRACKET, tokens.get(2).type());
        assertEquals(TokenType.RIGHT_BRACKET, tokens.get(3).type());
        assertEquals(TokenType.LEFT_PAREN, tokens.get(4).type());
        assertEquals(TokenType.RIGHT_PAREN, tokens.get(5).type());
        assertEquals(TokenType.COMMA, tokens.get(6).type());
        assertEquals(TokenType.DOT, tokens.get(7).type());
        assertEquals(TokenType.EQUALS, tokens.get(8).type());
    }

    @Test
    void skipsLineComments() {
        List<Token> tokens = tokenize("// comment\nname");
        assertEquals(TokenType.IDENTIFIER, tokens.get(0).type());
        assertEquals("name", tokens.get(0).lexeme());
    }

    @Test
    void skipsBlockComments() {
        List<Token> tokens = tokenize("/* comment\nmultiline */ name");
        assertEquals(TokenType.IDENTIFIER, tokens.get(0).type());
        assertEquals("name", tokens.get(0).lexeme());
    }

    @Test
    void endsWithEofToken() {
        List<Token> tokens = tokenize("name");
        assertEquals(TokenType.EOF, tokens.get(tokens.size() - 1).type());
    }

    @Test
    void reportsLineAndColumn() {
        List<Token> tokens = tokenize("name\n  port");
        assertEquals(1, tokens.get(0).line());
        assertEquals(1, tokens.get(0).column());
        assertEquals(2, tokens.get(1).line());
        assertEquals(3, tokens.get(1).column());
    }

    @Test
    void throwsOnUnexpectedCharacter() {
        assertThrows(LarveyLexerException.class, () -> tokenize("$"));
    }

    @Test
    void throwsOnUnterminatedString() {
        assertThrows(LarveyLexerException.class, () -> tokenize("\"unterminated"));
    }

    @Test
    void throwsOnUnterminatedStringWithNewline() {
        assertThrows(LarveyLexerException.class, () -> tokenize("\"unterminated\nstring\""));
    }

    @Test
    void throwsOnInvalidNumber() {
        assertThrows(LarveyLexerException.class, () -> tokenize("42abc"));
    }

    @Test
    void throwsOnUnterminatedBlockComment() {
        assertThrows(LarveyLexerException.class, () -> tokenize("/* unterminated"));
    }
}