package com.habbashx.larvey.plugin.psi

import com.habbashx.larvey.plugin.LarveyLanguage
import com.habbashx.larvey.plugin.lexer.LarveyLexer
import com.habbashx.larvey.plugin.parser.LarveyParser
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType

class LarveyFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, LarveyLanguage) {
    override fun getFileType() = com.habbashx.larvey.plugin.LarveyFileType.INSTANCE
    override fun toString(): String = "Larvey File"
}

class LarveyParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?) = LarveyLexer()
    override fun createParser(project: Project?) = LarveyParser()
    override fun getFileNodeType(): IFileElementType = IFileElementType(LarveyLanguage)
    override fun getWhitespaceTokens() = LarveyTokenTypes.WHITE_SPACES
    override fun getCommentTokens() = LarveyTokenTypes.COMMENTS
    override fun getStringLiteralElements() = LarveyTokenTypes.STRINGS
    override fun createElement(node: ASTNode): PsiElement = ASTWrapperPsiElement(node)
    override fun createFile(viewProvider: FileViewProvider): PsiFile = LarveyFile(viewProvider)
    override fun spaceExistenceTypeBetweenTokens(left: ASTNode, right: ASTNode): ParserDefinition.SpaceRequirements =
        ParserDefinition.SpaceRequirements.MAY
}
