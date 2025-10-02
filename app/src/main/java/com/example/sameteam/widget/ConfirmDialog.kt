package com.example.sameteam.widget

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.sameteam.R
import com.google.android.material.button.MaterialButton

class ConfirmDialog(context: Context, val msg: String, val place: String) : DialogFragment() {

    var mContext = context

    interface ConfirmClickListener {
        fun onConfirm(place: String)
    }

    var confirmClickListener: ConfirmClickListener?=null

    lateinit var btnConfirm: MaterialButton
    lateinit var btnCancel: MaterialButton
    lateinit var message: TextView

    fun setConfirmClickListener(confirmClickListener: ConfirmClickListener): Void? {

        this.confirmClickListener = confirmClickListener;
        return null;

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = activity?.layoutInflater?.inflate(R.layout.dialog_confirm, null)

        if (dialog != null && dialog!!.window != null) {
            dialog!!.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
            dialog!!.window?.requestFeature(Window.FEATURE_NO_TITLE);
        }

        if (view != null) {
            btnConfirm = view.findViewById(R.id.btnConfirm)
            btnCancel = view.findViewById(R.id.btnCancel)
            message = view.findViewById(R.id.alertMessage)
            message.text = msg
        }

        if (place == "AlreadyCompleted") {
            btnConfirm.text = "Ok"
            btnCancel.visibility = View.GONE
        }

        btnConfirm.setOnClickListener {
            if (confirmClickListener == null) {
                confirmClickListener = (activity as ConfirmClickListener?)!!
            }
            confirmClickListener?.onConfirm(place)
            dismiss()
        }

        btnCancel.setOnClickListener {
//            Toast.makeText(requireContext(),"Cancel",Toast.LENGTH_SHORT).show()
            dismiss()
        }

        return view
    }
}