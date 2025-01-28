package com.example.sameteam.homeScreen

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.example.sameteam.MyApplication
import com.example.sameteam.R
import com.example.sameteam.authScreens.model.LoginResponseModel
import com.example.sameteam.base.BaseActivity
import com.example.sameteam.databinding.ActivityHomeBinding
import com.example.sameteam.helper.Constants
import com.example.sameteam.helper.Constants.ACTION_NEW_FCM_EVENT
import com.example.sameteam.helper.Constants.COMPLETE_TASK
import com.example.sameteam.helper.Constants.DELETE_TASK
import com.example.sameteam.helper.Constants.EMPTY_FCM_MESSAGE
import com.example.sameteam.helper.Constants.EXTRA_FCM_MESSAGE
import com.example.sameteam.helper.Constants.MI_OVERLAY_PERMISSION_CHECKED_KEY
import com.example.sameteam.helper.Constants.NAVIGATE
import com.example.sameteam.helper.Constants.PLAN_STATUS
import com.example.sameteam.helper.Constants.PLAY_SERVICES_REQUEST_CODE
import com.example.sameteam.helper.Constants.SHOW_CHAT_BADGE
import com.example.sameteam.helper.Constants.SHOW_NOTIFICATION_BADGE
import com.example.sameteam.helper.SharedPrefs
import com.example.sameteam.helper.Utils
import com.example.sameteam.helper.Utils.loadBannerAd
import com.example.sameteam.homeScreen.bottomNavigation.chatModule.activity.UserDirectoryActivity
import com.example.sameteam.homeScreen.drawerNavigation.activity.MyEventsActivity
import com.example.sameteam.homeScreen.drawerNavigation.activity.ProfileActivity
import com.example.sameteam.quickBlox.util.SharedPrefsHelper
import com.example.sameteam.quickBlox.util.longToast
import com.example.sameteam.widget.ConfirmDialog
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.quickblox.auth.session.QBSettings
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.*


class HomeActivity : BaseActivity<ActivityHomeBinding>(), ConfirmDialog.ConfirmClickListener {
    private val TAG = "HomeActivity"

    override fun layoutID() = R.layout.activity_home

    override fun viewModel() = ViewModelProvider(
        this,
        ViewModelProvider.AndroidViewModelFactory.getInstance(this.application)
    ).get(HomeVM::class.java)

    lateinit var homeVM: HomeVM
    lateinit var binding: ActivityHomeBinding
    lateinit var navController: NavController

    private val monthTitleFormatter = DateTimeFormatter.ofPattern("MMMM")
    lateinit var user: LoginResponseModel.User
    private var activityResultLauncher: ActivityResultLauncher<Intent>? = null
    lateinit var receiver: MyReceiver
    lateinit var chatBadgeView: View
    lateinit var notiBadgeView: View

    var showSearch = true
    lateinit var alertDialog: AlertDialog

