package com.example.sameteam.homeScreen.bottomNavigation.chatModule.adapter

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import com.bumptech.glide.Glide
import com.example.sameteam.R
import com.example.sameteam.quickBlox.chat.ChatHelper
import com.example.sameteam.quickBlox.util.shortToast
import com.google.gson.Gson
import com.quickblox.chat.model.QBAttachment
import com.quickblox.core.QBEntityCallback
import com.quickblox.core.QBProgressCallback
import com.quickblox.core.Utils
import com.quickblox.core.exception.QBResponseException
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet


const val MAX_FILE_SIZE_100MB = 104857600

class AttachmentPreviewAdapter(
    val context: Context,
    private val attachmentCountChangedListener: AttachmentCountChangedListener,
    val errorListener: AttachmentUploadErrorListener
) : BaseAdapter() {

    private val mainThreadHandler = Handler(Looper.getMainLooper())

    private var _fileQBAttachmentMap: MutableMap<File, QBAttachment> =
        Collections.synchronizedMap(HashMap())
    val uploadedAttachments: Collection<QBAttachment>
        get() = HashSet(_fileQBAttachmentMap.values)

    private var fileUploadProgressMap: MutableMap<File, Int> =
        Collections.synchronizedMap(HashMap())
    private var fileList: MutableList<File> = ArrayList()

    fun add(item: File) {
        Log.e("19/01 item -=-=-==->", (item.isFile ?: false).toString())
        if (item.length() <= MAX_FILE_SIZE_100MB) {
            fileUploadProgressMap[item] = 1
            ChatHelper.loadFileAsAttachment(item, object : QBEntityCallback<QBAttachment> {
                override fun onSuccess(result: QBAttachment, params: Bundle?) {
                    fileUploadProgressMap.remove(item)
                    _fileQBAttachmentMap[item] = result
                    notifyDataSetChanged()
                }

                override fun onError(e: QBResponseException) {
                    Log.e("19/01 QBResponseException -=-=-==->", e.message.toString())
                    Log.e("19/01 QBResponseException 2 -=-=-==->", Gson().toJson(e.errors))
                    errorListener.onAttachmentUploadError(e)
                    remove(item)
                }
            }, QBProgressCallback { progress ->
                fileUploadProgressMap[item] = progress
                mainThreadHandler.post {
                    if (progress % 5 == 0) {
                        notifyDataSetChanged()
                    }
                }
            })
            fileList.add(item)
            attachmentCountChangedListener.onAttachmentCountChanged(count)
        } else {
            shortToast(R.string.error_attachment_size)
        }
    }

    fun remove(item: File) {
        fileUploadProgressMap.remove(item)
        _fileQBAttachmentMap.remove(item)
        fileList.remove(item)
        attachmentCountChangedListener.onAttachmentCountChanged(count)
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val holder: ViewHolder
        var modifiedView = convertView
        if (modifiedView == null) {
            holder = ViewHolder()
            modifiedView = LayoutInflater.from(context)
                .inflate(R.layout.item_attachment_preview, parent, false)
            holder.attachmentImageView = modifiedView.findViewById(R.id.image_attachment_preview)
            holder.progressBar = modifiedView.findViewById(R.id.progress_attachment_preview)
            holder.deleteButton = modifiedView.findViewById(R.id.button_attachment_preview_delete)
            modifiedView.tag = holder
        } else {
            holder = modifiedView.tag as ViewHolder
        }

        val attachmentFile = getItem(position)
        Log.e("23/01 attachmentFile -=-=-=>", attachmentFile.name)
        val width = context.resources.getDimension(R.dimen.scale_80dp).toInt()
        val height = context.resources.getDimension(R.dimen.scale_80dp).toInt()

        Log.e("23/01 attachmentFile -=-=-=>", attachmentFile.name)
        com.example.sameteam.helper.Utils.getMimeTypeExtension(
            File(attachmentFile.path)
        )
            ?.let { Log.e("23/01 attachmentFile path 111111-=-=-=>", it) }

        com.example.sameteam.helper.Utils.getMimeTypeExtension(
            File(attachmentFile.path)
        )
            ?.let {
                Log.e("23/01 attachmentFile path 22222-=-=-=>", it)

                if (it.contains("pdf")) {
                    //PDF

                    try {
                        Glide.with(context)
                            .load(R.drawable.attachment_pdf_file_placeholder)
                            .override(width, height)
                            .into(holder.attachmentImageView)
                    } catch (e: IllegalArgumentException) {
                        Log.d("AttachmentPreview", e.message!!)

                    }

                }
                else if (it.contains("doc") || it.contains("docx")) {
                    //doc or docx

                    try {
                        Glide.with(context)
                            .load(R.drawable.attachment_file_placeholder)
                            .override(width, height)
                            .into(holder.attachmentImageView)
                    } catch (e: IllegalArgumentException) {
                        Log.d("AttachmentPreview", e.message!!)

                    }

                } else if (it.contains("xls") || it.contains("xlsx")) {
                    //doc or docx

                    try {
                        Glide.with(context)
                            .load(R.drawable.attachment_excel_file_placeholder)
                            .override(width, height)
                            .into(holder.attachmentImageView)
                    } catch (e: IllegalArgumentException) {
                        Log.d("AttachmentPreview", e.message!!)

                    }

                } else if (it.contains("csv")) {
                    //doc or docx

                    try {
                        Glide.with(context)
                            .load(R.drawable.attachment_csv_file_placeholder)
                            .override(width, height)
                            .into(holder.attachmentImageView)
                    } catch (e: IllegalArgumentException) {
                        Log.d("AttachmentPreview", e.message!!)

                    }

                } else if (it.contains("txt")) {
                    //doc or docx

                    try {
                        Glide.with(context)
                            .load(R.drawable.attachment_txt_file_placeholder)
                            .override(width, height)
                            .into(holder.attachmentImageView)
                    } catch (e: IllegalArgumentException) {
                        Log.d("AttachmentPreview", e.message!!)

                    }

                } else if (it.contains("jpeg") || it.contains("jpg") || it.contains("png")
                    || it.contains("gif")
                ) {

                    try {
                        Glide.with(context)
                            .load(attachmentFile)
                            .override(width, height)
                            .into(holder.attachmentImageView)
                    } catch (e: IllegalArgumentException) {
                        Log.d("AttachmentPreview", e.message!!)

                    }

                } else {
                    Log.e("AttachmentPreview ELSE","ELSE ofAttachmentPreview View")
                }

            }
        /*  try {
              Glide.with(context)
                  .load(attachmentFile)
                  .override(width, height)
                  .into(holder.attachmentImageView)
          } catch (e: IllegalArgumentException) {
              Log.d("AttachmentPreview", e.message!!)

          }*/

        if (isFileUploading(attachmentFile)) {
            holder.progressBar.visibility = View.VISIBLE
            holder.deleteButton.visibility = View.GONE
            holder.deleteButton.setOnClickListener(null)
            val progress = fileUploadProgressMap[attachmentFile]!!
            holder.progressBar.progress = progress
        } else {
            holder.progressBar.visibility = View.GONE
            holder.deleteButton.visibility = View.VISIBLE
            holder.deleteButton.setOnClickListener { remove(attachmentFile) }
        }
        return modifiedView!!
    }

    fun remove(qbAttachment: QBAttachment) {
        if (_fileQBAttachmentMap.containsValue(qbAttachment)) {
            for (file in _fileQBAttachmentMap.keys) {
                val attachment = _fileQBAttachmentMap[file]
                if (attachment == qbAttachment) {
                    remove(file)
                    break
                }
            }
        }
    }

    private fun isFileUploading(attachmentFile: File): Boolean {
        return fileUploadProgressMap.containsKey(attachmentFile) && !_fileQBAttachmentMap.containsKey(
            attachmentFile
        )
    }

    override fun getItem(position: Int): File {
        return fileList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return fileList.size
    }

    private class ViewHolder {
        lateinit var attachmentImageView: ImageView
        lateinit var progressBar: ProgressBar
        lateinit var deleteButton: ImageButton
    }

    interface AttachmentCountChangedListener {
        fun onAttachmentCountChanged(count: Int)
    }

    interface AttachmentUploadErrorListener {
        fun onAttachmentUploadError(e: QBResponseException)
    }
}