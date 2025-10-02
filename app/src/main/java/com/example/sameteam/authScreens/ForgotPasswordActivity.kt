package com.example.sameteam.authScreens


import android.view.inputmethod.EditorInfo
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import com.example.sameteam.R
import com.example.sameteam.authScreens.viewModel.ForgotVM
import com.example.sameteam.base.BaseActivity
import com.example.sameteam.databinding.ActivityForgotPasswordBinding
import com.example.sameteam.helper.Constants
import com.example.sameteam.helper.Utils

class ForgotPasswordActivity : BaseActivity<ActivityForgotPasswordBinding>() {

    override fun layoutID() = R.layout.activity_forgot_password

    override fun viewModel() = ViewModelProvider(
        this,
        ViewModelProvider.AndroidViewModelFactory.getInstance(this.application)
    ).get(ForgotVM::class.java)

    lateinit var forgotVM: ForgotVM
    lateinit var binding: ActivityForgotPasswordBinding

    override fun initActivity(mBinding: ViewDataBinding) {
        forgotVM = getViewModel() as ForgotVM
        binding = mBinding as ActivityForgotPasswordBinding

        binding.btnBack.setOnClickListener{
            onBackPressed()
        }

        binding.btnSend.setOnClickListener{
            Utils.hideKeyboard(this)
            forgotVM.onSendClicked(binding.txtEmail.text.toString())
        }

        binding.txtEmail.setOnEditorActionListener { v, actionId, event ->
            var mHandled = false
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                mHandled = true
                Utils.hideKeyboard(this)
                forgotVM.onSendClicked(binding.txtEmail.text.toString())
            }
            mHandled
        }

        forgotVM.observedChanges().observe(this) { event ->
            event?.getContentIfNotHandled()?.let {
                when (it) {
                    Constants.VISIBLE -> {
                        showSimpleProgress(binding.btnSend)
                    }
                    Constants.HIDE -> {
                        hideProgressDialog(binding.btnSend, getString(R.string.submit))
                    }
                    Constants.NAVIGATE -> {
                        startActivity(CheckEmailActivity::class.java)
                        finish()
                    }
                    else -> {
                        showMessage(it)
                    }
                }
            }

        }

    }

}