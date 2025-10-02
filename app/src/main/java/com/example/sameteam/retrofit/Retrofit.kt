package com.example.sameteam.retrofit

import com.amazonaws.http.HttpClient
import com.example.sameteam.BuildConfig
import com.example.sameteam.MyApplication
import com.example.sameteam.helper.SharedPrefs
import com.google.gson.GsonBuilder
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class Retrofit {

    companion object Singleton {
        private lateinit var mRetrofit: Retrofit

        var client: OkHttpClient? = null

        fun init() {
            val httpClient = OkHttpClient.Builder()

            httpClient.readTimeout(10, TimeUnit.MINUTES)
            httpClient.connectTimeout(1, TimeUnit.MINUTES)
            httpClient.writeTimeout(10, TimeUnit.MINUTES)

            if (BuildConfig.FLAVOR == "client") {

                httpClient.addInterceptor { chain ->
                    val original = chain.request()
                    val builder = original.newBuilder()
                    builder.header("Content-Type", "application/json")
                    SharedPrefs.getToken(MyApplication.getInstance())?.let {
                        builder.header(
                            "api-key",
                            it
                        )
                    }
                    builder.method(original.method, original.body)
                    chain.proceed(builder.build())
                }
            } else {
                httpClient.addInterceptor { chain ->
                    val original = chain.request()
                    val builder = original.newBuilder()
                    builder.header("Content-Type", "application/json")
                    SharedPrefs.getToken(MyApplication.getInstance())?.let {
                        builder.header(
                            "api-key",
                            it
                        )
                    }
                    builder.method(original.method, original.body)
                    chain.proceed(builder.build())
                }

            }

            val interceptor = HttpLoggingInterceptor()
            if (BuildConfig.DEBUG)
                interceptor.level = HttpLoggingInterceptor.Level.BODY
            else
                interceptor.level = HttpLoggingInterceptor.Level.NONE

            httpClient.addInterceptor(interceptor)

//            val client = httpClient.build()
            client = httpClient.build()
            client!!.dispatcher.maxRequests = Integer.MAX_VALUE
            val gson = GsonBuilder()
                .setLenient()
                .create()

            mRetrofit = Retrofit.Builder()
                .baseUrl(API.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
        }

        fun getRetrofit(): Retrofit {
            return mRetrofit
        }


        fun cancelAllRequest() {
            if (client != null) {
                client?.dispatcher?.cancelAll()
            }
        }

    }


}