package com.example.sameteam.homeScreen.bottomNavigation.userDirectoryModule.viewModel

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.example.sameteam.BuildConfig
import com.example.sameteam.MyApplication
import com.example.sameteam.R
import com.example.sameteam.amazonS3.S3Util
import com.example.sameteam.authScreens.model.LoginResponseModel
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
import java.lang.Exception

class EditProfileVM(val context: Application) : BaseViewModel(context), OnResponseListener {
    private val TAG = "EditProfileVM"

    lateinit var loginResponseModel :LoginResponseModel.User
    val REQUEST_PERMISSION_SETTING = 101
    private var messageString = MutableLiveData<Event<String>>()
    var selectedPath:String? = null
    var imageName:String? = null

    private fun setMessage(msg: String) {
        messageString.value = Event(msg)
    }

    fun observedChanges() = messageString

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

    fun onEditPressed(mContext: Context){
        if(isValid()){
            setMessage(Constants.VISIBLE)
            checkProfileImage(mContext)
        }
    }

    private fun checkProfileImage(mContext: Context) {
        if(selectedPath.isNullOrBlank() || imageName.isNullOrBlank()){
            callEditAPI()
        }
        else
            uploadProfilePic(selectedPath!!,imageName!!,mContext)

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
                    loginResponseModel.profile_picture = baseUrl+key
                    callEditAPI()
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


    private fun callEditAPI(){
        Log.d(TAG, "Request Edit API: ${Gson().toJson(loginResponseModel)}")
        APITask.getInstance().callEditUser(this, loginResponseModel)?.let { mDisposable?.add(it) }
    }

    override fun <T> onResponseReceived(response: T, requestCode: Int) {
        val user = SharedPrefs.getUser(MyApplication.getInstance())
        val newData = (response as RegisterResponseModel).data
        if(user != null){
            user.first_name = newData.first_name
            user.last_name = newData.last_name
            user.title = newData.title
            user.profile_picture = newData.profile_picture
            user.mobile_number = newData.mobile_number
            user.country_code = newData.country_code
            SharedPrefs.setUser(MyApplication.getInstance(),user)
        }

        setMessage(Constants.HIDE)
        setMessage(Constants.NAVIGATE)
        Toast.makeText(context, (response as RegisterResponseModel).message, Toast.LENGTH_SHORT)
            .show()
    }

    override fun onResponseError(message: String, requestCode: Int, responseCode: Int) {
        setMessage(Constants.HIDE)
        Log.d(TAG, "Response Edit API Error : Code $responseCode  Message $message")
        setMessage(message)
    }

    private fun isValid(): Boolean {
        if (loginResponseModel.first_name.isNullOrBlank()) {
            setMessage(context.getString(R.string.enter_FN))
            return false
        } else if (loginResponseModel.last_name.isNullOrBlank()) {
            setMessage(context.getString(R.string.enter_LN))
            return false
        } else if (loginResponseModel.mobile_number.isNullOrBlank()) {
            setMessage(context.getString(R.string.enter_number))
            return false
        }
        else if (!isValidMobileNumber(loginResponseModel.country_code.toString()+loginResponseModel.mobile_number)) {
            setMessage(context.getString(R.string.enter_valid_number))
            return false
        }
//        else if (registerRequestModel.password.isNullOrBlank()) {
//            setMessage(context.getString(R.string.enter_password))
//            return false
//        }
        else return true
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
}