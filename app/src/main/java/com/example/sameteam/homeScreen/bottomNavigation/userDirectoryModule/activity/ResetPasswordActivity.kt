package com.example.sameteam.homeScreen.bottomNavigation.userDirectoryModule.activity

import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import com.example.sameteam.R
import com.example.sameteam.homeScreen.bottomNavigation.userDirectoryModule.viewModel.ResetPassVM
import com.example.sameteam.base.BaseActivity
import com.example.sameteam.databinding.ActivityResetPasswordBinding
import com.example.sameteam.helper.Constants
import com.example.sameteam.helper.Utils
import com.example.sameteam.helper.Utils.loadBannerAd

class ResetPasswordActivity : BaseActivity<ActivityResetPasswordBinding>() {

    override fun layoutID() = R.layout.activity_reset_password

    override fun viewModel() = ViewModelProvider(
        this,
        ViewModelProvider.AndroidViewModelFactory.getInstance(this.application)
    ).get(ResetPassVM::class.java)

    lateinit var resetPassVM: ResetPassVM
    lateinit var binding: ActivityResetPasswordBinding

    override fun initActivity(mBinding: ViewDataBinding) {
        resetPassVM = getViewModel() as ResetPassVM
        binding = mBinding as ActivityResetPasswordBinding

        binding.customToolbar.rightIcon.visibility = View.GONE
        binding.customToolbar.title.text = getString(R.string.change_password)
        binding.customToolbar.leftIcon.setOnClickListener {
            onBackPressed()
        }

        binding.adView.loadBannerAd()

        binding.btnSend.setOnClickListener {
            Utils.hideKeyboard(this)
            binding.passwordInput.error = null
            binding.txtNewPassword.transformationMethod = PasswordTransformationMethod()
            resetPassVM.onSendClicked()
        }

        binding.txtConfirmPassword.setOnEditorActionListener { v, actionId, event ->
            var mHandled = false
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                mHandled = true
                Utils.hideKeyboard(this)
                binding.passwordInput.error = null
                binding.txtNewPassword.transformationMethod = PasswordTransformationMethod()
                resetPassVM.onSendClicked()
            }
            mHandled
        }

        /**
         * General Observer
         */
        resetPassVM.observedChanges().observe(this) { event ->
            event?.getContentIfNotHandled()?.let {
                when (it) {
                    Constants.VISIBLE -> {
                        showSimpleProgress(binding.btnSend)
                    }
                    Constants.HIDE -> {
                        hideProgressDialog(binding.btnSend, getString(R.string.reset_password))
                    }
                    Constants.NAVIGATE -> {
                        onBackPressed()
                    }
                    getString(R.string.invalid_password_error) -> {
                        binding.txtNewPassword.transformationMethod = null
                        binding.passwordInput.error = getString(R.string.invalid_password_error)
                    }
                    else -> {
                        binding.passwordInput.error = null
                        showMessage(it)
                    }
                }
            }

        }

    }

}