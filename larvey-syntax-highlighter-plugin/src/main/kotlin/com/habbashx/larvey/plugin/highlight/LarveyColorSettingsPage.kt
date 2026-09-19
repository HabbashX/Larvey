package com.habbashx.larvey.plugin.highlight

import com.habbashx.larvey.plugin.LarveyFileType
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

class LarveyColorSettingsPage : ColorSettingsPage {
    override fun getDisplayName(): String = "Larvey"
    override fun getIcon(): Icon = LarveyFileType.INSTANCE.icon
    override fun getHighlighter(): SyntaxHighlighter = LarveySyntaxHighlighter()

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = arrayOf(
        AttributesDescriptor("Key", LarveyTextAttributes.KEY),
        AttributesDescriptor("Block name", LarveyTextAttributes.BLOCK_NAME),
        AttributesDescriptor("String", LarveyTextAttributes.STRING),
        AttributesDescriptor("Number", LarveyTextAttributes.NUMBER),
        AttributesDescriptor("Boolean", LarveyTextAttributes.BOOLEAN),
        AttributesDescriptor("Null", LarveyTextAttributes.NULL),
        AttributesDescriptor("Function call", LarveyTextAttributes.FUNCTION_CALL),
        AttributesDescriptor("Interpolation", LarveyTextAttributes.INTERPOLATION),
        AttributesDescriptor("Line comment", LarveyTextAttributes.LINE_COMMENT),
        AttributesDescriptor("Block comment", LarveyTextAttributes.BLOCK_COMMENT),
        AttributesDescriptor("Braces", LarveyTextAttributes.BRACES),
        AttributesDescriptor("Brackets", LarveyTextAttributes.BRACKETS),
        AttributesDescriptor("Parentheses", LarveyTextAttributes.PARENTHESES),
        AttributesDescriptor("Equals", LarveyTextAttributes.EQUALS),
        AttributesDescriptor("Comma", LarveyTextAttributes.COMMA),
        AttributesDescriptor("Dot", LarveyTextAttributes.DOT),
        AttributesDescriptor("Bad character", LarveyTextAttributes.BAD_CHARACTER)
    )

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> = mapOf(
        "key" to LarveyTextAttributes.KEY,
        "block" to LarveyTextAttributes.BLOCK_NAME,
        "fn" to LarveyTextAttributes.FUNCTION_CALL,
        "interp" to LarveyTextAttributes.INTERPOLATION
    )

    override fun getDemoText(): String = """// GazaPay service configuration
<block>app</block> {
    <key>name</key> = "GazaPay"
    <key>version</key> = "1.0.0"
    <key>debug</key> = true
    <key>retries</key> = 3
    <key>ratio</key> = 1.5
    <key>mode</key> = null
    <key>ports</key> = [8080, 8081]
    <key>url</key> = "<interp>${'$'}{host}</interp>:<interp>${'$'}{port}</interp>"
    <key>username</key> = <fn>env</fn>("DB_USERNAME", "root")
    <key>server</key> {
        <key>host</key> = "0.0.0.0"
        <key>port</key> = 8080
    }
}
/* multi-line
   comment */
"""
}
