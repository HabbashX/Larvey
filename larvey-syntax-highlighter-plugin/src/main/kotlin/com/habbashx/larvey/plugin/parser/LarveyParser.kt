package com.habbashx.larvey.plugin.parser

import com.habbashx.larvey.plugin.psi.LarveyTokenTypes
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType

class LarveyParser : PsiParser {
    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        val marker = builder.mark()
        parseMembers(builder, true)
        marker.done(root)
        return builder.treeBuilt
    }

    private fun parseMembers(builder: PsiBuilder, topLevel: Boolean) {
        while (!builder.eof()) {
            val type = builder.tokenType
            if (!topLevel && type == LarveyTokenTypes.RIGHT_BRACE) return
            if (type == LarveyTokenTypes.IDENTIFIER) {
                parseMember(builder)
            } else {
                builder.error("Expected identifier")
                builder.advanceLexer()
            }
        }
    }

    private fun parseMember(builder: PsiBuilder) {
        val actual = builder.mark()
        builder.advanceLexer()
        when (builder.tokenType) {
            LarveyTokenTypes.EQUALS -> {
                builder.advanceLexer()
                parseValue(builder)
                actual.done(LarveyTokenTypes.ASSIGNMENT)
            }
            LarveyTokenTypes.LEFT_BRACE -> {
                builder.advanceLexer()
                parseMembers(builder, false)
                if (builder.tokenType == LarveyTokenTypes.RIGHT_BRACE) {
                    builder.advanceLexer()
                } else {
                    builder.error("Expected '}'")
                }
                actual.done(LarveyTokenTypes.BLOCK)
            }
            else -> {
                builder.error("Expected '=' or '{'")
                actual.drop()
            }
        }
    }

    private fun parseValue(builder: PsiBuilder) {
        when (builder.tokenType) {
            LarveyTokenTypes.STRING,
            LarveyTokenTypes.INTEGER,
            LarveyTokenTypes.DECIMAL,
            LarveyTokenTypes.TRUE,
            LarveyTokenTypes.FALSE,
            LarveyTokenTypes.NULL -> builder.advanceLexer()
            LarveyTokenTypes.LEFT_BRACKET -> parseArray(builder)
            LarveyTokenTypes.LEFT_BRACE -> parseObject(builder)
            LarveyTokenTypes.IDENTIFIER -> {
                if (builder.lookAhead(1) == LarveyTokenTypes.LEFT_PAREN) {
                    parseFunctionCall(builder)
                } else {
                    builder.error("Expected value")
                    builder.advanceLexer()
                }
            }
            else -> {
                builder.error("Expected value")
                if (!builder.eof()) builder.advanceLexer()
            }
        }
    }

    private fun parseArray(builder: PsiBuilder) {
        val marker = builder.mark()
        builder.advanceLexer()
        while (!builder.eof() && builder.tokenType != LarveyTokenTypes.RIGHT_BRACKET) {
            parseValue(builder)
            if (builder.tokenType == LarveyTokenTypes.COMMA) {
                builder.advanceLexer()
            } else if (builder.tokenType != LarveyTokenTypes.RIGHT_BRACKET) {
                builder.error("Expected ',' or ']'")
                if (builder.tokenType != null && builder.tokenType != LarveyTokenTypes.RIGHT_BRACKET) {
                    builder.advanceLexer()
                }
            }
        }
        if (builder.tokenType == LarveyTokenTypes.RIGHT_BRACKET) {
            builder.advanceLexer()
        } else {
            builder.error("Expected ']'")
        }
        marker.done(LarveyTokenTypes.ARRAY_LITERAL)
    }

    private fun parseObject(builder: PsiBuilder) {
        val marker = builder.mark()
        builder.advanceLexer()
        while (!builder.eof() && builder.tokenType != LarveyTokenTypes.RIGHT_BRACE) {
            if (builder.tokenType == LarveyTokenTypes.IDENTIFIER) {
                parseInlineAssignment(builder)
                if (builder.tokenType == LarveyTokenTypes.COMMA) {
                    builder.advanceLexer()
                }
            } else {
                builder.error("Expected identifier")
                builder.advanceLexer()
            }
        }
        if (builder.tokenType == LarveyTokenTypes.RIGHT_BRACE) {
            builder.advanceLexer()
        } else {
            builder.error("Expected '}'")
        }
        marker.done(LarveyTokenTypes.OBJECT_LITERAL)
    }

    private fun parseInlineAssignment(builder: PsiBuilder) {
        val marker = builder.mark()
        builder.advanceLexer()
        if (builder.tokenType == LarveyTokenTypes.EQUALS) {
            builder.advanceLexer()
        } else {
            builder.error("Expected '='")
        }
        parseValue(builder)
        marker.done(LarveyTokenTypes.ASSIGNMENT)
    }

    private fun parseFunctionCall(builder: PsiBuilder) {
        val marker = builder.mark()
        builder.advanceLexer()
        if (builder.tokenType == LarveyTokenTypes.LEFT_PAREN) {
            builder.advanceLexer()
        } else {
            builder.error("Expected '('")
            marker.done(LarveyTokenTypes.FUNCTION_CALL)
            return
        }
        while (!builder.eof() && builder.tokenType != LarveyTokenTypes.RIGHT_PAREN) {
            parseValue(builder)
            if (builder.tokenType == LarveyTokenTypes.COMMA) {
                builder.advanceLexer()
            } else if (builder.tokenType != LarveyTokenTypes.RIGHT_PAREN) {
                builder.error("Expected ',' or ')'")
                builder.advanceLexer()
            }
        }
        if (builder.tokenType == LarveyTokenTypes.RIGHT_PAREN) {
            builder.advanceLexer()
        } else {
            builder.error("Expected ')'")
        }
        marker.done(LarveyTokenTypes.FUNCTION_CALL)
    }
}
