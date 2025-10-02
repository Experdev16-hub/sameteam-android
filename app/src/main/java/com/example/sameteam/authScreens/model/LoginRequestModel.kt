package com.example.sameteam.authScreens.model

class LoginRequestModel(
    var email: String? = null,
    var password: String? = null,
    var platform_type: String = "ANDROID",
    var device_token: String? = null,
)