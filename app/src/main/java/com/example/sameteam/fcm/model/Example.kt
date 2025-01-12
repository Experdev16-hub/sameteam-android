package com.example.sameteam.fcm.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Example {
    @SerializedName("notification_type")
    @Expose
    var notificationType: String? = null

    @SerializedName("aps")
    @Expose
    var aps: Aps? = null

    @SerializedName("message")
    @Expose
    var message: String? = null
}

class Aps {
    @SerializedName("alert")
    @Expose
    var alert: Alert? = null
}

class Alert {
    @SerializedName("title")
    @Expose
    var title: String? = null

    @SerializedName("subtitle")
    @Expose
    var subtitle: String? = null

    @SerializedName("body")
    @Expose
    var body: String? = null
}