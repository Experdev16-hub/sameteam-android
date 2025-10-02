package com.example.sameteam.splash

import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.QueryProductDetailsParams
import com.example.sameteam.MyApplication
import com.example.sameteam.R
import com.example.sameteam.authScreens.LoginActivity
import com.example.sameteam.base.BaseActivity
import com.example.sameteam.databinding.ActivitySplashBinding
import com.example.sameteam.helper.SharedPrefs
import com.example.sameteam.homeScreen.HomeActivity
import com.example.sameteam.onBoarding.OnBoardingActivity

class SplashActivity : BaseActivity<ActivitySplashBinding>() {
    private val TAG = "SplashActivity"

    override fun layoutID() = R.layout.activity_splash

    override fun viewModel() = ViewModelProvider(
        this,
        ViewModelProvider.AndroidViewModelFactory.getInstance(this.application)
    ).get(SplashVM::class.java)

    lateinit var splashVM: SplashVM
    lateinit var binding: ActivitySplashBinding

    override fun initActivity(mBinding: ViewDataBinding) {
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        splashVM = (getViewModel() as SplashVM)
        binding = mBinding as ActivitySplashBinding

        binding.logoImage.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in))

        splashVM.start()
        splashVM.observedChanges().observe(this) { event ->
            event?.getContentIfNotHandled()?.let {
                when (it) {
                    "Navigate" -> {
                        if (SharedPrefs.getOnboardStatus(MyApplication.getInstance()) == true) {
                            startActivity(OnBoardingActivity::class.java)
                            finish()
                        } else {
                            val user = SharedPrefs.getUser(MyApplication.getInstance())
                            if (user != null && user.id != 0) {
                                startActivity(HomeActivity::class.java)
                                finish()
                            } else {
                                startActivity(LoginActivity::class.java)
                                finish()
                            }
                        }

                    }
                }
            }
        }
    }


}