package com.example.sameteam.authScreens

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.sameteam.R


class ApprovalActivity : AppCompatActivity() {

    lateinit var btnBack:ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_approval)

        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener {
            onBackPressed()
            finish()
        }


    }

}