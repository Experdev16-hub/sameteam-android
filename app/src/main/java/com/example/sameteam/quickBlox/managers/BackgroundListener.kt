package com.example.sameteam.quickBlox.managers

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.quickblox.chat.QBChatService
import com.quickblox.chat.model.QBPresence

class LifecycleListener : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    internal fun onBackground() {
        Log.d("onBackground", "Application")
//        if(QBChatService.getInstance() != null){
//            QBChatService.getInstance().roster?.sendPresence(QBPresence(QBPresence.Type.offline))
//        }
    }

//    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
//    internal fun onResume() {
//        Log.d("OnResume", "Application")
//        if(QBChatService.getInstance() != null){
//            QBChatService.getInstance().roster?.sendPresence(QBPresence(QBPresence.Type.online))
//        }
//    }
//
//    @OnLifecycleEvent(Lifecycle.Event.ON_START)
//    internal fun onStart() {
//        Log.d("OnResume", "Application")
//        if(QBChatService.getInstance() != null) {
//            QBChatService.getInstance().roster?.sendPresence(QBPresence(QBPresence.Type.online))
//        }
//    }
}