package com.example.sameteam.retrofit

import android.content.Intent
import android.util.Log
import androidx.core.app.ActivityCompat.finishAffinity
import androidx.core.app.NotificationManagerCompat
import com.example.sameteam.MyApplication
import com.example.sameteam.R
import com.example.sameteam.authScreens.LoginActivity
import com.example.sameteam.base.BaseResponse
import com.example.sameteam.helper.RefreshTokenRequestModel
import com.example.sameteam.helper.RefreshTokenResponseModel
import com.example.sameteam.helper.SharedPrefs
import com.example.sameteam.quickBlox.QbDialogHolder
import com.example.sameteam.quickBlox.chat.ChatHelper
import com.example.sameteam.quickBlox.db.QbUsersDbManager
import com.example.sameteam.quickBlox.util.SharedPrefsHelper
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import retrofit2.Response


class APICallback<T>(
    private val mListener: OnResponseListener,
    private val requestCode: Int,
    val request: Observable<Response<T>>
) : DisposableObserver<Response<T>>() {
    private val TAG = "APICallback"

    var mDisposable: Disposable? = null
    val Message = "message"

   companion object var is403Error: Boolean? = false

    override fun onNext(response: Response<T>) {
        var msg: String?
        try {
            msg = (response.body() as BaseResponse<*>).message
        } catch (e: Exception) {
            try {
                val jsonObject = JSONObject(response.errorBody()?.string()!!)
                msg = jsonObject.getString("message")
            } catch (e: Exception) {
                msg = null
            }
        }
        when (response.code()) {
            200 -> {
                mListener.onResponseReceived(response.body(), requestCode)
            }
            203 -> mListener.onResponseError(
                msg ?: "Non-Authoritative Information",
                requestCode,
                response.code()
            )
            204 -> mListener.onResponseError(
                "No Content",
                requestCode,
                response.code()
            )
            208 ->
                mListener.onResponseError(
                    "You have used this address in your product. You can't delete this address",
                    requestCode,
                    response.code()
//                mListener.onResponseError(
//                jobj?.getString(Message) ?: "Already Reported",
//                requestCode,
//                response.code()
                )
            302 -> mListener.onResponseError(
                msg ?: "Check input",
                requestCode,
                response.code()
            )
            400 -> mListener.onResponseError(
                msg ?: "Bad Request",
                requestCode,
                response.code()
            )
            401 -> {

                if (!is403Error!! ?: false) {
//                    is403Error = true
                    try {
                        val user = SharedPrefs.getUser(MyApplication.getInstance())
                        if (user != null && user.id != 0) {
                            callRefreshToken()
                        } else {
                            mListener.onResponseError(
                                msg ?: MyApplication.getInstance()
                                    .getString(R.string.alert_no_user_found_401),
                                requestCode,
                                response.code()
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        mListener.onResponseError(
                            msg ?: MyApplication.getInstance()
                                .getString(R.string.alert_no_user_found_401),
                            requestCode,
                            response.code()
                        )
                    }
                }

                // mListener.onResponseError("Unauthorised USER....", requestCode, 401)
            }
            403 -> {
                mListener.onResponseError(
                    msg ?: MyApplication.getInstance().getString(R.string.alert_no_user_found_401),
                    requestCode,
                    response.code() ?: 403
                )
            }
            404 -> mListener.onResponseError(
                msg ?: "Not Found",
                requestCode,
                response.code()
            )
            406 ->
                mListener.onResponseError(
                    msg ?: "Something went wrong, Please try later",
                    requestCode,
                    response.code()
                )
            500 -> mListener.onResponseError("Internal Server Error", requestCode, response.code())
//            412 -> {
//                callRefreshToken()
//            }

            else -> mListener.onResponseError(
                "Something went wrong, Please try later",
                requestCode,
                0
            )
        }
    }

    override fun onError(e: Throwable) {
        Log.d(TAG, "onError: ${e.message}")
        if (e is java.net.ConnectException) {
            mListener.onResponseError(e.message!!, requestCode, 0)
        }
    }

    override fun onComplete() {
        Log.d(TAG, "onComplete: Called")
    }

    private fun callRefreshToken() {
        SharedPrefs.clearToken(MyApplication.getInstance())
        val refreshTokenRequestModel = RefreshTokenRequestModel()
        refreshTokenRequestModel.refresh_token =
            SharedPrefs.getUser(MyApplication.getInstance())?.refresh_token

        mDisposable = APITask.getInstance().callAccessToken(object : OnResponseListener {
            override fun <T> onResponseReceived(response: T, requestCode: Int) {
                SharedPrefs.storeToken(
                    MyApplication.getInstance(),
                    (response as RefreshTokenResponseModel).data.token as String
                )
                request.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(APICallback(mListener, requestCode, request))
            }

            override fun onResponseError(message: String, requestCode: Int, responseCode: Int) {

                if (responseCode == 403) {

                    is403Error = true
                    mListener.onResponseError(
                        message ?: MyApplication.getInstance()
                            .getString(R.string.alert_no_user_found_401),
                        requestCode,
                        responseCode
                    )

                    try {
                        NotificationManagerCompat.from(MyApplication.getInstance()).cancelAll()
                        ChatHelper.destroy()
                        SharedPrefsHelper.clearAllData()
                        SharedPrefs.clearAllData(MyApplication.getInstance())
                        QbDialogHolder.clear()
                        QbUsersDbManager.clearDB()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    try {
                        Retrofit.cancelAllRequest()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    val intent = Intent(MyApplication.getInstance(), LoginActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
//                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    MyApplication.getInstance().startActivity(intent)

                } else {
                    mListener.onResponseError(
                        "Unauthorized access, Please login again",
                        requestCode,



                        0
                    )
                }
            }
        }, refreshTokenRequestModel, requestCode)
    }
}