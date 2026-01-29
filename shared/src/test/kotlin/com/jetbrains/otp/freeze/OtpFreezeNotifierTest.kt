package com.jetbrains.otp.freeze

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OtpFreezeNotifierTest {
    private val abbreviator = StackTraceAbbreviator()

    private fun assertAbbreviation(expected: String, fullName: String) {
        assertEquals(expected, abbreviator.abbreviateFullyQualifiedName(fullName))
    }

    private fun assertNotAbbreviated(fullName: String) {
        assertNull(abbreviator.abbreviateFullyQualifiedName(fullName))
    }

    @Test
    fun `JDK package with module prefix`() {
        assertAbbreviation("j.i.FileInputStream.readBytes", "java.base@21.0.9/java.io.FileInputStream.readBytes")
    }

    @Test
    fun `JDK package with different module version`() {
        assertAbbreviation("j.u.c.ConcurrentHashMap.get", "java.base@17.0.1/java.util.concurrent.ConcurrentHashMap.get")
    }

    @Test
    fun `JDK lang package with module prefix`() {
        assertAbbreviation("j.l.String.valueOf", "java.base@21.0.9/java.lang.String.valueOf")
    }

    @Test
    fun `java io package without module prefix`() {
        assertAbbreviation("j.i.FileInputStream.readBytes", "java.io.FileInputStream.readBytes")
    }

    @Test
    fun `java util package`() {
        assertAbbreviation("j.u.ArrayList.add", "java.util.ArrayList.add")
    }

    @Test
    fun `java util concurrent package`() {
        assertAbbreviation("j.u.c.ConcurrentHashMap.get", "java.util.concurrent.ConcurrentHashMap.get")
    }

    @Test
    fun `java lang package`() {
        assertAbbreviation("j.l.String.valueOf", "java.lang.String.valueOf")
    }

    @Test
    fun `javax swing package`() {
        assertAbbreviation("j.s.JFrame.setVisible", "javax.swing.JFrame.setVisible")
    }

    @Test
    fun `jdk internal package`() {
        assertAbbreviation("j.i.m.Unsafe.allocateMemory", "jdk.internal.misc.Unsafe.allocateMemory")
    }

    @Test
    fun `sun misc package`() {
        assertAbbreviation("s.m.Unsafe.getInt", "sun.misc.Unsafe.getInt")
    }

    @Test
    fun `com intellij package`() {
        assertAbbreviation("c.i.o.d.Logger.info", "com.intellij.openapi.diagnostic.Logger.info")
    }

    @Test
    fun `com jetbrains package`() {
        assertAbbreviation("c.j.r.p.RdServerProtocol.send", "com.jetbrains.rdserver.protocol.RdServerProtocol.send")
    }

    @Test
    fun `org jetbrains package`() {
        assertAbbreviation("o.j.k.i.KotlinPluginUtil.isKotlinFile", "org.jetbrains.kotlin.idea.KotlinPluginUtil.isKotlinFile")
    }

    @Test
    fun `single package level`() {
        assertAbbreviation("j.String", "java.String")
    }

    @Test
    fun `deeply nested package`() {
        assertAbbreviation("j.u.c.a.AtomicInteger.incrementAndGet", "java.util.concurrent.atomic.AtomicInteger.incrementAndGet")
    }

    @Test
    fun `class with dollar sign in name`() {
        assertAbbreviation("j.u.Map\$Entry.getKey", "java.util.Map\$Entry.getKey")
    }

    @Test
    fun `nested inner class with IntelliJ package`() {
        assertAbbreviation("c.i.o.e.Editor\$SelectionModel.getSelectedText", "com.intellij.openapi.editor.Editor\$SelectionModel.getSelectedText")
    }

    @Test
    fun `apache commons package not abbreviated`() {
        assertNotAbbreviated("org.apache.commons.lang3.StringUtils.isEmpty")
    }

    @Test
    fun `custom package not abbreviated`() {
        assertNotAbbreviated("com.example.myapp.MyClass.myMethod")
    }

    @Test
    fun `simple class name not abbreviated`() {
        assertNotAbbreviated("MyClass.myMethod")
    }

    @Test
    fun `module prefix without slash not abbreviated`() {
        assertNotAbbreviated("mymodule@1.0.0.MyClass.myMethod")
    }

    @Test
    fun `empty string not abbreviated`() {
        assertNotAbbreviated("")
    }

    @Test
    fun `module prefix with no class after slash not abbreviated`() {
        assertNotAbbreviated("java.base@21.0.9/")
    }

    @Test
    fun `org springframework package not abbreviated`() {
        assertNotAbbreviated("org.springframework.boot.SpringApplication.run")
    }

    @Test
    fun `com google package not abbreviated`() {
        assertNotAbbreviated("com.google.common.collect.ImmutableList.of")
    }

    @Test
    fun `io netty package not abbreviated`() {
        assertNotAbbreviated("io.netty.channel.ChannelHandlerContext.write")
    }

    @Test
    fun `kotlin stdlib package not abbreviated`() {
        assertNotAbbreviated("kotlin.collections.CollectionsKt.listOf")
    }

    @Test
    fun `scala package not abbreviated`() {
        assertNotAbbreviated("scala.collection.immutable.List.apply")
    }

    @Test
    fun `org junit package not abbreviated`() {
        assertNotAbbreviated("org.junit.Assert.assertEquals")
    }

    @Test
    fun `com fasterxml jackson package not abbreviated`() {
        assertNotAbbreviated("com.fasterxml.jackson.databind.ObjectMapper.readValue")
    }

    @Test
    fun `net minecraft package not abbreviated`() {
        assertNotAbbreviated("net.minecraft.server.MinecraftServer.tick")
    }

    @Test
    fun `abbreviateStackTraces integrates with full stack trace processing`() {
        val stackTrace = """
            java.lang.RuntimeException: Test exception
                at java.base@21.0.9/java.io.FileInputStream.readBytes(FileInputStream.java:123)
                at java.util.ArrayList.add(ArrayList.java:456)
                at com.intellij.openapi.diagnostic.Logger.info(Logger.java:789)
                at com.example.myapp.MyClass.myMethod(MyClass.java:100)
        """.trimIndent()

        val result = abbreviator.abbreviateStackTraces(stackTrace)

        assert(result.contains("j.i.FileInputStream.readBytes"))
        assert(result.contains("j.u.ArrayList.add"))
        assert(result.contains("c.i.o.d.Logger.info"))
        assert(result.contains("com.example.myapp.MyClass.myMethod"))
    }

    @Test
    fun `truncateToMaxBytes does not truncate when under limit`() {
        val text = "Hello World"
        val result = abbreviator.truncateToMaxBytes(text, 100)
        assertEquals(text, result)
    }

    @Test
    fun `truncateToMaxBytes truncates when over limit`() {
        val text = "Hello World, this is a long text"
        val result = abbreviator.truncateToMaxBytes(text, 20)
        assert(result.endsWith("... (truncated)"))
        assert(result.toByteArray(Charsets.UTF_8).size <= 20)
    }

    @Test
    fun `truncateToMaxBytes handles multi-byte UTF-8 characters`() {
        val text = "Hello 世界 мир 🌍"
        val maxBytes = 30
        val result = abbreviator.truncateToMaxBytes(text, maxBytes)

        val resultBytes = result.toByteArray(Charsets.UTF_8)
        assert(resultBytes.size <= maxBytes) { "Result has ${resultBytes.size} bytes, expected <= $maxBytes" }

        if (text.toByteArray(Charsets.UTF_8).size > maxBytes) {
            assert(result.contains("truncated")) { "Expected truncation marker in result" }
        }

        resultBytes.toString(Charsets.UTF_8)
    }

    @Test
    fun `truncateToMaxBytes handles exact boundary`() {
        val text = "Test"
        val suffix = "\n... (truncated)"
        val maxBytes = text.toByteArray(Charsets.UTF_8).size + suffix.toByteArray(Charsets.UTF_8).size
        val result = abbreviator.truncateToMaxBytes(text, maxBytes)
        assertEquals(text, result)
    }

    @Test
    fun `truncateToMaxBytes handles very small limit`() {
        val text = "Hello World"
        val result = abbreviator.truncateToMaxBytes(text, 5)
        assert(result.toByteArray(Charsets.UTF_8).size <= 5)
    }
}