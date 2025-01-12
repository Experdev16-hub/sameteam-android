package com.example.sameteam.homeScreen.drawerNavigation.activity

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.core.app.NotificationManagerCompat
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.sameteam.MyApplication
import com.example.sameteam.R
import com.example.sameteam.authScreens.LoginActivity
import com.example.sameteam.authScreens.model.LoginResponseModel
import com.example.sameteam.base.BaseActivity
import com.example.sameteam.databinding.ActivityProfileBinding
import com.example.sameteam.helper.Constants
import com.example.sameteam.helper.SharedPrefs
import com.example.sameteam.helper.Utils.loadBannerAd
import com.example.sameteam.homeScreen.bottomNavigation.userDirectoryModule.activity.EditProfileActivity
import com.example.sameteam.homeScreen.bottomNavigation.userDirectoryModule.activity.ResetPasswordActivity
import com.example.sameteam.homeScreen.drawerNavigation.viewModel.ProfileVM
import com.example.sameteam.quickBlox.QbDialogHolder
import com.example.sameteam.quickBlox.chat.ChatHelper
import com.example.sameteam.quickBlox.db.QbUsersDbManager
import com.example.sameteam.quickBlox.util.SharedPrefsHelper
import com.example.sameteam.widget.ConfirmDialog
import com.google.android.gms.ads.AdRequest
import com.quickblox.messages.services.QBPushManager
import com.quickblox.messages.services.SubscribeService


class ProfileActivity : BaseActivity<ActivityProfileBinding>(), ConfirmDialog.ConfirmClickListener {
    private val TAG = "ProfileActivity"

    override fun layoutID() = R.layout.activity_profile

    override fun viewModel() = ViewModelProvider(
        this,
        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    ).get(ProfileVM::class.java)

    lateinit var binding: ActivityProfileBinding
    lateinit var profileVM: ProfileVM
    lateinit var user: LoginResponseModel.User


    override fun onResume() {
        super.onResume()
        user = SharedPrefs.getUser(MyApplication.getInstance())!!

        if (user.profile_picture.isNullOrBlank()) {
            Glide.with(this)
                .asDrawable()
                .load(R.drawable.profile_photo)
                .centerCrop()
                .into(binding.profileImage)
        } else {
            Glide.with(this)
                .load(user.profile_picture)
                .error(R.drawable.profile_photo)
                .placeholder(R.drawable.profile_photo)
                .centerCrop()
                .into(binding.profileImage)
        }

        binding.name.text = "${user.first_name} ${user.last_name}"
        binding.position.text = user.title
        binding.txtEmail.text = user.email
        binding.txtPhone.text = "${user.country_code}-${user.mobile_number}"

    }

