package com.example.sameteam.homeScreen.bottomNavigation.notificationModule

import android.util.Log
import android.view.View
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sameteam.R
import com.example.sameteam.base.BaseFragment
import com.example.sameteam.databinding.FragmentNotificationBinding
import com.example.sameteam.helper.Constants
import com.example.sameteam.helper.Constants.TYPE_NOTIFICATION_CENTER
import com.example.sameteam.homeScreen.bottomNavigation.notificationModule.adapter.NotificationAdapter
import com.example.sameteam.homeScreen.bottomNavigation.notificationModule.model.NotificationModel
import com.example.sameteam.homeScreen.bottomNavigation.notificationModule.viewModel.NotificationFragVM
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList


class NotificationFragment : BaseFragment<FragmentNotificationBinding>() {

    private val TAG = "NotificationFragment"

    override fun layoutID() = R.layout.fragment_notification

    override fun viewModel() = ViewModelProvider(
        this,
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    ).get(NotificationFragVM::class.java)

    lateinit var binding: FragmentNotificationBinding
    lateinit var notificationFragVM: NotificationFragVM

    var items = ArrayList<NotificationModel>()
    var isPagination = false
    var isCalled = false
    var itemPosition: Int? = null
    var userResponse: String? = null

    override fun onResume() {
        super.onResume()
        items.clear()
        isCalled = false
        notificationFragVM.callGetNotifications(false, 0)
    }

    override fun initFragment(mBinding: ViewDataBinding) {
        notificationFragVM = getViewModel() as NotificationFragVM
        binding = mBinding as FragmentNotificationBinding

        binding.recView.layoutManager = LinearLayoutManager(context)
//        val adpater = NotificationAdapter(requireContext())
        val adapter = NotificationAdapter(requireContext(), items) { item, position, view ->
            when (view) {
                0 -> {
                    Log.d(TAG, "initFragment: Accepted $position")
                    if (item.task_participant_id != null) {
                        itemPosition = position
                        userResponse = Constants.ACCEPTED
                        notificationFragVM.callParticipantAPI(
                            item.task_participant_id!!,
                            Constants.ACCEPTED
                        )
                    }
                }

                1 -> {
                    Log.d(TAG, "initFragment: Declined $position")
                    if (item.task_participant_id != null) {
                        itemPosition = position
                        userResponse = Constants.DECLINED
                        notificationFragVM.callParticipantAPI(
                            item.task_participant_id!!,
                            Constants.DECLINED
                        )
                    }
                }
            }
        }

        binding.recView.adapter = adapter
        binding.recView.addItemDecoration(StickyRecyclerHeadersDecoration(adapter))


        /**
         *  All notifications list Observer
         */
        notificationFragVM.listDataObserver().observe(this) { event ->
            event?.getContentIfNotHandled()?.let {
                if (notificationFragVM.pageCounter == 0) {
                    items.clear()
                }

                items.addAll(it)
                (binding.recView.adapter as NotificationAdapter).notifyDataSetChanged()
                isCalled = false

                if (items.size == 0 && notificationFragVM.totalRecords == 0) {
                    binding.noDataLayout.visibility = View.VISIBLE
                    binding.recViewLayout.visibility = View.GONE
                }
            }
        }

        binding.recView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                if (!recyclerView.canScrollVertically(1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    isPagination = true
                    if (!isCalled) {
                        notificationFragVM.callGetNotifications(true, items.size)
                        isCalled = true
                    }
                }
            }
        })

        /**
         * General Observer
         */
        notificationFragVM.observedChanges().observe(this) { event ->
            event?.getContentIfNotHandled()?.let {
                when (it) {
                    Constants.VISIBLE -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }

                    Constants.HIDE -> {
                        binding.progressBar.visibility = View.GONE
                    }

                    Constants.NAVIGATE -> {

                        if (itemPosition != null && userResponse != null) {

                            try {
                                if (userResponse == Constants.ACCEPTED) {

                                    if (notificationFragVM.taskParticipantResponseModel.data.response != Constants.PENDING
                                        && notificationFragVM.taskParticipantResponseModel.data.response != Constants.DECLINED
                                    )
                                        items[itemPosition!!].participant_response =
                                            Constants.ACCEPTED

                                } else if (userResponse == Constants.DECLINED) {
                                    items[itemPosition!!].participant_response = Constants.DECLINED
                                } else {
                                    items[itemPosition!!].participant_response = Constants.PENDING
                                }
                                (binding.recView.adapter as NotificationAdapter).notifyItemChanged(
                                    itemPosition!!
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    else -> {
                        showMessage(it)
                    }
                }
            }

        }
    }
}