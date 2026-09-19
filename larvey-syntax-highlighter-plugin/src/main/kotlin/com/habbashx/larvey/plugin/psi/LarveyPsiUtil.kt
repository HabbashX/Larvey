package com.habbashx.larvey.plugin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

object LarveyPsiUtil {
    fun keyOf(node: ASTNode): String? {
        if (node.elementType != LarveyTokenTypes.ASSIGNMENT && node.elementType != LarveyTokenTypes.BLOCK) return null
        var child = node.firstChildNode
        while (child != null) {
            if (child.elementType == LarveyTokenTypes.IDENTIFIER) return child.text
            child = child.treeNext
        }
        return null
    }

    fun membersOf(node: ASTNode): List<ASTNode> {
        val result = ArrayList<ASTNode>()
        var child = node.firstChildNode
        while (child != null) {
            val type = child.elementType
            if (type == LarveyTokenTypes.ASSIGNMENT || type == LarveyTokenTypes.BLOCK) result.add(child)
            child = child.treeNext
        }
        return result
    }

    fun findByPath(root: ASTNode, segments: List<String>): ASTNode? {
        var current = root
        for ((index, segment) in segments.withIndex()) {
            var found: ASTNode? = null
            for (member in membersOf(current)) {
                if (keyOf(member) == segment) {
                    found = member
                    break
                }
            }
            if (found == null) return null
            if (index == segments.size - 1) return found
            if (found.elementType != LarveyTokenTypes.BLOCK) return null
            current = found
        }
        return null
    }

    fun keyElement(node: ASTNode): PsiElement? {
        var child = node.firstChildNode
        while (child != null) {
            if (child.elementType == LarveyTokenTypes.IDENTIFIER) return child.psi
            child = child.treeNext
        }
        return null
    }

    fun collectPaths(node: ASTNode, prefix: String, out: MutableList<String>) {
        for (member in membersOf(node)) {
            val key = keyOf(member) ?: continue
            val path = if (prefix.isEmpty()) key else "$prefix.$key"
            out.add(path)
            if (member.elementType == LarveyTokenTypes.BLOCK) {
                collectPaths(member, path, out)
            }
        }
    }

    fun scopeNames(file: PsiElement, offset: Int): List<String> {
        val names = ArrayList<String>()
        val leaf = file.findElementAt(offset) ?: return names
        var current = innermostBlockNode(leaf.node)
        while (current != null) {
            for (member in membersOf(current)) {
                keyOf(member)?.let { if (!names.contains(it)) names.add(it) }
            }
            current = enclosingBlock(current)
        }
        return names
    }

    fun findInScope(file: PsiElement, offset: Int, segments: List<String>): ASTNode? {
        val leaf = file.findElementAt(offset) ?: return null
        var block = innermostBlockNode(leaf.node)
        while (block != null) {
            findByPath(block, segments)?.let { return it }
            block = enclosingBlock(block)
        }
        return file.node?.let { findByPath(it, segments) }
    }

    fun innermostBlockNode(node: ASTNode?): ASTNode? {
        var cursor = node
        while (cursor != null) {
            if (cursor.elementType == LarveyTokenTypes.BLOCK) return cursor
            cursor = cursor.treeParent
        }
        return null
    }

    fun enclosingBlock(node: ASTNode): ASTNode? {
        var parent = node.treeParent
        while (parent != null) {
            if (parent.elementType == LarveyTokenTypes.BLOCK) return parent
            parent = parent.treeParent
        }
        return null
    }
}
