package com.example.sameteam.homeScreen.bottomNavigation.taskModule

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.example.sameteam.MyApplication
import com.example.sameteam.R
import com.example.sameteam.USER_DEFAULT_PASSWORD
import com.example.sameteam.authScreens.LoginActivity
import com.example.sameteam.authScreens.model.LoginResponseModel
import com.example.sameteam.base.BaseFragment
import com.example.sameteam.databinding.CalendarDayLayoutBinding
import com.example.sameteam.databinding.CalendarHeaderBinding
import com.example.sameteam.databinding.FragmentTaskBinding
import com.example.sameteam.helper.Constants
import com.example.sameteam.helper.SharedPrefs
import com.example.sameteam.helper.Utils
import com.example.sameteam.helper.utcTimestampToLocalDateTime
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.CalendarFragment
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.activity.CreateTaskActivity
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.adapter.TaskListAdapter
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.adapter.ViewPagerAdapter
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.interfaceses.OnBottomSheetDismissListener
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.viewModel.TaskFragVM
import com.example.sameteam.quickBlox.QbDialogHolder
import com.example.sameteam.quickBlox.chat.ChatHelper
import com.example.sameteam.quickBlox.db.QbUsersDbManager
import com.example.sameteam.quickBlox.service.LoginService
import com.example.sameteam.quickBlox.util.SharedPrefsHelper
import com.example.sameteam.quickBlox.util.longToast
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.Gson
import com.kizitonwose.calendarview.model.CalendarDay
import com.kizitonwose.calendarview.model.CalendarMonth
import com.kizitonwose.calendarview.model.DayOwner
import com.kizitonwose.calendarview.model.InDateStyle
import com.kizitonwose.calendarview.ui.DayBinder
import com.kizitonwose.calendarview.ui.MonthHeaderFooterBinder
import com.kizitonwose.calendarview.ui.ViewContainer
import com.kizitonwose.calendarview.utils.next
import com.kizitonwose.calendarview.utils.previous
import com.kizitonwose.calendarview.utils.yearMonth
import com.quickblox.auth.session.QBSessionManager
import com.quickblox.chat.QBChatService
import com.quickblox.chat.QBRoster
import com.quickblox.chat.listeners.QBRosterListener
import com.quickblox.chat.listeners.QBSubscriptionListener
import com.quickblox.chat.model.QBPresence
import com.quickblox.core.QBEntityCallback
import com.quickblox.core.exception.QBResponseException
import com.quickblox.core.helper.StringifyArrayList
import com.quickblox.core.request.GenericQueryRule
import com.quickblox.core.request.QBPagedRequestBuilder
import com.quickblox.messages.services.QBPushManager
import com.quickblox.messages.services.SubscribeService
import com.quickblox.users.QBUsers
import com.quickblox.users.model.QBUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jivesoftware.smack.SmackException
import org.jivesoftware.smack.XMPPException
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.*


class TaskFragment : BaseFragment<FragmentTaskBinding>(), OnBottomSheetDismissListener {
    private val TAG = "TaskFragment"

    val tabNames = listOf("Today", "Tomorrow", "This week", "All")
    override fun layoutID() = com.example.sameteam.R.layout.fragment_task

    override fun viewModel() = ViewModelProvider(
        this,
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    ).get(TaskFragVM::class.java)

    lateinit var taskVM: TaskFragVM
    lateinit var binding: FragmentTaskBinding

    lateinit var tabLayout: TabLayout
    lateinit var viewPager: ViewPager2
    var pos = 3
    private val dateList = mutableSetOf<LocalDate>()
    private val monthList = mutableSetOf<String>()
    private var selectedDate = LocalDate.now()
    private val today = LocalDate.now()
    private val monthTitleFormatter = DateTimeFormatter.ofPattern("MMMM")
    val monthTitleFormatter2 = DateTimeFormatter.ofPattern("MMM")
    private val currentMonth: YearMonth = YearMonth.now()
    private var firstMonth: YearMonth = currentMonth.minusMonths(12)
    private var lastMonth: YearMonth = currentMonth.plusMonths(12)

    private var startDate: LocalDate? = null
    private var endDate: LocalDate? = null
    var expanded = false
    lateinit var currentUser: LoginResponseModel.User
    private lateinit var qbUser: QBUser
    var callBackVariable = ""
    var dateString = ""
    var dateString1 = ""
    var dateString2 = ""
    lateinit var adapter: TaskListAdapter
    lateinit var firstDate: LocalDate
    lateinit var lastDate: LocalDate
    lateinit var endMonthValue: String
    lateinit var startMonthValue: String
//    lateinit var receiver: MyReceiver

//    inner class MyReceiver : BroadcastReceiver() {
//        override fun onReceive(context: Context?, intent: Intent?) {
//            Log.e("Intent", "Action ${intent?.action}")
//            lifecycleScope.launch {
//                if (intent?.action.equals("taskShow")) {
//                    binding.searchLayout.visibility = View.VISIBLE
//                }
//                else if(intent?.action.equals("taskHide") ){
//                    binding.searchLayout.visibility = View.GONE
//                }
//
//            }
//        }
//    }

