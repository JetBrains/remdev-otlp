package com.jetbrains.otp.settings

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

    private fun setOptionsWithFallback(options: List<Pair<String, String?>>) {
        val targetFile = VMOptions.getUserOptionsFile() ?: VMOptions.getPlatformOptionsFile()
        VMOptions.setOptions(options, targetFile)
    }
}
