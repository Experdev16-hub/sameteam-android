package com.example.sameteam.homeScreen.drawerNavigation.activity

import android.content.Intent
import android.view.View
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sameteam.R
import com.example.sameteam.base.BaseActivity
import com.example.sameteam.databinding.ActivityMyEventsBinding
import com.example.sameteam.helper.Constants
import com.example.sameteam.helper.Utils.loadBannerAd
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.activity.CreateTaskActivity
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.EventModel
import com.example.sameteam.homeScreen.drawerNavigation.MyEventListAdapter
import com.example.sameteam.homeScreen.drawerNavigation.viewModel.MyEventsVM

class MyEventsActivity : BaseActivity<ActivityMyEventsBinding>() {
    override fun layoutID() = R.layout.activity_my_events

    override fun viewModel() = ViewModelProvider(
        this,
        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    ).get(MyEventsVM::class.java)

    lateinit var binding: ActivityMyEventsBinding
    lateinit var myEventsVM: MyEventsVM

    var items = ArrayList<EventModel>()


    override fun initActivity(mBinding: ViewDataBinding) {
        myEventsVM = getViewModel() as MyEventsVM
        binding = mBinding as ActivityMyEventsBinding

        binding.customToolbar.rightIcon.visibility = View.GONE
        binding.customToolbar.title.text = getString(R.string.my_events)
        binding.customToolbar.leftIcon.setOnClickListener {
            onBackPressed()
        }

        binding.adView.loadBannerAd()

        // Call My Events API
        myEventsVM.callMyEvents()

        binding.recView.layoutManager = LinearLayoutManager(this)

        binding.btnAddEvent.setOnClickListener {
            Intent(this, AddEventActivity::class.java)
                .also {
                    startActivityForResult(it, ADD_EVENT_REQUEST_CODE)
                }
        }

        /**
         * General Observer
         */
        myEventsVM.observedChanges().observe(this) { event ->
            event?.getContentIfNotHandled()?.let {
                when (it) {
                    Constants.VISIBLE -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    Constants.HIDE -> {
                        binding.progressBar.visibility = View.GONE
                    }
                    else -> {
                        showMessage(it)
                    }
                }
            }
        }

        myEventsVM.observeEventsList().observe(this) { event ->
            event?.getContentIfNotHandled()?.let {

                if (!it.isNullOrEmpty()) {

                   /* val adapter = MyEventListAdapter(this, it, onEditClicked = {

                        Intent(this, AddEventActivity::class.java)
                            .apply {
                                putExtra("eventId", it)
                                startActivityForResult(this, ADD_EVENT_REQUEST_CODE)
                            }

                    })*/

                     val adapter = MyEventListAdapter(this, it, onItemClicked = { eventId, view->
                         when(view){
                             R.id.img_edit->{
                                 Intent(this, AddEventActivity::class.java)
                                     .apply {
                                         putExtra("eventId", eventId)
                                         startActivityForResult(this, ADD_EVENT_REQUEST_CODE)
                                     }
                             }
                             R.id.ivCreateActivity->{
                                 Intent(this, CreateTaskActivity::class.java)
                                     .apply {
                                         putExtra("eventId", eventId)
                                         startActivity(this)
                                     }
                             }
                         }
                     })
                    binding.recView.adapter = adapter
                    (binding.recView.adapter as MyEventListAdapter).notifyDataSetChanged()
                    binding.recViewLayout.visibility = View.VISIBLE
                    binding.noDataLayout.visibility = View.GONE

                } else {

                    binding.recViewLayout.visibility = View.GONE
                    binding.noDataLayout.visibility = View.VISIBLE

                }
            }

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_EVENT_REQUEST_CODE && resultCode == RESULT_OK) {
            myEventsVM.callMyEvents()

        }
    }

    companion object {
        const val ADD_EVENT_REQUEST_CODE = 100
    }
}