package com.example.sameteam.helper

object Keys {

    init {
        // Used to load the 'native-lib' library on application startup.
        System.loadLibrary("native-lib")
    }

    //Native calls from C++ to Kotlin
    external fun developmentAWSSecretKey(): String
    external fun liveAWSSecretKey(): String


    external fun developmentAWSAccessKey(): String
    external fun liveAWSAccessKey(): String


    external fun developmentAWSBucketRegion(): String
    external fun liveAWSBucketRegion(): String


    external fun developmentAWSBaseURL(): String
    external fun liveAWSBaseURL(): String


    external fun developmentAWSBucketName(): String
    external fun liveAWSBucketName(): String
}