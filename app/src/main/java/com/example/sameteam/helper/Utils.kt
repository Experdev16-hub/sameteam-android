package com.example.sameteam.helper

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputFilter
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.sameteam.BuildConfig
import com.example.sameteam.authScreens.model.UserModel
import com.example.sameteam.helper.Constants.APR
import com.example.sameteam.helper.Constants.AUG
import com.example.sameteam.helper.Constants.DEC
import com.example.sameteam.helper.Constants.FEB
import com.example.sameteam.helper.Constants.JAN
import com.example.sameteam.helper.Constants.JUL
import com.example.sameteam.helper.Constants.JUN
import com.example.sameteam.helper.Constants.MAR
import com.example.sameteam.helper.Constants.MAY
import com.example.sameteam.helper.Constants.NOV
import com.example.sameteam.helper.Constants.OCT
import com.example.sameteam.helper.Constants.SEP
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.commons.io.FilenameUtils
import com.google.gson.Gson
import org.webrtc.ContextUtils.getApplicationContext
import java.io.File
import java.time.YearMonth
import java.util.*


object Utils {

    fun getMimeTypeExtension(fileName: File): String? {
        try {
            val extension: String = FilenameUtils.getExtension(fileName.getAbsolutePath())
            return extension
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""

    }

    fun checkSelectValidFile(filePath: File): Boolean? {
        try {
            return (getMimeTypeExtension(filePath)?.equals(
                "pdf",
                true
            ) == true || getMimeTypeExtension(filePath)?.equals(
                "doc",
                true
            ) == true || getMimeTypeExtension(filePath)?.equals(
                "docx",
                true
            ) == true || getMimeTypeExtension(filePath)?.equals(
                "txt",
                true
            ) == true || getMimeTypeExtension(filePath)?.equals(
                "jpeg",
                true
            ) == true || getMimeTypeExtension(filePath)?.equals(
                "jpg",
                true
            ) == true || getMimeTypeExtension(filePath)?.equals("png", true) == true


                    || getMimeTypeExtension(filePath)?.equals("CSV", true) == true

                    || getMimeTypeExtension(filePath)?.equals("XLS", true) == true

                    || getMimeTypeExtension(filePath)?.equals("XLSX", true) == true

                    /*|| getMimeTypeExtension(filePath)
                ?.equals("gif", true) == true*/)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

    }

    fun hideKeyboard(activity: Activity) {
        val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        //Find the currently focused view, so we can grab the correct window token from it.
        var view = activity.currentFocus
        //If no_data_layout view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }


    fun isConnected(context: Context): Boolean {
        var result = false
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.activeNetwork ?: return false
            val actNw =
                connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
            result = when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            connectivityManager.run {
                connectivityManager.activeNetworkInfo?.run {
                    result = when (type) {
                        ConnectivityManager.TYPE_WIFI -> true
                        ConnectivityManager.TYPE_MOBILE -> true
                        ConnectivityManager.TYPE_ETHERNET -> true
                        else -> false
                    }

                }
            }
        }

        return result
    }


    var thumbColumns = arrayOf(MediaStore.Video.Thumbnails.DATA)
    var mediaColumns = arrayOf(MediaStore.Video.Media._ID)

    var EMOJI_FILTER = InputFilter { source, start, end, dest, dstart, dend ->
        for (index in start until end) {
            val type = Character.getType(source[index])
            if (type == Character.SURROGATE.toInt() || type == Character.OTHER_SYMBOL.toInt()) {
                return@InputFilter ""
            }
        }
        null
    }


    fun getUserFromQBUser(string: String?): UserModel {
        val gson = Gson()
        return gson.fromJson(string, UserModel::class.java)
    }

    fun getMonth(monthName: String): YearMonth {
        var monthVal = 0
        when (monthName) {
            JAN -> monthVal = 1
            FEB -> monthVal = 2
            MAR -> monthVal = 3
            APR -> monthVal = 4
            MAY -> monthVal = 5
            JUN -> monthVal = 6
            JUL -> monthVal = 7
            AUG -> monthVal = 8
            SEP -> monthVal = 9
            OCT -> monthVal = 10
            NOV -> monthVal = 11
            DEC -> monthVal = 12
        }

        val year = YearMonth.now().year
        return YearMonth.of(year, monthVal)
    }

    @JvmStatic
    fun getPermissionAsPerAndroidVersion(): ArrayList<String> {
        val list = ArrayList<String>()
        /*return if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(android.Manifest.permission.CAMERA)
            list.add(android.Manifest.permission.READ_MEDIA_IMAGES)
            list.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            list.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            list
        } else {
            list.add(android.Manifest.permission.CAMERA)
            list
        }*/

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(android.Manifest.permission.CAMERA)
//            list.add(android.Manifest.permission.READ_MEDIA_IMAGES)
            list.add(Manifest.permission.READ_MEDIA_IMAGES)
            list.add(Manifest.permission.READ_MEDIA_AUDIO)
            list.add(Manifest.permission.READ_MEDIA_VIDEO)

            return list

        } else {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                list.add(android.Manifest.permission.CAMERA)
//            list.add(android.Manifest.permission.READ_MEDIA_IMAGES)
                list.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                list.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                list
            } else {
                list.add(android.Manifest.permission.CAMERA)
//            list.add(android.Manifest.permission.READ_MEDIA_IMAGES)
                list.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                list.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                list
            }
        }


    }

    fun checkPermissions(
        context: Context,
        permissionAsPerAndroidVersion: ArrayList<String>
    ): Boolean {
        var result: Int
        val listPermissionsNeeded: MutableList<String> = ArrayList()
        for (p in permissionAsPerAndroidVersion) {
            result = ContextCompat.checkSelfPermission(context, p)
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p)
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            return false
        }
        return true
    }

    fun AdView.loadBannerAd() {
        if (SharedPrefs.getUser(context)?.plan_upgrade == true) {
            isEnabled = false
            visibility = View.GONE
        } else {
            isEnabled = true
            visibility = View.VISIBLE
            val adRequest = AdRequest.Builder().build()
            loadAd(adRequest)
        }
    }

}


object ActivityLifecycle : Application.ActivityLifecycleCallbacks {
    private var foreground = false

    fun isBackground(): Boolean {
        return !foreground
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

    }

    override fun onActivityStarted(activity: Activity) {

    }

    override fun onActivityResumed(activity: Activity) {
        foreground = true
    }

    override fun onActivityPaused(activity: Activity) {
        foreground = false
    }

    override fun onActivityStopped(activity: Activity) {

    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

    }

    override fun onActivityDestroyed(activity: Activity) {

    }
}