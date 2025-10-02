package com.example.sameteam.quickBlox.callback

import android.os.Bundle
import com.quickblox.core.QBEntityCallback
import com.quickblox.core.exception.QBResponseException


open class QbEntityCallbackImpl<T> : QBEntityCallback<T> {

    override fun onSuccess(result: T, bundle: Bundle?) {

    }

    override fun onError(e: QBResponseException) {

    }
}