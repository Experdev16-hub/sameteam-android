package com.example.sameteam.homeScreen.bottomNavigation.calendarModule

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sameteam.MyApplication
import com.example.sameteam.R
import com.example.sameteam.USER_DEFAULT_PASSWORD
import com.example.sameteam.authScreens.LoginActivity
import com.example.sameteam.authScreens.model.LoginResponseModel
import com.example.sameteam.base.BaseFragment
import com.example.sameteam.databinding.CalendarDayLayoutBinding
import com.example.sameteam.databinding.FragmentCalendarBinding
import com.example.sameteam.helper.*
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.adapter.CalendarTaskAdapter
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.bottomSheet.ChooseMonthBottomSheet
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.viewModel.CalendarFragVM
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.adapter.TaskListAdapter
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.interfaceses.OnBottomSheetDismissListener
import com.example.sameteam.quickBlox.QbDialogHolder
import com.example.sameteam.quickBlox.chat.ChatHelper
import com.example.sameteam.quickBlox.db.QbUsersDbManager
import com.example.sameteam.quickBlox.service.LoginService
import com.example.sameteam.quickBlox.util.SharedPrefsHelper
import com.example.sameteam.quickBlox.util.longToast
import com.google.gson.Gson
import com.kizitonwose.calendarview.model.CalendarDay
import com.kizitonwose.calendarview.model.DayOwner
import com.kizitonwose.calendarview.model.InDateStyle
import com.kizitonwose.calendarview.ui.DayBinder
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
import org.jivesoftware.smack.SmackException
import org.jivesoftware.smack.XMPPException
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.*
import kotlinx.coroutines.*
import kotlin.collections.List

