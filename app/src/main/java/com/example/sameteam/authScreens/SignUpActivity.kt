package com.example.sameteam.authScreens

import android.content.Intent
import android.net.Uri
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.inputmethod.EditorInfo
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.sameteam.MyApplication
import com.example.sameteam.R
import com.example.sameteam.authScreens.viewModel.SignUpVM
import com.example.sameteam.base.BaseActivity
import com.example.sameteam.databinding.ActivitySignUpBinding
import com.example.sameteam.helper.Constants
import com.example.sameteam.helper.ImagePickerActivity
import com.example.sameteam.helper.SharedPrefs
import com.example.sameteam.helper.Utils
import com.example.sameteam.homeScreen.HomeActivity
import com.example.sameteam.widget.ConfirmDialog


class SignUpActivity : BaseActivity<ActivitySignUpBinding>(), ConfirmDialog.ConfirmClickListener {
    private val TAG = "SignUpActivity"

    override fun layoutID() = R.layout.activity_sign_up

    override fun viewModel() = ViewModelProvider(
        this,
        ViewModelProvider.AndroidViewModelFactory.getInstance(this.application)
    ).get(SignUpVM::class.java)

    lateinit var signUpVM: SignUpVM
    lateinit var binding: ActivitySignUpBinding
    var activityResultLauncher: ActivityResultLauncher<Intent>? = null


    override fun initActivity(mBinding: ViewDataBinding) {
        signUpVM = getViewModel() as SignUpVM
        binding = mBinding as ActivitySignUpBinding

        if(!SharedPrefs.getToken(MyApplication.getInstance()).isNullOrBlank())
            SharedPrefs.clearToken(MyApplication.getInstance())

        //Edit Text Emoji filter
        binding.firstName.filters = arrayOf(Utils.EMOJI_FILTER)
        binding.lastName.filters = arrayOf(Utils.EMOJI_FILTER)
        binding.txtTitle.filters = arrayOf(Utils.EMOJI_FILTER)
        binding.txtEmail.filters = arrayOf(Utils.EMOJI_FILTER)
        binding.txtMobile.filters = arrayOf(Utils.EMOJI_FILTER)
        binding.txtPassword.filters = arrayOf(Utils.EMOJI_FILTER)

        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        binding.btnSubmit.setOnClickListener {
            Utils.hideKeyboard(this)
            binding.passwordInput.error = null
            binding.txtPassword.transformationMethod = PasswordTransformationMethod()
            signUpVM.onRegisterPressed(this)
        }

        binding.imgLayout.setOnClickListener {
            signUpVM.onProfileImageClick(it)
        }

        binding.ccpLoadFullNumber.setOnCountryChangeListener {
            signUpVM.registerRequestModel.country_code = "+"+binding.ccpLoadFullNumber.selectedCountryCode
        }

        /**
         * General Observer
         */
        signUpVM.observedChanges().observe(this) { event ->
            event?.getContentIfNotHandled()?.let {
                when (it) {
                    Constants.VISIBLE -> {
                        showSimpleProgress(binding.btnSubmit)
                    }
                    Constants.HIDE -> {
                        hideProgressDialog(binding.btnSubmit, getString(R.string.submit))
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
                    getString(R.string.invalid_password_error) -> {
                        binding.txtPassword.transformationMethod = null
                        binding.passwordInput.error = getString(R.string.invalid_password_error)
                    }
                    else -> {
                        binding.passwordInput.error = null
                        showMessage(it)
                    }
                }
            }
        }

        /**
         * Updated version of StartActivityForResult
         */
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
                            .into(binding.profilePic)

//                        signUpVM.uploadProfilePic(selectedPhotoUri?.path ?: "", selectedPhotoUri?.lastPathSegment ?: "",this)

                        signUpVM.selectedPath = selectedPhotoUri?.path
                        signUpVM.imageName = selectedPhotoUri?.lastPathSegment

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }


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

    override fun onConfirm(place: String) {

    }


}