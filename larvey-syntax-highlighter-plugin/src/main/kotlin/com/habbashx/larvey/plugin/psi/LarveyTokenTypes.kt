package com.habbashx.larvey.plugin.psi

import com.habbashx.larvey.plugin.LarveyLanguage
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

class LarveyTokenType(debugName: String) : IElementType(debugName, LarveyLanguage)

class LarveyElementType(debugName: String) : IElementType(debugName, LarveyLanguage)

object LarveyTokenTypes {
    @JvmField val IDENTIFIER = LarveyTokenType("IDENTIFIER")
    @JvmField val STRING = LarveyTokenType("STRING")
    @JvmField val INTEGER = LarveyTokenType("INTEGER")
    @JvmField val DECIMAL = LarveyTokenType("DECIMAL")
    @JvmField val TRUE = LarveyTokenType("TRUE")
    @JvmField val FALSE = LarveyTokenType("FALSE")
    @JvmField val NULL = LarveyTokenType("NULL")
    @JvmField val EQUALS = LarveyTokenType("EQUALS")
    @JvmField val LEFT_BRACE = LarveyTokenType("LEFT_BRACE")
    @JvmField val RIGHT_BRACE = LarveyTokenType("RIGHT_BRACE")
    @JvmField val LEFT_BRACKET = LarveyTokenType("LEFT_BRACKET")
    @JvmField val RIGHT_BRACKET = LarveyTokenType("RIGHT_BRACKET")
    @JvmField val LEFT_PAREN = LarveyTokenType("LEFT_PAREN")
    @JvmField val RIGHT_PAREN = LarveyTokenType("RIGHT_PAREN")
    @JvmField val COMMA = LarveyTokenType("COMMA")
    @JvmField val DOT = LarveyTokenType("DOT")
    @JvmField val LINE_COMMENT = LarveyTokenType("LINE_COMMENT")
    @JvmField val BLOCK_COMMENT = LarveyTokenType("BLOCK_COMMENT")
    @JvmField val WHITE_SPACE = LarveyTokenType("WHITE_SPACE")
    @JvmField val BAD_CHARACTER = LarveyTokenType("BAD_CHARACTER")

    @JvmField val FILE = LarveyElementType("FILE")
    @JvmField val BLOCK = LarveyElementType("BLOCK")
    @JvmField val ASSIGNMENT = LarveyElementType("ASSIGNMENT")
    @JvmField val OBJECT_LITERAL = LarveyElementType("OBJECT_LITERAL")
    @JvmField val ARRAY_LITERAL = LarveyElementType("ARRAY_LITERAL")
    @JvmField val FUNCTION_CALL = LarveyElementType("FUNCTION_CALL")

    @JvmField val WHITE_SPACES = com.intellij.psi.tree.TokenSet.create(WHITE_SPACE, TokenType.WHITE_SPACE)
    @JvmField val COMMENTS = com.intellij.psi.tree.TokenSet.create(LINE_COMMENT, BLOCK_COMMENT)
    @JvmField val STRINGS = com.intellij.psi.tree.TokenSet.create(STRING)
}
