package com.habbashx.larvey.plugin

import com.habbashx.larvey.plugin.psi.LarveyTokenTypes
import com.intellij.lang.BracePair
import com.intellij.lang.Commenter
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

class LarveyCommenter : Commenter {
    override fun getLineCommentPrefix(): String = "//"
    override fun getBlockCommentPrefix(): String = "/*"
    override fun getBlockCommentSuffix(): String = "*/"
    override fun getCommentedBlockCommentPrefix(): String? = null
    override fun getCommentedBlockCommentSuffix(): String? = null
}

class LarveyBraceMatcher : PairedBraceMatcher {
    override fun getPairs(): Array<BracePair> = arrayOf(
        BracePair(LarveyTokenTypes.LEFT_BRACE, LarveyTokenTypes.RIGHT_BRACE, true),
        BracePair(LarveyTokenTypes.LEFT_BRACKET, LarveyTokenTypes.RIGHT_BRACKET, false),
        BracePair(LarveyTokenTypes.LEFT_PAREN, LarveyTokenTypes.RIGHT_PAREN, false)
    )

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true

    override fun getCodeConstructStart(file: PsiFile, openingBraceOffset: Int): Int = openingBraceOffset
}
