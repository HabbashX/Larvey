package com.habbashx.larvey.plugin.annotator

import com.habbashx.larvey.lexer.Lexer
import com.habbashx.larvey.lexer.TokenType
import com.habbashx.larvey.parser.Parser
import com.habbashx.larvey.plugin.highlight.LarveyTextAttributes
import com.habbashx.larvey.plugin.psi.LarveyFile
import com.habbashx.larvey.semantic.Configuration
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement

class LarveyAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is LarveyFile) return
        val project = element.project
        val document = PsiDocumentManager.getInstance(project).getDocument(element) ?: return
        val text = element.text
        val tokens = try {
            Lexer(text).tokenize()
        } catch (e: com.habbashx.larvey.exception.LarveyLexerException) {
            holder.newAnnotation(HighlightSeverity.ERROR, e.message ?: "Lexer error").range(offsetOf(document, text, e.line, e.column)).create()
            return
        }
        val ast = try {
            Parser(tokens).parse()
        } catch (e: com.habbashx.larvey.exception.LarveyParseException) {
            holder.newAnnotation(HighlightSeverity.ERROR, e.message ?: "Parse error").range(offsetOf(document, text, e.line, e.column)).create()
            return
        }
        try {
            Configuration.from(ast)
        } catch (e: com.habbashx.larvey.exception.LarveySemanticException) {
            holder.newAnnotation(HighlightSeverity.ERROR, e.message ?: "Semantic error").range(offsetOf(document, text, e.line, e.column)).create()
        }
        highlightFunctions(tokens, document, text, holder)
    }

    private fun highlightFunctions(
        tokens: List<com.habbashx.larvey.lexer.Token>,
        document: Document,
        text: String,
        holder: AnnotationHolder
    ) {
        for (i in tokens.indices) {
            val token = tokens[i]
            if (token.type != TokenType.IDENTIFIER) continue
            val next = tokens.drop(i + 1).firstOrNull { it.type != TokenType.EOF } ?: continue
            if (next.type != TokenType.LEFT_PAREN) continue
            val start = offsetOf(document, text, token.line, token.column).startOffset
            val range = TextRange(start, (start + token.lexeme.length).coerceAtMost(text.length))
            if (!FunctionRegistryHolder.has(token.lexeme)) {
                holder.newAnnotation(HighlightSeverity.WARNING, "Unknown function '${token.lexeme}'").range(range).create()
            } else {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION).range(range)
                    .textAttributes(LarveyTextAttributes.FUNCTION_CALL).create()
            }
        }
    }

    private fun offsetOf(document: Document, text: String, line: Int, column: Int): TextRange {
        val lineIndex = (line - 1).coerceIn(0, (document.lineCount - 1).coerceAtLeast(0))
        val start = (document.getLineStartOffset(lineIndex) + (column - 1)).coerceIn(0, text.length)
        return TextRange(start, (start + 1).coerceAtMost(text.length))
    }

    private object FunctionRegistryHolder {
        val registry = com.habbashx.larvey.function.FunctionRegistry()
        fun has(name: String): Boolean = registry.has(name)
    }
}
