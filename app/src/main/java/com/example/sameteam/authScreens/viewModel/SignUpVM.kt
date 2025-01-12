package com.example.sameteam.authScreens.viewModel

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.example.sameteam.BuildConfig
import com.example.sameteam.R
import com.example.sameteam.amazonS3.S3Util
import com.example.sameteam.authScreens.model.RegisterRequestModel
import com.example.sameteam.authScreens.model.RegisterResponseModel
import com.example.sameteam.base.BaseViewModel
import com.example.sameteam.helper.*
import com.example.sameteam.retrofit.APITask
import com.example.sameteam.retrofit.OnResponseListener
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.michaelrocks.libphonenumber.android.NumberParseException
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import java.io.File
import java.util.*
import java.util.regex.Pattern


class SignUpVM(val context: Application) : BaseViewModel(context), OnResponseListener {

    private val TAG = "Register"

    var registerRequestModel = RegisterRequestModel()
    val REQUEST_PERMISSION_SETTING = 101
    private var messageString = MutableLiveData<Event<String>>()
    var selectedPath:String? = null
    var imageName:String? = null

    private fun setMessage(msg: String) {
        messageString.value = Event(msg)
    }

    fun observedChanges() = messageString

    fun onRegisterPressed(mContext: Context) {
        if (isValid()) {
            registerRequestModel.email = registerRequestModel.email?.trim()
            setMessage(Constants.VISIBLE)
            checkProfileImage(mContext)
        }
    }

    fun onProfileImageClick(view: View) {
        Dexter.withContext(context)
            .withPermissions(Utils.getPermissionAsPerAndroidVersion())
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {
                        showImagePickerOptions(view)
                    } else {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts(
                            "package",
                            ((view.context as Activity?)!!).packageName,
                            null
                        )
                        intent.data = uri
                        ((view.context as Activity?)!!).startActivityForResult(
                            intent,
                            REQUEST_PERMISSION_SETTING
                        )
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest>,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }).check()
    }


    private fun showImagePickerOptions(view: View) {
        ImagePickerActivity.showImagePickerOptions(
            (view.context as Activity?)!!,
            object : ImagePickerActivity.PickerOptionListener {
                override fun onTakeVideoSelected() {

                }

                override fun onTakeCameraSelected() {
                    setMessage(Constants.CAMERA_INTENT)
                }

                override fun onChooseGallerySelected() {
                    setMessage(Constants.GALLERY_INTENT)
                }
            },
            0
        )
    }

    fun uploadProfilePic(mSelectedMediaPath: String, imageFileName: String, context: Context) {

        val key = "sameteam/profilepic/$imageFileName"
        val transferUtility = S3Util.getTransferUtility(context)

        var baseUrl = ""
        var bucketName = ""
        if(BuildConfig.FLAVOR == "client"){
            baseUrl = Keys.liveAWSBaseURL()
            bucketName = Keys.liveAWSBucketName()
        }
        else {
            baseUrl = Keys.developmentAWSBaseURL()
            bucketName = Keys.developmentAWSBucketName()
        }

        val originalImageTransferUtility = transferUtility.upload(
            bucketName,
            key,
            File(mSelectedMediaPath)
        )

        originalImageTransferUtility.setTransferListener(object : TransferListener {
            override fun onStateChanged(id: Int, state: TransferState?) {
                if(TransferState.COMPLETED == originalImageTransferUtility.state){
                    Log.d(TAG, "onStateChanged: Success ${baseUrl+key}")
                    registerRequestModel.profile_picture = baseUrl+key
                    callRegisterAPI()
                }
            }

            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {

            }

            override fun onError(id: Int, ex: Exception?) {
                setMessage(Constants.HIDE)
                setMessage(context.getString(R.string.something_went_wrong))
            }

        })
    }


    private fun isValid(): Boolean {
        if (registerRequestModel.first_name.isNullOrBlank()) {
            setMessage(context.getString(R.string.enter_FN))
            return false
        } else if (registerRequestModel.last_name.isNullOrBlank()) {
            setMessage(context.getString(R.string.enter_LN))
            return false
        } else if (registerRequestModel.email.isNullOrBlank()) {
            setMessage(context.getString(R.string.enter_email))
            return false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(registerRequestModel.email.toString().trim()).matches()) {
            setMessage(context.getString(R.string.enter_valid_email))
            return false
        } else if (registerRequestModel.mobile_number.isNullOrBlank()) {
            setMessage(context.getString(R.string.enter_number))
            return false
        } else if (!isValidMobileNumber(registerRequestModel.country_code.toString()+registerRequestModel.mobile_number)) {
            setMessage(context.getString(R.string.enter_valid_number))
            return false
        } else if (registerRequestModel.password.isNullOrBlank()) {
            setMessage(context.getString(R.string.enter_password))
            return false
        }
        else if (!isValidPassword(registerRequestModel.password.toString())) {
            setMessage(context.getString(R.string.invalid_password_error))
            return false
        }
        else return true
    }

    // Password must be 8 characters long with at least 1 letter, 1 number and 1 special character.
    private fun isValidPassword(password: String): Boolean {
        return Pattern.compile("(?=.*[0-9])(?=.*[a-z]*[A-Z])(?=.*[!@#$%^&+*=])(?=\\S+$).{8,}")
            .matcher(password)
            .matches()
    }

    fun isValidMobileNumber(number: String): Boolean{
        Log.d(TAG, "isValidMobileNumber: $number")
        val phoneUtil: PhoneNumberUtil = PhoneNumberUtil.createInstance(context)
        return try {
            val phoneNumberProto = phoneUtil.parse(number, null)
            val isValid = phoneUtil.isValidNumber(phoneNumberProto) // returns true if valid
            isValid
        } catch (e: NumberParseException) {
            e.printStackTrace()
            false
        }
    }

    private fun checkProfileImage(mContext: Context) {
        if(selectedPath.isNullOrBlank() || imageName.isNullOrBlank()){
            registerRequestModel.profile_picture = ""
            callRegisterAPI()
        }
        else
            uploadProfilePic(selectedPath!!,imageName!!,mContext)

    }

    fun callRegisterAPI(){
        Log.d(TAG, "Request Register API: ${Gson().toJson(registerRequestModel)}")
        APITask.getInstance().callRegister(this, registerRequestModel)?.let { mDisposable?.add(it) }
    }

    override fun <T> onResponseReceived(response: T, requestCode: Int) {
        setMessage(Constants.HIDE)
        Toast.makeText(context, (response as RegisterResponseModel).message, Toast.LENGTH_LONG)
            .show()
        setMessage(Constants.NAVIGATE)
    }

    override fun onResponseError(message: String, requestCode: Int, responseCode: Int) {
        setMessage(Constants.HIDE)
        Log.d(TAG, "Response Register API Error : Code $responseCode  Message $message")
        setMessage(message)
    }

}