package com.example.sameteam.homeScreen.drawerNavigation.activity

import android.content.DialogInterface
import android.graphics.Color
import android.util.Log
import android.view.View
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sameteam.BR
import com.example.sameteam.R
import com.example.sameteam.authScreens.model.UserModel
import com.example.sameteam.base.BaseActivity
import com.example.sameteam.base.BaseViewModel
import com.example.sameteam.base.BindingAdapter
import com.example.sameteam.databinding.ActivityAddEventBinding
import com.example.sameteam.helper.*
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.bottomSheet.InvitePeopleBottomSheet
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.bottomSheet.SelectTaskTimeBottomSheet
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.bottomSheet.SpecificDateBottomSheet
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.EventModel
import com.example.sameteam.homeScreen.bottomNavigation.chatModule.adapter.UserDirectoryAdapter
import com.example.sameteam.homeScreen.drawerNavigation.viewModel.AddEventVM
import com.example.sameteam.quickBlox.db.QbUsersDbManager
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.quickblox.chat.model.QBChatDialog
import com.quickblox.users.model.QBUser
import java.text.SimpleDateFormat
import java.util.*

class AddEventActivity : BaseActivity<ActivityAddEventBinding>(),
    UserDirectoryAdapter.InvitePeopleListener, SelectedUsersBottomSheet.OnUserSearchedListener,
    SpecificDateBottomSheet.SpecificDateListener,
    SelectTaskTimeBottomSheet.TaskTimeListener, UserDirectoryAdapter.InviteTeamListener {

    private val TAG = "AddEventActivity"

    private lateinit var binding: ActivityAddEventBinding

    lateinit var addEventVM: AddEventVM

    private lateinit var fragment: Fragment

    var userList = ArrayList<UserModel>()
    var selectedUserList = ArrayList<UserModel>()
    var participantsIds = mutableSetOf<Int>()

    private var isEdit = false

    private var eventId: Int? = null

    private var lastSelectedColor: Int? = null

    private var mInterstitialAd: InterstitialAd? = null

    var qbChatDialog: QBChatDialog? = null
    private var currentUser: QBUser? = null
    lateinit var qbDialogUsers: ArrayList<QBUser>
    lateinit var qbDialogUserIds: ArrayList<Int>
    private var teamNames = kotlin.collections.ArrayList<String>()

    override fun layoutID(): Int = R.layout.activity_add_event

    override fun viewModel(): BaseViewModel = ViewModelProvider(this).get(AddEventVM::class.java)

    override fun initActivity(mBinding: ViewDataBinding) {

        binding = mBinding as ActivityAddEventBinding

        addEventVM = getViewModel() as AddEventVM

        binding.customToolbar.rightIcon.visibility = View.GONE
        binding.customToolbar.leftIcon.setOnClickListener {
            onBackPressed()
        }

        eventId = intent.getIntExtra("eventId", -1)

        if (eventId != -1) {
            isEdit = true
            binding.customToolbar.title.text = getString(R.string.edit_event)
            binding.btnCreateTask.text = getString(R.string.submit)
            binding.startTeamLayout.visibility = View.GONE

            addEventVM.getEventById(eventId!!)

        } else {

            binding.customToolbar.title.text = getString(R.string.create_event)
            binding.btnCreateTask.text = getString(R.string.create_event)

        }

        if (SharedPrefs.getUser(this)?.plan_upgrade == false)
            loadInterstitialAd()

        addEventVM.getUserList()

        binding.edtColor.setOnClickListener {
            colorPicker()
        }

        binding.cvColor.setOnClickListener {
            colorPicker()
        }

        binding.btnInvite.setOnClickListener {
            fragment = InvitePeopleBottomSheet(this, userList, selectedUserList)
            (fragment as InvitePeopleBottomSheet).show(
                supportFragmentManager,
                InvitePeopleBottomSheet::class.java.name
            )
        }

        addEventVM.observedChanges().observe(this) { event ->
            event?.getContentIfNotHandled()?.let {
                when (it) {
                    Constants.VISIBLE -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    Constants.HIDE -> {
                        binding.progressBar.visibility = View.GONE
                    }

                    Constants.NAVIGATE -> {
                        setResult(RESULT_OK)
                        if (mInterstitialAd != null) {
                            mInterstitialAd?.show(this)
                        } else {
                            Log.d("TAG", "The interstitial ad wasn't ready yet.")
                            onBackPressed()
                        }
                    }
                    else -> {
                        binding.progressBar.visibility = View.GONE
                        showMessage(it)
                    }
                }
            }
        }

        addEventVM.observeUsers().observe(this) { event ->
            event?.getContentIfNotHandled()?.let {
                if (it.isNotEmpty()) {
                    userList = it
                }
            }
        }

        addEventVM.observeEventDetails().observe(this) { event ->
            event?.getContentIfNotHandled()?.let {
                loadEventDetails(it)
            }
        }

        binding.btnCreateTask.setOnClickListener {
            Utils.hideKeyboard(this)

            addEventVM.addEventRequestModel.title = binding.eventName.text.toString()
            addEventVM.addEventRequestModel.event_description = binding.description.text.toString()
            addEventVM.addEventRequestModel.start_date = binding.eventStartDate.text.toString()
            addEventVM.addEventRequestModel.end_date = binding.eventEndDate.text.toString()
            addEventVM.addEventRequestModel.start_time = binding.eventStartTime.text.toString()
            addEventVM.addEventRequestModel.end_time = binding.eventEndTime.text.toString()
            addEventVM.addEventRequestModel.colour = binding.edtColor.text.toString()

            val startDate = binding.eventStartDate.text.toString()
            val endDate = binding.eventEndDate.text.toString()
            val mTimeZone =
                SimpleDateFormat("Z", Locale.getDefault()).format(System.currentTimeMillis())

            val startDateTimeString =
                startDate + "T" + binding.eventStartTime.text.toString() + ".000" + mTimeZone
            val endDateTimeString =
                endDate + "T" + binding.eventEndTime.text.toString() + ".000" + mTimeZone

            addEventVM.eventStartTimestamp = localToUTCTimestamp(startDateTimeString)
            addEventVM.eventEndTimestamp = localToUTCTimestamp(endDateTimeString)

            addEventVM.addEventRequestModel.id = eventId

            val tempArrayList = ArrayList<Int>()
            tempArrayList.addAll(participantsIds)
            addEventVM.addEventRequestModel.user_ids = tempArrayList.toIntArray()

            addEventVM.onCreateClicked(isEdit)

        }

        binding.eventStartTime.setOnClickListener {
            Utils.hideKeyboard(this)
            fragment = SelectTaskTimeBottomSheet(Constants.START_DATE)
            (fragment as SelectTaskTimeBottomSheet).show(
                supportFragmentManager,
                SelectTaskTimeBottomSheet::class.java.name
            )
        }

        binding.eventEndTime.setOnClickListener {
            Utils.hideKeyboard(this)
            fragment = SelectTaskTimeBottomSheet(Constants.END_DATE)
            (fragment as SelectTaskTimeBottomSheet).show(
                supportFragmentManager,
                SelectTaskTimeBottomSheet::class.java.name
            )
        }

        binding.eventStartDate.setOnClickListener {
            Utils.hideKeyboard(this)
            val fragment = SpecificDateBottomSheet(Constants.START_DATE)
            fragment.show(
                supportFragmentManager,
                SpecificDateBottomSheet::class.java.name
            )
        }

        binding.eventEndDate.setOnClickListener {
            Utils.hideKeyboard(this)
            val fragment = SpecificDateBottomSheet(Constants.END_DATE)
            fragment.show(
                supportFragmentManager,
                SpecificDateBottomSheet::class.java.name
            )
        }

        loadUserAdapter()

    }

    private fun loadEventDetails(eventDetails: EventModel) {

        binding.eventName.setText(eventDetails.title)
        binding.description.setText(eventDetails.event_description)
        binding.eventStartDate.setText(eventDetails.start_date)
        binding.eventStartTime.setText(eventDetails.start_time)
        binding.eventEndDate.setText(eventDetails.end_date)
        binding.eventEndTime.setText(eventDetails.end_time)
        binding.edtColor.setText(eventDetails.colour)
        binding.cvColor.setCardBackgroundColor(Color.parseColor(eventDetails.colour))

        selectedUserList = eventDetails.event_user.map { it.user } as ArrayList<UserModel>

        eventDetails.event_user.forEach {
            it.user?.id?.let { it1 -> participantsIds.add(it1) }
        }

        loadUserAdapter()
    }

    private fun loadUserAdapter() {
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, true)
        layoutManager.stackFromEnd = true
        binding.recView.layoutManager = layoutManager
        binding.recView.adapter = BindingAdapter(
            layoutId = R.layout.participants_user_card,
            br = BR.model,
            list = selectedUserList,
            clickListener = { view, position ->
                when (view.id) {
                    R.id.btnRemove -> {

                        val user = selectedUserList[position]

                        selectedUserList.removeAt(position)

                        participantsIds.remove(user.id)

                        binding.recView.adapter?.notifyDataSetChanged()

                    }
                }
            }
        )
    }

    private fun colorPicker() {
        ColorPickerDialogBuilder
            .with(this)
            .setTitle("Choose color")
            .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
            .density(12)
            .noSliders()
            .setPositiveButton(
                "ok"
            ) { d, selectedColor, allColors ->
                lastSelectedColor = selectedColor
                val hexColor = Integer.toHexString(lastSelectedColor!!).substring(2)
                binding.edtColor.setText("#$hexColor")
                binding.cvColor.setCardBackgroundColor(lastSelectedColor!!)

            }
            .setNegativeButton("cancel", DialogInterface.OnClickListener { dialog, which ->
                dialog.dismiss()
            })
            .build()
            .show()
    }


    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            this,
            getString(R.string.interstitial_unit_id),
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError.toString())
                    mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "Ad was loaded.")
                    mInterstitialAd = interstitialAd
                    loadInterstitialCallbacks()
                }
            })

    }

    private fun loadInterstitialCallbacks() {
        mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                // Called when a click is recorded for an ad.
                Log.d(TAG, "Ad was clicked.")
            }

            override fun onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                Log.d(TAG, "Ad dismissed fullscreen content.")
                mInterstitialAd = null
                onBackPressed()
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                // Called when ad fails to show.
                Log.e(TAG, "Ad failed to show fullscreen content.")
                mInterstitialAd = null
                onBackPressed()
            }

            override fun onAdImpression() {
                // Called when an impression is recorded for an ad.
                Log.d(TAG, "Ad recorded an impression.")
            }

            override fun onAdShowedFullScreenContent() {
                // Called when ad is shown.
                Log.d(TAG, "Ad showed fullscreen content.")
            }
        }
    }

    /**
     * Override method of UserDirectoryAdapter when user is invited
     */
    override fun onInvite(participant: UserModel) {
        if (!participantsIds.contains(participant.id)) {
            selectedUserList.add(participant)
            participantsIds.add(participant.id!!)
        }
        binding.recView.scrollToPosition(selectedUserList.size - 1)
        addEventVM.onInviteUserClicked(selectedUserList)
        binding.recView.adapter?.notifyDataSetChanged()
    }

    /**
     * Override method of UserDirectoryAdapter when user is removed
     */
    override fun onRemove(participant: UserModel) {

        for (user in selectedUserList) {
            if (user.id == participant.id) {
                selectedUserList.remove(user)
                participantsIds.remove(participant.id)
                break
            }
        }
        binding.recView.scrollToPosition(selectedUserList.size - 1)
        addEventVM.onInviteUserClicked(selectedUserList)
        binding.recView.adapter?.notifyDataSetChanged()

    }

    override fun onTaskTimeDone(value: String, location: String) {
        if (location == Constants.START_DATE)
            binding.eventStartTime.setText(value)
        else if (location == Constants.END_DATE)
            binding.eventEndTime.setText(value)
    }

    override fun onSpecificDateDone(value: String, location: String) {
        if (location == Constants.START_DATE)
            binding.eventStartDate.setText(value)
        else if (location == Constants.END_DATE)
            binding.eventEndDate.setText(value)
    }

    override fun onUserSearched(search_key: String) {
        addEventVM.getUserList(search_key)
    }


    /**
     * Override method of UserDirectoryAdapter when team is added
     */
    override fun onInvite(chatDialog: QBChatDialog) {

        teamNames.add(chatDialog.name)

        if (!chatDialog.occupants.isNullOrEmpty()) {
            for (id in chatDialog.occupants) {
                if (id != currentUser?.id) {
                    val qbUser = QbUsersDbManager.getUserById(id)
                    if (qbUser != null && !qbUser.customData.isNullOrBlank()) {
                        val user = Utils.getUserFromQBUser(qbUser.customData)
                        if (!participantsIds.contains(user.id)) {
                            selectedUserList.add(user)
                            participantsIds.add(user.id!!)
                        }

                        if (::qbDialogUserIds.isInitialized && qbChatDialog != null && !qbDialogUserIds.contains(
                                qbUser.id
                            )
                        ) {
                            qbDialogUserIds.add(qbUser.id)
                            qbDialogUsers.add(qbUser)
                        }
                    }
                }
            }
        }

        binding.recView.scrollToPosition(selectedUserList.size - 1)
//        createTaskVM.onInviteUserClicked(selectedUserList)
        binding.recView.adapter?.notifyDataSetChanged()
    }

    /**
     * Override method of UserDirectoryAdapter when team is removed
     */
    override fun onRemove(chatDialog: QBChatDialog) {

        teamNames.remove(chatDialog.name)

        if (!chatDialog.occupants.isNullOrEmpty()) {
            for (id in chatDialog.occupants) {
                if (id != currentUser?.id) {
                    val qbUser = QbUsersDbManager.getUserById(id)
                    if (qbUser != null && !qbUser.customData.isNullOrBlank()) {
                        val tempUser = Utils.getUserFromQBUser(qbUser.customData)

                        for (user in selectedUserList) {
                            if (user.id == tempUser.id) {
                                selectedUserList.remove(user)
                                participantsIds.remove(user.id)
                                break
                            }
                        }

                        if (::qbDialogUserIds.isInitialized && qbChatDialog != null) {
                            if (qbDialogUserIds.contains(qbUser.id)) {
                                qbDialogUserIds.remove(tempUser.id)
                            }
                            if (qbDialogUsers.contains(qbUser)) {
                                qbDialogUsers.remove(qbUser)
                            }
                        }
                    }
                }
            }
        }

        binding.recView.scrollToPosition(selectedUserList.size - 1)
//        createTaskVM.onInviteUserClicked(selectedUserList)
        binding.recView.adapter?.notifyDataSetChanged()

    }

}