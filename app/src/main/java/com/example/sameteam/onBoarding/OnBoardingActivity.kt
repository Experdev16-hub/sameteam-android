package com.example.sameteam.onBoarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.example.sameteam.MyApplication
import com.example.sameteam.R
import com.example.sameteam.authScreens.LoginActivity
import com.example.sameteam.helper.SharedPrefs
import com.example.sameteam.onBoarding.model.OnBoardingItem
import com.google.android.material.button.MaterialButton


class OnBoardingActivity : AppCompatActivity() {

    lateinit var onboardingAdapter: OnBoardingAdapter
    lateinit var layoutOnboardingIndicator: LinearLayout
    lateinit var buttonOnboardingAction: MaterialButton
    lateinit var skip: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_on_boarding)

        layoutOnboardingIndicator = findViewById(R.id.layoutOnboardingIndicators)
        buttonOnboardingAction = findViewById(R.id.btnNext)
        skip = findViewById(R.id.txtSkip)

        setOnboardingItem()

        SharedPrefs.setOnboardStatus(MyApplication.getInstance(),false)

        val onboardingViewPager = findViewById<ViewPager2>(R.id.onboardingViewPager)
        onboardingViewPager.adapter = onboardingAdapter

        setOnboardingIndicator()
        setCurrentOnboardingIndicators(0)

        onboardingViewPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setCurrentOnboardingIndicators(position)
            }
        })

        buttonOnboardingAction.setOnClickListener {
            if (onboardingViewPager.currentItem + 1 < onboardingAdapter.itemCount) {
                onboardingViewPager.currentItem = onboardingViewPager.currentItem + 1
            } else {
                startActivity(Intent(applicationContext, LoginActivity::class.java))
                finish()
            }
        }

        skip.setOnClickListener{
            val intent = Intent(this,LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

    }

    private fun setOnboardingIndicator() {
        val indicators: Array<ImageView?> = arrayOfNulls(onboardingAdapter.itemCount)
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(8, 0, 8, 0)
        for (i in indicators.indices) {
            indicators[i] = ImageView(applicationContext)
            indicators[i]?.setImageDrawable(
                ContextCompat.getDrawable(
                    applicationContext, R.drawable.onboarding_indicator_inactive
                )
            )
            indicators[i]?.layoutParams = layoutParams
            layoutOnboardingIndicator.addView(indicators[i])
        }
    }


    private fun setCurrentOnboardingIndicators(index: Int) {
        val childCount = layoutOnboardingIndicator.childCount
        for (i in 0 until childCount) {
            val imageView: ImageView = layoutOnboardingIndicator.getChildAt(i) as ImageView
            if (i == index) {
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(
                        applicationContext,
                        R.drawable.onboarding_indicator_active
                    )
                )
            } else {
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(
                        applicationContext,
                        R.drawable.onboarding_indicator_inactive
                    )
                )
            }


        }
        if (index == onboardingAdapter.itemCount - 1) {
            buttonOnboardingAction.text = getString(R.string.get_started)
            skip.visibility = View.INVISIBLE
        } else {
            buttonOnboardingAction.text = getString(R.string.next)
            skip.visibility = View.VISIBLE

        }

    }

    private fun setOnboardingItem() {
        val onBoardingItems = arrayListOf<OnBoardingItem>()

        val onBoarding1 = OnBoardingItem()
        onBoarding1.title = getString(R.string.ACTIVITIES)
        onBoarding1.description = getString(R.string.desc_1)
        onBoarding1.image = R.drawable.onboarding_1

        val onBoarding2 = OnBoardingItem()
        onBoarding2.title = getString(R.string.COMMUNICATIONS)
        onBoarding2.description = getString(R.string.desc_2)
        onBoarding2.image = R.drawable.onboarding_2

        val onBoarding3 = OnBoardingItem()
        onBoarding3.title = getString(R.string.TEAMS)
        onBoarding3.description =  getString(R.string.desc_3)
        onBoarding3.image = R.drawable.onboarding_3



        onBoardingItems.add(onBoarding1)
        onBoardingItems.add(onBoarding2)
        onBoardingItems.add(onBoarding3)
        onboardingAdapter = OnBoardingAdapter(onBoardingItems)
    }
}