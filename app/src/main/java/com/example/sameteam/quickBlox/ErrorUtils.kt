package com.example.sameteam.quickBlox

import android.annotation.SuppressLint
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.example.sameteam.MyApplication
import com.example.sameteam.R
import com.google.android.material.snackbar.Snackbar

private val NO_CONNECTION_ERROR = MyApplication.getInstance().getString(R.string.error_connection)
private val NO_RESPONSE_TIMEOUT = MyApplication.getInstance().getString(R.string.error_response_timeout)
private val NO_SERVER_CONNECTION = MyApplication.getInstance().getString(R.string.no_server_connection)

fun showSnackbar(view: View, @StringRes errorMessageResource: Int, e: Exception?,
                 @StringRes actionLabel: Int, clickListener: View.OnClickListener?): Snackbar {
    val error = if (e == null) "" else e.message
    val noConnection = NO_CONNECTION_ERROR == error
    val timeout = error!!.startsWith(NO_RESPONSE_TIMEOUT)
    return if (noConnection || timeout) {
        showSnackbar(view, NO_SERVER_CONNECTION, actionLabel, clickListener)
    } else if (errorMessageResource == 0) {
        showSnackbar(view, error, actionLabel, clickListener)
    } else if (errorMessageResource != 0) {
        showSnackbar(view, MyApplication.getInstance().getString(errorMessageResource), actionLabel, clickListener)
    } else if (error == "") {
        showSnackbar(view, errorMessageResource, NO_SERVER_CONNECTION, actionLabel, clickListener)
    } else {
        showSnackbar(view, errorMessageResource, error, actionLabel, clickListener)
    }
}

fun showSnackbar(view: View, @StringRes errorMessage: Int, error: String,
                         @StringRes actionLabel: Int, clickListener: View.OnClickListener?): Snackbar {
    val errorMessageString = MyApplication.getInstance().getString(errorMessage)
    val message = String.format("%s: %s", errorMessageString, error)
    return showSnackbar(view, message, actionLabel, clickListener)
}

@SuppressLint("ResourceAsColor")
fun showSnackbar(view: View, message: String,
                         @StringRes actionLabel: Int,
                         clickListener: View.OnClickListener?): Snackbar {
    val snackbar = Snackbar.make(view, message.trim { it <= ' ' }, Snackbar.LENGTH_LONG)
    if (clickListener != null) {
        snackbar.setAction(actionLabel, clickListener)
        snackbar.setActionTextColor(ContextCompat.getColor(MyApplication.getInstance(), R.color.colorPrimary))
        snackbar.setTextColor(ContextCompat.getColor(MyApplication.getInstance(), R.color.colorPrimary))
    }
    snackbar.show()
    return snackbar
}

fun Snackbar.setTextColor(color: Int): Snackbar {
    val tv = view.findViewById(R.id.snackbar_text) as TextView
    tv.setTextColor(color)
    return this
}