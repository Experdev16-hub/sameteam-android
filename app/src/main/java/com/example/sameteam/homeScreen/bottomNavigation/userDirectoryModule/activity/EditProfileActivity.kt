package com.example.sameteam.homeScreen.bottomNavigation.userDirectoryModule.activity


import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.sameteam.MyApplication
import com.example.sameteam.R
import com.example.sameteam.authScreens.model.LoginResponseModel
import com.example.sameteam.base.BaseActivity
import com.example.sameteam.databinding.ActivityEditProfileBinding
import com.example.sameteam.helper.Constants
import com.example.sameteam.helper.ImagePickerActivity
import com.example.sameteam.helper.SharedPrefs
import com.example.sameteam.helper.Utils
import com.example.sameteam.helper.Utils.loadBannerAd
import com.example.sameteam.homeScreen.bottomNavigation.userDirectoryModule.viewModel.EditProfileVM
import com.example.sameteam.widget.ConfirmDialog

class EditProfileActivity : BaseActivity<ActivityEditProfileBinding>(),ConfirmDialog.ConfirmClickListener {
    private val TAG = "EditProfileActivity"

    override fun layoutID() = R.layout.activity_edit_profile

    override fun viewModel() = ViewModelProvider(
        this,
        ViewModelProvider.AndroidViewModelFactory.getInstance(this.application)
    ).get(EditProfileVM::class.java)

    lateinit var binding: ActivityEditProfileBinding
    lateinit var editProfileVM: EditProfileVM
    lateinit var user: LoginResponseModel.User
    var activityResultLauncher: ActivityResultLauncher<Intent>? = null


    override fun initActivity(mBinding: ViewDataBinding) {
        binding = mBinding as ActivityEditProfileBinding
        editProfileVM = getViewModel() as EditProfileVM

        user = SharedPrefs.getUser(MyApplication.getInstance())!!
        editProfileVM.loginResponseModel = user
        binding.ccpLoadFullNumber.setCountryForPhoneCode(user.country_code?.substring(1)!!.toInt())
        Glide.with(this)
            .load(user.profile_picture)
            .error(R.drawable.profile_photo)
            .placeholder(R.drawable.profile_photo)
            .centerCrop()
            .into(binding.profileImage)


        binding.adView.loadBannerAd()

        binding.customToolbar.rightIcon.visibility = View.GONE
        binding.customToolbar.title.text = getString(R.string.edit_profile)
        binding.customToolbar.leftIcon.setOnClickListener {
            onBackPressed()
        }

        // Edit Text Emoji input filter
        binding.firstName.filters = arrayOf(Utils.EMOJI_FILTER)
        binding.lastName.filters = arrayOf(Utils.EMOJI_FILTER)
        binding.txtTitle.filters = arrayOf(Utils.EMOJI_FILTER)
        binding.txtMobile.filters = arrayOf(Utils.EMOJI_FILTER)

        binding.profileImage.setOnClickListener {
            editProfileVM.onProfileImageClick(it)
        }

        binding.btnDone.setOnClickListener {
            Utils.hideKeyboard(this)
            val confirmDialog = ConfirmDialog(this,"Are you sure you want to change the information?","EditUser")
            confirmDialog.show(supportFragmentManager,"Confirm")
        }

        binding.ccpLoadFullNumber.setOnCountryChangeListener {
            editProfileVM.loginResponseModel.country_code = "+"+binding.ccpLoadFullNumber.selectedCountryCode
        }

        binding.txtMobile.setOnEditorActionListener { v, actionId, event ->
            var mHandled = false
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                mHandled = true
                Utils.hideKeyboard(this)
                val confirmDialog = ConfirmDialog(this,"Are you sure you want to change the information?","EditUser")
                confirmDialog.show(supportFragmentManager,"Confirm")
            }
            mHandled
        }

        editProfileVM.observedChanges().observe(this) { event ->
            event?.getContentIfNotHandled()?.let {
                when (it) {
                    Constants.VISIBLE -> {
                        showSimpleProgress(binding.btnDone)
                    }
                    Constants.HIDE -> {
                        hideProgressDialog(binding.btnDone, getString(R.string.submit))
                    }
                    Constants.NAVIGATE -> {
                        onBackPressed()
                    }
                    Constants.CAMERA_INTENT -> {
                        launchCameraIntent()
                    }
                    Constants.GALLERY_INTENT -> {
                        launchGalleryIntent()
                    }
                    else -> {
                        showMessage(it)
                    }
                }
            }
        }

        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK && result.data != null) {
                    Log.d(TAG, "initActivity: ${result.data!!.getParcelableExtra<Uri>("path")} ")

                    val selectedPhotoUri = result.data!!.getParcelableExtra<Uri>("path")
                    try {

                        Log.d(TAG, "initActivity last segment: ${selectedPhotoUri?.lastPathSegment}")


                        Glide.with(this)
                            .load(selectedPhotoUri)
                            .error(R.drawable.profile_photo)
                            .placeholder(R.drawable.profile_photo)
                            .circleCrop()
                            .into(binding.profileImage)


                        editProfileVM.selectedPath = selectedPhotoUri?.path
                        editProfileVM.imageName = selectedPhotoUri?.lastPathSegment

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
    }

    override fun onConfirm(place: String) {
       editProfileVM.onEditPressed(this)
    }

    private fun launchCameraIntent() {
        val intent = Intent(this, ImagePickerActivity::class.java)
        intent.putExtra(
            ImagePickerActivity.INTENT_IMAGE_PICKER_OPTION,
            ImagePickerActivity.REQUEST_IMAGE_CAPTURE
        )

        // setting aspect ratio
        intent.putExtra(ImagePickerActivity.INTENT_LOCK_ASPECT_RATIO, true)
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_X, 1) // 16x9, 1x1, 3:4, 3:2
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_Y, 1)

        // setting maximum bitmap width and height
        intent.putExtra(ImagePickerActivity.INTENT_SET_BITMAP_MAX_WIDTH_HEIGHT, true)
        intent.putExtra(ImagePickerActivity.INTENT_BITMAP_MAX_WIDTH, 1000)
        intent.putExtra(ImagePickerActivity.INTENT_BITMAP_MAX_HEIGHT, 1000)


        activityResultLauncher?.launch(intent)

    }

    private fun launchGalleryIntent() {
        val intent = Intent(this, ImagePickerActivity::class.java)
        intent.putExtra(
            ImagePickerActivity.INTENT_IMAGE_PICKER_OPTION,
            ImagePickerActivity.REQUEST_GALLERY_IMAGE
        )

        // setting aspect ratio
        intent.putExtra(ImagePickerActivity.INTENT_LOCK_ASPECT_RATIO, true)
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_X, 1) // 16x9, 1x1, 3:4, 3:2
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_Y, 1)

        activityResultLauncher?.launch(intent)
    }

}