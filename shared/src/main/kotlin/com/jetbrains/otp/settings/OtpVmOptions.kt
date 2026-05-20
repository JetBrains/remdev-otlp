package com.jetbrains.otp.settings

import com.intellij.diagnostic.EditMemorySettingsService
import com.intellij.diagnostic.VMOptions
import com.intellij.openapi.util.Pair
import java.io.IOException

@Suppress("UnstableApiUsage")
object OtpVmOptions {
    @Throws(IOException::class)
    fun setProperty(name: String, value: String) {
        val option: Pair<String, String?> = Pair.create("-D$name=", value)
        setOptionsWithFallback(listOf(option))
    }

    @Throws(IOException::class)
    fun setHeapSizeMb(valueMb: Int) {
        val memorySettingsService = EditMemorySettingsService.getInstance()
        if (memorySettingsService.userOptionsFile != null) {
            memorySettingsService.save(VMOptions.MemoryKind.HEAP, valueMb)
            return
        }

        val option: Pair<String, String?> = Pair.create(VMOptions.MemoryKind.HEAP.option, "${valueMb}m")
        setOptionsWithFallback(listOf(option))
    }

    private fun setOptionsWithFallback(options: List<Pair<String, String?>>) {
        val targetFile = VMOptions.getUserOptionsFile() ?: VMOptions.getPlatformOptionsFile()
        VMOptions.setOptions(options, targetFile)
    }
}
