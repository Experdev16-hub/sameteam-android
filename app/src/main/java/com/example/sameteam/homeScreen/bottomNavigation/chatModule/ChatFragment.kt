package com.example.sameteam.homeScreen.bottomNavigation.chatModule

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.os.Build 
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.sameteam.MyApplication
import com.example.sameteam.R
import com.example.sameteam.authScreens.LoginActivity
import com.example.sameteam.authScreens.model.LoginResponseModel
import com.example.sameteam.base.BaseFragment
import com.example.sameteam.databinding.FragmentChatBinding
import com.example.sameteam.helper.Constants
import com.example.sameteam.helper.Constants.ACTION_NEW_FCM_EVENT
import com.example.sameteam.helper.Constants.EXTRA_FCM_MESSAGE
import com.example.sameteam.helper.Constants.EXTRA_IS_INCOMING_CALL
import com.example.sameteam.helper.Constants.PLAY_SERVICES_REQUEST_CODE
import com.example.sameteam.helper.SharedPrefs
import com.example.sameteam.homeScreen.HomeActivity
import com.example.sameteam.homeScreen.bottomNavigation.chatModule.activity.CallActivity
import com.example.sameteam.homeScreen.bottomNavigation.chatModule.activity.UserDirectoryActivity
import com.example.sameteam.homeScreen.bottomNavigation.chatModule.adapter.ChatListAdapter
import com.example.sameteam.homeScreen.bottomNavigation.chatModule.adapter.UserDirectoryAdapter
import com.example.sameteam.homeScreen.bottomNavigation.chatModule.viewModel.ChatFragVM
import com.example.sameteam.quickBlox.QbChatDialogMessageListenerImpl
import com.example.sameteam.quickBlox.QbDialogHolder
import com.example.sameteam.quickBlox.VerboseQbChatConnectionListener
import com.example.sameteam.quickBlox.chat.ChatHelper
import com.example.sameteam.quickBlox.db.QbUsersDbManager
import com.example.sameteam.quickBlox.managers.DialogsManager
import com.example.sameteam.quickBlox.service.CallService
import com.example.sameteam.quickBlox.showSnackbar
import com.example.sameteam.quickBlox.util.SharedPrefsHelper
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.quickblox.chat.QBChatService
import com.quickblox.chat.QBIncomingMessagesManager
import com.quickblox.chat.QBRoster
import com.quickblox.chat.QBSystemMessagesManager
import com.quickblox.chat.exception.QBChatException
import com.quickblox.chat.listeners.QBChatDialogMessageListener
import com.quickblox.chat.listeners.QBRosterListener
import com.quickblox.chat.listeners.QBSubscriptionListener
import com.quickblox.chat.listeners.QBSystemMessageListener
import com.quickblox.chat.model.QBChatDialog
import com.quickblox.chat.model.QBChatMessage
import com.quickblox.chat.model.QBDialogType
import com.quickblox.chat.model.QBPresence
import com.quickblox.core.QBEntityCallback
import com.quickblox.core.exception.QBResponseException
import com.quickblox.core.request.QBRequestGetBuilder
import com.quickblox.messages.services.QBPushManager
import com.quickblox.messages.services.SubscribeService
import com.quickblox.users.model.QBUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jivesoftware.smack.ConnectionListener
import org.jivesoftware.smack.SmackException
import org.jivesoftware.smack.XMPPException
import com.example.sameteam.helper.Constants.EMPTY_FCM_MESSAGE
import java.lang.IllegalArgumentException


class ChatFragment : BaseFragment<FragmentChatBinding>(), DialogsManager.ManagingDialogsCallbacks {

    private val TAG = "ChatFragment"
    private var isDialogsLoading = false
    private var pendingLoadRequest = false
    
    override fun layoutID() = R.layout.fragment_chat

    override fun viewModel() = ViewModelProvider(
        this,
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    ).get(ChatFragVM::class.java)

    lateinit var chatFragmentVM: ChatFragVM
    lateinit var binding: FragmentChatBinding

    lateinit var chatListAdapter: ChatListAdapter
    var hasMoreDialogs = true
//    private val joinerTasksSet = HashSet<DialogJoinerAsyncTask>()

    lateinit var currentUser: LoginResponseModel.User
    private var currentQBUser: QBUser? = null

