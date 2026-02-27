package com.jetbrains.otp.settings

import com.intellij.openapi.components.Service

@Service(Service.Level.APP)
class OtpDiagnosticFrequentSpanFilterService : FrequentSpanFilterService {
    override fun isFrequentSpansEnabled(): Boolean {
        return OtpDiagnosticSettings.getInstance().isFrequentSpansEnabled()
    }
}