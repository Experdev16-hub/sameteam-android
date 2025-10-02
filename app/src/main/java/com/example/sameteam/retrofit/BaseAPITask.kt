package com.example.sameteam.retrofit

import android.content.Context
import android.util.Log
import com.example.sameteam.MyApplication
import com.example.sameteam.R
import com.example.sameteam.helper.Utils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import retrofit2.Response

open class BaseAPITask {

    private fun isInternetAvailable(context: Context): Boolean {
        return Utils.isConnected(context)
    }

    private fun noInternetError(context: Context): String {
        return context.getString(R.string.no_internet)
    }


    protected fun <T> getRequest(
        request: Observable<Response<T>>,
        mListener: OnResponseListener,
        requestCode: Int
    ): DisposableObserver<*>? {
        return if (isInternetAvailable(MyApplication.getInstance())) {
            request.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(APICallback(mListener, requestCode, request))
        } else {
            mListener.onResponseError(noInternetError(MyApplication.getInstance()), requestCode, 100)
            null
        }

    }

}