    lateinit var contactsRoster: QBRoster
    private var allDialogsMessagesListener: QBChatDialogMessageListener =
        AllDialogsMessageListener()
    private var systemMessagesListener: SystemMessagesListener = SystemMessagesListener()
    private lateinit var systemMessagesManager: QBSystemMessagesManager
    private lateinit var incomingMessagesManager: QBIncomingMessagesManager
    private lateinit var chatConnectionListener: ConnectionListener
    private lateinit var pushBroadcastReceiver: BroadcastReceiver
    private var dialogsManager: DialogsManager = DialogsManager()

    var searchMap = HashMap<String, QBChatDialog>()
    var firstSearch = true
    lateinit var receiver: MyReceiver

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
override fun onResume() {
    super.onResume()

    chatFragmentVM.callMyProfile()

    if (QBChatService.getInstance().roster != null) {
        contactsRoster = QBChatService.getInstance().roster
        contactsRoster.subscriptionMode = QBRoster.SubscriptionMode.mutual
        contactsRoster.addRosterListener(rosterListener)
        contactsRoster.addSubscriptionListener(subscriptionListener)
    }

    if (ChatHelper.isLogged()) {
        checkPlayServicesAvailable()
        registerQbChatListeners()
        if (QbDialogHolder.dialogsMap.isNotEmpty()) {
            loadDialogsFromQb(true, true)
        } else {
            loadDialogsFromQb(false, true)
        }
    } else {
        reloginToChat()
    }

    if(SharedPrefsHelper.hasQbUser())
        currentQBUser = SharedPrefsHelper.getQbUser()

    SharedPrefs.saveGroupName(MyApplication.getInstance(),"")
    pushBroadcastReceiver = PushBroadcastReceiver()
    val intentFilter  = IntentFilter()
    intentFilter.addAction(ACTION_NEW_FCM_EVENT)
    intentFilter.addAction(EXTRA_FCM_MESSAGE)
    intentFilter.addAction(EMPTY_FCM_MESSAGE)
    activity?.registerReceiver(
        pushBroadcastReceiver,
        intentFilter, RECEIVER_EXPORTED,
    )
}


    override fun initFragment(mBinding: ViewDataBinding) {
        chatFragmentVM = getViewModel() as ChatFragVM
        binding = mBinding as FragmentChatBinding

        receiver = MyReceiver()
        val intentFilter  = IntentFilter()
        intentFilter.addAction("chatShow")
        intentFilter.addAction("chatHide")
        intentFilter.addAction(ACTION_NEW_FCM_EVENT)
        intentFilter.addAction(EXTRA_FCM_MESSAGE)
        intentFilter.addAction(EMPTY_FCM_MESSAGE)
        context?.registerReceiver(receiver, intentFilter, RECEIVER_EXPORTED)

        val dialogs = ArrayList(QbDialogHolder.dialogsMap.values)
        if (isAdded) {
            chatListAdapter = ChatListAdapter(requireActivity(), dialogs)
            binding.recView.adapter = chatListAdapter
        }

        if (!ChatHelper.isLogged()) {
            reloginToChat()
        }

        currentUser = SharedPrefs.getUser(MyApplication.getInstance())!!
        if(SharedPrefsHelper.hasQbUser())
            currentQBUser = SharedPrefsHelper.getQbUser()

        initConnectionListener()


        binding.btnFAB.setOnClickListener {
            startActivity(UserDirectoryActivity::class.java)
        }

//        showMessage("Work In Progress")

        /**
         * General Observer
         */
        chatFragmentVM.observedChanges().observe(this) { event ->
            event?.getContentIfNotHandled()?.let {
                if (Constants.FORCE_LOGOUT == it) {
                    unsubscribeFromPushes()
                }
            }
        }

//
//        (activity as HomeActivity).homeVM.observeRightIcon().observe(this, Observer { event ->
//            event?.getContentIfNotHandled()?.let {
//               if(it == "hide" && !firstSearch){
//                   binding.searchLayout.visibility = View.GONE
//                   firstSearch = true
//               }
//                else{
//                    binding.searchLayout.visibility = View.VISIBLE
//                    firstSearch = false
//               }
//            }
//        })

        /**
         * Edit text change listener for search functionality
         */
        binding.txtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.progressBar.visibility = View.VISIBLE
            }