    override fun initActivity(mBinding: ViewDataBinding) {
        binding = mBinding as ActivityProfileBinding
        profileVM = getViewModel() as ProfileVM

        binding.customToolbar.rightIcon.visibility = View.VISIBLE
        binding.customToolbar.title.text = getString(R.string.profile)
        binding.customToolbar.rightIcon.setImageResource(R.drawable.ic_edit)
        binding.customToolbar.leftIcon.setOnClickListener {
            onBackPressed()
        }
        binding.customToolbar.rightIcon.setOnClickListener {
            startActivity(EditProfileActivity::class.java)
        }

        binding.adView.loadBannerAd()

        user = SharedPrefs.getUser(MyApplication.getInstance())!!
        Log.d(TAG, "initFragment: ${user.notification_status}")
        binding.btnToggle.isChecked = user.notification_status == "on"

        if (user.profile_picture.isNullOrBlank()) {
            Glide.with(this)
                .asDrawable()
                .load(R.drawable.profile_photo)
                .centerCrop()
                .into(binding.profileImage)
        } else {
            Glide.with(this)
                .load(user.profile_picture)
                .error(R.drawable.profile_photo)
                .placeholder(R.drawable.profile_photo)
                .centerCrop()
                .into(binding.profileImage)
        }


        binding.name.text = "${user.first_name} ${user.last_name}"
        binding.position.text = user.title
        binding.txtEmail.text = user.email
        binding.txtPhone.text = "${user.country_code}-${user.mobile_number}"


        binding.logout.setOnClickListener {
            profileVM.onLogout()
        }

        binding.changePass.setOnClickListener {
            startActivity(ResetPasswordActivity::class.java)
        }

        binding.btnToggle.setOnCheckedChangeListener { buttonView, isChecked ->
            profileVM.callNotificationAPI(isChecked)
        }

        binding.btnInfo.setOnClickListener {
            val confirmDialog = ConfirmDialog(
                this, "You won’t receive any phone or video calls when turned off.", "AlreadyCompleted"
            )
            confirmDialog.show(supportFragmentManager, "Confirm")
        }

        binding.deleteAccount.setOnClickListener {
            val confirmDialog =
                ConfirmDialog(
                    this,
                    "Are you sure you want to delete your account? This action cannot be undone.",
                    Constants.DELETE_ACCOUNT
                )
            confirmDialog.show(supportFragmentManager, "DeleteAccount")
        }

        binding.upgradeAccount.setOnClickListener {
            val viewIntent = Intent(
                "android.intent.action.VIEW",
                Uri.parse("https://subadmin.sameteam.app/")
            )
            startActivity(viewIntent)
        }

        /**
         * General Observer
         */
        profileVM.observedChanges().observe(this) { event ->
            event?.getContentIfNotHandled()?.let {
                when (it) {
                    Constants.VISIBLE -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }

                    Constants.HIDE -> {
                        binding.progressBar.visibility = View.GONE
                    }

                    Constants.NAVIGATE, Constants.FORCE_LOGOUT -> {
                        unsubscribeFromPushes(false)
                    }

                    Constants.DELETE_ACCOUNT -> {
                        unsubscribeFromPushes(true)
                    }

                    Constants.PLAN_STATUS -> {
                        binding.adView.loadBannerAd()

                        binding.upgradeAccount.visibility =
                            if (SharedPrefs.getUser(this)?.plan_upgrade == true)
                                View.GONE else View.VISIBLE

                    }

                    else -> {
                        showMessage(it)
                    }
                }
            }
        }
    }

    override fun onConfirm(place: String) {
        if (place == Constants.DELETE_ACCOUNT) {
            profileVM.callDeleteUserAccount()
        }
    }

    private fun unsubscribeFromPushes(isUserDeleted: Boolean) {

        if (QBPushManager.getInstance().isSubscribedToPushes) {
            QBPushManager.getInstance().addListener(object : QBPushManager.QBSubscribeListener {
                override fun onSubscriptionCreated() {

                }

                override fun onSubscriptionError(e: Exception?, i: Int) {
                    Log.d("Subscription", "SubscriptionError" + e?.localizedMessage)

                    showMessage(getString(R.string.something_went_wrong))
                    binding.progressBar.visibility = View.GONE
                    e?.printStackTrace()
                }

                override fun onSubscriptionDeleted(success: Boolean) {
                    Log.d(TAG, "Subscription Deleted -> Success: $success")
                    QBPushManager.getInstance().removeListener(this)
                    performLogout(isUserDeleted)
                }
            })
            SubscribeService.unSubscribeFromPushes(this)
        } else {
            performLogout(isUserDeleted)
        }
    }

    fun performLogout(isUserDeleted: Boolean) {
        NotificationManagerCompat.from(MyApplication.getInstance()).cancelAll()
        binding.progressBar.visibility = View.GONE
        if (isUserDeleted)
            showMessage(getString(R.string.user_deleted_successfully))
        else
            showMessage(getString(R.string.logged_out))
        ChatHelper.destroy()
        SharedPrefsHelper.clearAllData()
        SharedPrefs.clearAllData(MyApplication.getInstance())
        QbDialogHolder.clear()
        QbUsersDbManager.clearDB()
        startActivity(LoginActivity::class.java)
        finishAffinity()
    }

    override fun onStart() {
        super.onStart()
        profileVM.callMyProfile()
    }
}