package com.example.sameteam.base

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.example.sameteam.BR
import com.example.sameteam.R
import com.example.sameteam.quickBlox.chat.ChatHelper
import com.example.sameteam.quickBlox.util.SharedPrefsHelper
import com.github.razir.progressbutton.DrawableButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.google.android.material.button.MaterialButton
import com.quickblox.chat.QBChatService
import com.quickblox.core.QBEntityCallback
import com.quickblox.core.exception.QBResponseException
import com.quickblox.users.model.QBUser
import java.io.Serializable

abstract class BaseActivity<T : ViewDataBinding> : AppCompatActivity() {

    abstract fun layoutID(): Int

    abstract fun viewModel(): BaseViewModel

    abstract fun initActivity(mBinding: ViewDataBinding)

    var savedInstanceState: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.savedInstanceState = savedInstanceState
        mBinding = DataBindingUtil.setContentView<T>(this, layoutID())
        mBinding.lifecycleOwner = this
        mBinding.setVariable(BR.viewModel, viewModel())
//        binding.setVariable(BR.handler, this)
        initActivity(mBinding)

    }
    override fun onResume() {
        Log.e("BASE ACTIVITY Resume","LOGIN QUICK")

        super.onResume()
        val currentUser = ChatHelper.getCurrentUser()
        Log.e("BASE ACTIVITY USER",currentUser?.fullName.toString())

        if (currentUser != null && !QBChatService.getInstance().isLoggedIn) {
            Log.d("BASE Activity", "Resuming with Relogin")
            ChatHelper.login(SharedPrefsHelper.getQbUser()!!, object : QBEntityCallback<QBUser> {
                override fun onSuccess(qbUser: QBUser?, b: Bundle?) {
                    Log.d("BASE Activity", "Relogin Successful")
                    reloginToChat()
                }

                override fun onError(e: QBResponseException?) {
                    e?.message?.let { Log.d("BASE Activity", it) }
                }
            })

        } else {
            Log.d("BASE Activity", "Resuming without Relogin to Chat")
            onResumeFinished()
        }
    }
    fun getViewModel(): Any = viewModel()
    private fun reloginToChat() {
        ChatHelper.loginToChat(SharedPrefsHelper.getQbUser()!!, object : QBEntityCallback<Void> {
            override fun onSuccess(aVoid: Void?, bundle: Bundle?) {
                Log.d("BASE Activity", "Relogin to Chat Successful")
                onResumeFinished()
            }

            override fun onError(e: QBResponseException?) {
                Log.d("BASE Activity", "Relogin to Chat Error: " + e?.message)
                onResumeFinished()
            }
        })
    }
    open fun onResumeFinished() {
        // Need to Override onResumeFinished() method in nested classes if we need to handle returning from background in Activity
    }

    fun startActivity(cls: Class<*>) {
        val intent = Intent(this, cls)
        startActivity(intent)
        overridePendingTransitionEnter()
    }

    fun startActivityWithData(cls: Class<*>, obj: Any) {
        val intent = Intent(this, cls)
        if (obj is Serializable) intent.putExtra("Extras", obj)
        startActivity(intent)
        overridePendingTransitionEnter()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransitionExit()
    }

    fun overridePendingTransitionEnter() {
        overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left)
    }

    /**
     * Overrides the pending Activity transition by performing the "Exit" animation.
     */
    private fun overridePendingTransitionExit() {
        overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right)
    }

    fun overridePendingTransitionDown() {
        overridePendingTransition(R.anim.slide_up, R.anim.slide_down)
    }

    protected fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    protected fun showSimpleProgress(button: MaterialButton) {
        button.showProgress {
            progressColor = Color.WHITE
            gravity = DrawableButton.GRAVITY_CENTER
        }
        button.isEnabled = false
    }

    protected fun hideProgressDialog(button: MaterialButton, text: String) {
        if (!button.isEnabled) {
            button.isEnabled = true
            button.hideProgress(text)
        }
    }

    companion object {
        lateinit var mBinding:ViewDataBinding
    }

}