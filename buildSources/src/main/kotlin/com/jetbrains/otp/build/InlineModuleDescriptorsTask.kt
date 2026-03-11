package com.jetbrains.otp.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

abstract class InlineModuleDescriptorsTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val rootPluginXml: RegularFileProperty

    @get:Input
    abstract val moduleDescriptors: MapProperty<String, String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun inlineDescriptors() {
        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(rootPluginXml.get().asFile)

        val contentElement = document.documentElement
            .getElementsByTagName("content")
            .item(0) as? Element
            ?: error("Root plugin.xml is missing the <content> section.")

        val moduleElements = mutableListOf<Element>()
        for (index in 0 until contentElement.childNodes.length) {
            val node = contentElement.childNodes.item(index)
            if (node is Element && node.tagName == "module") {
                moduleElements += node
            }
        }

        val descriptors = moduleDescriptors.get()
        moduleElements.forEach { moduleElement ->
            val moduleName = moduleElement.getAttribute("name")
            val descriptorContent = descriptors[moduleName]
                ?: error("Missing descriptor mapping for module '$moduleName'.")

            while (moduleElement.firstChild != null) {
                moduleElement.removeChild(moduleElement.firstChild)
            }

            moduleElement.appendChild(
                document.createCDATASection(
                    "\n$descriptorContent\n        "
                )
            )
        }

        val targetFile = outputFile.get().asFile
        targetFile.parentFile.mkdirs()

        TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.INDENT, "yes")
            setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
            setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
        }.transform(DOMSource(document), StreamResult(targetFile))
    }
}
