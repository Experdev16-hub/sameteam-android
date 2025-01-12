package com.example.sameteam.fcm

import android.os.Bundle
import android.util.Log
import com.example.sameteam.R
import com.example.sameteam.helper.Constants
import com.quickblox.core.QBEntityCallback
import com.quickblox.core.exception.QBResponseException
import com.quickblox.core.helper.StringifyArrayList
import com.quickblox.messages.QBPushNotifications
import com.quickblox.messages.model.QBEvent
import com.quickblox.messages.model.QBEventType
import com.quickblox.messages.model.QBNotificationType
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

fun sendPushMessage(recipients: ArrayList<Int>, senderName: String, newSessionID: String,
                    opponentsIDs: String, opponentsNames: String, isVideoCall: Boolean, groupName: String) {

    val outMessage = String.format(R.string.text_push_notification_message.toString(), senderName)

//    val currentTime = Calendar.getInstance().time
//    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
//    val eventDate = simpleDateFormat.format(currentTime)

//    val timeStamp = System.currentTimeMillis()


    // Send Push: create QuickBlox Push Notification Event
    val qbEvent = QBEvent()
    qbEvent.notificationType = QBNotificationType.PUSH
    qbEvent.environment = Constants.QBEnvironment
    qbEvent.type = QBEventType.ONE_SHOT
    // Generic push - will be delivered to all platforms (Android, iOS, WP, Blackberry..)

    val json = JSONObject()
    try {
        json.put("message", outMessage)
        json.put("ios_voip", "1")
        json.put("VOIPCall", "1")
        json.put("sessionID", newSessionID)
        json.put("opponentsIDs", opponentsIDs)
        json.put("contactIdentifier", opponentsNames)
        json.put("conferenceType", if (isVideoCall) "1" else "2")
        json.put("timestamp", System.currentTimeMillis().toString())
        json.put("groupName", groupName)
    } catch (e: JSONException) {
        e.printStackTrace()
    }

    qbEvent.message = json.toString()

    val userIds = StringifyArrayList(recipients)
    qbEvent.userIds = userIds

    QBPushNotifications.createEvents(qbEvent).performAsync(object : QBEntityCallback<List<QBEvent>> {
        override fun onSuccess(p0: List<QBEvent>?, p1: Bundle?) {
            Log.d("FCM", "onSuccess: ")
        }

        override fun onError(p0: QBResponseException?) {
            Log.d("FCM", "onError: ${p0?.printStackTrace()}")
        }
    })
}