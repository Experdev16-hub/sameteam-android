package com.example.sameteam.homeScreen.bottomNavigation.taskModule.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.childFragment.AllTasksFragment

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int {
        return 4
    }

    override fun createFragment(position: Int): Fragment {
       return AllTasksFragment(position)
    }
}