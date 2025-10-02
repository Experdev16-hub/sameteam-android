package com.example.sameteam.quickBlox.base

import android.app.NotificationManager
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.sameteam.MyApplication
import com.example.sameteam.R
import com.example.sameteam.quickBlox.chat.ChatHelper
import com.example.sameteam.quickBlox.showSnackbar
import com.example.sameteam.quickBlox.util.SharedPrefsHelper
import com.quickblox.chat.QBChatService
import com.quickblox.core.QBEntityCallback
import com.quickblox.core.exception.QBResponseException
import com.quickblox.users.model.QBUser

private const val DUMMY_VALUE = "dummy_value"


abstract class BaseActivity2 : AppCompatActivity() {

    private val TAG = BaseActivity2::class.java.simpleName
    private var progressDialog: ProgressDialog? = null

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        outState.putInt(DUMMY_VALUE, 0)
        super.onSaveInstanceState(outState, outPersistentState)
    }

    override fun onPause() {
        super.onPause()
        hideProgressDialog()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    protected fun showMessage(message: String) {
        Toast.makeText(MyApplication.getInstance(), message, Toast.LENGTH_LONG).show()
    }

    protected fun showErrorSnackbar(@StringRes resId: Int, e: Exception?, clickListener: View.OnClickListener?) {
        val rootView = window.decorView.findViewById<View>(android.R.id.content)
        rootView?.let {
            showSnackbar(it, resId, e, R.string.dlg_retry, clickListener)
        }
    }

    protected fun showProgressDialog(@StringRes messageId: Int) {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(this)
            progressDialog!!.isIndeterminate = true
            progressDialog!!.setCancelable(false)
            progressDialog!!.setCanceledOnTouchOutside(false)

            // Disable the back button
            val keyListener = DialogInterface.OnKeyListener { dialog,
                                                              keyCode,
                                                              event ->
                keyCode == KeyEvent.KEYCODE_BACK
            }
            progressDialog!!.setOnKeyListener(keyListener)
        }
        progressDialog!!.setMessage(getString(messageId))
        try {
            progressDialog!!.show()
        } catch (e: Exception) {
            e.message?.let { Log.d(TAG, it) }
        }
    }

    protected fun hideProgressDialog() {
        progressDialog?.let {
            if (it.isShowing) {
                it.dismiss()
            }
        }
    }

    protected fun isProgresDialogShowing(): Boolean {
        if (progressDialog != null && progressDialog?.isShowing != null) {
            return progressDialog!!.isShowing
        } else {
            return false
        }
    }

    override fun onResume() {
        super.onResume()
        val currentUser = ChatHelper.getCurrentUser()
        hideNotifications()
        if (currentUser != null && !QBChatService.getInstance().isLoggedIn) {
            Log.d(TAG, "Resuming with Relogin")
            ChatHelper.login(SharedPrefsHelper.getQbUser()!!, object : QBEntityCallback<QBUser> {
                override fun onSuccess(qbUser: QBUser?, b: Bundle?) {
                    Log.d(TAG, "Relogin Successful")
                    reloginToChat()
                }

                override fun onError(e: QBResponseException?) {
                    e?.message?.let { Log.d(TAG, it) }
                }
            })

        } else {
            Log.d(TAG, "Resuming without Relogin to Chat")
            onResumeFinished()
        }
    }

    private fun reloginToChat() {
        ChatHelper.loginToChat(SharedPrefsHelper.getQbUser()!!, object : QBEntityCallback<Void> {
            override fun onSuccess(aVoid: Void?, bundle: Bundle?) {
                Log.d(TAG, "Relogin to Chat Successful")
                onResumeFinished()
            }

            override fun onError(e: QBResponseException?) {
                Log.d(TAG, "Relogin to Chat Error: " + e?.message)
                onResumeFinished()
            }
        })
    }

    private fun hideNotifications() {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }

    open fun onResumeFinished() {
        // Need to Override onResumeFinished() method in nested classes if we need to handle returning from background in Activity
    }

    protected fun checkPermissions(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (checkPermission(permission)) {
                return true
            }
        }
        return false
    }

    protected fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED
    }

    inner class KeyEventListener : DialogInterface.OnKeyListener {
        override fun onKey(dialog: DialogInterface?, keyCode: Int, keyEvent: KeyEvent?): Boolean {
            return keyCode == KeyEvent.KEYCODE_BACK
        }
    }
}