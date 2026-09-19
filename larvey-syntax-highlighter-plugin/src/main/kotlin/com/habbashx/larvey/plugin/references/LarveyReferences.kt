package com.habbashx.larvey.plugin.references

import com.habbashx.larvey.plugin.LarveyLanguage
import com.habbashx.larvey.plugin.psi.LarveyPsiUtil
import com.habbashx.larvey.plugin.psi.LarveyTokenTypes
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext

class LarveyReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement().withElementType(LarveyTokenTypes.STRING).withLanguage(LarveyLanguage),
            LarveyInterpolationReferenceProvider()
        )
    }
}

class LarveyInterpolationReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val text = element.text
        val refs = ArrayList<PsiReference>()
        var i = 0
        while (i < text.length) {
            if (text.startsWith("\$\${", i)) {
                i += 3
                continue
            }
            if (text.startsWith("\${", i)) {
                val end = text.indexOf('}', i + 2)
                if (end < 0) break
                val expr = text.substring(i + 2, end).trim()
                if (expr.isNotEmpty()) {
                    refs.add(LarveyInterpolationReference(element, TextRange(i, end + 1), expr))
                }
                i = end + 1
            } else {
                i++
            }
        }
        return refs.toTypedArray()
    }
}

class LarveyInterpolationReference(
    element: PsiElement,
    range: TextRange,
    private val path: String
) : PsiReferenceBase<PsiElement>(element, range, false) {
    override fun resolve(): PsiElement? {
        val file = element.containingFile ?: return null
        val target = LarveyPsiUtil.findInScope(file, element.textOffset, path.split(".")) ?: return null
        return LarveyPsiUtil.keyElement(target)
    }

    override fun getVariants(): Array<Any> {
        val file = element.containingFile ?: return emptyArray()
        return LarveyPsiUtil.scopeNames(file, element.textOffset).toTypedArray()
    }
}