    inner class MyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.e("Intent", "Action ${intent?.action}")
            lifecycleScope.launch {
                if (intent?.action.equals(SHOW_CHAT_BADGE)) {
                    if (::chatBadgeView.isInitialized) {
                        chatBadgeView.visibility = View.VISIBLE
                    }
                }
                if (intent?.action.equals(SHOW_NOTIFICATION_BADGE)) {
                    if (::notiBadgeView.isInitialized) {
                        notiBadgeView.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        user = SharedPrefs.getUser(MyApplication.getInstance())!!

        // Set drawer layout
        if (user.profile_picture.isNullOrBlank()) {
            Glide.with(this)
                .asDrawable()
                .load(R.drawable.profile_photo)
                .centerCrop()
                .into(binding.profileImage)
        } else {
            Glide.with(this)
                .load(user.profile_picture)
                .error(R.drawable.profile_photo)
                .placeholder(R.drawable.profile_photo)
                .centerCrop()
                .into(binding.profileImage)
        }

        binding.name.text = user.first_name + " " + user.last_name
        binding.position.text = user.title

        Log.d(TAG, "FCM: Token ${SharedPrefs.getFcmToken(MyApplication.getInstance())}")

        registerReceiver()

    }

    fun Context.showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun initActivity(mBinding: ViewDataBinding) {

        homeVM = getViewModel() as HomeVM
        binding = mBinding as ActivityHomeBinding

        this.showToast("Hello from Toast!")

        val local = SimpleDateFormat("Z", Locale.getDefault()).format(System.currentTimeMillis())
        Log.d(TAG, "initActivity Timezone: $local")

        binding.customToolbar.leftIcon.visibility = View.GONE
        user = SharedPrefs.getUser(MyApplication.getInstance())!!

        QBSettings.getInstance().isEnablePushNotification = true

        if (user.profile_picture.isNullOrBlank()) {
            Glide.with(this)
                .asDrawable()
                .load(R.drawable.profile_photo)
                .centerCrop()
                .into(binding.profileImage)
        } else {
            Glide.with(this)
                .load(user.profile_picture)
                .error(R.drawable.profile_photo)
                .placeholder(R.drawable.profile_photo)
                .centerCrop()
                .into(binding.profileImage)

        }
        binding.name.text = user.first_name + " " + user.last_name
        binding.position.text = user.title

        homeVM.observedChanges().observe(this) { event ->
            event?.getContentIfNotHandled()?.let {
                when (it) {
                    PLAN_STATUS -> {
                        binding.adView.loadBannerAd()

                        binding.upgradeAccount.visibility =
                            if (SharedPrefs.getUser(this)?.plan_upgrade == true)
                                View.GONE else View.VISIBLE
                    }
                }
            }
        }

        /**
         * Set up Drawer
         */
        val toggle: ActionBarDrawerToggle = object : ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.customToolbar.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        ) {
            override fun onDrawerClosed(drawerView: View) {
                // Triggered once the drawer closes
                super.onDrawerClosed(drawerView)
                try {
                    val inputMethodManager =
                        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
                } catch (e: Exception) {
                    e.stackTrace
                }
            }

            override fun onDrawerOpened(drawerView: View) {
                // Triggered once the drawer opens
                super.onDrawerOpened(drawerView)
                try {
                    val inputMethodManager =
                        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
                } catch (e: Exception) {
                    e.stackTrace
                }
            }
        }

        toggle.isDrawerIndicatorEnabled = false
        toggle.setHomeAsUpIndicator(R.drawable.ic_drawer_icon)
        toggle.setToolbarNavigationClickListener {
            if (binding.drawerLayout.isDrawerVisible(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                binding.drawerLayout.openDrawer(GravityCompat.START);
            }
        }

        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()


        /**
         * Setup BottomNavigation with navController
         */
        navController = findNavController(R.id.nav_host_fragment)
        binding.bottomNavView.setupWithNavController(navController)
        binding.bottomNavView.itemIconTintList = null

        /**
         * BadgeView for Chat icon bottomNavigation
         */
        val menuView = binding.bottomNavView.getChildAt(0) as? BottomNavigationMenuView
        val itemView = menuView?.getChildAt(1) as? BottomNavigationItemView
        chatBadgeView =
            LayoutInflater.from(this).inflate(R.layout.notification_badge, menuView, false)
        itemView?.addView(chatBadgeView)
        chatBadgeView.visibility = View.GONE

        /**
         * BadgeView for Chat icon bottomNavigation
         */
        val itemView1 = menuView?.getChildAt(2) as? BottomNavigationItemView
        notiBadgeView =
            LayoutInflater.from(this).inflate(R.layout.notification_badge, menuView, false)
        itemView1?.addView(notiBadgeView)
        notiBadgeView.visibility = View.GONE

        /**
         * MyBroadcastReceiver
         */
        receiver = MyReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction(SHOW_CHAT_BADGE)
        intentFilter.addAction(SHOW_NOTIFICATION_BADGE)
        registerReceiver(receiver, intentFilter, RECEIVER_EXPORTED)


        /**
         * Bottom Navigation
         */
        navController.addOnDestinationChangedListener { controller, destination, listener ->
            Log.d(TAG, "initActivity: Destination " + destination.id)

            when (destination.id) {
//                R.id.calendarFragment -> {
//                    binding.customToolbar.title.visibility = View.VISIBLE
//                    binding.customToolbar.title.text = getString(R.string.calendar)
//                    binding.customToolbar.rightIcon.visibility = View.VISIBLE
//                    binding.customToolbar.rightIcon.setImageResource(R.drawable.ic_user)
//                    binding.customToolbar.toolbar.elevation = 0F
//                }

                R.id.taskFragment -> {
                    binding.customToolbar.title.visibility = View.VISIBLE
                    binding.customToolbar.title.text = getString(R.string.team_calendar)
                    binding.customToolbar.rightIcon.visibility = View.VISIBLE
                    binding.customToolbar.rightIcon.setImageResource(R.drawable.ic_user)
                    binding.customToolbar.toolbar.elevation = 0F
                }

                R.id.chatFragment -> {
                    SharedPrefs.setChatBadgeAvailable(this@HomeActivity, false)
                    chatBadgeView.visibility = View.GONE

                    binding.customToolbar.title.visibility = View.VISIBLE
                    binding.customToolbar.title.text = getString(R.string.messages)
                    binding.customToolbar.rightIcon.visibility = View.VISIBLE
                    binding.customToolbar.rightIcon.setImageResource(R.drawable.ic_search)
                    binding.customToolbar.toolbar.elevation = 1F
//                    showMessage("In progress")
                }

                R.id.notificationFragment -> {
                    SharedPrefs.setNotificationBadgeAvailable(this@HomeActivity, false)

                    notiBadgeView.visibility = View.GONE

                    binding.customToolbar.title.visibility = View.VISIBLE
                    binding.customToolbar.title.text = getString(R.string.notification)
                    binding.customToolbar.rightIcon.visibility = View.INVISIBLE
                    binding.customToolbar.toolbar.elevation = 1F
                }

                R.id.userDirectoryFragment -> {
                    binding.customToolbar.title.visibility = View.VISIBLE
                    binding.customToolbar.title.text = getString(R.string.user_directory)
                    binding.customToolbar.rightIcon.visibility = View.VISIBLE
                    binding.customToolbar.rightIcon.setImageResource(R.drawable.ic_search)
                    binding.customToolbar.toolbar.elevation = 1F

                }
            }

        }


        /**
         * On Notification Click listeners, extract intents and navigate
         */
        val deletedGroup = intent.getBooleanExtra("deleted", false)
        val deletedTask = intent.getBooleanExtra("taskDeleted", false)
        if (deletedGroup)
            goToChatFragment()
        else if (deletedTask)
            goToTaskFragment()

        val navigateValue = intent.getStringExtra("Navigate")
        if (navigateValue == "Chat")
            goToChatFragment()
        else if (navigateValue == "Notification")
            goToNotificationFragment()

        binding.myEvents.setOnClickListener {
            startActivity(MyEventsActivity::class.java)
        }
        binding.activities.setOnClickListener {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            }
            navController.navigate(R.id.taskFragment)
        }
        binding.createTeam.setOnClickListener {
            startActivity(UserDirectoryActivity::class.java)
        }

        binding.myProfile.setOnClickListener {
            startActivity(ProfileActivity::class.java)
        }

        binding.upgradeAccount.setOnClickListener {
            val viewIntent = Intent(
                "android.intent.action.VIEW",
                Uri.parse("https://subadmin.sameteam.app/")
            )
            startActivity(viewIntent)
        }

        binding.termsAndConditions.setOnClickListener {
            val url = "https://sameteam.app/terms"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }

        binding.privacyPolicy.setOnClickListener {
            val url = "https://sameteam.app/privacy"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }
        binding.needHelp.setOnClickListener {
            val url = "https://sameteam.app/faq"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }

        /**
         * Dynamic toolbar Right Icon click listener based on current destination of fragment
         */
        binding.customToolbar.rightIcon.setOnClickListener {
            if (navController.currentDestination?.id == R.id.userDirectoryFragment) {
                if (showSearch) {
                    sendBroadcast(Intent("allUsersShow"))
                    showSearch = !showSearch
                } else {
                    sendBroadcast(Intent("allUsersHide"))
                    showSearch = !showSearch
                }
            } else if (navController.currentDestination?.id == R.id.chatFragment) {
                if (showSearch) {
                    sendBroadcast(Intent("chatShow"))
                    showSearch = !showSearch
                } else {
                    sendBroadcast(Intent("chatHide"))
                    showSearch = !showSearch
                }

            }
//            else if (navController.currentDestination?.id == R.id.taskFragment) {
//                if(showSearch) {
//                    sendBroadcast(Intent("taskShow"))
//                    showSearch = !showSearch
//                }
//                else{
//                    sendBroadcast(Intent("taskHide"))
//                    showSearch = !showSearch
//                }
//            }
            else if (navController.currentDestination?.id == R.id.taskFragment) {
                startActivity(ProfileActivity::class.java)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Dexter.withContext(this) // below line is use to request the number of permissions which are required in our app.
                .withPermissions(
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {

                        if (p0?.areAllPermissionsGranted() == false) {
                            val notificationPermission = Manifest.permission.POST_NOTIFICATIONS
                            val bluetoothPermission = Manifest.permission.BLUETOOTH_SCAN
                            val bluetoothPermission2 = Manifest.permission.BLUETOOTH_CONNECT

                            if (checkCallingOrSelfPermission(notificationPermission) != PackageManager.PERMISSION_GRANTED) {
                                showSettingDialog()
                            }

                            if (checkCallingOrSelfPermission(bluetoothPermission) != PackageManager.PERMISSION_GRANTED &&
                                checkCallingOrSelfPermission(bluetoothPermission2) != PackageManager.PERMISSION_GRANTED
                            ) {
                                showSettingDialog2()
                            }
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        p0: MutableList<PermissionRequest>?,
                        p1: PermissionToken?
                    ) {
                        p1?.continuePermissionRequest()
                    }


                }).check()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            Dexter.withContext(this)
                .withPermissions(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                        if (p0?.areAllPermissionsGranted() == false) {
                            showSettingDialog2()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        p0: MutableList<PermissionRequest>?,
                        p1: PermissionToken?
                    ) {
                        p1?.continuePermissionRequest()
                    }

                }).check()
        }

        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                Log.d(TAG, "initActivity: Activity Result passed")
                checkOverlayPermissions()
            }

        /**
         * Overlay Permission
         */
        checkOverlayPermissions()
    }