class CalendarFragment : BaseFragment<FragmentCalendarBinding>(),
    CalendarTaskAdapter.confirmTaskListener, OnBottomSheetDismissListener {

    private val TAG = "CalendarFragment"

    override fun layoutID() = R.layout.fragment_calendar

    override fun viewModel() = ViewModelProvider(
        this,
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    ).get(CalendarFragVM::class.java)

    lateinit var calendarVM: CalendarFragVM
    lateinit var binding: FragmentCalendarBinding

    var expanded = false

    private var selectedDate = LocalDate.now()
    private val today = LocalDate.now()
    private val dateList = mutableSetOf<LocalDate>()
    private val monthList = mutableSetOf<String>()

    private val monthTitleFormatter = DateTimeFormatter.ofPattern("MMMM")
    val monthTitleFormatter2 = DateTimeFormatter.ofPattern("MMM")

    private val currentMonth: YearMonth = YearMonth.now()
    private var firstMonth: YearMonth = currentMonth.minusMonths(12)
    private var lastMonth: YearMonth = currentMonth.plusMonths(12)
    var callBackVariable = ""
    var dateString = ""
    lateinit var firstDate: LocalDate
    lateinit var lastDate: LocalDate
    lateinit var endMonthValue: String
    lateinit var startMonthValue: String
    lateinit var  scrollDate1:String
    lateinit var  scrollDate2:String
    var downY = 0f
    var upY = 0f
    var downX = 0f
    var upX = 0f
    val MIN_DISTANCE = 100

    lateinit var adapter: TaskListAdapter
    val monthHashMap = hashMapOf(
        "01" to "Jan", "02" to "Feb", "03" to "Mar", "04" to "Apr", "05" to "May", "06" to "Jun",
        "07" to "Jul", "08" to "Aug", "09" to "Sep", "10" to "Oct", "11" to "Nov", "12" to "Dec"
    )

    lateinit var currentUser: LoginResponseModel.User
    private lateinit var qbUser: QBUser

    companion object {
        lateinit var contactsRoster: QBRoster
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /**
         * Collapse Calendar when first initialize with expanded, it is necessary for calendar to expand first,
         * so don't delete this
         */
//        lifecycleScope.launch {
//            delay(700)
//            expandCollapse(weekToMonth = false, false)
//        }

        /**
         * Shows spotlight for create task when first login
         */
        if (SharedPrefs.getSpotlight(MyApplication.getInstance()) == true) {
            lifecycleScope.launch {
                delay(3000)
                val firstRoot = FrameLayout(requireContext())
                val first = layoutInflater.inflate(R.layout.layout_target, firstRoot)
//                val target = Target.Builder()
//                    .setAnchor(binding.btnAddTask)
//                    .setOverlay(first)
//                    .setEffect(RippleEffect(100f, 200f, argb(30, 124, 255, 90)))
//                    .setOnTargetListener(object : OnTargetListener {
//                        override fun onStarted() {
//                        }
//
//                        override fun onEnded() {
//                        }
//                    })
//                    .build()

//                val spotlight = Spotlight.Builder(requireActivity())
//                    .setTargets(target)
//                    .setBackgroundColor(
//                        ContextCompat.getColor(
//                            requireContext(),
//                            R.color.spotlightBackground
//                        )
//                    )
//                    .setDuration(1000L)
//                    .setAnimation(DecelerateInterpolator(2f))
//                    .setOnSpotlightListener(object : OnSpotlightListener {
//                        override fun onStarted() {
//                        }
//
//                        override fun onEnded() {
//                        }
//                    })
//                    .build()
//
//                spotlight.start()

//                val close = View.OnClickListener {
//                    spotlight.finish()
//                    SharedPrefs.setSpotlight(MyApplication.getInstance(), false)
//                }

                //  first.findViewById<View>(R.id.close_spotlight).setOnClickListener(close)
            }
        }


    }

    override fun onResume() {
        super.onResume()

        //Fetch Tasks API
        getTaskList(today)
        val monthValue = monthTitleFormatter2.format(today.yearMonth)
        Log.e("CallBack", "callBackVariable value: onResume ")

        calendarVM.callTaskDates(
            "01-$monthValue-${today.year}",
            "${today.yearMonth.lengthOfMonth()}-$monthValue-${today.year}"
        )

        monthList.clear()
        dateList.clear()

        //Fetch Month Task Dates
//        val month = today.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
//        val year = today.year
//        val dateString = "01-$month-$year"
//        calendarVM.callTaskDates(dateString,"${today.lengthOfMonth()}-$month-$year")


        currentUser = SharedPrefs.getUser(MyApplication.getInstance())!!
        if (SharedPrefsHelper.hasQbUser()) {
            restoreChatSession()
        } else {
            startLoginProcess()
        }
        SharedPrefs.removeAllOnlineUserIds(MyApplication.getInstance())
        SharedPrefs.saveGroupName(MyApplication.getInstance(), "")


    }


    @SuppressLint("ClickableViewAccessibility")
    override fun initFragment(mBinding: ViewDataBinding) {


        calendarVM = getViewModel() as CalendarFragVM
        binding = mBinding as FragmentCalendarBinding

        val daysOfWeek = WeekFields.of(Locale.getDefault())
        var mDay = daysOfWeek.firstDayOfWeek

        binding.legendLayout.root.children.forEachIndexed { index, view ->
            (view as TextView).apply {
                text = mDay.getDisplayName(TextStyle.NARROW, Locale.ENGLISH)
                    .uppercase(Locale.getDefault())
                mDay = mDay.plus(1)
            }
        }

        binding.exFiveMonthYearText.setOnClickListener {
            val fragment = ChooseMonthBottomSheet { monthName ->
                run {
                    val tempMonth = Utils.getMonth(monthName)
                    val tempLocalDate = LocalDate.of(tempMonth.year, tempMonth.month, 1)
                    binding.calendarView.smoothScrollToMonth(tempMonth)
                    selectedDate = tempLocalDate
                    binding.calendarView.notifyCalendarChanged()
                    getTaskList(selectedDate)
                    if (!expanded)
                        expandCollapse(true, false)
                }
            }
            fragment.show(
                requireActivity().supportFragmentManager,
                ChooseMonthBottomSheet::class.java.name
            )
        }

        //Fetch Tasks API
        binding.recView.layoutManager = LinearLayoutManager(requireContext())
        adapter = TaskListAdapter(this,requireContext())
        binding.recView.adapter = adapter
       // adapter.setConfirmTaskListener(this)
        binding.lifecycleOwner = this

        //Fetch Tasks API
        getTaskList(today)

//        //Fetch Month Task Dates
//        val month = today.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
//        val year = today.year
//        val dateString = "01-$month-$year"
//        calendarVM.callTaskDates(dateString,"${today.lengthOfMonth()}-$month-$year")


        adapter.addLoadStateListener { loadState ->
            if (loadState.refresh == LoadState.Loading) {
//                binding.progressBar.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
                binding.noDataLayout.visibility = View.GONE
            } else {
                binding.progressBar.visibility = View.GONE

                if (adapter.itemCount == 0) {
                    binding.noDataLayout.visibility = View.VISIBLE
                    binding.recView.visibility = View.GONE
                } else {
                    Log.d(TAG, "initFragment Item: ${adapter.itemCount}")
                    binding.noDataLayout.visibility = View.GONE
                    binding.recView.visibility = View.VISIBLE
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


        /**
         * This is Date(any date in the calendar view) Click Listener for the calendar
         */
        class DayViewContainer(view: View) : ViewContainer(view) {

            val dayBinding = CalendarDayLayoutBinding.bind(view)
            lateinit var day: CalendarDay
            val textView = dayBinding.calendarDayText
            val dotView = dayBinding.dotView
            val selectedDotView = dayBinding.dotView2

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
                            textView.setTextColor(ContextCompat.getColor(context!!, R.color.white))
                            textView.setBackgroundResource(R.drawable.selected_date_background)
//                            dotView.background.setTint(ContextCompat.getColor(context!!, R.color.white))
                            dotView.visibility = View.INVISIBLE
                            if (selectedDate in dateList) selectedDotView.visibility =
                                View.VISIBLE
                        }

                        today == day.date -> {
                            textView.setTextColor(
                                ContextCompat.getColor(
                                    context!!,
                                    R.color.colorPrimary
                                )
                            )
                            textView.setBackgroundResource(R.color.white)
                        }

                        else -> {
                            textView.setTextColor(
                                ContextCompat.getColor(
                                    context!!,
                                    R.color.darkGrey
                                )
                            )
                            textView.setBackgroundResource(R.color.white)
                        }
                    }
                } else {
                    textView.setTextColor(
                        ContextCompat.getColor(
                            context!!,
                            R.color.white
                        )
                    )
                    textView.setBackgroundResource(R.color.white)
                    dotView.visibility = View.INVISIBLE
                    selectedDotView.visibility = View.INVISIBLE
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
                scrollDate1=""
                scrollDate2=""
                scrollDate1="01-$monthValue-${it.year}"
                scrollDate2="${it.yearMonth.lengthOfMonth()}-$monthValue-${it.year}"

                if (!monthList.contains(testString)) {
                    Log.e("CallBack", "callBackVariable value: teststring ")
                    callBackVariable = "TestString"
                    calendarVM.callTaskDates(
                        scrollDate1,
                        scrollDate2
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
                    if (!monthList.contains(testString1)) {
                        callBackVariable = "OnInitEnd"
                        Log.e("CallBack", "callBackVariable value: $callBackVariable ")

                        calendarVM.callTaskDates(
                            "01-$endMonthValue-${lastDate.year}",
                            "${lastDate.yearMonth.lengthOfMonth()}-$endMonthValue-${lastDate.year}"
                        )
                    }
                    if (!monthList.contains(testString2)) {
                        callBackVariable = "OnInitStart"
                        Log.e("CallBack", "callBackVariable value: $callBackVariable ")

                        calendarVM.callTaskDates(
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


//        binding.calendarView.addOnItemTouchListener(object :
//            RecyclerView.SimpleOnItemTouchListener() {
//
//            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
//
////                if(rv.scrollState == RecyclerView.SCROLL_STATE_DRAGGING)
////                    return true
//
//
//                when (e.action) {
//                    MotionEvent.ACTION_DOWN -> {
//                        downY = e.y
//                        downX = e.x
//                    }
//                    MotionEvent.ACTION_UP -> {
//
//                        upY = e.y
//                        upX = e.x
//                        val deltaY = downY - upY
//
//                        Log.d(TAG, "onInterceptTouchEvent: " + abs(downX - upX))
//
//
//                        if (kotlin.math.abs(deltaY) > MIN_DISTANCE) {
//
//                            if (deltaY < 0) {
//                                if (!expanded) {
//                                    expandCollapse(weekToMonth = true, false)
//                                    return true
//                                } else {
//                                    return rv.scrollState == RecyclerView.SCROLL_STATE_DRAGGING
//                                }
//
//                            }
//                            if (deltaY > 0) {
//                                if (expanded) {
//                                    expandCollapse(weekToMonth = false, false)
//                                    return true
//                                } else {
//                                    return rv.scrollState == RecyclerView.SCROLL_STATE_DRAGGING
//                                }
//
//                            }
//
//                            return true
//                        }
//
//                    }
//
//                }
//                return false
//            }
//        })


        /**
         * Collapse calendar if expanded when scrolled
         */
        binding.recView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            var ydy = 0

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val offset = dy - ydy
                ydy = dy

                val hide =
                    binding.recView.scrollState == RecyclerView.SCROLL_STATE_DRAGGING && offset > 10

                if (hide && expanded) {
                    expandCollapse(weekToMonth = false, false)
                }
            }
        })

//        binding.btnAddTask.setOnClickListener {
//            startActivity(CreateTaskActivity::class.java)
//        }

        /**
         * General Observer
         */
        calendarVM.observedChanges().observe(this) { event ->
            event?.getContentIfNotHandled()?.let {
                when (it) {
                    Constants.VISIBLE -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }

                    Constants.HIDE -> {
                        binding.progressBar.visibility = View.GONE
                    }

                    Constants.NAVIGATE -> {
                        getTaskList(selectedDate)
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

        calendarVM.observeDateList().observe(this) { event ->
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
                            if (temp != null) {
                                convertTimestampToLocalDate(temp.toLocalDate())
                            }
                        }

                        binding.calendarView.notifyCalendarChanged()
                    }


                }
            }
        }
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
            Log.d("DebugTomorrow", "Calender Frag DateCheck: $dateString tomorrow ")

            callBackVariable = "OnDate"

            calendarVM.fetchTasksLiveData(dateString).observe(viewLifecycleOwner) {
                lifecycleScope.launch {
                    adapter.submitData(it)
                }
            }
        } else {
            showMessage(getString(R.string.no_internet))
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

    private fun startLoginProcess() {

        qbUser = QBUser()
        Log.e("QB User Calender Fragment", Gson().toJson(qbUser))

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

//    private fun updateUser(user: QBUser) {
//        binding.progressBar.visibility = View.VISIBLE
//
//        ChatHelper.updateUser(user, object : QBEntityCallback<QBUser> {
//            override fun onSuccess(qbUser: QBUser, bundle: Bundle?) {
////                showMessage("Logged in as " + user.fullName)
//                loginToChat(user)
//            }
//
//            override fun onError(e: QBResponseException) {
//                binding.progressBar.visibility = View.GONE
//                longToast(R.string.login_chat_login_error)
//                performLogout()
//
//            }
//        })
//    }

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

    override fun confirmTask(place: String) {
//        Toast.makeText(context, "new vick place -=-==--=> $place", Toast.LENGTH_SHORT).show()

        if (place != null && place.isNotEmpty()) {
            var result = place.filter { it.isDigit() }
//            Log.e("09/01 result -=-=-=-=>", result.toString())
//            Log.e("09/01 result -=-=-=-=>", Gson().toJson(adapter.snapshot().toMutableList()))

            /*  var list: MutableList<TaskDetailsResponseModel.Data?> = mutableListOf()
              list = adapter.snapshot().toMutableList()!!;
              Log.e("09/01 list list -=-=-=-=>", Gson().toJson(list))

              if (result != null && result.isNotEmpty()) {

                  val item = list.find { it?.id == result.toIntOrNull() }
                  list.remove(item)
                  Log.e("09/01 list list 22222 -=-=-=-=>", Gson().toJson(list))
              }

              lifecycleScope.launch {
                  adapter.submitData(PagingData<list>)
              }

              adapter.notifyDataSetChanged()*/

            if (result != null && result.isNotEmpty()) {

                result.toIntOrNull()?.let { calendarVM.callTaskChangeStatus(it) }

            } else {
                showMessage(getString(R.string.something_went_wrong))
            }
        }

    }

    override fun onBottomSheetDismissed() {
        Log.e("CallBack", "CallBack from Calender Fragment")
        Log.e("CallBack", "callBackVariable value: $callBackVariable ")

        if (callBackVariable == "OnDate") {
            calendarVM.callTaskDates(
                scrollDate1,
                scrollDate2
            )

            calendarVM.fetchTasksLiveData(dateString).observe(viewLifecycleOwner) {
                lifecycleScope.launch {
                    adapter.submitData(it)
                }
            }
        }
        if (callBackVariable == "OnInitEnd") {
            calendarVM.callTaskDates(
                "01-$endMonthValue-${lastDate.year}",
                "${lastDate.yearMonth.lengthOfMonth()}-$endMonthValue-${lastDate.year}"
            )
            calendarVM.fetchTasksLiveData(dateString).observe(viewLifecycleOwner) {
                lifecycleScope.launch {
                    adapter.submitData(it)
                }
            }
        }
        if (callBackVariable == "OnInitStart") {
            calendarVM.callTaskDates(
                "01-$startMonthValue-${firstDate.year}",
                "${firstDate.yearMonth.lengthOfMonth()}-$startMonthValue-${firstDate.year}"
            )
            calendarVM.fetchTasksLiveData(dateString).observe(viewLifecycleOwner) {
                lifecycleScope.launch {
                    adapter.submitData(it)
                }
            }
        }
        if (callBackVariable == "TestString") {
            calendarVM.callTaskDates(
                scrollDate1,
                scrollDate2
            )
            calendarVM.fetchTasksLiveData(dateString).observe(viewLifecycleOwner) {
                lifecycleScope.launch {
                    adapter.submitData(it)
                }
            }
        }
    }

}