    companion object {
        lateinit var contactsRoster: QBRoster
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initFragment(mBinding: ViewDataBinding) {
        taskVM = getViewModel() as TaskFragVM
        binding = mBinding as FragmentTaskBinding

//        receiver = MyReceiver()
//        val intentFilter  = IntentFilter()
//        intentFilter.addAction("taskShow")
//        intentFilter.addAction("taskHide")
//        context?.registerReceiver(receiver, intentFilter)


        val daysOfWeek = WeekFields.of(Locale.getDefault())
        getTaskList(today)

        currentUser = SharedPrefs.getUser(requireActivity())!!
        Log.e("Current User", Gson().toJson(currentUser))
        if (SharedPrefsHelper.hasQbUser()) {
            restoreChatSession()
        } else {
            startLoginProcess()
        }
        SharedPrefs.removeAllOnlineUserIds(MyApplication.getInstance())
        SharedPrefs.saveGroupName(MyApplication.getInstance(), "")

        binding.recView.layoutManager = LinearLayoutManager(requireContext())
        adapter = TaskListAdapter(this, requireContext())
        binding.recView.adapter = adapter
        binding.lifecycleOwner = this

        binding.btnAddTask.setOnClickListener {
            startActivity(CreateTaskActivity::class.java)
        }
        adapter.addLoadStateListener { loadState ->
            if (loadState.refresh == LoadState.Loading) {
                binding.progressBar.visibility = View.VISIBLE
                binding.noDataLayout.visibility = View.GONE
            } else {
                binding.progressBar.visibility = View.GONE

                if (adapter.itemCount == 0 && startDate != null && endDate != null) {
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
         * This is Date(any date in the calendar view) Click Listener for the calendar
         */
        class DayViewContainer(view: View) : ViewContainer(view) {

            val dayBinding = CalendarDayLayoutBinding.bind(view)
            lateinit var day: CalendarDay
            val textView = dayBinding.calendarDayText
            val dotView = dayBinding.dotView
            val selectedDotView = dayBinding.dotView2

            /**
             * The calendar view has "start date" and "end date" to filter tasks between these 2 dates
             *  End date must be greater than start date
             */

            init {
                view.setOnClickListener {
                    if (day.owner == DayOwner.THIS_MONTH) {
                        selectDate(day.date)
                        binding.calendarView.notifyDayChanged(day)
                    }
                }
            }
        }

        /**
         * This is to set the date view style, fully customizable
         * Shows different backgrounds for start & end date
         * Shows different background for dates selected between start & end date
         */
        binding.calendarView.dayBinder = object : DayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, day: CalendarDay) {
                container.day = day
                val textView = container.textView
                val dotView = container.dotView
                val selectedDotView = container.selectedDotView
                textView.text = day.date.dayOfMonth.toString()

                if (day.owner == DayOwner.THIS_MONTH) {
                    if (day.date in dateList) {
                        dotView.visibility = View.VISIBLE
                        selectedDotView.visibility = View.INVISIBLE
                    } else {
                        dotView.visibility = View.INVISIBLE
                        selectedDotView.visibility = View.INVISIBLE
                    }
                    when {
                        selectedDate == day.date -> {
                            textView.setTextColor(
                                ContextCompat.getColor(
                                    context!!,
                                    com.example.sameteam.R.color.white
                                )
                            )
                            textView.setBackgroundResource(com.example.sameteam.R.drawable.selected_date_background)
//                            dotView.background.setTint(ContextCompat.getColor(context!!, R.color.white))
                            dotView.visibility = View.INVISIBLE
                            if (selectedDate in dateList) selectedDotView.visibility =
                                View.VISIBLE
                        }

                        today == day.date -> {
                            textView.setTextColor(
                                ContextCompat.getColor(
                                    context!!,
                                    com.example.sameteam.R.color.colorPrimary
                                )
                            )
                            textView.setBackgroundResource(com.example.sameteam.R.color.white)
                        }

                        else -> {
                            textView.setTextColor(
                                ContextCompat.getColor(
                                    context!!,
                                    com.example.sameteam.R.color.darkGrey
                                )
                            )
                            textView.setBackgroundResource(com.example.sameteam.R.color.white)
                        }
                    }
                } else {
                    textView.setTextColor(
                        ContextCompat.getColor(
                            context!!,
                            com.example.sameteam.R.color.white
                        )
                    )
                    textView.setBackgroundResource(com.example.sameteam.R.color.white)
                    dotView.visibility = View.INVISIBLE
                    selectedDotView.visibility = View.INVISIBLE
                }
            }
        }

        class MonthViewContainer(view: View) : ViewContainer(view) {
            val legendLayout = CalendarHeaderBinding.bind(view).legendLayout.root
        }

        binding.calendarView.monthHeaderBinder = object :
            MonthHeaderFooterBinder<MonthViewContainer> {
            override fun create(view: View) = MonthViewContainer(view)
            override fun bind(container: MonthViewContainer, month: CalendarMonth) {
                // Setup each header day text if we have not done that already.
                if (container.legendLayout.tag == null) {
                    container.legendLayout.tag = month.yearMonth
                    container.legendLayout.children.map { it as TextView }
                        .forEachIndexed { index, tv ->
                            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                        }
                    month.yearMonth
                }
            }
        }

        /**
         * Month Scroll change listener
         */
        binding.calendarView.monthScrollListener = {

            // Add Months automatically if users keeps scrolling next month
            if (it.year == lastMonth.year) {
                lastMonth = lastMonth.plusMonths(12)
                binding.calendarView.updateMonthRange(firstMonth, lastMonth)
            }

            if (binding.calendarView.maxRowCount == 6) {
                // Calendar is in expanded mode
                binding.exFiveMonthYearText.text =
                    "${monthTitleFormatter.format(it.yearMonth)} ${it.yearMonth.year}"

                val monthValue = monthTitleFormatter2.format(it.yearMonth)

                val testString = "${monthValue}-${it.year}"
                if (!monthList.contains(testString)) {
                    taskVM.callTaskDates(
                        "01-$monthValue-${it.year}",
                        "${it.yearMonth.lengthOfMonth()}-$monthValue-${it.year}"
                    )
                }

            } else {
                // Calendar is in collapse mode

                firstDate = it.weekDays.first().first().date
                lastDate = it.weekDays.last().last().date

                /**
                 * Setting the month and year text as per the dates in calendar
                 */
                if (firstDate.yearMonth == lastDate.yearMonth) {
                    binding.exFiveMonthYearText.text =
                        "${monthTitleFormatter.format(firstDate.yearMonth)} ${firstDate.yearMonth.year}"

                } else {

                    if (firstDate.year == lastDate.year) {
                        binding.exFiveMonthYearText.text =
                            "${monthTitleFormatter2.format(firstDate)} - ${
                                monthTitleFormatter2.format(
                                    lastDate
                                )
                            } ${firstDate.yearMonth.year}"

                    } else {
                        binding.exFiveMonthYearText.text =
                            "${monthTitleFormatter2.format(firstDate)} ${firstDate.yearMonth.year} - ${
                                monthTitleFormatter2.format(
                                    lastDate
                                )
                            } ${lastDate.yearMonth.year}"
                    }
                    endMonthValue = ""
                    startMonthValue = ""
                    endMonthValue = monthTitleFormatter2.format(lastDate.yearMonth)
                    startMonthValue = monthTitleFormatter2.format(firstDate.yearMonth)

                    val testString1 = "${endMonthValue}-${lastDate.year}"
                    val testString2 = "${startMonthValue}-${firstDate.year}"
                    Log.d("DebugTomorrow", "Task Frag On Init Check: $testString1 ")

                    if (!monthList.contains(testString1)) {
                        callBackVariable = "OnInitEnd"
                        taskVM.fetchTasksLiveData(
                            "01-$endMonthValue-${lastDate.year}",
                            "${lastDate.yearMonth.lengthOfMonth()}-$endMonthValue-${lastDate.year}"
                        )
                    }
                    if (!monthList.contains(testString2)) {
                        callBackVariable = "OnInitStart"
                        taskVM.fetchTasksLiveData(
                            "01-$startMonthValue-${firstDate.year}",
                            "${firstDate.yearMonth.lengthOfMonth()}-$startMonthValue-${firstDate.year}"
                        )
                    }
                }
            }
        }

        /**
         * Next month button
         */
        binding.exFiveNextMonthImage.setOnClickListener {
            if (binding.calendarView.maxRowCount == 6) {
                binding.calendarView.findFirstVisibleMonth()?.let {
                    binding.calendarView.smoothScrollToMonth(it.yearMonth.next)
                }
            } else {
                val lastDate = binding.calendarView.findLastVisibleDay()?.date
                if (lastDate != null) {
                    binding.calendarView.smoothScrollToDate(lastDate.plusDays(1))
                }
            }
        }

        /**
         * Previous month button
         */
        binding.exFivePreviousMonthImage.setOnClickListener {
            if (binding.calendarView.maxRowCount == 6) {
                binding.calendarView.findFirstVisibleMonth()?.let {
                    binding.calendarView.smoothScrollToMonth(it.yearMonth.previous)
                }
            } else {
                val firstDate = binding.calendarView.findFirstVisibleDay()?.date
                if (firstDate != null) {
                    binding.calendarView.smoothScrollToDate(firstDate.minusDays(1))
                }
            }

        }

        /**
         * Swipe gesture on calendar for expand and collapse
         */

        binding.calendarLayout.setOnTouchListener(object : OnSwipeTouchListener(requireContext()) {
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                return super.onTouch(v, event)
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
            }

            override fun onSwipeLeft() {
                super.onSwipeLeft()
            }

            override fun onSwipeTop() {
                super.onSwipeTop()
                expandCollapse(weekToMonth = false, false)
            }

            override fun onSwipeBottom() {
                super.onSwipeBottom()
                if (!expanded)
                    expandCollapse(weekToMonth = true, false)
            }
        })

