package com.example.sameteam.quickBlox.util

import android.util.Log
import com.example.sameteam.MyApplication
import com.example.sameteam.helper.SharedPrefs
import com.example.sameteam.homeScreen.bottomNavigation.chatModule.activity.CallActivity
import com.quickblox.videochat.webrtc.QBRTCSession
import com.quickblox.videochat.webrtc.callbacks.QBRTCClientSessionCallbacksImpl

object WebRtcSessionManager : QBRTCClientSessionCallbacksImpl() {
    private val TAG = WebRtcSessionManager::class.java.simpleName

    private var currentSession: QBRTCSession? = null

    fun getCurrentSession(): QBRTCSession? {
        return currentSession
    }

    fun setCurrentSession(qbCurrentSession: QBRTCSession?) {
        currentSession = qbCurrentSession
    }

    override fun onReceiveNewSession(session: QBRTCSession) {
        Log.d(TAG, "onReceiveNewSession to WebRtcSessionManager")

        val currentUser = SharedPrefs.getUser(MyApplication.getInstance())

        if(currentUser != null && currentUser.notification_status != "off"){
            if (currentSession == null) {
                setCurrentSession(session)
                val groupName = if(SharedPrefs.getGroupName(MyApplication.getInstance()).isBlank()) "" else SharedPrefs.getGroupName(MyApplication.getInstance())

                CallActivity.start(MyApplication.getInstance() , true, groupName)
            }
        }
    }

    override fun onSessionClosed(session: QBRTCSession?) {
        Log.d(TAG, "onSessionClosed WebRtcSessionManager")

        if (session == getCurrentSession()) {
            setCurrentSession(null)
        }
    }
}