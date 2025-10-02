package com.example.sameteam.helper

import android.content.Context
import android.content.SharedPreferences
import com.example.sameteam.authScreens.model.LoginResponseModel
import com.example.sameteam.quickBlox.util.SharedPrefsHelper
import com.google.gson.Gson

object SharedPrefs {

    private const val SHARED_PREF = "sharedPreference"
    private const val ONBOARD = "onboard"
    private const val TOKEN = "token"
    private const val USER = "user"
    private const val FCM_TOKEN = "fcmToken"
    private const val ONLINE_USERS = "online_users"
    private const val INCOMING_CALL_GROUP = "incoming_call_group"
    private const val SPOTLIGHT = "spotlight"

    private const val notificationBadgeAvailable = "notificationBadgeAvailable"
    private const val chatBadgeAvailable = "chatBadgeAvailable"


    private fun getSharedPreference(context: Context): SharedPreferences? {
        var sp: SharedPreferences? = null
        try {
            sp = context.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE)
        } catch (ignored: Exception) {

        }
        return sp
    }

    fun getOnboardStatus(context: Context): Boolean? {
        return getSharedPreference(context)?.getBoolean(ONBOARD, true)
    }

    fun setOnboardStatus(context: Context, value: Boolean) {
        val prefsEditor = getSharedPreference(context)?.edit()
        prefsEditor?.putBoolean(ONBOARD, value)
        prefsEditor?.apply()
    }

    fun getSpotlight(context: Context): Boolean? {
        return getSharedPreference(context)?.getBoolean(SPOTLIGHT, true)
    }

    fun setNotificationBadgeAvailable(context: Context, value: Boolean) {
        val prefsEditor = getSharedPreference(context)?.edit()
        prefsEditor?.putBoolean(notificationBadgeAvailable, value)
        prefsEditor?.apply()
    }

    fun getNotificationBadgeAvailable(context: Context): Boolean? {
        return getSharedPreference(context)?.getBoolean(notificationBadgeAvailable, false)
    }

    fun setChatBadgeAvailable(context: Context, value: Boolean) {
        val prefsEditor = getSharedPreference(context)?.edit()
        prefsEditor?.putBoolean(chatBadgeAvailable, value)
        prefsEditor?.apply()
    }

    fun getChatBadgeAvailable(context: Context): Boolean? {
        return getSharedPreference(context)?.getBoolean(chatBadgeAvailable, false)
    }

    fun setSpotlight(context: Context, value: Boolean) {
        val prefsEditor = getSharedPreference(context)?.edit()
        prefsEditor?.putBoolean(SPOTLIGHT, value)
        prefsEditor?.apply()
    }

    fun storeToken(context: Context, string: String) {
        val prefsEditor = getSharedPreference(context)?.edit()
        prefsEditor?.putString(TOKEN, string)
        prefsEditor?.apply()
    }

    fun clearToken(context: Context) {
        getSharedPreference(context)?.edit()?.remove(TOKEN)?.apply()
    }

    fun getToken(context: Context): String? {
        return getSharedPreference(context)?.getString(TOKEN, "")
    }

    fun setUser(context: Context, user: LoginResponseModel.User) {
        val prefsEditor = getSharedPreference(context)?.edit()
        val gson = Gson()
        val json = gson.toJson(user)
        prefsEditor?.putString(USER, json)
        prefsEditor?.apply()
    }

//    fun removeUser(context: Context) {
//        val prefsEditor = getSharedPreference(context)?.edit()
//        prefsEditor?.putString(USER, "")
//        prefsEditor?.apply()
//    }

    fun getUser(context: Context): LoginResponseModel.User? {
        val gson = Gson()
        val json = getSharedPreference(context)
            ?.getString(USER, "")
        return gson.fromJson(json, LoginResponseModel.User::class.java)
    }

    fun storeFcmToken(context: Context, string: String) {

        val prefsEditor = getSharedPreference(context)?.edit()
        prefsEditor?.putString(FCM_TOKEN, string)
        prefsEditor?.apply()
    }

    fun getFcmToken(context: Context): String? {
        return getSharedPreference(context)?.getString(FCM_TOKEN, "")
    }

//    fun removeFCMToken(context: Context) {
//        val prefsEditor = getSharedPreference(context)?.edit()
//        prefsEditor?.putString(FCM_TOKEN, "")
//        prefsEditor?.apply()
//    }

    fun saveOnlineUserId(context: Context, id: Int) {
        val editor = getSharedPreference(context)?.edit()
        val gson = Gson()

        val stringIds = getSharedPreference(context)?.getString(ONLINE_USERS, "")

        if (stringIds.isNullOrBlank()) {
            val array = intArrayOf(id)
            editor?.putString(ONLINE_USERS, gson.toJson(array))
            editor?.apply()
        } else {
            val arrayIds = gson.fromJson(stringIds, Array<Int>::class.java)
            if (!arrayIds.contains(id)) {
                val newArray = arrayIds.plus(id)
                editor?.putString(ONLINE_USERS, gson.toJson(newArray))
                editor?.apply()
            }
        }
    }

    fun removeOnlineUserId(context: Context, id: Int) {
        val editor = getSharedPreference(context)?.edit()
        val gson = Gson()

        val stringIds = getSharedPreference(context)?.getString(ONLINE_USERS, "")

        if (!stringIds.isNullOrBlank()) {
            val arrayIds = gson.fromJson(stringIds, Array<Int>::class.java)

            val newArray = arrayIds.filter { it != id }
            editor?.putString(ONLINE_USERS, gson.toJson(newArray))
            editor?.apply()
        }
    }


    fun removeAllOnlineUserIds(context: Context) {
        val editor = getSharedPreference(context)?.edit()
        editor?.putString(ONLINE_USERS, "")
        editor?.apply()
    }


    fun getOnlineUserIds(context: Context): ArrayList<Int> {
        val gson = Gson()

        val stringIds = getSharedPreference(context)?.getString(ONLINE_USERS, "")

        if (!stringIds.isNullOrBlank()) {
            val arrayIds = gson.fromJson(stringIds, Array<Int>::class.java)
            return arrayIds.toCollection(ArrayList())
        } else {
            return ArrayList()
        }
    }


    fun saveGroupName(context: Context, name: String) {
        val editor = getSharedPreference(context)?.edit()
        editor?.putString(INCOMING_CALL_GROUP, name)
        editor?.apply()
    }

    fun getGroupName(context: Context): String {
        val name = getSharedPreference(context)?.getString(INCOMING_CALL_GROUP, "")
        return if (name.isNullOrBlank()) "" else name

    }


    fun clearAllData(context: Context) {
        val editor = getSharedPreference(context)?.edit()

        editor?.putString(TOKEN, "")
        editor?.putString(USER, "")
        editor?.putString(FCM_TOKEN, "")
        editor?.putString(ONLINE_USERS, "")
        editor?.apply()
    }
}