        binding.calendarView.setup(firstMonth, lastMonth, daysOfWeek.firstDayOfWeek)
        binding.calendarView.scrollToMonth(currentMonth)

        setupViewPager()

        /**
         * Tabs layout for "All", "Today", "Tomorrow" & "This week" filters
         * Show different Styling for selected and unselected tabs
         */
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab != null) {

                    binding.btnAddTask.visibility = View.VISIBLE
                    binding.fragmentContainer.visibility = View.GONE
                    binding.viewPager.visibility = View.VISIBLE
                    binding.frameLayout.visibility = View.GONE

                    viewPager.currentItem = tab.position
                    pos = tab.position
                    val selectedTextView =
                        (tabLayout.getTabAt(tab.position)?.customView?.findViewById(R.id.txt_tab_name) as TextView?)
                    selectedTextView?.setTextColor(
                        ContextCompat.getColor(
                            requireActivity(),
                            R.color.white
                        )
                    )
                    selectedTextView?.setBackgroundResource(R.drawable.tab_selected_background)
//                    (tabLayout.getTabAt(tab.position)?.customView?.findViewById(R.id.txt_tab_name) as TextView?)?.typeface =
//                        ResourcesCompat.getFont(requireContext(),R.font.avenirnext_demibold)
                    selectedTextView?.setTypeface(selectedTextView.typeface, Typeface.BOLD)

                    binding.btnCalendar.setBackgroundResource(R.color.white)
                    binding.btnCalendar.setColorFilter(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.black
                        )
                    )
                    binding.me.setBackgroundResource(com.example.sameteam.R.color.white)
                    binding.me.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            com.example.sameteam.R.color.black
                        )
                    )
                    binding.calendarLayout.visibility = View.GONE

                }

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                if (tab != null) {
                    val textView =
                        (tabLayout.getTabAt(tab.position)?.customView?.findViewById(com.example.sameteam.R.id.txt_tab_name) as TextView?)
                    textView?.setTextColor(
                        ContextCompat.getColor(
                            requireActivity(),
                            com.example.sameteam.R.color.black
                        )
                    )
                    textView?.setBackgroundResource(com.example.sameteam.R.color.white)
                    textView?.setPadding(0, 0, 0, 0)
                    textView?.setTypeface(null, Typeface.NORMAL)
                }

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                if (tab != null) {
                    binding.btnAddTask.visibility = View.VISIBLE
                    binding.fragmentContainer.visibility = View.GONE
                    binding.viewPager.visibility = View.VISIBLE
                    binding.frameLayout.visibility = View.GONE

                    viewPager.currentItem = tab.position
                    pos = tab.position
                    val selectedTextView =
                        (tabLayout.getTabAt(tab.position)?.customView?.findViewById(com.example.sameteam.R.id.txt_tab_name) as TextView?)
                    selectedTextView?.setTextColor(
                        ContextCompat.getColor(
                            requireActivity(),
                            com.example.sameteam.R.color.white
                        )
                    )
                    selectedTextView?.setBackgroundResource(com.example.sameteam.R.drawable.tab_selected_background)
                    selectedTextView?.setTypeface(selectedTextView.typeface, Typeface.BOLD)


                    binding.me.setBackgroundResource(com.example.sameteam.R.color.white)
                    binding.me.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            com.example.sameteam.R.color.black
                        )
                    )

                    binding.btnCalendar.setBackgroundResource(com.example.sameteam.R.color.white)
                    binding.btnCalendar.setColorFilter(
                        ContextCompat.getColor(
                            requireContext(),
                            com.example.sameteam.R.color.black
                        )
                    )
                    binding.calendarLayout.visibility = View.GONE

                }
            }
        })

        binding.me.setOnClickListener {
            binding.btnAddTask.visibility = View.GONE
            binding.calendarLayout.visibility = View.GONE
            binding.viewPager.visibility = View.GONE
            binding.fragmentContainer.visibility = View.VISIBLE
            binding.frameLayout.visibility = View.GONE

            val textView =
                (tabLayout.getTabAt(pos)?.customView?.findViewById(com.example.sameteam.R.id.txt_tab_name) as TextView?)
            textView?.setTextColor(
                ContextCompat.getColor(
                    requireActivity(),
                    com.example.sameteam.R.color.black
                )
            )
            textView?.setBackgroundResource(com.example.sameteam.R.color.white)
            textView?.setPadding(0, 0, 0, 0)
            textView?.setTypeface(null, Typeface.NORMAL)

            binding.btnCalendar.setBackgroundResource(com.example.sameteam.R.color.white)
            binding.btnCalendar.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    com.example.sameteam.R.color.black
                )
            )

            binding.me.setBackgroundResource(com.example.sameteam.R.drawable.ic_rectangle_background)
            val textMe = binding.me as TextView
            binding.me.setTextColor(
                ContextCompat.getColor(
                    requireActivity(),
                    com.example.sameteam.R.color.white
                )
            )

            val nextFrag = CalendarFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(com.example.sameteam.R.id.fragment_container, nextFrag, "findThisFragment")
                .addToBackStack(null)
                .commit()

