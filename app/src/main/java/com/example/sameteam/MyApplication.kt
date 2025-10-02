package com.example.sameteam

import android.app.Application
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.sameteam.helper.ActivityLifecycle
import com.example.sameteam.helper.SharedPrefs
import com.example.sameteam.quickBlox.db.DbHelper
import com.example.sameteam.quickBlox.managers.LifecycleListener
import com.example.sameteam.retrofit.Retrofit
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.quickblox.auth.session.QBSettings
import com.quickblox.messages.services.QBPushManager

//Chat settings
const val USER_DEFAULT_PASSWORD = "Sameteam123"
const val CHAT_PORT = 5223
const val SOCKET_TIMEOUT = 300
const val KEEP_ALIVE: Boolean = true
const val USE_TLS: Boolean = true
const val AUTO_JOIN: Boolean = false
const val AUTO_MARK_DELIVERED: Boolean = true
const val RECONNECTION_ALLOWED: Boolean = true
const val ALLOW_LISTEN_NETWORK: Boolean = true

//App credentials
private const val APPLICATION_ID = "95122"
private const val AUTH_KEY = "K-hPEtKJLNa8yzV"
private const val AUTH_SECRET = "yB6v2MbvD5qYKSB"
private const val ACCOUNT_KEY = "nuHDzakdqeq_oQocGR7s"

//Chat credentials range
private const val MAX_PORT_VALUE = 65535
private const val MIN_PORT_VALUE = 1000
private const val MIN_SOCKET_TIMEOUT = 300
private const val MAX_SOCKET_TIMEOUT = 60000

class MyApplication : Application() {

    private lateinit var dbHelper: DbHelper

    companion object Singleton {
        private lateinit var app: MyApplication
        fun getInstance(): MyApplication {
            return app
        }
    }

    override fun onCreate() {
        super.onCreate()
        app = this
        Retrofit.init()
        registerActivityLifecycleCallbacks(ActivityLifecycle)
        dbHelper = DbHelper(this)
        checkAppCredentials()
        checkChatSettings()
        initCredentials()
        initPushManager()

        Firebase.crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

        ProcessLifecycleOwner.get().lifecycle.addObserver(LifecycleListener())

        FirebaseMessaging.getInstance().token.addOnSuccessListener { result ->
            Log.d("FCM", "Token: $result")
            SharedPrefs.storeFcmToken(this, result)
        }

        MobileAds.initialize(this) {}

    }

    private fun checkAppCredentials() {
        if (APPLICATION_ID.isEmpty() || AUTH_KEY.isEmpty() || AUTH_SECRET.isEmpty() || ACCOUNT_KEY.isEmpty()) {
            throw AssertionError("Add Creds")
        }
    }

    private fun checkChatSettings() {
        if (USER_DEFAULT_PASSWORD.isEmpty() || CHAT_PORT !in MIN_PORT_VALUE..MAX_PORT_VALUE
            || SOCKET_TIMEOUT !in MIN_SOCKET_TIMEOUT..MAX_SOCKET_TIMEOUT
        ) {
            throw AssertionError("Some parameter for Chat Settings is wrong. Please check the Chat Settings")
        }
    }

    private fun initCredentials() {
        QBSettings.getInstance().init(applicationContext, APPLICATION_ID, AUTH_KEY, AUTH_SECRET)
        QBSettings.getInstance().accountKey = ACCOUNT_KEY

        // Uncomment and put your Api and Chat servers endpoints if you want to point the sample
        // against your own server.
        //
        // QBSettings.getInstance().setEndpoints("https://your_api_endpoint.com", "your_chat_endpoint", ServiceZone.PRODUCTION);
        // QBSettings.getInstance().zone = ServiceZone.PRODUCTION
    }

    @Synchronized
    fun getDbHelper(): DbHelper {
        return dbHelper
    }

    private fun initPushManager() {
        QBPushManager.getInstance().addListener(object : QBPushManager.QBSubscribeListener {
            override fun onSubscriptionCreated() {
//                shortToast("Subscription Created")
                Log.d("Subscription", "SubscriptionCreated")
            }

            override fun onSubscriptionError(e: Exception, resultCode: Int) {
                Log.d("Subscription", "SubscriptionError" + e.localizedMessage)
                if (resultCode >= 0) {
                    val error = GoogleApiAvailability.getInstance().getErrorString(resultCode)
                    Log.d("Subscription", "SubscriptionError playServicesAbility: $error")
                }
//                shortToast(e.localizedMessage)
            }

            override fun onSubscriptionDeleted(success: Boolean) {

            }
        })
    }

}