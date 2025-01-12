package com.example.sameteam.homeScreen.bottomNavigation.calendarModule.bottomSheet

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sameteam.R
import com.example.sameteam.authScreens.model.UserModel
import com.example.sameteam.databinding.BottomSheetInvitePeopleBinding
import com.example.sameteam.generated.callback.OnClickListener
import com.example.sameteam.helper.Constants
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.activity.CreateTaskActivity
import com.example.sameteam.homeScreen.bottomNavigation.chatModule.adapter.UserDirectoryAdapter
import com.example.sameteam.homeScreen.drawerNavigation.activity.AddEventActivity
import com.example.sameteam.quickBlox.QbDialogHolder
import com.example.sameteam.quickBlox.chat.ChatHelper
import com.example.sameteam.quickBlox.util.SharedPrefsHelper
import com.example.sameteam.quickBlox.util.shortToast
import com.example.sameteam.widget.ConfirmDialog
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.crashlytics.internal.model.CrashlyticsReport.Session.User
import com.google.gson.Gson
import com.quickblox.chat.model.QBChatDialog
import com.quickblox.chat.model.QBDialogType
import com.quickblox.core.QBEntityCallback
import com.quickblox.core.exception.QBResponseException
import com.quickblox.core.request.QBRequestGetBuilder
import com.quickblox.users.model.QBUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class InvitePeopleBottomSheet(
    val activity: Context,
    val users: ArrayList<UserModel>,
    var selectedUsers: ArrayList<UserModel>
) : BottomSheetDialogFragment(), UserDirectoryAdapter.clickMyMethod {
    private val TAG = "InvitePeopleBottomSheet"

    //Implemented in CreateTaskActivity
    interface AllPeopleSelectedListener {
        fun onAllSelected(tag: Boolean)
    }

    lateinit var allPeopleSelectedListener: AllPeopleSelectedListener

    lateinit var binding: BottomSheetInvitePeopleBinding
    var usersData = ArrayList<UserModel>()
    var filteredUsersData = ArrayList<UserModel>()
    var searchList = ArrayList<UserModel>()
    var isSearch = false
    var hasMoreDialogs = true

    private var currentUser = QBUser()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding =
            DataBindingUtil.inflate(inflater, R.layout.bottom_sheet_invite_people, container, false)

        if (ChatHelper.getCurrentUser() != null) {
            currentUser = ChatHelper.getCurrentUser()!!
        } else {
            Log.e(TAG, "Finishing $TAG. Current user is null")
        }
        /**
         * This will expand the bottom sheet up to maximum height needed for bottom sheet,
         * if height is more then scroll will be there
         */
        dialog?.setOnShowListener { dialog ->
            val d = dialog as BottomSheetDialog
            val bottomSheet: View? =
                d.findViewById(com.google.android.material.R.id.design_bottom_sheet)
            val sheetBehavior = BottomSheetBehavior.from(bottomSheet!!)
            sheetBehavior.peekHeight = bottomSheet.height
        }

        usersData = users
        filteredUsersData = usersData.filterParticipants() as ArrayList<UserModel>
        Log.i("    " + usersData.size.toString(), "User List")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.userRecView.adapter =
            UserDirectoryAdapter(
                activity,
                filteredUsersData,
                selectedUsers,
                ArrayList()
            )

        binding.userRecView.adapter.let {
            it
        }

        UserDirectoryAdapter.setListener(this@InvitePeopleBottomSheet)

        /* fun UserDirectoryAdapter.setListener(l: () -> Unit) =setListener(object :
             UserDirectoryAdapter.clickMyMethod
         {
             override fun onInviteClickMyMethod(participant: UserModel) {
                Log.e("06/10 dsfdsf --=-=>","fdfds")
             }

             override fun onRemoveClickMyMethod(participant: UserModel) {
                 Log.e("06/10 participant --=-=>","participant")
             }
         })*/

        (binding.userRecView.adapter as UserDirectoryAdapter).notifyDataSetChanged()

        binding.txtUserNotFound.visibility =
            if (filteredUsersData.isEmpty()) View.VISIBLE else View.GONE

        binding.scrollLayout.visibility = View.INVISIBLE

        binding.txtInputSearch.txtSearch.isEnabled = true

        binding.btnBack.setOnClickListener {
            dismiss()
        }

        binding.btnInfo.setOnClickListener {
            val confirmDialog = ConfirmDialog(
                requireActivity(),
                " Create a team to share tasks with in the Communications menu.", "AlreadyCompleted"
            )
            confirmDialog.show(requireActivity().supportFragmentManager, "Confirm")
        }

        if (ChatHelper.isLogged()) {
            checkPlayServicesAvailable()
            loadDialogsFromQb(false, false)
        } else {
            reloginToChat()
        }

        /**
         * Invite all users button
         */
        binding.btnSelectAll.setOnClickListener {
            allPeopleSelectedListener = activity as AllPeopleSelectedListener

            if (selectedUsers.size == usersData.size)
                allPeopleSelectedListener.onAllSelected(false)
            else
                allPeopleSelectedListener.onAllSelected(true)
        }

        binding.btnDone.setOnClickListener {
            dismiss()
        }

        /**
         * Observe changes made by UserDirectoryAdapter in inviting users
         */

        if (context is AddEventActivity)
            (activity as AddEventActivity).addEventVM.observeSelectedUsers()
        else
            (activity as CreateTaskActivity).createTaskVM.observeSelectedUsers()

                .observe(this) { event ->
                    event?.getContentIfNotHandled()?.let {
                        selectedUsers = it
                        if (isSearch)
                            binding.userRecView.adapter = UserDirectoryAdapter(
                                activity,
                                searchList,
                                selectedUsers,
                                ArrayList()
                            )
                        else
                            binding.userRecView.adapter = UserDirectoryAdapter(
                                activity,
                                filteredUsersData,
                                selectedUsers,
                                ArrayList()
                            )

                        (binding.userRecView.adapter as UserDirectoryAdapter).notifyDataSetChanged()
                    }
                }

        /**
         * Edit text change listener for search functionality
         */
        binding.txtInputSearch.txtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (binding.txtInputSearch.txtSearch.text.toString()
                        .isBlank()
                ) {
                    // if search text is empty, show all users
                    isSearch = false

                    binding.userRecView.adapter =
                        UserDirectoryAdapter(
                            activity,
                            filteredUsersData,
                            selectedUsers,
                            ArrayList()
                        )

                    (binding.userRecView.adapter as UserDirectoryAdapter).notifyDataSetChanged()

                    binding.teamLayout.visibility = View.VISIBLE

                    binding.progressBar.visibility = View.GONE

                    if (filteredUsersData.isEmpty()) {

                        binding.txtUserNotFound.text = getString(R.string.search_with_email)
                        binding.individualsLayout.visibility = View.GONE
                        binding.txtUserNotFound.visibility = View.VISIBLE

                    } else {

                        binding.individualsLayout.visibility = View.VISIBLE
                        binding.txtUserNotFound.visibility = View.GONE

                    }

                } else {
                    isSearch = true
                    binding.teamLayout.visibility = View.GONE
                    val string = binding.txtInputSearch.txtSearch.text.toString()

                    searchList =
                        if (Patterns.EMAIL_ADDRESS.matcher(string).matches())
                            ArrayList(usersData.filter {
                                it.email.toString().contains(string, ignoreCase = true)
                            })
                        else
                            ArrayList(filteredUsersData.filter {

                                "${it.first_name} ${it.last_name}".contains(
                                    string,
                                    ignoreCase = true
                                )
                                        && it.participant_user == true

                            })

                    binding.userRecView.adapter =
                        UserDirectoryAdapter(
                            activity,
                            searchList,
                            selectedUsers,
                            ArrayList()
                        )

                    (binding.userRecView.adapter as UserDirectoryAdapter).notifyDataSetChanged()

                    if (searchList.isEmpty()) {

                        binding.individualsLayout.visibility = View.GONE
                        binding.txtUserNotFound.visibility = View.VISIBLE

                    } else {

                        binding.individualsLayout.visibility = View.VISIBLE
                        binding.txtUserNotFound.visibility = View.GONE

                    }
                    binding.txtUserNotFound.text =
                        if (searchList.isEmpty()) getString(R.string.participant_not_found) else getString(
                            R.string.search_with_email
                        )

//                    binding.txtUserNotFound.visibility =
//                        if (searchList.isEmpty()) View.VISIBLE else View.GONE

                    binding.progressBar.visibility = View.GONE

                }
            }

        })

        binding.txtInputSearch.txtSearch.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH && v.text.isNotBlank()) {

                /**
                 * Search if firstname or lastname contains matching search string
                 */

                isSearch = true
                binding.teamLayout.visibility = View.GONE
                val string = binding.txtInputSearch.txtSearch.text.toString()

                searchList =
                    if (Patterns.EMAIL_ADDRESS.matcher(string).matches())
                        ArrayList(usersData.filter {
                            it.email.toString().contains(string, ignoreCase = true)
                        })
                    else
                        ArrayList(filteredUsersData.filter {

                            "${it.first_name} ${it.last_name}".contains(
                                string,
                                ignoreCase = true
                            )
                                    && it.participant_user == true

                        })

                binding.userRecView.adapter =
                    UserDirectoryAdapter(
                        activity,
                        searchList,
                        selectedUsers,
                        ArrayList()
                    )

                (binding.userRecView.adapter as UserDirectoryAdapter).notifyDataSetChanged()


                if (searchList.isEmpty()) {

                    binding.individualsLayout.visibility = View.GONE
                    binding.txtUserNotFound.visibility = View.VISIBLE

                } else {

                    binding.individualsLayout.visibility = View.VISIBLE
                    binding.txtUserNotFound.visibility = View.GONE

                }

                binding.txtUserNotFound.text =
                    if (searchList.isEmpty()) getString(R.string.participant_not_found) else getString(
                        R.string.search_with_email
                    )

                binding.txtUserNotFound.visibility =
                    if (searchList.isEmpty()) View.VISIBLE else View.GONE

                binding.progressBar.visibility = View.GONE

                true
            } else false
        })
    }

    private fun checkPlayServicesAvailable() {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(activity)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(
                    requireActivity(),
                    resultCode,
                    Constants.PLAY_SERVICES_REQUEST_CODE
                )?.show()
            } else {
                shortToast("This device is not supported")
            }
        }
    }

    private fun reloginToChat() {
        binding.progressBar.visibility = View.VISIBLE
        if (SharedPrefsHelper.hasQbUser()) {
            ChatHelper.loginToChat(
                SharedPrefsHelper.getQbUser()!!,
                object : QBEntityCallback<Void> {
                    override fun onSuccess(aVoid: Void?, bundle: Bundle?) {
                        checkPlayServicesAvailable()
                        loadDialogsFromQb(false, false)
                    }

                    override fun onError(e: QBResponseException) {
                        binding.progressBar.visibility = View.GONE
                        shortToast(R.string.reconnect_failed)
                    }
                })
        }
    }

    private fun loadDialogsFromQb(silentUpdate: Boolean, clearDialogHolder: Boolean) {
        binding.progressBar.visibility = View.VISIBLE


        val requestBuilder = QBRequestGetBuilder()
        requestBuilder.limit = 100

//        searchMap.clear()

        ChatHelper.getDialogs(requestBuilder, object : QBEntityCallback<ArrayList<QBChatDialog>> {
            override fun onSuccess(dialogs: ArrayList<QBChatDialog>, bundle: Bundle?) {
                if (dialogs.size < 100) {
                    hasMoreDialogs = false
                }
                if (clearDialogHolder) {
                    QbDialogHolder.clear()
                    hasMoreDialogs = true
                }

                val groupDialogs =
                    dialogs.filter { it.type != QBDialogType.PRIVATE && it.userId == currentUser.id }

                Log.d(TAG, "onSuccess: ${groupDialogs.size}")

                updateDialogsAdapter(groupDialogs)

//
//                for(dialog in dialogs){
//                    if(dialog.type != QBDialogType.PRIVATE){
//                        searchMap[dialog.name] = dialog
//                    }
//                    else{
//                        val newOccupants = dialog.occupants
//                        newOccupants.remove(currentQBUser?.id)
//                        if(!newOccupants.isNullOrEmpty()) {
//                            val qbUser = QbUsersDbManager.getUserById(newOccupants[0])
//                            searchMap[qbUser?.fullName.toString()] = dialog
//                        }
//                    }
//                }

//                val joinerTask = DialogJoinerAsyncTask(requireActivity(), dialogs)
//                joinerTasksSet.add(joinerTask)
//                joinerTask.execute()

//                lifecycleScope.executeAsyncTask(
//                    onPreExecute = {
//
//                    },
//                    onPostExecute = {
//
//                    },
//                    doInBackground = {
//                        ChatHelper.join(dialogs)
//                    }
//                )

                if (hasMoreDialogs) {
                    loadDialogsFromQb(true, false)
                }

                binding.progressBar.visibility = View.GONE
            }

            override fun onError(e: QBResponseException) {
                e.message?.let { shortToast(it) }
                binding.progressBar.visibility = View.GONE
                binding.scrollLayout.visibility = View.VISIBLE

            }
        })

    }

    fun updateDialogsAdapter(dialogs: List<QBChatDialog>) {
        binding.teamRecView.layoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        binding.teamRecView.adapter =
            UserDirectoryAdapter(activity, ArrayList(), ArrayList(), ArrayList(dialogs))
        binding.scrollLayout.visibility = View.VISIBLE


    }

    fun <R> CoroutineScope.executeAsyncTask(
        onPreExecute: () -> Unit,
        doInBackground: () -> R,
        onPostExecute: (R) -> Unit
    ) = launch {
        onPreExecute() // runs in Main Thread
        val result = withContext(Dispatchers.IO) {
            doInBackground() // runs in background thread without blocking the Main Thread
        }
        onPostExecute(result) // runs in Main Thread
    }

    private fun ArrayList<UserModel>.filterParticipants() = filter { it.participant_user == true }

    override fun onInviteChatDialogClickMyMethod(participant: QBChatDialog) {
        Log.e("06/10 onInviteChat", "onInviteChatDialogClickMyMethod")
        Log.e("06/10 participant --=-=>", Gson().toJson(participant))


        binding.userRecView.adapter =
            UserDirectoryAdapter(
                activity,
                filteredUsersData,
                selectedUsers,
                ArrayList()
            )

        (binding.userRecView.adapter as UserDirectoryAdapter).notifyDataSetChanged()

    }

    override fun onRemoveChatDialogClickMyMethod(participant: QBChatDialog) {
        Log.e("06/10 onInviteChat", "onInviteChatDialogClickMyMethod")
        Log.e("06/10 participant --=-=>", Gson().toJson(participant))

        binding.userRecView.adapter =
            UserDirectoryAdapter(
                activity,
                filteredUsersData,
                selectedUsers,
                ArrayList()
            )

        (binding.userRecView.adapter as UserDirectoryAdapter).notifyDataSetChanged()
    }


}


