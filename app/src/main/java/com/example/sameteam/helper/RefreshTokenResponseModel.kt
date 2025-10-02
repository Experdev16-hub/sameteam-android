package com.example.sameteam.helper

import com.example.sameteam.base.BaseResponse

class RefreshTokenResponseModel : BaseResponse<RefreshTokenResponseModel.Data>() {

    inner class Data(
        var token:String? =null
    )
}