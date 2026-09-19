package com.habbashx.larvey.plugin

import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

class LarveyFileType private constructor() : LanguageFileType(LarveyLanguage) {
    override fun getName(): String = "Larvey"
    override fun getDescription(): String = "Larvey configuration file"
    override fun getDefaultExtension(): String = "larvey"
    override fun getIcon(): Icon = IconLoader.getIcon("/META-INF/pluginIcon.svg", javaClass)

    companion object {
        @JvmField
        val INSTANCE = LarveyFileType()
    }
}

class LarveyFileTypeFactory : FileTypeFactory() {
    override fun createFileTypes(consumer: FileTypeConsumer) {
        consumer.consume(LarveyFileType.INSTANCE, "larvey")
    }
}
