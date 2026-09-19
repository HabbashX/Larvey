package com.habbashx.larvey.plugin.completion

import com.habbashx.larvey.plugin.LarveyLanguage
import com.habbashx.larvey.plugin.lexer.LarveyLexer
import com.habbashx.larvey.plugin.psi.LarveyPsiUtil
import com.habbashx.larvey.plugin.psi.LarveyTokenTypes
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.tree.IElementType
import com.intellij.util.ProcessingContext

class LarveyCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(LarveyLanguage),
            Provider()
        )
    }

    private class Provider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            val file = parameters.originalFile
            val caret = parameters.offset
            val text = file.text
            if (caret > text.length) return
            when (val ctx = classify(text, caret)) {
                is Ctx.Value -> {
                    result.addElement(keyword("true"))
                    result.addElement(keyword("false"))
                    result.addElement(keyword("null"))
                    for (fn in FUNCTIONS) result.addElement(function(fn, true))
                }
                is Ctx.FunctionName -> {
                    for (fn in FUNCTIONS) result.addElement(function(fn, false))
                }
                is Ctx.Interpolation -> {
                    if (ctx.parentPath.isEmpty()) {
                        val seen = LinkedHashSet<String>()
                        for (name in LarveyPsiUtil.scopeNames(file, caret)) {
                            if (seen.add(name)) result.addElement(variable(name))
                        }
                        val root = file.node ?: return
                        for (member in LarveyPsiUtil.membersOf(root)) {
                            val key = LarveyPsiUtil.keyOf(member) ?: continue
                            if (seen.add(key)) result.addElement(variable(key))
                        }
                    } else {
                        val root = file.node ?: return
                        val parent = LarveyPsiUtil.findByPath(root, ctx.parentPath)
                            ?: LarveyPsiUtil.findInScope(file, caret, ctx.parentPath) ?: return
                        val set = result.withPrefixMatcher(ctx.childPrefix)
                        for (member in LarveyPsiUtil.membersOf(parent)) {
                            val key = LarveyPsiUtil.keyOf(member) ?: continue
                            set.addElement(variable(key))
                        }
                    }
                }
                is Ctx.None -> Unit
            }
        }

        private fun keyword(name: String): LookupElement =
            LookupElementBuilder.create(name).withTypeText("keyword")

        private fun function(fn: Fn, withParens: Boolean): LookupElement {
            val builder = LookupElementBuilder.create(fn.name)
                .withTypeText(fn.doc)
                .withTailText(fn.signature, true)
                .withIcon(AllIcons.Nodes.Function)
            return if (withParens) builder.withInsertHandler(parenHandler) else builder
        }

        private fun variable(name: String): LookupElement =
            LookupElementBuilder.create(name).withTypeText("property").withIcon(AllIcons.Nodes.Variable)

        private val parenHandler = InsertHandler<LookupElement> { context, _ ->
            val offset = context.tailOffset
            context.document.insertString(offset, "()")
            context.editor.caretModel.moveToOffset(offset + 1)
        }

        private sealed interface Ctx {
            data object None : Ctx
            data object Value : Ctx
            data object FunctionName : Ctx
            data class Interpolation(val parentPath: List<String>, val childPrefix: String) : Ctx
        }

        private fun classify(text: String, caret: Int): Ctx {
            val lexer = LarveyLexer()
            lexer.start(text, 0, text.length, 0)
            val braces = ArrayDeque<IElementType>()
            var lastSignificant: IElementType? = null
            var beforeLast: IElementType? = null
            var caretIdentEnd = -1
            while (lexer.tokenType != null) {
                val type = lexer.tokenType!!
                val start = lexer.tokenStart
                val end = lexer.tokenEnd
                if (type == LarveyTokenTypes.LINE_COMMENT || type == LarveyTokenTypes.BLOCK_COMMENT) {
                    if (caret in start until end) return Ctx.None
                    lexer.advance()
                    continue
                }
                if (type == LarveyTokenTypes.STRING && caret in start until end) {
                    return stringContext(text.substring(start, end), caret - start)
                }
                if (type == LarveyTokenTypes.IDENTIFIER && caret in start..end) {
                    caretIdentEnd = end
                    break
                }
                if (end <= caret) {
                    when (type) {
                        LarveyTokenTypes.LEFT_BRACE, LarveyTokenTypes.LEFT_BRACKET, LarveyTokenTypes.LEFT_PAREN ->
                            braces.addLast(type)
                        LarveyTokenTypes.RIGHT_BRACE, LarveyTokenTypes.RIGHT_BRACKET, LarveyTokenTypes.RIGHT_PAREN ->
                            if (braces.isNotEmpty()) braces.removeLast()
                        LarveyTokenTypes.WHITE_SPACE -> Unit
                        else -> {
                            beforeLast = lastSignificant
                            lastSignificant = type
                        }
                    }
                    lexer.advance()
                    continue
                }
                break
            }
            val inner = braces.lastOrNull()
            if (caretIdentEnd >= 0) {
                var j = caretIdentEnd
                while (j < text.length && text[j].isWhitespace()) j++
                if (j < text.length && text[j] == '(') return Ctx.FunctionName
                return if (inner == LarveyTokenTypes.LEFT_BRACKET || inner == LarveyTokenTypes.LEFT_PAREN || lastSignificant == LarveyTokenTypes.EQUALS) Ctx.Value else Ctx.None
            }
            return when (lastSignificant) {
                LarveyTokenTypes.EQUALS -> Ctx.Value
                LarveyTokenTypes.LEFT_BRACKET -> Ctx.Value
                LarveyTokenTypes.LEFT_PAREN -> Ctx.Value
                LarveyTokenTypes.COMMA -> if (inner == LarveyTokenTypes.LEFT_BRACKET || inner == LarveyTokenTypes.LEFT_PAREN) Ctx.Value else Ctx.None
                LarveyTokenTypes.IDENTIFIER -> when {
                    inner == LarveyTokenTypes.LEFT_BRACKET || inner == LarveyTokenTypes.LEFT_PAREN -> Ctx.Value
                    beforeLast == LarveyTokenTypes.EQUALS -> Ctx.Value
                    else -> Ctx.None
                }
                else -> Ctx.None
            }
        }

        private fun stringContext(tokenText: String, offsetInToken: Int): Ctx {
            if (offsetInToken <= 1) return Ctx.Value
            var searchFrom = 0
            var openIndex = -1
            while (true) {
                val next = tokenText.indexOf("\${", searchFrom)
                if (next < 0 || next >= offsetInToken) break
                if (next >= 1 && tokenText[next - 1] == '$') {
                    searchFrom = next + 2
                    continue
                }
                openIndex = next
                searchFrom = next + 2
            }
            if (openIndex < 0) return Ctx.None
            val close = tokenText.indexOf('}', openIndex + 2)
            if (close >= 0 && close < offsetInToken) return Ctx.None
            val prefix = tokenText.substring(openIndex + 2, offsetInToken.coerceAtMost(tokenText.length - 1))
            val dot = prefix.lastIndexOf('.')
            return if (dot < 0) {
                Ctx.Interpolation(emptyList(), prefix)
            } else {
                Ctx.Interpolation(prefix.substring(0, dot).split("."), prefix.substring(dot + 1))
            }
        }

        companion object {
            data class Fn(val name: String, val signature: String, val doc: String)

            val FUNCTIONS = listOf(
                Fn("env", "(key[, default])", "Read an environment variable"),
                Fn("sys", "(key[, default])", "Read a Java system property"),
                Fn("file", "(path[, default])", "Read a UTF-8 text file"),
                Fn("property", "(path[, default])", "Reference another configuration value"),
                Fn("concat", "(a, b, ...)", "Concatenate strings"),
                Fn("upper", "(str)", "Convert a string to upper case"),
                Fn("lower", "(str)", "Convert a string to lower case"),
                Fn("trim", "(str)", "Strip surrounding whitespace")
            )
        }
    }
}
