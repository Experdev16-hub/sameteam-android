package com.example.sameteam.homeScreen.bottomNavigation.taskModule.childFragment

import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sameteam.MyApplication
import com.example.sameteam.R
import com.example.sameteam.authScreens.LoginActivity
import com.example.sameteam.base.BaseFragment
import com.example.sameteam.databinding.FragmentAllTasksBinding
import com.example.sameteam.helper.Constants
import com.example.sameteam.helper.SharedPrefs
import com.example.sameteam.helper.Utils
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.adapter.TaskListAdapter
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.interfaceses.OnBottomSheetDismissListener
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.viewModel.AllTasksFragVM
import com.example.sameteam.quickBlox.QbDialogHolder
import com.example.sameteam.quickBlox.chat.ChatHelper
import com.example.sameteam.quickBlox.db.QbUsersDbManager
import com.example.sameteam.quickBlox.util.SharedPrefsHelper
import com.quickblox.messages.services.QBPushManager
import com.quickblox.messages.services.SubscribeService
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*


class AllTasksFragment(val position: Int) : BaseFragment<FragmentAllTasksBinding>(),
    OnBottomSheetDismissListener {
    private val TAG = "AllTasksFragment"

    override fun layoutID() = R.layout.fragment_all_tasks

    override fun viewModel() = ViewModelProvider(
        this,
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    ).get(AllTasksFragVM::class.java)

    lateinit var allTasksFragVM: AllTasksFragVM
    lateinit var binding: FragmentAllTasksBinding
    lateinit var callBackVariable: String
    var dateString = ""
    var dateString1 = ""
    var dateString2 = ""
    lateinit var adapter: TaskListAdapter
    val today = LocalDate.now()

    var isCalled = false

    override fun onResume() {
        super.onResume()
        SharedPrefs.saveGroupName(MyApplication.getInstance(), "")

        when (position) {
            /*   0 -> {
                   if(Utils.isConnected(requireActivity())){
                       allTasksFragVM.fetchTasksLiveData("","").observe(viewLifecycleOwner, Observer {
                           lifecycleScope.launch {
                               adapter.submitData(it)
                           }
                       })
                   }
                   else{
                       showMessage(getString(R.string.no_internet))
                   }
               }
               1 -> {
                   getTaskList(today)
               }
               2 -> {
                   getTaskList(today.plusDays(1))
               }
               3 -> {
                  getWeekTaskList()
               }*/


            0 -> {
                getTaskList(today)
            }

            1 -> {
                getTaskList(today.plusDays(1))
            }

            2 -> {
                getWeekTaskList()
            }

            3 -> {
                if (Utils.isConnected(requireActivity())) {
                    callBackVariable = "All"
                    Log.d("DebugTomorrow", "All Task Frag DateCheck:  ")

                    allTasksFragVM.fetchTasksLiveData("", "").observe(viewLifecycleOwner, Observer {
                        lifecycleScope.launch {
                            adapter.submitData(it)
                        }
                    })
                } else {
                    showMessage(getString(R.string.no_internet))
                }
            }

        }

        allTasksFragVM.callMyProfile()
    }


    override fun initFragment(mBinding: ViewDataBinding) {
        allTasksFragVM = getViewModel() as AllTasksFragVM
        binding = mBinding as FragmentAllTasksBinding

        Log.d(TAG, "initFragment: $position")

        binding.recView.layoutManager = LinearLayoutManager(requireContext())
        adapter = TaskListAdapter(this, requireContext())
        binding.recView.adapter = adapter
        binding.lifecycleOwner = this

        when (position) {
            0 -> {
                if (Utils.isConnected(requireActivity())) {
                    callBackVariable = "All"
                    Log.d("DebugTomorrow", "All Task Frag DateCheck:  ")

                    allTasksFragVM.fetchTasksLiveData("", "").observe(viewLifecycleOwner, Observer {
                        lifecycleScope.launch {
                            adapter.submitData(it)
                        }
                    })
                } else {
                    showMessage(getString(R.string.no_internet))
                }
            }

            1 -> {
                getTaskList(today)
            }

            2 -> {
                getTaskList(today.plusDays(1))
            }

            3 -> {
                getWeekTaskList()
            }
        }




        adapter.addLoadStateListener { loadState ->
            if (loadState.refresh == LoadState.Loading) {
                binding.progressBar.visibility = View.VISIBLE
                binding.noDataLayout.visibility = View.GONE
            } else {
                binding.progressBar.visibility = View.GONE

                if (adapter.itemCount == 0) {
                    binding.noDataLayout.visibility = View.VISIBLE
                    binding.recViewLayout.visibility = View.GONE
                } else {
                    binding.noDataLayout.visibility = View.GONE
                    binding.recViewLayout.visibility = View.VISIBLE
                }

                val error = when {
                    loadState.prepend is LoadState.Error -> loadState.prepend as LoadState.Error
                    loadState.append is LoadState.Error -> loadState.append as LoadState.Error
                    loadState.refresh is LoadState.Error -> loadState.refresh as LoadState.Error
                    else -> null
                }

                error?.let {
                    Toast.makeText(requireActivity(), it.error.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        /**
         * General Observer
         */
        allTasksFragVM.observedChanges().observe(this) { event ->
            event?.getContentIfNotHandled()?.let {
                when (it) {
                    Constants.VISIBLE -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }

                    Constants.HIDE -> {
                        binding.progressBar.visibility = View.GONE
                    }

                    Constants.FORCE_LOGOUT -> {
                        unsubscribeFromPushes()
                    }

                    else -> {
                        if (it != "Internal Server Error")
                            showMessage(it)
                    }
                }
            }
        }

    }


    private fun getTaskList(localDate: LocalDate) {
        if (Utils.isConnected(requireActivity())) {
//            var dateString = ""
            dateString = ""
            dateString += if (localDate.dayOfMonth < 10)
                "0${localDate.dayOfMonth}"
            else
                "${localDate.dayOfMonth}"

            dateString += "-${
                localDate.month.getDisplayName(
                    TextStyle.SHORT,
                    Locale.ENGLISH
                )
            }-${localDate.year}"
            Log.d("DebugTomorrow", "All Task Frag DateCheck: $dateString tomorrow ")

            callBackVariable = "DayWise"
            allTasksFragVM.fetchTasksLiveData(dateString, dateString).observe(viewLifecycleOwner) {
                lifecycleScope.launch {
                    adapter.submitData(it)
                }
            }
        } else {
            showMessage(getString(R.string.no_internet))
        }
    }

    private fun getWeekTaskList() {
        if (Utils.isConnected(requireActivity())) {

            val firstDate: LocalDate
            val lastDate: LocalDate

            if (today.dayOfWeek.value == 7) {
                firstDate = today
                lastDate = today.plusDays(6)
            } else {
                firstDate = today.minusDays(today.dayOfWeek.value.toLong())
                lastDate = today.plusDays(6 - today.dayOfWeek.value.toLong())
            }

            Log.d(TAG, "onResume: $firstDate  $lastDate")



            dateString1 += if (firstDate.dayOfMonth < 10)
                "0${firstDate.dayOfMonth}"
            else
                "${firstDate.dayOfMonth}"

            dateString1 += "-${
                firstDate.month.getDisplayName(
                    TextStyle.SHORT,
                    Locale.ENGLISH
                )
            }-${firstDate.year}"


            dateString2 += if (lastDate.dayOfMonth < 10)
                "0${lastDate.dayOfMonth}"
            else
                "${lastDate.dayOfMonth}"

            dateString2 += "-${
                lastDate.month.getDisplayName(
                    TextStyle.SHORT,
                    Locale.ENGLISH
                )
            }-${lastDate.year}"
            callBackVariable = "WeekWise"
            Log.d("DebugTomorrow", "All Task Frag DateCheck: $dateString1 ")

            allTasksFragVM.fetchTasksLiveData(dateString1, dateString2)
                .observe(viewLifecycleOwner) {
                    lifecycleScope.launch {
                        adapter.submitData(it)
                    }
                }
        } else {
            showMessage(getString(R.string.no_internet))
        }
    }


    private fun unsubscribeFromPushes() {
        if (QBPushManager.getInstance().isSubscribedToPushes) {
            QBPushManager.getInstance().addListener(object : QBPushManager.QBSubscribeListener {
                override fun onSubscriptionCreated() {}

                override fun onSubscriptionError(e: Exception?, i: Int) {
                    Log.d("Subscription", "SubscriptionError" + e?.localizedMessage)

                    showMessage(getString(R.string.something_went_wrong))
                    e?.printStackTrace()
                }

                override fun onSubscriptionDeleted(success: Boolean) {
                    Log.d(TAG, "Subscription Deleted -> Success: $success")
                    QBPushManager.getInstance().removeListener(this)
                    performLogout()
                }
            })
            SubscribeService.unSubscribeFromPushes(requireContext())
        } else {
            performLogout()
        }
    }

    fun performLogout() {
        ChatHelper.destroy()
        SharedPrefsHelper.clearAllData()
        SharedPrefs.clearAllData(MyApplication.getInstance())
        QbDialogHolder.clear()
        QbUsersDbManager.clearDB()
        startActivity(LoginActivity::class.java)
        requireActivity().finish()
    }

    override fun onBottomSheetDismissed() {
        Log.e("CallBack", "CallBack from ALL Task Fragment")
        when (callBackVariable) {
            "WeekWise" -> {
                getWeekTaskList()
            }

            "DayWise" -> {
                allTasksFragVM.fetchTasksLiveData(dateString, dateString)
                    .observe(viewLifecycleOwner) {
                        lifecycleScope.launch {
                            adapter.submitData(it)
                        }
                    }
            }

            "All" -> {
                allTasksFragVM.fetchTasksLiveData("", "").observe(viewLifecycleOwner, Observer {
                    lifecycleScope.launch {
                        adapter.submitData(it)
                    }
                })
            }
        }
    }
}