            override fun afterTextChanged(s: Editable?) {

                Log.d(TAG, "afterTextChanged: ${searchMap.toString()}")


                if (binding.txtSearch.text.toString().isBlank()) {  // if search text is empty, show all users
                    loadDialogsFromQb(false, false)
                } else {

                    /**
                     * Search if firstname or lastname contains matching search string OR email matches search string
                     */
                    val string = binding.txtSearch.text.toString()
                    val nameList = searchMap.keys
                    val matchList = ArrayList(nameList.filter { it.contains(string, ignoreCase = true)})

                    val newDialogs = ArrayList<QBChatDialog>()

                    for(value in matchList){
                        val temp = searchMap[value]
                        if(temp != null)
                            newDialogs.add(temp)
                    }

                    chatListAdapter.updateList(newDialogs.toList())
                    binding.progressBar.visibility = View.GONE
                }
            }

        })
    }

    inner class MyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.e("Intent", "Action ${intent?.action}")
            lifecycleScope.launch {
                if (intent?.action.equals("chatShow")) {
                    binding.searchLayout.visibility = View.VISIBLE
                }
                else if(intent?.action.equals("chatHide") ){
                    binding.searchLayout.visibility = View.GONE
                }
                if (intent?.action.equals(EXTRA_FCM_MESSAGE)) {
                    loadDialogsFromQb(false, false)
                }
                else if (intent?.action.equals(ACTION_NEW_FCM_EVENT)) {
                    loadDialogsFromQb(false, false)
                }
            }
        }
    }


    override fun onPause() {
    super.onPause()
    ChatHelper.removeConnectionListener(chatConnectionListener)
    
    isDialogsLoading = false
    pendingLoadRequest = false

    try {
        activity?.unregisterReceiver(pushBroadcastReceiver)
    } catch (e: IllegalArgumentException) {
        Log.e(TAG, "Push receiver not registered: ${e.message}")
    }


    
    fun onStop() {
        super.onStop()
        unregisterQbChatListeners()
    }

    fun onDestroy() {
        super.onDestroy()
        unregisterQbChatListeners()
        context?.unregisterReceiver(receiver)
    }

    private fun reloginToChat() {
        binding.progressBar.visibility = View.VISIBLE
        if (SharedPrefsHelper.hasQbUser()) {
            ChatHelper.loginToChat(
                SharedPrefsHelper.getQbUser()!!,
                object : QBEntityCallback<Void> {
                    override fun onSuccess(aVoid: Void?, bundle: Bundle?) {
                        Log.d(TAG, "Relogin Successful")
                        checkPlayServicesAvailable()
                        registerQbChatListeners()
                        loadDialogsFromQb(false, false)
                    }

                    override fun onError(e: QBResponseException) {
                        Log.d(TAG, "Relogin Failed " + e.message)
                        binding.progressBar.visibility = View.GONE
                        shortToast(getString(R.string.reconnect_failed))
                    }
                })
        }
    }

    private fun checkPlayServicesAvailable() {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(requireContext())
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(
                    requireActivity(),
                    resultCode,
                    PLAY_SERVICES_REQUEST_CODE
                )?.show()
            } else {
                Log.e(TAG, "This device is not supported.")
                shortToast("This device is not supported")
            }
        }
    }

    private fun isCallServiceRunning(serviceClass: Class<*>): Boolean {
        val manager =
            requireActivity().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val services = manager.getRunningServices(Integer.MAX_VALUE)
        for (service in services) {
            if (CallService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun clearAppNotifications() {
        val notificationManager =
            requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }


private fun loadDialogsFromQb(silentUpdate: Boolean, clearDialogHolder: Boolean) {
    if (isDialogsLoading) {
        pendingLoadRequest = true
        return
    }
    
    isDialogsLoading = true
    binding.recView.visibility = View.GONE
    binding.noDataLayout.visibility = View.GONE
    binding.progressBar.visibility = View.VISIBLE

    val requestBuilder = QBRequestGetBuilder()
    requestBuilder.limit = 100

    searchMap.clear()

    ChatHelper.getDialogs(requestBuilder, object : QBEntityCallback<ArrayList<QBChatDialog>> {
        override fun onSuccess(dialogs: ArrayList<QBChatDialog>, bundle: Bundle?) {
            isDialogsLoading = false
            
            if (pendingLoadRequest) {
                pendingLoadRequest = false
                Handler(android.os.Looper.getMainLooper()).postDelayed({
                    loadDialogsFromQb(false, true)
                }, 300)
                return
            }
            
            if (dialogs.size < 100) {
                hasMoreDialogs = false
            }
            if (clearDialogHolder) {
                QbDialogHolder.clear()
                hasMoreDialogs = true
            }
            
            QbDialogHolder.addDialogs(dialogs)
            
            for(dialog in dialogs){
                if(dialog.type != QBDialogType.PRIVATE){
                    searchMap[dialog.name] = dialog
                } else {
                    val newOccupants = ArrayList(dialog.occupants)
                    newOccupants.remove(currentQBUser?.id)
                    if(!newOccupants.isNullOrEmpty()) {
                        val qbUser = QbUsersDbManager.getUserById(newOccupants[0])
                        if (qbUser?.fullName != null) {
                            searchMap[qbUser.fullName] = dialog
                        }
                    }
                }
            }

            updateDialogsAdapter()

            binding.recView.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE

            if(dialogs.isEmpty()){
                binding.recViewLayout.visibility = View.GONE
                binding.noDataLayout.visibility = View.VISIBLE
            } else {
                binding.recViewLayout.visibility = View.VISIBLE
                binding.noDataLayout.visibility = View.GONE
            }
        }


        override fun onError(e: QBResponseException) {
            isDialogsLoading = false
            pendingLoadRequest = false
            
            e.message?.let { shortToast(it) }
            binding.recView.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
            
            if (QbDialogHolder.dialogsMap.isEmpty()) {
                binding.recViewLayout.visibility = View.GONE
                binding.noDataLayout.visibility = View.VISIBLE
            }
        }
    }

    private fun initConnectionListener() {
        val rootView: View = binding.recView
        chatConnectionListener = object : VerboseQbChatConnectionListener(rootView) {
            override fun reconnectionSuccessful() {
                super.reconnectionSuccessful()
                loadDialogsFromQb(false, true)
            }
        }
    }




    private fun updateDialogsAdapter() {
    if (!isAdded || isRemoving) {
        return
    }
    
    try {
        val listDialogs = ArrayList(QbDialogHolder.dialogsMap.values)
        chatListAdapter.updateList(listDialogs)
    } catch (e: Exception) {
        Log.e(TAG, "Error updating dialog adapter: ${e.message}")
    }
    }
    fun shortToast(msg: String) {
        Toast.makeText(MyApplication.getInstance(), msg, Toast.LENGTH_SHORT).show()
    }


//    private class DialogJoinerAsyncTask(
//        activity: Activity,
//        private val dialogs: ArrayList<QBChatDialog>
//    ) : BaseAsyncTask<Void, Void, Void>() {
////        private val activityRef: WeakReference<TestActivity> = WeakReference(activity)
//
//        @Throws(Exception::class)
//        override fun performInBackground(vararg params: Void): Void? {
//            if (!isCancelled) {
//                ChatHelper.join(dialogs)
//            }
//            return null
//        }
//
//        override fun onResult(result: Void?) {
//
//        }
//
//        override fun onException(e: Exception) {
//            super.onException(e)
//            if (!isCancelled) {
//                Log.d("Dialog Joiner Task", "Error: $e")
//            }
//        }
//
//        override fun onCancelled() {
//            super.onCancelled()
//            cancel(true)
//        }
//    }


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

    private fun registerQbChatListeners() {
        ChatHelper.addConnectionListener(chatConnectionListener)
        try {
            systemMessagesManager = QBChatService.getInstance().systemMessagesManager
            incomingMessagesManager = QBChatService.getInstance().incomingMessagesManager
        } catch (e: Exception) {
            Log.d(TAG, "Can not get SystemMessagesManager. Need relogin. " + e.message)
            reloginToChat()
            return
        }

        if (incomingMessagesManager == null) {
            reloginToChat()
            return
        }
        systemMessagesManager.addSystemMessageListener(systemMessagesListener)
        incomingMessagesManager.addDialogMessageListener(allDialogsMessagesListener)
        dialogsManager.addManagingDialogsCallbackListener(this)

        pushBroadcastReceiver = PushBroadcastReceiver()
        val intentFilter  = IntentFilter()
        intentFilter.addAction(ACTION_NEW_FCM_EVENT)
        intentFilter.addAction(EXTRA_FCM_MESSAGE)
        intentFilter.addAction(EMPTY_FCM_MESSAGE)
        activity?.registerReceiver(
            pushBroadcastReceiver,
            intentFilter, RECEIVER_EXPORTED
        )
    }

    private fun unregisterQbChatListeners() {
        if (::incomingMessagesManager.isInitialized)
            incomingMessagesManager.removeDialogMessageListrener(allDialogsMessagesListener)

        if (::systemMessagesManager.isInitialized)
            systemMessagesManager.removeSystemMessageListener(systemMessagesListener)

        dialogsManager.removeManagingDialogsCallbackListener(this)
    }


    private inner class AllDialogsMessageListener : QbChatDialogMessageListenerImpl() {
        override fun processMessage(
            dialogID: String,
            qbChatMessage: QBChatMessage,
            senderID: Int?
        ) {
            Log.d(TAG, "Processing received Message: " + qbChatMessage.body)
            if (senderID != currentQBUser?.id) {
                dialogsManager.onGlobalMessageReceived(dialogID, qbChatMessage)
            }
        }
    }

    private inner class SystemMessagesListener : QBSystemMessageListener {
        override fun processMessage(qbChatMessage: QBChatMessage) {
            Log.d(TAG, "processMessage: ")
            dialogsManager.onSystemMessageReceived(qbChatMessage)
        }

        override fun processError(e: QBChatException, qbChatMessage: QBChatMessage) {

        }
    }

    private inner class PushBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Get extra data included in the Intent
            val message = intent.getStringExtra(EXTRA_FCM_MESSAGE)
            Log.v(TAG, "Received broadcast " + intent.action + " with data: " + message)
            if (intent.action.equals(EXTRA_FCM_MESSAGE)) {
                loadDialogsFromQb(false, false)
            }
            else if (intent.action.equals(ACTION_NEW_FCM_EVENT)) {
                loadDialogsFromQb(false, false)
            }
        }
    }


    private val rosterListener: QBRosterListener = object : QBRosterListener {
        override fun entriesDeleted(userIds: Collection<Int>) {

        }

        override fun entriesAdded(userIds: Collection<Int>) {
            Log.d(TAG, "entriesAdded: ${userIds.size}")
        }

        override fun entriesUpdated(userIds: Collection<Int>) {
            Log.d(TAG, "entriesUpdated: ${userIds.size}")
        }

        override fun presenceChanged(presence: QBPresence) {
            Log.d(TAG, "presenceChanged: Called")
            // if a user uses several devices, you need to do additional check for presence
            val qbPresence = contactsRoster.getPresence(presence.userId)

            if (qbPresence.type == QBPresence.Type.online) {
                Log.d(TAG, "presenceChanged: Online")
                SharedPrefs.saveOnlineUserId(MyApplication.getInstance(), qbPresence.userId)
                chatListAdapter.notifyDataSetChanged()
            } else {
                Log.d(TAG, "presenceChanged: Offline")
                SharedPrefs.removeOnlineUserId(MyApplication.getInstance(), qbPresence.userId)
                chatListAdapter.notifyDataSetChanged()
            }
        }
    }

    var subscriptionListener = QBSubscriptionListener {
        try {
            contactsRoster.confirmSubscription(it)
        } catch (e: SmackException.NotConnectedException) {
            Log.d(TAG, "Error : ${e.message}")
        } catch (e: SmackException.NotLoggedInException) {
            Log.d(TAG, "Error : ${e.message}")

        } catch (e: SmackException.NoResponseException) {
            Log.d(TAG, "Error : ${e.message}")

        } catch (e: XMPPException) {
            Log.d(TAG, "Error : ${e.message}")
        }
    }

    private fun unsubscribeFromPushes() {
        if (QBPushManager.getInstance().isSubscribedToPushes) {
            QBPushManager.getInstance().addListener(object : QBPushManager.QBSubscribeListener {
                override fun onSubscriptionCreated() {

                }

                override fun onSubscriptionError(e: Exception?, i: Int) {
                    Log.d("Subscription", "SubscriptionError" + e?.message)

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
        startActivity(LoginActivity::class.java)
        requireActivity().finish()
    }

    override fun onDialogCreated(chatDialog: QBChatDialog) {
        loadDialogsFromQb(true, false)
    }

    override fun onDialogUpdated(chatDialog: String) {
        updateDialogsAdapter()

    }

    override fun onNewDialogLoaded(chatDialog: QBChatDialog) {
        updateDialogsAdapter()

    }}
}
