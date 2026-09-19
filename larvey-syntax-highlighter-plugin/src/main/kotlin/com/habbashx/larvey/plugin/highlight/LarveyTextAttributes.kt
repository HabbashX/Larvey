package com.habbashx.larvey.plugin.highlight

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey

object LarveyTextAttributes {
    @JvmField val KEY = key("LARVEY_KEY", DefaultLanguageHighlighterColors.IDENTIFIER)
    @JvmField val STRING = key("LARVEY_STRING", DefaultLanguageHighlighterColors.STRING)
    @JvmField val NUMBER = key("LARVEY_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
    @JvmField val BOOLEAN = key("LARVEY_BOOLEAN", DefaultLanguageHighlighterColors.KEYWORD)
    @JvmField val NULL = key("LARVEY_NULL", DefaultLanguageHighlighterColors.KEYWORD)
    @JvmField val FUNCTION_CALL = key("LARVEY_FUNCTION_CALL", DefaultLanguageHighlighterColors.FUNCTION_CALL)
    @JvmField val INTERPOLATION = key("LARVEY_INTERPOLATION", DefaultLanguageHighlighterColors.PARAMETER)
    @JvmField val LINE_COMMENT = key("LARVEY_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
    @JvmField val BLOCK_COMMENT = key("LARVEY_BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT)
    @JvmField val BRACES = key("LARVEY_BRACES", DefaultLanguageHighlighterColors.BRACES)
    @JvmField val BRACKETS = key("LARVEY_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS)
    @JvmField val PARENTHESES = key("LARVEY_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES)
    @JvmField val EQUALS = key("LARVEY_EQUALS", DefaultLanguageHighlighterColors.OPERATION_SIGN)
    @JvmField val COMMA = key("LARVEY_COMMA", DefaultLanguageHighlighterColors.COMMA)
    @JvmField val DOT = key("LARVEY_DOT", DefaultLanguageHighlighterColors.DOT)
    @JvmField val BAD_CHARACTER = key("LARVEY_BAD_CHARACTER", DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE)

    private fun key(name: String, fallback: TextAttributesKey): TextAttributesKey =
        TextAttributesKey.createTextAttributesKey(name, fallback)
}
