package com.example.sameteam.onBoarding

import com.example.sameteam.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.example.sameteam.onBoarding.model.OnBoardingItem


class OnBoardingAdapter(onBoardingItems: List<OnBoardingItem>?): RecyclerView.Adapter<OnBoardingAdapter.OnBoardingViewHolder>() {

    private var onBoardingItems: List<OnBoardingItem>? = null

    init {
        this.onBoardingItems = onBoardingItems
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnBoardingViewHolder {
        return OnBoardingViewHolder(
            LayoutInflater.from(parent.getContext()).inflate(
                R.layout.boarding_layout, parent, false
            )
        );
    }

    override fun onBindViewHolder(@NonNull holder: OnBoardingViewHolder, position: Int) {
        holder.setOnBoardingData(onBoardingItems!![position])
    }

    override fun getItemCount(): Int {
        return onBoardingItems!!.size
    }

    class OnBoardingViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textTitle: TextView = itemView.findViewById(R.id.textTitle)
        private val textDescription: TextView = itemView.findViewById(R.id.textDescription)
        private val imageOnboarding: ImageView = itemView.findViewById(R.id.imageOnboarding)
        fun setOnBoardingData(onBoardingItem: OnBoardingItem) {
            textTitle.text = onBoardingItem.title
            textDescription.text = onBoardingItem.description
            imageOnboarding.setImageResource(onBoardingItem.image)
        }

    }
}