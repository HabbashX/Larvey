package com.habbashx.larvey.plugin.annotator

import com.habbashx.larvey.lexer.Lexer
import com.habbashx.larvey.parser.Parser
import com.habbashx.larvey.plugin.highlight.LarveyTextAttributes
import com.habbashx.larvey.plugin.lexer.LarveyLexer
import com.habbashx.larvey.plugin.psi.LarveyFile
import com.habbashx.larvey.plugin.psi.LarveyTokenTypes
import com.habbashx.larvey.semantic.Configuration
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType

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
        highlightFunctions(text, holder)
    }

    private fun highlightFunctions(
        text: String,
        holder: AnnotationHolder
    ) {
        val lexer = LarveyLexer()
        lexer.start(text, 0, text.length, 0)
        val types = ArrayList<IElementType?>()
        val starts = ArrayList<Int>()
        val ends = ArrayList<Int>()
        while (lexer.tokenType != null) {
            types.add(lexer.tokenType)
            starts.add(lexer.tokenStart)
            ends.add(lexer.tokenEnd)
            lexer.advance()
        }
        for (i in types.indices) {
            val type = types[i] ?: continue
            if (type == LarveyTokenTypes.STRING) {
                highlightInterpolation(text, starts[i], ends[i], holder)
                continue
            }
            if (type != LarveyTokenTypes.IDENTIFIER) continue
            var j = i + 1
            while (j < types.size && types[j] == LarveyTokenTypes.WHITE_SPACE) j++
            if (j >= types.size) continue
            val next = types[j]
            val range = TextRange(starts[i], ends[i])
            val name = text.substring(starts[i], ends[i])
            if (next == LarveyTokenTypes.LEFT_PAREN) {
                if (!FunctionRegistryHolder.has(name)) {
                    holder.newAnnotation(HighlightSeverity.WARNING, "Unknown function '$name'").range(range).create()
                } else {
                    holder.newSilentAnnotation(HighlightSeverity.INFORMATION).range(range)
                        .textAttributes(LarveyTextAttributes.FUNCTION_CALL).create()
                }
            } else if (next == LarveyTokenTypes.LEFT_BRACE) {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION).range(range)
                    .textAttributes(LarveyTextAttributes.BLOCK_NAME).create()
            }
        }
    }

    private fun highlightInterpolation(text: String, start: Int, end: Int, holder: AnnotationHolder) {
        var i = start + 1
        while (i < end) {
            if (text.startsWith("\$\${", i)) {
                i += 3
                continue
            }
            if (text.startsWith("\${", i)) {
                val close = text.indexOf('}', i + 2)
                if (close < 0 || close >= end) return
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(TextRange(i, close + 1))
                    .textAttributes(LarveyTextAttributes.INTERPOLATION).create()
                i = close + 1
            } else {
                i++
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
