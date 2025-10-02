package com.example.sameteam.authScreens


import android.content.Intent
import android.graphics.Color.argb
import android.graphics.drawable.shapes.RectShape
import android.graphics.drawable.shapes.RoundRectShape
import android.graphics.drawable.shapes.Shape
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import android.widget.Toast.makeText
import androidx.constraintlayout.solver.widgets.Rectangle
import androidx.core.content.ContextCompat
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.example.sameteam.BuildConfig
import com.example.sameteam.MyApplication
import com.example.sameteam.R
import com.example.sameteam.authScreens.viewModel.LoginVM
import com.example.sameteam.base.BaseActivity
import com.example.sameteam.databinding.ActivityLoginBinding
import com.example.sameteam.helper.Constants
import com.example.sameteam.helper.SharedPrefs
import com.example.sameteam.helper.Utils
import com.example.sameteam.homeScreen.HomeActivity
import com.example.sameteam.retrofit.APICallback
import com.google.common.collect.ImmutableList
import com.takusemba.spotlight.OnSpotlightListener
import com.takusemba.spotlight.OnTargetListener
import com.takusemba.spotlight.Spotlight
import com.takusemba.spotlight.Target
import com.takusemba.spotlight.effet.RippleEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class LoginActivity : BaseActivity<ActivityLoginBinding>() {
    private val TAG = "LoginActivity"

    override fun layoutID() = R.layout.activity_login

    override fun viewModel() = ViewModelProvider(
        this,
        ViewModelProvider.AndroidViewModelFactory.getInstance(this.application)
    ).get(LoginVM::class.java)

    lateinit var loginVM: LoginVM
    lateinit var binding: ActivityLoginBinding

    private lateinit var billingClient: BillingClient
    private var isBillingClientReady = false


    override fun initActivity(mBinding: ViewDataBinding) {

        loginVM = getViewModel() as LoginVM

        binding = mBinding as ActivityLoginBinding

        if(!SharedPrefs.getToken(MyApplication.getInstance()).isNullOrBlank())
            SharedPrefs.clearToken(MyApplication.getInstance())

        //Edit Text Emoji filter
        binding.txtEmail.filters = arrayOf(Utils.EMOJI_FILTER)
        binding.txtPassword.filters = arrayOf(Utils.EMOJI_FILTER)

        setupBillingClient()

        /**
         * General Observer
         */
        loginVM.observedChanges().observe(this, Observer { event ->
            event?.getContentIfNotHandled()?.let {
                when(it){
                    Constants.VISIBLE -> {
                       showSimpleProgress(binding.btnLogin)
                    }
                    Constants.HIDE -> {
                        hideProgressDialog(binding.btnLogin,getString(R.string.login))
                    }
                    Constants.NAVIGATE -> {
                        startActivity(HomeActivity::class.java)
                        finish()
                    }
//                    "Approval" -> {
//                        startActivity(ApprovalActivity::class.java)
//                    }
                    else ->{
                        showMessage(it)
                    }
                }
            }
        })

        binding.btnLogin.setOnClickListener {
            Utils.hideKeyboard(this)
            loginVM.onLoginPressed()
        }

        binding.txtSignup.setOnClickListener {
            startActivity(SignUpActivity::class.java)
        }

        binding.forgotPass.setOnClickListener {
            startActivity(ForgotPasswordActivity::class.java)
        }

        binding.txtPassword.setOnEditorActionListener { v, actionId, event ->
            var mHandled = false
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                mHandled = true
                Utils.hideKeyboard(this@LoginActivity)
                loginVM.onLoginPressed()
            }
            mHandled
        }
    }

    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(this)
            .enablePendingPurchases()
            .setListener { billingResult, purchases ->
                // Purchase updates listener: not used in this example since we're only querying product details.
            }
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    isBillingClientReady = true
                    Log.d("SubscriptionBilling", "Billing Client connected.")
                    val subscriptionIds = listOf("yearly_subscription")

                    billingClient.queryPurchasesAsync(BillingClient.ProductType.SUBS){ billingResult, list ->
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            if (list.size > 0) {
                                // Activate premium feature
                                println("[Purchase] - Premium user validated")
                                // Process each purchase if needed
                            } else {
                                // De-activated o push to premium feature
                                val intent = Intent(this@LoginActivity, SubscriptionActivity::class.java)
                                startActivity(intent)
                            }
                        }
                    }
                } else {
                    Log.e("SubscriptionBilling", "Billing Client connection failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                isBillingClientReady = false
                Log.e("SubscriptionBilling", "Billing Service disconnected.")
                // Optionally, implement reconnection logic here.
            }
        })
        // Check for active subscription entitlements.
    }

}