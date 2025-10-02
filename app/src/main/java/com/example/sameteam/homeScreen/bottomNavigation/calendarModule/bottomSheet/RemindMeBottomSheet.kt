package com.example.sameteam.homeScreen.bottomNavigation.calendarModule.bottomSheet

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.sameteam.R
import com.example.sameteam.databinding.BottomSheetRemindMeBinding
import com.example.sameteam.databinding.RemindMeCardLayoutBinding
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.RemindMeModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class RemindMeBottomSheet(private val selectedItems: ArrayList<RemindMeModel>) :
    BottomSheetDialogFragment() {

    //Implemented in CreateTaskActivity
    interface RemindMeListener {
        fun onRemind(selectedItems: ArrayList<String>)
    }

    lateinit var remindMeListener: RemindMeListener

    lateinit var binding: BottomSheetRemindMeBinding
    val listItems = ArrayList<String>()
    private val unitMap = hashMapOf("m" to "minutes", "h" to "hours", "d" to "day")
    val selectedStringList = ArrayList<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,

        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.bottom_sheet_remind_me, container, false)

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


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listItems.add(getString(R.string.five_mins))
        listItems.add(getString(R.string.ten_mins))
        listItems.add(getString(R.string.fifteen_mins))
        listItems.add(getString(R.string.thirty_mins))
        listItems.add(getString(R.string.fortyfive_mins))
        listItems.add(getString(R.string.one_hour))
        listItems.add(getString(R.string.two_hours))
        listItems.add(getString(R.string.one_day))

        for (item in selectedItems) {
            if (item.value == "1" && item.unit == "h")
                selectedStringList.add("1 hour")
            else {
                val temp = "${item.value} ${unitMap.getValue(item.unit.toString())}"
                selectedStringList.add(temp)
            }
        }

        binding.recView.adapter = MyAdapter(requireContext(), listItems, selectedStringList)

        binding.btnCross.setOnClickListener {
            dismiss()
        }

        binding.btnBack.setOnClickListener {
            dismiss()
        }

        binding.txtNotRemind.setOnClickListener {
            remindMeListener = context as RemindMeListener
            remindMeListener.onRemind(arrayListOf(getString(R.string.dont_remind)))
            dismiss()
        }

        binding.btnDone.setOnClickListener {
            remindMeListener = context as RemindMeListener
            remindMeListener.onRemind(MyAdapter.mStringList)
            dismiss()
        }

    }

    class MyAdapter(
        val context: Context,
        val items: ArrayList<String>,
        selectedStringList: ArrayList<String>
    ) : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

        companion object{
            lateinit var mStringList: ArrayList<String>
        }

        init {
            mStringList = selectedStringList
        }

        private val unitMapReverse =
            hashMapOf("minutes" to "m", "hour" to "h", "hours" to "h", "day" to "d")

        class MyViewHolder(val binding: RemindMeCardLayoutBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            return MyViewHolder(
                DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.remind_me_card_layout,
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.binding.textValue.text = items[position]

            if (mStringList.contains(items[position])) {
                setSelected(
                    holder.binding.parent,
                    holder.binding.textValue,
                    holder.binding.btnChecked
                )
            } else {
                setUnselected(
                    holder.binding.parent,
                    holder.binding.textValue,
                    holder.binding.btnChecked
                )
            }

            holder.binding.parent.setOnClickListener {
                if (holder.binding.parent.tag == "0")
                    setSelected(
                        holder.binding.parent,
                        holder.binding.textValue,
                        holder.binding.btnChecked
                    )
                else
                    setUnselected(
                        holder.binding.parent,
                        holder.binding.textValue,
                        holder.binding.btnChecked
                    )

            }

        }

        private fun setSelected(background: RelativeLayout, txt: TextView, value: ImageView) {
            background.tag = "1"
            background.setBackgroundResource(R.color.lightBlue)
            txt.typeface = ResourcesCompat.getFont(context, R.font.avenirnext_demibold)
            txt.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
            value.visibility = View.VISIBLE

            val splitString = txt.text.toString().split(" ").toTypedArray()
            val temp = "${splitString[0]} ${unitMapReverse.getValue(splitString[1])}"
            mStringList.add(temp)
        }

        private fun setUnselected(background: RelativeLayout, txt: TextView, value: ImageView) {
            background.tag = "0"
            background.setBackgroundResource(R.color.white)
            txt.typeface = ResourcesCompat.getFont(context, R.font.avenirnext_regular)
            txt.setTextColor(ContextCompat.getColor(context, R.color.darkGrey))
            value.visibility = View.GONE

            val splitString = txt.text.toString().split(" ").toTypedArray()
            val temp = "${splitString[0]} ${unitMapReverse.getValue(splitString[1])}"
            mStringList.remove(temp)
        }

        override fun getItemCount(): Int {
            return items.size
        }
    }
}