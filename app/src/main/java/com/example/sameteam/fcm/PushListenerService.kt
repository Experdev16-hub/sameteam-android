package com.example.sameteam.fcm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.BADGE_ICON_LARGE
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import com.example.sameteam.MyApplication
import com.example.sameteam.R
import com.example.sameteam.helper.Constants.EXTRA_FCM_MESSAGE
import com.example.sameteam.helper.Constants.SHOW_CHAT_BADGE
import com.example.sameteam.helper.Constants.SHOW_NOTIFICATION_BADGE
import com.example.sameteam.helper.SharedPrefs
import com.example.sameteam.homeScreen.HomeActivity
import com.example.sameteam.quickBlox.service.LoginService
import com.example.sameteam.quickBlox.util.SharedPrefsHelper
import com.google.firebase.messaging.RemoteMessage
import com.quickblox.messages.services.SubscribeService
import com.quickblox.messages.services.fcm.QBFcmPushListenerService
import com.quickblox.users.model.QBUser
import org.json.JSONObject

class PushListenerService : QBFcmPushListenerService() {
    private val TAG = PushListenerService::class.java.simpleName


    /**
     *     Notifications types are Event, Task, Chat, GroupChat
     */

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "onNewToken: $token")
        val tokenRefreshed = true
        SharedPrefs.storeFcmToken(this, token)
        SubscribeService.subscribeToPushes(MyApplication.getInstance(), tokenRefreshed)
    }

    override fun onMessageReceived(p0: RemoteMessage) {

        Log.d(TAG, "onMessageReceived: Main Data ${p0}")
        val messageMap = p0.data

        val user = SharedPrefs.getUser(MyApplication.getInstance())
        var notificationStatus = false

        if (user != null)
            notificationStatus = true
        //user.notification_status == "on"

        if (notificationStatus) {
            if (!messageMap["notification_type"].isNullOrBlank()) {
                when (messageMap["notification_type"]) {
                    "1", "2", "3", "4", "5", "6" -> {
                        try {
                            SharedPrefs.setNotificationBadgeAvailable(applicationContext, true)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        sendBroadcast(Intent(SHOW_NOTIFICATION_BADGE))
                        if (!messageMap["title"].isNullOrBlank() && !messageMap["body"].isNullOrBlank())// "body" replaced with "message"
                            sendNotification(
                                messageMap["notification_type"],
                                messageMap["title"],
                                messageMap["body"],// "body" replaced with "message"
                                ""
                            )
                    }

                    "Chat" -> {
                        try {
                            SharedPrefs.setChatBadgeAvailable(applicationContext, true)

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        val formattedNotification = chatMessageFormater(messageMap.toString())
                        val notificationType: String =
                            formattedNotification?.getString("notification_type")!!

                        val aps = formattedNotification.getString("aps").let { JSONObject(it) }
                        val alert: JSONObject = aps.getJSONObject("alert")
                        val title = alert.getString("title")
                        val body = alert.getString("body")
                        val subtitle = alert.optString(
                            "subtitle",
                            ""
                        ) // Using optString provides a default value if th
                        sendBroadcast(Intent(SHOW_CHAT_BADGE))
                        sendNotification(
                            notificationType,
                            title,
                            body,
                            ""
                        )
                    }

                    "GroupChat" -> {
                        try {
                            SharedPrefs.setChatBadgeAvailable(applicationContext, true)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        sendBroadcast(Intent(SHOW_CHAT_BADGE))
                        val formattedNotification = chatMessageFormater(messageMap.toString())
                        val notificationType: String =
                            formattedNotification?.optString("notification_type")!!

                        val aps = formattedNotification.getString("aps").let { JSONObject(it) }
                        val alert: JSONObject = aps.getJSONObject("alert")
                        val title = alert.getString("title")
                        val body = alert.getString("body")
                        val subtitle = alert.optString(
                            "subtitle",
                            ""
                        ) // Using optString provides a default value if th
                        if (subtitle != "") {
                            sendNotification(
                                notificationType,
                                title,
                                body,// "body" replaced with "message"
                                subtitle
                            )
                        } else {
                            sendNotification(
                                notificationType,
                                title,
                                body,// "body" replaced with "message"
                                ""
                            )
                        }

                    }
                }
            } else {

                //Call Notification
                if (messageMap["groupName"].isNullOrBlank())
                    SharedPrefs.saveGroupName(MyApplication.getInstance(), "")
                else
                    SharedPrefs.saveGroupName(
                        MyApplication.getInstance(),
                        messageMap["groupName"].toString()
                    )


                if (SharedPrefsHelper.hasQbUser()) {
                    val qbUser: QBUser? = SharedPrefsHelper.getQbUser()
                    if (qbUser != null) {
                        Log.d(TAG, "App has logged user " + qbUser.id)
                        LoginService.startBackground(this, qbUser)
                    }
                }
            }
        }
    }

    private fun chatMessageFormater(receivedPayload: String): JSONObject? {

        try {
            // Convert the received format to standard JSON format
            var receivedPayload = receivedPayload.replace("=>", ":")
            Log.d(TAG, "onMessageReceived: Data ${receivedPayload}")
            receivedPayload = receivedPayload.replace(", message=", "")
            Log.d(TAG, "onMessageReceived: Data ${receivedPayload}")
//            receivedPayload = receivedPayload.replace("\"", "")
//            Log.d(TAG, "onMessageReceived: Data ${receivedPayload}")
            receivedPayload = receivedPayload.replace("=", ":")
            Log.d(TAG, "onMessageReceived: Data ${receivedPayload}")

            val jsonObject = JSONObject(receivedPayload)
//            val notificationType: String = jsonObject.getString("notification_type")
//            val aps = JSONObject(jsonObject.getString("aps"))
//            val alert: JSONObject = aps.getJSONObject("alert")
//            val title = alert.getString("title")
//            val body = alert.getString("body")
//            val subtitle = alert.optString(
//                "subtitle",
//                ""
//            ) // Using optString provides a default value if the key doesn't exist
//
//            // Now you can use these extracted values as needed
//            Log.d(TAG, "onMessageReceived: Data ${notificationType}")
//            Log.d(TAG, "onMessageReceived: Data ${title}")
//            Log.d(TAG, "onMessageReceived: Data ${body}")
//            Log.d(TAG, "onMessageReceived: Data ${subtitle}")
//
//            println("Notification Type: $notificationType")
//            println("Title: $title")
//            println("Body: $body")
//            println("Subtitle: $subtitle")
            return jsonObject
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Log.d(TAG, "onMessageReceived: Data ${e}")
            // Handle parsing error or add more specific error handling
        }
        return null
    }


    private fun sendNotification(
        type: String?,
        title: String?,
        messageBody: String?,
        subtext: String?
    ) {

        val intent = Intent(this, HomeActivity::class.java)
        intent.putExtra(EXTRA_FCM_MESSAGE, messageBody)
        if (type == "Chat" || type == "GroupChat") {
            intent.putExtra("Navigate", "Chat")
            sendBroadcast(Intent(EXTRA_FCM_MESSAGE))
        } else if (type == "1" || type == "2" || type == "3" || type == "4" || type == "5" || type == "6")
            intent.putExtra("Navigate", "Notification")

        var msgContent = ""

        if (messageBody != null) {
            msgContent = messageBody
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        val pendingIntent: PendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(
                this,
                0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            PendingIntent.getActivity(
                this, 0 /* Request code */, intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        // receiverId == userid then display own item detail else display renter item details
        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setColor(ContextCompat.getColor(MyApplication.getInstance(), R.color.colorPrimary))
            .setSmallIcon(R.drawable.ic_app_icon)
            .setContentTitle(title)
            .setPriority(2) // max priority
            .setContentText(msgContent)
            .setAutoCancel(true)
            .setBadgeIconType(BADGE_ICON_LARGE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        if (!subtext.isNullOrBlank())
            notificationBuilder.setSubText(subtext)

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            channel.setShowBadge(true)

            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(
            notificationId++ /* ID of notification */,
            notificationBuilder.build()
        )
    }

    companion object {
        @JvmStatic
        var notificationId = 0
        private const val TAG = "MyFirebaseMsgService"
    }

    override fun sendPushMessage(data: MutableMap<Any?, Any?>?, from: String?, message: String?) {
        super.sendPushMessage(data, from, message)
        Log.v(TAG, "From: $from")
        Log.v(TAG, "Message: $message")
        if (SharedPrefsHelper.hasQbUser()) {
            val qbUser = SharedPrefsHelper.getQbUser()
            LoginService.startBackground(MyApplication.getInstance(), qbUser!!)
        }
    }

    private fun getProperString(text: String): String {
        val array = text.split(":").toTypedArray()
        val hours = array[0].toInt()
        return "$hours:${array[1]}"
    }
}