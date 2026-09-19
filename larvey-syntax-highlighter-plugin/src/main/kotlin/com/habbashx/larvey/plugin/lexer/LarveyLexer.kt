package com.habbashx.larvey.plugin.lexer

import com.habbashx.larvey.plugin.psi.LarveyTokenTypes
import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType

class LarveyLexer : LexerBase() {
    private var buffer: CharSequence = ""
    private var bufferEnd = 0
    private var tokenStart = 0
    private var tokenEnd = 0
    private var tokenType: IElementType? = null

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.bufferEnd = endOffset
        this.tokenStart = startOffset
        this.tokenEnd = startOffset
        advance()
    }

    override fun getState(): Int = 0
    override fun getTokenType(): IElementType? = tokenType
    override fun getTokenStart(): Int = tokenStart
    override fun getTokenEnd(): Int = tokenEnd
    override fun getBufferSequence(): CharSequence = buffer
    override fun getBufferEnd(): Int = bufferEnd

    override fun advance() {
        tokenStart = tokenEnd
        if (tokenStart >= bufferEnd) {
            tokenType = null
            return
        }
        val c = buffer[tokenStart]
        when {
            c == ' ' || c == '\t' || c == '\r' || c == '\n' -> {
                var i = tokenStart + 1
                while (i < bufferEnd) {
                    val ch = buffer[i]
                    if (ch != ' ' && ch != '\t' && ch != '\r' && ch != '\n') break
                    i++
                }
                tokenEnd = i
                tokenType = LarveyTokenTypes.WHITE_SPACE
            }
            c == '/' && peek(1) == '/' -> {
                var i = tokenStart + 2
                while (i < bufferEnd && buffer[i] != '\n') i++
                tokenEnd = i
                tokenType = LarveyTokenTypes.LINE_COMMENT
            }
            c == '/' && peek(1) == '*' -> {
                var i = tokenStart + 2
                while (i < bufferEnd && !(buffer[i] == '*' && i + 1 < bufferEnd && buffer[i + 1] == '/')) i++
                tokenEnd = if (i < bufferEnd) i + 2 else bufferEnd
                tokenType = LarveyTokenTypes.BLOCK_COMMENT
            }
            c == '"' -> {
                var i = tokenStart + 1
                while (i < bufferEnd) {
                    val ch = buffer[i]
                    if (ch == '\\' && i + 1 < bufferEnd) {
                        i += 2
                        continue
                    }
                    if (ch == '"') {
                        i++
                        break
                    }
                    if (ch == '\n') break
                    i++
                }
                tokenEnd = i
                tokenType = LarveyTokenTypes.STRING
            }
            c.isDigit() || (c == '-' && peek(1)?.isDigit() == true) -> {
                var i = tokenStart + 1
                while (i < bufferEnd && buffer[i].isDigit()) i++
                var decimal = false
                if (i < bufferEnd && buffer[i] == '.' && i + 1 < bufferEnd && buffer[i + 1].isDigit()) {
                    decimal = true
                    i++
                    while (i < bufferEnd && buffer[i].isDigit()) i++
                }
                if (i < bufferEnd && (buffer[i] == 'e' || buffer[i] == 'E')) {
                    var j = i + 1
                    if (j < bufferEnd && (buffer[j] == '+' || buffer[j] == '-')) j++
                    if (j < bufferEnd && buffer[j].isDigit()) {
                        decimal = true
                        j++
                        while (j < bufferEnd && buffer[j].isDigit()) j++
                        i = j
                    }
                }
                tokenEnd = i
                tokenType = if (decimal) LarveyTokenTypes.DECIMAL else LarveyTokenTypes.INTEGER
            }
            c.isLetter() || c == '_' -> {
                var i = tokenStart + 1
                while (i < bufferEnd && (buffer[i].isLetterOrDigit() || buffer[i] == '_' || buffer[i] == '-')) i++
                tokenEnd = i
                val text = buffer.substring(tokenStart, tokenEnd)
                tokenType = when (text) {
                    "true" -> LarveyTokenTypes.TRUE
                    "false" -> LarveyTokenTypes.FALSE
                    "null" -> LarveyTokenTypes.NULL
                    else -> LarveyTokenTypes.IDENTIFIER
                }
            }
            c == '=' -> single(LarveyTokenTypes.EQUALS)
            c == '{' -> single(LarveyTokenTypes.LEFT_BRACE)
            c == '}' -> single(LarveyTokenTypes.RIGHT_BRACE)
            c == '[' -> single(LarveyTokenTypes.LEFT_BRACKET)
            c == ']' -> single(LarveyTokenTypes.RIGHT_BRACKET)
            c == '(' -> single(LarveyTokenTypes.LEFT_PAREN)
            c == ')' -> single(LarveyTokenTypes.RIGHT_PAREN)
            c == ',' -> single(LarveyTokenTypes.COMMA)
            c == '.' -> single(LarveyTokenTypes.DOT)
            else -> single(LarveyTokenTypes.BAD_CHARACTER)
        }
    }

    private fun single(type: IElementType) {
        tokenEnd = tokenStart + 1
        tokenType = type
    }

    private fun peek(offset: Int): Char? {
        val index = tokenStart + offset
        return if (index < bufferEnd) buffer[index] else null
    }
}
