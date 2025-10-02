package com.example.sameteam.quickBlox.util

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_EXPORTED
import android.content.Context.RECEIVER_NOT_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.concurrent.CopyOnWriteArraySet


@RequiresApi(Build.VERSION_CODES.O)
class NetworkConnectionChecker(context: Application) {

    private val connectivityManager: ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val listeners = CopyOnWriteArraySet<OnConnectivityChangedListener>()

    init {
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        context.registerReceiver(NetworkStateReceiver(), intentFilter,RECEIVER_EXPORTED)
    }

    fun registerListener(listener: OnConnectivityChangedListener) {
        listeners.add(listener)
        listener.connectivityChanged(isConnectedNow())
    }

    fun unregisterListener(listener: OnConnectivityChangedListener) {
        listeners.remove(listener)
    }

    private fun isConnectedNow(): Boolean {
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    private inner class NetworkStateReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val isConnectedNow = isConnectedNow()

            for (listener in listeners) {
                listener.connectivityChanged(isConnectedNow)
            }
        }
    }

    interface OnConnectivityChangedListener {
        fun connectivityChanged(availableNow: Boolean)
    }
}