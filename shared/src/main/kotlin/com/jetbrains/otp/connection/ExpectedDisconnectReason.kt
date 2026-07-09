package com.jetbrains.otp.connection

enum class ExpectedDisconnectReason(val attributeValue: String) {
    APP_CLOSING("app_closing"),
    CONNECTION_DECLINED("connection_declined"),
    HOST_EXIT("host_exit"),
    HOST_RESTART("host_restart"),
    HOST_SESSION_ENDED("host_session_ended"),
    TRANSPORT_CLOSED("transport_closed"),
    USER_DISCONNECT("user_disconnect"),
}
