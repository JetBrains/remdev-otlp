package com.jetbrains.otp.settings

import com.intellij.diagnostic.LoadingState
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.registry.RegistryKeyDescriptor
import kotlinx.coroutines.runBlocking
import org.jdom.Element
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class BackendPairedPluginStateEnforcementTest {
    private val originalProperty = System.getProperty(KEY)
    private val originalSupplier = Registry.registrySupplier
    private val originalLoadingState = LoadingState.values().last { it.isOccurred }

    @Before
    fun setUp() {
        initializeRegistry()
    }

    @After
    fun tearDown() {
        if (originalProperty == null) System.clearProperty(KEY) else System.setProperty(KEY, originalProperty)
        Registry.registrySupplier = originalSupplier
        LoadingState.setCurrentState(originalLoadingState)
    }

    @Test
    fun `reads default when unset`() = runBlocking {
        assertEquals(false, BackendOtpDiagnosticSettingsApiImpl().getDisablePairedPluginStateEnforcement())
    }

    @Test
    fun `reads runtime system property values`() = runBlocking {
        for (value in listOf("true", "false", "TRUE", "invalid", "")) {
            initializeRegistry()
            System.setProperty(KEY, value)
            assertEquals(value, value.equals("true", ignoreCase = true),
                         BackendOtpDiagnosticSettingsApiImpl().getDisablePairedPluginStateEnforcement())
        }
    }

    @Test
    fun `reads values set through registry API`() = runBlocking {
        for (value in listOf(true, false)) {
            initializeRegistry()
            Registry.get(KEY).setValue(value)
            assertEquals(value, BackendOtpDiagnosticSettingsApiImpl().getDisablePairedPluginStateEnforcement())
        }
    }

    @Test
    fun `reads saved registry and property conflicts with platform precedence`() = runBlocking {
        for (property in listOf(null, "true", "false")) {
            for (saved in listOf(null, "true", "false")) {
                initializeRegistry(loadState = false)
                if (property != null) System.setProperty(KEY, property)
                val state = Element("registry")
                if (saved != null) {
                    state.addContent(Element("entry").setAttribute("key", KEY).setAttribute("value", saved))
                }
                Registry.loadState(state, emptyMap())

                // IntelliJ discards saved entries equal to the default (false).
                val expected = saved == "true" || property == "true"
                assertEquals("property=$property, saved=$saved", expected,
                             BackendOtpDiagnosticSettingsApiImpl().getDisablePairedPluginStateEnforcement())
            }
        }
    }

    @Test
    fun `reads startup registry overrides`() = runBlocking {
        for (value in listOf("true", "false")) {
            initializeRegistry(loadState = false)
            System.setProperty(KEY, (!value.toBoolean()).toString())
            Registry.loadState(null, mapOf(KEY to value))
            assertEquals(value.toBoolean(), BackendOtpDiagnosticSettingsApiImpl().getDisablePairedPluginStateEnforcement())
        }
    }

    @Test
    fun `reads actual JVM D options in separate processes`() {
        for (value in listOf(true, false)) {
            runJvmProbe(listOf("-D$KEY=$value"), value)
        }
    }

    @Test
    fun `reads idea properties files in separate processes`() {
        withPropertiesFile { file, value ->
            runJvmProbe(listOf("-Didea.properties.file=$file"), value)
        }
    }

    @Test
    fun `JVM option takes precedence over idea properties file`() {
        withPropertiesFile { file, value ->
            runJvmProbe(listOf("-Didea.properties.file=$file", "-D$KEY=${!value}"), !value)
        }
    }

    private fun withPropertiesFile(test: (Path, Boolean) -> Unit) {
        val file = Files.createTempFile("paired-plugin-state-", ".properties")
        try {
            for (value in listOf(true, false)) {
                Files.writeString(file, "$KEY=$value\n")
                test(file, value)
            }
        } finally {
            Files.deleteIfExists(file)
        }
    }

    private fun runJvmProbe(options: List<String>, expected: Boolean) {
        val testClassResource = requireNotNull(javaClass.getResource("${javaClass.simpleName}.class"))
        val testClassesRoot = Path.of(testClassResource.toURI()).toString()
            .removeSuffix("${javaClass.name.replace('.', '/')}.class")
        val classpath = testClassesRoot + System.getProperty("path.separator") + System.getProperty("java.class.path")
        val process = ProcessBuilder(
            listOf(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Didea.home.path=${PathManager.getHomePath()}",
            ) + options + listOf(
                "-cp", classpath,
                JvmProbe::class.java.name,
            )
        ).redirectErrorStream(true).start()
        try {
            check(process.waitFor(30, TimeUnit.SECONDS)) { "JVM property probe timed out" }
            val output = process.inputStream.bufferedReader().readText()
            assertEquals(output, 0, process.exitValue())
            check(output.lineSequence().any { it == "backend-value=$expected" }) { output }
        } finally {
            process.destroyForcibly()
        }
    }

    companion object {
        private const val KEY = "plugins.rd.plugin.sync.disable.paired.plugin.state.enforcement"

        private fun initializeRegistry(clearProperty: Boolean = true, loadState: Boolean = true) {
            if (clearProperty) System.clearProperty(KEY)
            val registry = Registry()
            Registry.registrySupplier = { registry }
            LoadingState.setCurrentState(LoadingState.COMPONENTS_LOADED)
            Registry.setContributedKeys(mapOf(KEY to RegistryKeyDescriptor(KEY, "Test", "false", false, false, "test", "test")))
            if (loadState) Registry.loadState(null, emptyMap())
        }
    }

    object JvmProbe {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking {
            if (System.getProperty("idea.properties.file") != null) {
                PathManager.loadProperties()
            }
            initializeRegistry(clearProperty = false)
            println("backend-value=${BackendOtpDiagnosticSettingsApiImpl().getDisablePairedPluginStateEnforcement()}")
        }
    }
}