    private fun showSettingDialog() {
        if (user.notification_status == "on") {
            MaterialAlertDialogBuilder(this)
                .setIcon(R.drawable.ic_error_outline_gray_24dp)
                .setTitle("Notification Permission")
                .setMessage("Notification permission is required, Please allow notification permission from setting")
                .setPositiveButton("Ok") { _, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showSettingDialog2() {
        MaterialAlertDialogBuilder(this)
            .setIcon(R.drawable.ic_error_outline_gray_24dp)
            .setTitle("Bluetooth Permission Required")
            .setMessage("Please allow nearby device permission in App Permissions")
            .setPositiveButton("Ok") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { _, _ ->
                longToast("Application may not function properly.")
            }
            .show()
    }

    private fun goToChatFragment() {
        SharedPrefs.setChatBadgeAvailable(this@HomeActivity, false)
        chatBadgeView.visibility = View.GONE
        navController.navigate(R.id.chatFragment)
    }

    private fun goToTaskFragment() {
        navController.navigate(R.id.taskFragment)
    }

    private fun goToNotificationFragment() {
        SharedPrefs.setNotificationBadgeAvailable(this@HomeActivity, false)
        notiBadgeView.visibility = View.GONE
        navController.navigate(R.id.notificationFragment)
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            // Checking for fragment count on back stack
            if (supportFragmentManager.backStackEntryCount > 0) {
                // Go to the previous fragment
                supportFragmentManager.popBackStack()

            } else {
                // Exit the app
                SharedPrefs.removeAllOnlineUserIds(MyApplication.getInstance())
                super.onBackPressed()
            }
        }
    }

    override fun onConfirm(place: String) {
        Log.e("09/01 place =-=--=>", place);
        homeVM.callConfirm(place)
        if (place.equals("Delete", true)) {
            sendBroadcast(Intent(DELETE_TASK))
//        } else if (place.equals("Complete", true)) {
        } else if (place.contains("Complete", true)) {
            sendBroadcast(Intent(COMPLETE_TASK))
        }
    }

    private fun checkOverlayPermissions() {
        Log.e(TAG, "Checking Permissions")
        val miOverlayChecked = SharedPrefsHelper.get(MI_OVERLAY_PERMISSION_CHECKED_KEY, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Log.e(TAG, "Android Overlay Permission NOT Granted")
                buildOverlayPermissionAlertDialog()
                return
            } else if (isMiUi() && !miOverlayChecked) {
                Log.e(TAG, "Xiaomi Device. Need additional Overlay Permissions")
                buildMIUIOverlayPermissionAlertDialog()
                return
            }
        }
        Log.e(TAG, "All Overlay Permission Granted")
        return
    }


    private fun isMiUi(): Boolean {
        return !TextUtils.isEmpty(getSystemProperty("ro.miui.ui.version.name")) ||
                !TextUtils.isEmpty(getSystemProperty("ro.miui.ui.version.code"))
    }

    private fun getSystemProperty(propName: String): String? {
        val line: String
        var input: BufferedReader? = null
        try {
            val p = Runtime.getRuntime().exec("getprop $propName")
            input = BufferedReader(InputStreamReader(p.inputStream), 1024)
            line = input.readLine()
            input.close()
        } catch (ex: IOException) {
            return null
        } finally {
            if (input != null) {
                try {
                    input.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }
        return line
    }

    private fun buildOverlayPermissionAlertDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Overlay Permission Required")
        builder.setIcon(R.drawable.ic_error_outline_gray_24dp)
        builder.setMessage("To receive calls in background, please allow overlay permission in Android Settings")
        builder.setCancelable(false)

        builder.setNeutralButton("No") { dialog, which ->
            longToast("Calls may be missed while the application is in the background.")
        }

        builder.setPositiveButton("Settings") { dialog, which ->
            showAndroidOverlayPermissionsSettings()
        }

        alertDialog = builder.create()
        alertDialog.create()
        alertDialog.show()
    }

    private fun showAndroidOverlayPermissionsSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this@HomeActivity)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.data = Uri.parse("package:" + applicationContext.packageName)
            activityResultLauncher?.launch(intent)
        } else {
            Log.d(TAG, "Application Already has Overlay Permission")
        }
    }


    private fun buildMIUIOverlayPermissionAlertDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Additional Overlay Permission Required")
        builder.setIcon(R.drawable.ic_error_outline_orange_24dp)
        builder.setMessage("Please make sure that all additional permissions granted")
        builder.setCancelable(false)

        builder.setNeutralButton("I'm sure") { dialog, which ->
            SharedPrefsHelper.save(MI_OVERLAY_PERMISSION_CHECKED_KEY, true)
        }

        builder.setPositiveButton("Mi Settings") { dialog, which ->
            showMiUiPermissionsSettings()
            SharedPrefsHelper.save(MI_OVERLAY_PERMISSION_CHECKED_KEY, true)
        }

        alertDialog = builder.create()
        alertDialog.create()
        alertDialog.show()
    }

    private fun showMiUiPermissionsSettings() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.action = "miui.intent.action.APP_PERM_EDITOR"
        intent.setClassName(
            "com.miui.securitycenter",
            "com.miui.permcenter.permissions.PermissionsEditorActivity"
        )
        intent.putExtra("extra_pkgname", packageName)
        activityResultLauncher?.launch(intent)
    }

    private fun registerReceiver() {
        checkPlayServicesAvailable()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            pushBroadcastReceiver,
            IntentFilter(ACTION_NEW_FCM_EVENT),
        )
    }


    private fun checkPlayServicesAvailable() {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_REQUEST_CODE)?.show()
            } else {
                Log.i(TAG, "This device is not supported.")
                finish()
            }
        }
    }

    private val pushBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            var message = intent.getStringExtra(EXTRA_FCM_MESSAGE)
            if (TextUtils.isEmpty(message)) {
                message = EMPTY_FCM_MESSAGE
            }
            Log.i(TAG, "Receiving event $ACTION_NEW_FCM_EVENT with data: $message")
//            retrieveMessage(message)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(pushBroadcastReceiver)
        unregisterReceiver(receiver)
        if (this::alertDialog.isInitialized) {
            alertDialog.dismiss()
        }
    }

    override fun onStart() {
        super.onStart()

        homeVM.callMyProfile()

        try {
            if (::chatBadgeView.isInitialized && SharedPrefs.getChatBadgeAvailable(this@HomeActivity) == true) {
                chatBadgeView.visibility = View.VISIBLE
            }
            if (::notiBadgeView.isInitialized && SharedPrefs.getNotificationBadgeAvailable(this@HomeActivity) == true) {
                notiBadgeView.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }


    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val deletedGroup = intent?.getBooleanExtra("deleted", false)
        val deletedTask = intent?.getBooleanExtra("taskDeleted", false)
        if (deletedGroup == true)
            goToChatFragment()
        else if (deletedTask == true)
            goToTaskFragment()

        val navigateValue = intent?.getStringExtra("Navigate")
        if (navigateValue == "Chat")
            goToChatFragment()
        else if (navigateValue == "Notification")
            goToNotificationFragment()
    }

//    fun onBottomSheetDismissed() {
//        Log.e("Intent", "Call Back Called")
//    }

}