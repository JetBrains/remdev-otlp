package com.jetbrains.otp.settings

import com.intellij.openapi.components.service

interface FrequentSpanFilterService {
    fun isFrequentSpansEnabled(): Boolean

    companion object {
        fun getInstance(): FrequentSpanFilterService = service<FrequentSpanFilterService>()
    }
}