package com.habbashx.larvey.plugin.highlight

import com.habbashx.larvey.plugin.lexer.LarveyLexer
import com.habbashx.larvey.plugin.psi.LarveyTokenTypes
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType

class LarveySyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = LarveyLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when (tokenType) {
            LarveyTokenTypes.IDENTIFIER -> arrayOf(LarveyTextAttributes.KEY)
            LarveyTokenTypes.STRING -> arrayOf(LarveyTextAttributes.STRING)
            LarveyTokenTypes.INTEGER, LarveyTokenTypes.DECIMAL -> arrayOf(LarveyTextAttributes.NUMBER)
            LarveyTokenTypes.TRUE, LarveyTokenTypes.FALSE -> arrayOf(LarveyTextAttributes.BOOLEAN)
            LarveyTokenTypes.NULL -> arrayOf(LarveyTextAttributes.NULL)
            LarveyTokenTypes.LINE_COMMENT -> arrayOf(LarveyTextAttributes.LINE_COMMENT)
            LarveyTokenTypes.BLOCK_COMMENT -> arrayOf(LarveyTextAttributes.BLOCK_COMMENT)
            LarveyTokenTypes.LEFT_BRACE, LarveyTokenTypes.RIGHT_BRACE -> arrayOf(LarveyTextAttributes.BRACES)
            LarveyTokenTypes.LEFT_BRACKET, LarveyTokenTypes.RIGHT_BRACKET -> arrayOf(LarveyTextAttributes.BRACKETS)
            LarveyTokenTypes.LEFT_PAREN, LarveyTokenTypes.RIGHT_PAREN -> arrayOf(LarveyTextAttributes.PARENTHESES)
            LarveyTokenTypes.EQUALS -> arrayOf(LarveyTextAttributes.EQUALS)
            LarveyTokenTypes.COMMA -> arrayOf(LarveyTextAttributes.COMMA)
            LarveyTokenTypes.DOT -> arrayOf(LarveyTextAttributes.DOT)
            LarveyTokenTypes.BAD_CHARACTER -> arrayOf(LarveyTextAttributes.BAD_CHARACTER)
            else -> TextAttributesKey.EMPTY_ARRAY
        }
    }
}
