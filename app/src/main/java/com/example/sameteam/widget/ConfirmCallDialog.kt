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
import androidx.fragment.app.DialogFragment
import com.example.sameteam.R
import com.google.android.material.button.MaterialButton

class ConfirmCallDialog(context: Context, val type: String, var onItemClick: ((Boolean) -> Unit)?): DialogFragment() {

    var mContext = context
    lateinit var btnConfirm : MaterialButton
    lateinit var btnCancel : MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = activity?.layoutInflater?.inflate(R.layout.dialog_call_confirm,null)

        if (dialog != null && dialog!!.window != null) {
            dialog!!.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
            dialog!!.window?.requestFeature(Window.FEATURE_NO_TITLE);
        }

        if (view != null) {
            btnConfirm = view.findViewById(R.id.btnConfirm)
            btnCancel = view.findViewById(R.id.btnCancel)
        }

        if(type == mContext.getString(R.string.voice_call)){
            btnConfirm.text = mContext.getString(R.string.voice_call)
        }
        else if(type == mContext.getString(R.string.video_call)){
            btnConfirm.text = mContext.getString(R.string.video_call)
        }
        else{
            btnConfirm.visibility = View.GONE
        }

        btnConfirm.setOnClickListener {
            onItemClick?.invoke(true);
            dismiss()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        return view
    }
}