//            val manager: FragmentManager? = fragmentManager
//            val transaction: FragmentTransaction = manager!!.beginTransaction()
//            transaction.replace(com.example.sameteam.R.id.container, new CalendarFragment,"Calender Frag")
//            transaction.addToBackStack(null)
//            transaction.commit()

        }
        /**
         * Show and hide calendar view
         */
        binding.btnCalendar.setOnClickListener {
            binding.btnAddTask.visibility = View.VISIBLE

            binding.viewPager.visibility = View.GONE
            binding.frameLayout.visibility = View.VISIBLE

            val textView =
                (tabLayout.getTabAt(pos)?.customView?.findViewById(com.example.sameteam.R.id.txt_tab_name) as TextView?)
            textView?.setTextColor(
                ContextCompat.getColor(
                    requireActivity(),
                    com.example.sameteam.R.color.black
                )
            )
            textView?.setBackgroundResource(com.example.sameteam.R.color.white)
            textView?.setPadding(0, 0, 0, 0)
            textView?.setTypeface(null, Typeface.NORMAL)

            binding.me.setBackgroundResource(com.example.sameteam.R.color.white)
            binding.me.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    com.example.sameteam.R.color.black
                )
            )
            binding.btnCalendar.setBackgroundResource(com.example.sameteam.R.drawable.ic_rectangle_background)
            binding.btnCalendar.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    com.example.sameteam.R.color.white
                )
            )

            if (binding.calendarLayout.isVisible)
                binding.calendarLayout.visibility = View.GONE
            else
                binding.calendarLayout.visibility = View.VISIBLE


        }

        taskVM.observedChanges().observe(this) { event ->
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


        taskVM.observeDateList().observe(this) { event ->
            event?.getContentIfNotHandled()?.let {
                if (!it.isNullOrEmpty()) {
//                    val splitString = it[0].split("-")
//                    if (!splitString.isNullOrEmpty() && splitString.size == 3) {
//                        val monthVal = "${monthHashMap.getValue(splitString[1])}-${splitString[0]}"
//                        monthList.add(monthVal)
//                    }
//                    it.forEach { item ->
//                        dateList.add(getDateFromString2(item))
//                    }

                    lifecycleScope.launch(Dispatchers.Main) {
                        it.forEach { item ->
                            val temp = utcTimestampToLocalDateTime(item)
                            Log.d(TAG, "UTC Time Stamp$item")
                            Log.d(TAG, "UTC to Local Time Stamp" + temp.toString())
                            if (temp != null) {
                                convertTimestampToLocalDate(temp.toLocalDate())
                                // Log.d(TAG, "Local Time Stamp to local date"+convertTimestampToLocalDate(temp.toLocalDate()).toString())
                            }
                        }

                        binding.calendarView.notifyCalendarChanged()
                    }


                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        getTaskList(today)

        currentUser = SharedPrefs.getUser(MyApplication.getInstance())!!
        if (SharedPrefsHelper.hasQbUser()) {
            restoreChatSession()
        } else {
            startLoginProcess()
        }
        SharedPrefs.removeAllOnlineUserIds(MyApplication.getInstance())
        SharedPrefs.saveGroupName(MyApplication.getInstance(), "")

    }

    private fun restoreChatSession() {
        if (!ChatHelper.isLogged()) {
            val sessionUser = getUserFromSession()
            if (sessionUser == null) {
                startLoginProcess()
            } else {
                if (sessionUser.id != null && sessionUser.password != null) {
                    loginToChat(sessionUser)
                } else
                    startLoginProcess()
            }
        }
    }

    private fun loginToChat(user: QBUser) {
        binding.progressBar.visibility = View.VISIBLE
//        user.password = USER_DEFAULT_PASSWORD
        ChatHelper.loginToChat(user, object : QBEntityCallback<Void> {
            override fun onSuccess(result: Void?, bundle: Bundle?) {
                Log.d(TAG, "Chat login onSuccess()")
                binding.progressBar.visibility = View.GONE
                saveQBUser(user)

                activity?.let { LoginService.start(it, user) }

                contactsRoster = QBChatService.getInstance().roster
                contactsRoster.subscriptionMode = QBRoster.SubscriptionMode.mutual
                contactsRoster.addSubscriptionListener(subscriptionListener)
                contactsRoster.addRosterListener(rosterListener)
//                contactsRoster.sendPresence(QBPresence(QBPresence.Type.online))

                loadUsersWithTag()
            }

            override fun onError(e: QBResponseException) {
                if (e.message.equals("You have already logged in chat")) {
                    loginToChat(user)
                } else {
                    binding.progressBar.visibility = View.GONE
                    Log.d(TAG, "Chat login onError(): $e")
                    longToast(R.string.login_chat_login_error)
                    performLogout()

                }
            }
        })
    }

    private fun saveQBUser(user: QBUser) {
        QBUsers.getUser(user.id).performAsync(object : QBEntityCallback<QBUser> {
            override fun onSuccess(p0: QBUser?, p1: Bundle?) {
                if (p0 != null) {
                    p0.password = USER_DEFAULT_PASSWORD
                    SharedPrefsHelper.saveQbUser(p0)
                } else {
                    user.fullName = "${currentUser.first_name} ${currentUser.last_name}"
                    user.password = USER_DEFAULT_PASSWORD
                    val tagList = StringifyArrayList<String>()
                    tagList.add(currentUser.company?.company_code)
                    user.tags = tagList
                    SharedPrefsHelper.saveQbUser(user)
                }
            }

            override fun onError(p0: QBResponseException?) {
                p0?.printStackTrace()
            }
        })
    }

    private fun unsubscribeFromPushes() {
        if (QBPushManager.getInstance().isSubscribedToPushes) {
            QBPushManager.getInstance().addListener(object : QBPushManager.QBSubscribeListener {
                override fun onSubscriptionCreated() {

                }

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

    private fun loadUsersWithTag() {

        val rules = ArrayList<GenericQueryRule>()
        rules.add(GenericQueryRule(Constants.ORDER_RULE, Constants.ORDER_VALUE_UPDATED_AT))

        val requestBuilder = QBPagedRequestBuilder()
        requestBuilder.rules = rules
        requestBuilder.perPage = Constants.USERS_PAGE_SIZE
        requestBuilder.page = 1

        val loggedUser = activity?.let { SharedPrefs.getUser(it) }
        val arrayList = listOf(loggedUser?.company?.company_code)

        if (!arrayList.isNullOrEmpty()) {
            QBUsers.getUsers(requestBuilder)
                .performAsync(object : QBEntityCallback<ArrayList<QBUser>> {
                    override fun onSuccess(usersList: ArrayList<QBUser>, params: Bundle?) {
                        QbUsersDbManager.saveAllUsers(usersList, true)

                        for (item in usersList) {
                            contactsRoster.subscribe(item.id)
                        }
                    }

                    override fun onError(e: QBResponseException) {
                        e.printStackTrace()
                    }
                })
        } else {
            binding.progressBar.visibility = View.GONE
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
            if (presence == null) {
                // no user in your contact list
                return
            }
            // if a user uses several devices, you need to do additional check for presence
            val qbPresence = contactsRoster.getPresence(presence.userId)

            if (qbPresence.type == QBPresence.Type.online) {
                Log.d(TAG, "presenceChanged: Online")
                SharedPrefs.saveOnlineUserId(MyApplication.getInstance(), qbPresence.userId)
            } else {
                Log.d(TAG, "presenceChanged: Offline")
                SharedPrefs.removeOnlineUserId(MyApplication.getInstance(), qbPresence.userId)
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

    private fun getUserFromSession(): QBUser? {
        val user = SharedPrefsHelper.getQbUser()
        val qbSessionManager = QBSessionManager.getInstance()
        qbSessionManager.sessionParameters?.let {
            val userId = qbSessionManager.sessionParameters.userId
            user?.id = userId
            return user
        } ?: run {
            ChatHelper.destroy()
            return null
        }
    }

    private fun startLoginProcess() {

        qbUser = QBUser()
        Log.e("QB User Task Fragment", Gson().toJson(qbUser))

        qbUser.login = currentUser.email
//        qbUser.fullName = "${currentUser.first_name} ${currentUser.last_name}"
        qbUser.password = USER_DEFAULT_PASSWORD
//        val tagList = StringifyArrayList<String>()
//        tagList.add(currentUser.company?.company_code)
//        qbUser.tags = tagList
//        currentUser.company = null
//        qbUser.customDataAsObject = currentUser

        signIn(qbUser)
    }

    private fun signIn(user: QBUser) {
        binding.progressBar.visibility = View.VISIBLE

        ChatHelper.login(user, object : QBEntityCallback<QBUser> {
            override fun onSuccess(userFromRest: QBUser, bundle: Bundle?) {
//                if (userFromRest.fullName != null && userFromRest.fullName == user.fullName) {
////                    showMessage("Logged in as " + userFromRest.fullName)
//                    loginToChat(user)
//
//                } else {
//                    //Need to set password NULL, because server will update user only with NULL password
////                    user.password = null
////                    updateUser(user)
//                    loginToChat(user)
//                }
                loginToChat(user)

            }

            override fun onError(e: QBResponseException) {
                if (e.httpStatusCode == Constants.UNAUTHORIZED) {
                    signUp(user)
                } else {
                    binding.progressBar.visibility = View.GONE

                    longToast(R.string.login_chat_login_error)
                    performLogout()
                }
            }
        })
    }

    private fun signUp(user: QBUser) {
        SharedPrefsHelper.removeQbUser()
        QBUsers.signUp(user).performAsync(object : QBEntityCallback<QBUser> {
            override fun onSuccess(p0: QBUser?, p1: Bundle?) {
                binding.progressBar.visibility = View.GONE
                signIn(user)
            }

            override fun onError(exception: QBResponseException?) {
                binding.progressBar.visibility = View.GONE
                longToast(R.string.login_chat_login_error)
                performLogout()

            }
        })
    }

    private suspend fun convertTimestampToLocalDate(localDate: LocalDate): Int {
        val result = 0
        val waitFor = CoroutineScope(Dispatchers.IO).async {
            requireActivity().let {
                dateList.add(localDate)
                return@async
            }
        }
        waitFor.await()
        return result
    }

    private fun selectDate(date: LocalDate) {
        if (selectedDate != date) {
            val oldDate = selectedDate
            selectedDate = date
            oldDate?.let { binding.calendarView.notifyDateChanged(it) }
            binding.calendarView.notifyDateChanged(date)
//            updateAdapterForDate(date)

            //Fetch Tasks API
            getTaskList(date)

        }
    }

    private fun getTaskList(localDate: LocalDate) {

        if (Utils.isConnected(requireActivity())) {
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
            Log.d("DebugTomorrow", "Task Frag DateCheck: $dateString ")
            callBackVariable = "OnDate"

            taskVM.fetchTasksLiveData(dateString, dateString).observe(viewLifecycleOwner) {
                lifecycleScope.launch {
                    adapter.submitData(it)
                }
            }
        } else {
            showMessage(getString(com.example.sameteam.R.string.no_internet))
        }
    }

    fun setupViewPager() {
        val adapter = ViewPagerAdapter(requireActivity())
        binding.viewPager.adapter = adapter

        tabLayout = binding.tabLayout
        viewPager = binding.viewPager

        tabLayout.addTab(tabLayout.newTab().setText("All"))
        tabLayout.addTab(tabLayout.newTab().setText("Today"))
        tabLayout.addTab(tabLayout.newTab().setText("Tomorrow"))
        tabLayout.addTab(tabLayout.newTab().setText("This week"))

        TabLayoutMediator(
            tabLayout, viewPager
        ) { tab: TabLayout.Tab, position: Int ->
            tab.text = tabNames[position]
        }.attach()

        viewPager.isUserInputEnabled = false  // Disable swipe gesture for view pager


        for (i in 0 until tabLayout.tabCount) {

            tabLayout.getTabAt(i)?.setCustomView(com.example.sameteam.R.layout.custom_tab_layout)
            val textView =
                tabLayout.getTabAt(i)?.customView?.findViewById(com.example.sameteam.R.id.txt_tab_name) as TextView
            textView.text = "" + tabNames[i]
        }

        val txtView =
            (tabLayout.getTabAt(3)?.customView?.findViewById(com.example.sameteam.R.id.txt_tab_name) as TextView)
        txtView.setTextColor(
            ContextCompat.getColor(
                requireActivity(),
                com.example.sameteam.R.color.white
            )
        )
        txtView.setBackgroundResource(com.example.sameteam.R.drawable.tab_selected_background)
//        (tabLayout.getTabAt(0)?.customView?.findViewById(R.id.txt_tab_name) as TextView?)?.typeface =
//            ResourcesCompat.getFont(requireContext(),R.font.avenirnext_demibold)
        txtView.setTypeface(txtView.typeface, Typeface.BOLD)
        binding.btnCalendar.setBackgroundResource(com.example.sameteam.R.color.white)
        binding.btnCalendar.setColorFilter(
            ContextCompat.getColor(
                requireContext(),
                com.example.sameteam.R.color.black
            )
        )
        tabLayout.getTabAt(3)?.select()
    }

    private fun bindSummaryViews() {

        if (startDate != null && endDate != null) {
            lifecycleScope.launch {
                delay(600)
                binding.calendarLayout.visibility = View.GONE
                getRangeTaskList()

            }
        }
    }

    private fun getRangeTaskList() {
        if (Utils.isConnected(requireActivity())) {
            dateString1 = ""
            dateString2 = ""
            if (startDate != null && endDate != null) {
                dateString1 += if (startDate?.dayOfMonth!! < 10)
                    "0${startDate?.dayOfMonth}"
                else
                    "${startDate?.dayOfMonth}"

                dateString1 += "-${
                    startDate?.month?.getDisplayName(
                        TextStyle.SHORT,
                        Locale.ENGLISH
                    )
                }-${startDate?.year}"


                dateString2 += if (endDate?.dayOfMonth!! < 10)
                    "0${endDate?.dayOfMonth}"
                else
                    "${endDate?.dayOfMonth}"

                dateString2 += "-${
                    endDate?.month?.getDisplayName(
                        TextStyle.SHORT,
                        Locale.ENGLISH
                    )
                }-${endDate?.year}"
                Log.d("DebugTomorrow", "Task Frag RangeCheck: $dateString1 ")
                callBackVariable = "OnRangeList"

                taskVM.fetchTasksLiveData(dateString1, dateString2).observe(viewLifecycleOwner) {
                    lifecycleScope.launch {
                        adapter.submitData(it)
                    }
                }
            }
        } else {
            showMessage(getString(com.example.sameteam.R.string.no_internet))
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        try {
            //Register or UnRegister your broadcast receiver here
//            context?.unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    /**
     * Swipe Gesture listener for calendar layout
     */
    open inner class OnSwipeTouchListener(ctx: Context?) :
        View.OnTouchListener {
        private val gestureDetector: GestureDetector

        override fun onTouch(v: View, event: MotionEvent): Boolean {

            return gestureDetector.onTouchEvent(event)

        }

        private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onFling(
                e1: MotionEvent,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {

                var result = false
                try {
                    val diffY = e2.y - e1.y
                    val diffX = e2.x - e1.x
                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX > 0) {
                                onSwipeRight()
                            } else {
                                onSwipeLeft()
                            }
                            result = true
                        }
                    } else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) {
                            onSwipeBottom()
                        } else {
                            onSwipeTop()
                        }
                        result = true
                    }
                } catch (exception: Exception) {
                    exception.printStackTrace()
                }
                return result
            }


            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

        }

        open fun onSwipeRight() {}

        open fun onSwipeLeft() {}

        open fun onSwipeTop() {
            Log.d(TAG, "onSwipeTop: ")
        }

        open fun onSwipeBottom() {
            Log.d(TAG, "onSwipeBottom: ")
        }

        init {
            gestureDetector = GestureDetector(ctx, GestureListener())
        }
    }


    /**
     * Expand - Collapse logic for calendar
     */
    fun expandCollapse(weekToMonth: Boolean, firstTime: Boolean) {

        val firstDate: LocalDate
        val lastDate: LocalDate

        if (selectedDate.dayOfWeek.value == 7) {
            firstDate = selectedDate
            lastDate = selectedDate.plusDays(6)
        } else {
            firstDate = selectedDate.minusDays(selectedDate.dayOfWeek.value.toLong())
            lastDate = selectedDate.plusDays(6 - selectedDate.dayOfWeek.value.toLong())
        }

        val oneWeekHeight = binding.calendarView.daySize.height
        Log.d(TAG, "height expandCollapse: $oneWeekHeight")

        val oneMonthHeight = oneWeekHeight * 6


        val oldHeight = if (!weekToMonth) oneMonthHeight else oneWeekHeight
        val newHeight = if (!weekToMonth) oneWeekHeight else oneMonthHeight

        // Animate calendar height changes.
        val animator = ValueAnimator.ofInt(oldHeight, newHeight)
        animator.addUpdateListener { animator ->
            binding.calendarView.updateLayoutParams {
                height = animator.animatedValue as Int
            }
        }

        animator.doOnStart {
            if (weekToMonth) {
                binding.calendarView.updateMonthConfiguration(
                    inDateStyle = InDateStyle.ALL_MONTHS,
                    maxRowCount = 6,
                    hasBoundaries = true
                )
            }
        }
        animator.doOnEnd {
            if (!weekToMonth) {
                binding.calendarView.updateMonthConfiguration(
                    inDateStyle = InDateStyle.FIRST_MONTH,
                    maxRowCount = 1,
                    hasBoundaries = false
                )
            }

            if (!weekToMonth) {
                // We want the first visible day to remain
                // visible when we change to week mode.
                binding.calendarView.scrollToDate(firstDate)
                expanded = false


            } else {
                // When changing to month mode, we choose current
                // month if it is the only one in the current frame.
                // if we have multiple months in one frame, we prefer
                // the second one unless it's an outDate in the last index.

                expanded = true

                if (firstDate.yearMonth == lastDate.yearMonth) {
                    binding.calendarView.scrollToMonth(firstDate.yearMonth)
                } else {
                    // We compare the next with the last month on the calendar so we don't go over.
//                    binding.calendarView.scrollToMonth(minOf(firstDate.yearMonth.next, lastMonth))
                    binding.calendarView.scrollToMonth(selectedDate.yearMonth)
                }
                if (firstTime)
                    expandCollapse(weekToMonth = false, false)

                binding.calendarView.notifyCalendarChanged()
            }
        }
        animator.duration = 250
        animator.start()
    }

    override fun onBottomSheetDismissed() {
        Log.e("CallBack", "CallBack from Task Fragment")
        Log.e("CallBack", "callBackVariable value: $callBackVariable ")
        if (callBackVariable == "OnDate") {
            taskVM.fetchTasksLiveData(dateString, dateString).observe(viewLifecycleOwner) {
                lifecycleScope.launch {
                    adapter.submitData(it)
                }
            }
        }
        if (callBackVariable == "OnInitStart") {
            taskVM.fetchTasksLiveData(
                "01-$startMonthValue-${firstDate.year}",
                "${firstDate.yearMonth.lengthOfMonth()}-$startMonthValue-${firstDate.year}"
            )
        }
        if (callBackVariable == "OnInitEnd") {
            taskVM.fetchTasksLiveData(
                "01-$endMonthValue-${lastDate.year}",
                "${lastDate.yearMonth.lengthOfMonth()}-$endMonthValue-${lastDate.year}"
            )
        }

        if (callBackVariable == "OnRangeList") {
            taskVM.fetchTasksLiveData(dateString1, dateString2).observe(viewLifecycleOwner) {
                lifecycleScope.launch {
                    adapter.submitData(it)
                }
            }
        }
    }
}