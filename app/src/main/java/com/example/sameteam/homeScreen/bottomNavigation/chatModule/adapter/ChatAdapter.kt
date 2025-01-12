package com.example.sameteam.homeScreen.bottomNavigation.chatModule.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Outline
import android.graphics.drawable.Drawable
import android.media.ThumbnailUtils
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.sameteam.R
import com.example.sameteam.databinding.ListItemMessageLeftBinding
import com.example.sameteam.databinding.ListItemMessageRightBinding
import com.example.sameteam.databinding.ListItemNotificationMessageBinding
import com.example.sameteam.helper.Constants.CHAT_RECEIVED_VIEW_TYPE
import com.example.sameteam.helper.Constants.CHAT_SENT_VIEW_TYPE
import com.example.sameteam.helper.Constants.CUSTOM_VIEW_TYPE
import com.example.sameteam.helper.Constants.FILE_DOWNLOAD_ATTEMPS_COUNT
import com.example.sameteam.helper.Constants.TYPE_ATTACH_LEFT
import com.example.sameteam.helper.Constants.TYPE_ATTACH_RIGHT
import com.example.sameteam.helper.Constants.TYPE_NOTIFICATION_CENTER
import com.example.sameteam.helper.Utils
import com.example.sameteam.helper.getDateAsHeaderId
import com.example.sameteam.homeScreen.bottomNavigation.chatModule.activity.ChatActivity
import com.example.sameteam.quickBlox.PaginationHistoryListener
import com.example.sameteam.quickBlox.chat.ChatHelper
import com.example.sameteam.quickBlox.db.QbUsersDbManager
import com.example.sameteam.quickBlox.managers.PROPERTY_NOTIFICATION_TYPE
import com.example.sameteam.quickBlox.util.shortToast
import com.quickblox.chat.model.QBAttachment
import com.quickblox.chat.model.QBChatDialog
import com.quickblox.chat.model.QBChatMessage
import com.quickblox.chat.model.QBDialogType
import com.quickblox.content.QBContent
import com.quickblox.content.model.QBFile
import com.quickblox.core.QBEntityCallback
import com.quickblox.core.QBProgressCallback
import com.quickblox.core.exception.QBResponseException
import com.quickblox.core.helper.CollectionsUtil
import com.quickblox.users.model.QBUser
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import kotlinx.coroutines.*
import org.jivesoftware.smack.SmackException
import org.jivesoftware.smack.XMPPException
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    var context: Context,
    val chatDialog: QBChatDialog,
    val chatMessages: MutableList<QBChatMessage>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    StickyRecyclerHeadersAdapter<RecyclerView.ViewHolder> {

    private val TAG = "ChatAdapter"

    private var paginationListener: PaginationHistoryListener? = null
    private var attachImageClickListener: AttachClickListener? = null
    private var previousGetCount = 0
    private var fileLoadingAttemptsMap = HashMap<String, Int>()

    interface AttachClickListener {
        fun onAttachmentClicked(itemViewType: Int?, view: View, attachment: QBAttachment)
    }


    class SenderVH(val bindingSend: ListItemMessageRightBinding) :
        RecyclerView.ViewHolder(bindingSend.root)

    class ReceiverVH(val bindingRecv: ListItemMessageLeftBinding) :
        RecyclerView.ViewHolder(bindingRecv.root)

    class NotificationVH(val bindingNoti: ListItemNotificationMessageBinding) :
        RecyclerView.ViewHolder(bindingNoti.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            CHAT_SENT_VIEW_TYPE, TYPE_ATTACH_RIGHT -> {
                SenderVH(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.list_item_message_right,
                        parent,
                        false
                    )
                )
            }
            CHAT_RECEIVED_VIEW_TYPE, TYPE_ATTACH_LEFT -> {
                ReceiverVH(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.list_item_message_left,
                        parent,
                        false
                    )
                )
            }
            else -> {
                NotificationVH(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.list_item_notification_message,
                        parent,
                        false
                    )
                )
            }
        }

    }

    override fun getItemCount(): Int {
        return chatMessages.size
    }

    private fun getItem(position: Int): QBChatMessage? {
        if (position <= itemCount - 1) {
            return chatMessages[position]
        } else {
            return null
        }
    }

    fun addMessages(items: List<QBChatMessage>) {
        chatMessages.addAll(0, items)
        notifyItemRangeInserted(0, items.size)
    }

    fun setMessages(items: List<QBChatMessage>) {
        chatMessages.clear()
        chatMessages.addAll(items)
        notifyDataSetChanged()
    }

    fun addMessage(item: QBChatMessage) {
        this.chatMessages.add(item)
        this.notifyItemInserted(chatMessages.size - 1)
    }

    fun getMessages(): List<QBChatMessage> {
        return chatMessages
    }

    override fun getItemViewType(position: Int): Int {
        val chatMessage = chatMessages[position]

        var itemViewType = CUSTOM_VIEW_TYPE
        chatMessage.let {
            if (chatMessage.getProperty(PROPERTY_NOTIFICATION_TYPE) != null) {
                itemViewType = TYPE_NOTIFICATION_CENTER
            } else if (chatMessage.attachments?.isNotEmpty() == true) {
                val attachment = getAttachment(position)
                val photo = QBAttachment.PHOTO_TYPE.equals(attachment?.type, ignoreCase = true)
                val image = QBAttachment.IMAGE_TYPE.equals(attachment?.type, ignoreCase = true)
                val video = QBAttachment.VIDEO_TYPE.equals(attachment?.type, ignoreCase = true)
                val audio = QBAttachment.AUDIO_TYPE.equals(attachment?.type, ignoreCase = true)
                val file =
                    attachment?.type == "file" || attachment?.type!!.contains("file") || attachment.type == ""

                if (photo || image || video || audio || file) {
                    if (isIncoming(chatMessage)) {
                        itemViewType = TYPE_ATTACH_LEFT
                    } else {
                        itemViewType = TYPE_ATTACH_RIGHT
                    }
                }
            } else if (isIncoming(chatMessage)) {
                itemViewType = CHAT_RECEIVED_VIEW_TYPE
            } else {
                itemViewType = CHAT_SENT_VIEW_TYPE
            }
        }

        return itemViewType
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        downloadMore(position)

        val chatMessage = chatMessages[position]
        if (chatMessage != null && isIncoming(chatMessage) && !isReadByCurrentUser(chatMessage)) {
            readMessage(chatMessage)
        }

        when (getItemViewType(position)) {
            CHAT_RECEIVED_VIEW_TYPE -> {
                onBindViewMsgHolder(holder, chatMessage, true)
            }
            CHAT_SENT_VIEW_TYPE -> {
                onBindViewMsgHolder(holder, chatMessage, false)
            }
            TYPE_ATTACH_LEFT -> {
                onBindViewAttachHolder(holder, chatMessage, position, true)
            }
            TYPE_ATTACH_RIGHT -> {
                onBindViewAttachHolder(holder, chatMessage, position, false)
            }
            TYPE_NOTIFICATION_CENTER -> {
                onBindViewNotificationHolder(holder as NotificationVH, chatMessage)
            }
            else -> {
                Log.d(TAG, "onBindViewHolder TYPE_ATTACHMENT_CUSTOM")
            }
        }
    }

    private fun onBindViewNotificationHolder(holder: NotificationVH, chatMessage: QBChatMessage?) {
        chatMessage?.let {
            holder.bindingNoti.tvMessageBody.text = chatMessage.body
            holder.bindingNoti.tvTimeSent.text = getTime(chatMessage.dateSent)
        }
    }

    private fun onBindViewMsgHolder(
        holder: RecyclerView.ViewHolder,
        chatMessage: QBChatMessage?,
        isIncomingMessage: Boolean
    ) {
        chatMessage?.let {
            if (isIncomingMessage && (holder is ReceiverVH)) {
                val newBinding = holder.bindingRecv

                if (chatDialog.type != QBDialogType.PRIVATE) {
                    newBinding.tvUserName.visibility = View.VISIBLE
                    newBinding.tvUserName.text = getSenderName(chatMessage)

//                    val user = getQBUser(chatMessage)
//                    loadAvatar(newBinding, chatMessage, user)
                } else {
                    newBinding.tvUserName.visibility = View.GONE
                    newBinding.rlAvatarContainer.visibility = View.GONE
                }

                fillTextMessageHolder(
                    chatMessage,
                    newBinding.rlImageAttachContainer,
                    newBinding.llMessageBodyContainer,
                    newBinding.tvMessageBody,
                    newBinding.tvTimeSent
                )
            } else {
                val newBinding = (holder as SenderVH).bindingSend

                newBinding.tvUserName.visibility = View.GONE
                newBinding.rlAvatarContainer.visibility = View.GONE

                fillTextMessageHolder(
                    chatMessage,
                    newBinding.rlImageAttachContainer,
                    newBinding.llMessageBodyContainer,
                    newBinding.tvMessageBody,
                    newBinding.tvTimeSent
                )

                setMessageStatus(newBinding, chatMessage)
            }
        }
    }


    private fun fillTextMessageHolder(
        chatMessage: QBChatMessage, imageContainer: View, msgContainer: View,
        message: TextView, time: TextView
    ) {
        imageContainer.visibility = View.GONE
        msgContainer.visibility = View.VISIBLE
        message.text = chatMessage.body
        time.text = getTime(chatMessage.dateSent)

    }


    private fun onBindViewAttachHolder(
        holder: RecyclerView.ViewHolder,
        chatMessage: QBChatMessage?,
        position: Int,
        isIncomingMessage: Boolean
    ) {
        chatMessage?.let {
            if (isIncomingMessage) {
                (holder as ReceiverVH).bindingRecv.tvTimeSent.text = getTime(chatMessage.dateSent)
            } else {
                (holder as SenderVH).bindingSend.tvTimeSent.text = getTime(chatMessage.dateSent)
            }

            fillAttachHolder(holder, chatMessage, position, isIncomingMessage)
        }
    }

    private fun fillAttachHolder(
        holder: RecyclerView.ViewHolder,
        chatMessage: QBChatMessage,
        position: Int,
        isIncomingMessage: Boolean
    ) {

        if (isIncomingMessage && (holder is ReceiverVH)) {
            val newBinding = holder.bindingRecv

            if (chatDialog.type != QBDialogType.PRIVATE) {
                newBinding.tvUserName.visibility = View.VISIBLE
                newBinding.tvUserName.text = getSenderName(chatMessage)


//                    val user = getQBUser(chatMessage)
//                    loadAvatar(newBinding, chatMessage, user)
            } else {
                newBinding.tvUserName.visibility = View.GONE
                newBinding.rlAvatarContainer.visibility = View.GONE
            }

            displayAttachment(
                holder,
                position,
                newBinding.llMessageBodyContainer,
                newBinding.rlImageAttachContainer,
                newBinding.ivAttachImagePreview,
                newBinding.rlVideoAttachContainer,
                newBinding.rlFileAttachContainer,
                newBinding.tvAttachVideoName,
                newBinding.tvAttachVideoSize,
                newBinding.tvAttachFileName,
                newBinding.tvAttachFileSize,
                newBinding.videoAttachProgress,
                newBinding.ivAttachVideoPreview,
                newBinding.fileAttachProgress,
                newBinding.rlFileAttachPreviewContainer
            )

            setItemAttachClickListener(
                getAttachListenerByType(position),
                newBinding.llRootMessageItem,
                newBinding.llMessageContainer,
                getAttachment(position),
                position
            )
        } else {
            val newBinding = (holder as SenderVH).bindingSend

            newBinding.tvUserName.visibility = View.GONE
            newBinding.rlAvatarContainer.visibility = View.GONE

            displayAttachment(
                holder,
                position,
                newBinding.llMessageBodyContainer,
                newBinding.rlImageAttachContainer,
                newBinding.ivAttachImagePreview,
                newBinding.rlVideoAttachContainer,
                newBinding.rlFileAttachContainer,
                newBinding.tvAttachVideoName,
                newBinding.tvAttachVideoSize,
                newBinding.tvAttachFileName,
                newBinding.tvAttachFileSize,
                newBinding.videoAttachProgress,
                newBinding.ivAttachVideoPreview,
                newBinding.fileAttachProgress,
                newBinding.rlFileAttachPreviewContainer
            )

            setItemAttachClickListener(
                getAttachListenerByType(position),
                newBinding.llRootMessageItem,
                newBinding.llMessageContainer,
                getAttachment(position),
                position
            )

            setMessageStatus(newBinding, chatMessage)
        }


    }

    private fun displayAttachment(
        holder: RecyclerView.ViewHolder,
        position: Int,
        msgContainer: View,
        imageContainer: View,
        attachImage: ImageView,
        videoContainer: View,
        fileContainer: View,
        videoName: TextView,
        videoSize: TextView,
        fileName: TextView,
        fileSize: TextView,
        videoProgressBar: ProgressBar,
        videoImageView: ImageView,
        fileProgressBar: ProgressBar,
        rlFileAttachPreviewContainer: RelativeLayout
    ) {

        val attachment = getAttachment(position)
        attachment?.let {
            val photo = QBAttachment.PHOTO_TYPE.equals(attachment.type, ignoreCase = true)
            val image = QBAttachment.IMAGE_TYPE.equals(attachment.type, ignoreCase = true)
            val video = QBAttachment.VIDEO_TYPE.equals(
                attachment.type,
                ignoreCase = true
            ) || attachment.type.contains("video")
            val file =
                attachment.type == "file" || attachment.type.contains("file") || attachment.type == ""


            when {
                photo || image -> {
                    msgContainer.visibility = View.GONE
                    videoContainer.visibility = View.GONE
                    fileContainer.visibility = View.GONE
                    imageContainer.visibility = View.VISIBLE

//                    val forwardedFromName = chatMessage.getProperty(PROPERTY_FORWARD_USER_NAME) as String?
//                    if (forwardedFromName != null) {
//                        holder.llImageForwardContainer?.visibility = View.VISIBLE
//                        holder.tvImageForwardedFromUser?.text = forwardedFromName
//                    } else {
//                        holder.llImageForwardContainer?.visibility = View.GONE
//                    }

                    val imageUrl = QBFile.getPrivateUrlForUID(attachment.id)
                    Log.d(TAG, "displayAttachment: $imageUrl")

                    Glide.with(context)
                        .load(imageUrl)
                        .listener(getRequestListener(holder))
                        .into(attachImage)

                    makeRoundedCorners(attachImage, false)
                }
                video -> {
                    msgContainer.visibility = View.GONE
                    videoContainer.visibility = View.VISIBLE
                    fileContainer.visibility = View.GONE
                    imageContainer.visibility = View.GONE

                    videoName.text = attachment.name
                    videoSize.text = android.text.format.Formatter.formatShortFileSize(
                        context,
                        attachment.size.toLong()
                    )

//                    val forwardedFromName = chatMessage.getProperty(PROPERTY_FORWARD_USER_NAME) as String?
//                    if (forwardedFromName != null) {
//                        holder.llVideoForwardContainer?.visibility = View.VISIBLE
//                        holder.llVideoForwardedFromUser?.text = forwardedFromName
//                    } else {
//                        holder.llVideoForwardContainer?.visibility = View.GONE
//                    }

                    fileLoadingAttemptsMap.put(attachment.id, 0)

                    if (attachment.name == null) {
                        return
                    }
                    val fName = attachment.name
                    val newFile = File(context.filesDir, fName)

                    if (newFile.exists()) {
                        fillVideoFileThumb(newFile, videoProgressBar, videoImageView, position)
                    } else {
                        loadFileFromQB(videoProgressBar, attachment, newFile, position)
                    }
                }

                file -> {
                    msgContainer.visibility = View.GONE
                    videoContainer.visibility = View.GONE
                    fileContainer.visibility = View.VISIBLE
                    imageContainer.visibility = View.GONE

                    if (attachment.name.contains("pdf")) {
                        rlFileAttachPreviewContainer.setBackgroundResource(R.drawable.attachment_pdf_file_placeholder)
                    } else if (attachment.name.contains("txt")) {
                        rlFileAttachPreviewContainer.setBackgroundResource(R.drawable.attachment_txt_file_placeholder)
                    } else if (attachment.name.contains("xls")) {
                        rlFileAttachPreviewContainer.setBackgroundResource(R.drawable.attachment_excel_file_placeholder)
                    } else if (attachment.name.contains("csv")) {
                        rlFileAttachPreviewContainer.setBackgroundResource(R.drawable.attachment_csv_file_placeholder)
                    } else {
                        rlFileAttachPreviewContainer.setBackgroundResource(R.drawable.attachment_file_placeholder)
                    }

                    fileName.text = attachment.name
                    fileSize.text = android.text.format.Formatter.formatShortFileSize(
                        context,
                        attachment.size.toLong()
                    )

//                    val forwardedFromName = chatMessage.getProperty(PROPERTY_FORWARD_USER_NAME) as String?
//                    if (forwardedFromName != null) {
//                        holder.llFileForwardContainer?.visibility = View.VISIBLE
//                        holder.llFileForwardedFromUser?.text = forwardedFromName
//                    } else {
//                        holder.llFileForwardContainer?.visibility = View.GONE
//                    }

                    fileLoadingAttemptsMap.put(attachment.id, 0)

                    val fName = attachment.name
                    val newFile = File(context.filesDir, fName)

                    if (!newFile.exists()) {
                        loadFileFromQB(fileProgressBar, attachment, newFile, position)
                    }
                }
                else -> {
                    Toast.makeText(context, "Unknown Attachment Received", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun fillVideoFileThumb(
        file: File,
        progressBar: ProgressBar,
        videoImageView: ImageView,
        position: Int
    ) {

        val bitmap: Bitmap?

        if (Build.VERSION.SDK_INT >= 29) {
            val ca = CancellationSignal()
            val mSize = Size(96, 96)

            bitmap = ThumbnailUtils.createVideoThumbnail(file, mSize, ca)
        } else {
            bitmap = ThumbnailUtils.createVideoThumbnail(
                file.path,
                MediaStore.Video.Thumbnails.MINI_KIND
            )
        }


        val attachment = getAttachment(position)
        var attempts = fileLoadingAttemptsMap.get(attachment!!.id)
        if (attempts == null) {
            attempts = 0
        }

        if (bitmap == null && attempts <= FILE_DOWNLOAD_ATTEMPS_COUNT) {
            Log.d(TAG, "Thumbnail Bitmap is null from Downloaded File " + file.path)
            file.delete()
            Log.d(TAG, "Delete file and Reload")
            loadFileFromQB(progressBar, attachment, file, position)
        } else {
            videoImageView.setImageBitmap(bitmap)
            progressBar.visibility = View.GONE

//            makeRoundedCorners(holder.ivImageAttachPreview, true)
        }
    }

    private fun loadFileFromQB(
        progressBar: ProgressBar,
        attachment: QBAttachment?,
        file: File,
        position: Int
    ) {
        progressBar.visibility = View.VISIBLE
        Log.d(TAG, "Loading File as Attachment id = " + attachment?.id)

        // to define download attempts count for each videofile
        if (attachment != null) {
            val attachmentID = attachment.id
            val attempts = fileLoadingAttemptsMap.get(attachmentID)
            fileLoadingAttemptsMap.set(attachmentID, (attempts!! + 1))

            QBContent.downloadFile(attachmentID, object : QBProgressCallback {
                override fun onProgressUpdate(progress: Int) {
                    progressBar.progress = progress
                    Log.d(TAG, "Loading progress updated: $progress")
                }
            }, null).performAsync(object : QBEntityCallback<InputStream> {
                override fun onSuccess(inputStream: InputStream?, p1: Bundle?) {
                    Log.d(TAG, "Loading File as Attachment Successful")
                    if (inputStream != null) {
//                        LoaderAsyncTask(file, inputStream, holder, position).execute()
                    }
                }

                override fun onError(e: QBResponseException?) {
                    Log.d(TAG, e?.message!!)
                    progressBar.visibility = View.GONE
                }
            })
        }
    }

    private fun loadAvatar(
        binding: ListItemMessageLeftBinding,
        chatMessage: QBChatMessage,
        user: QBUser?
    ) {

        if (user != null && !user.customData.isNullOrBlank()) {
            val newUser = Utils.getUserFromQBUser(user.customData)

            if (!newUser.profile_picture.isNullOrBlank()) {

                binding.civAvatar.visibility = View.VISIBLE
                binding.initials.visibility = View.GONE

                Glide.with(context)
                    .load(newUser.profile_picture)
                    .error(R.drawable.profile_photo)
                    .placeholder(R.drawable.profile_photo)
                    .circleCrop()
                    .into(binding.civAvatar)
            } else {
                loadInitials(binding, chatMessage)
            }
        } else {
            loadInitials(binding, chatMessage)
        }
    }

    private fun loadInitials(binding: ListItemMessageLeftBinding, chatMessage: QBChatMessage) {

        val fullName = getSenderName(chatMessage)
        binding.initials.visibility = View.VISIBLE
        binding.civAvatar.visibility = View.GONE

        val textArray = fullName.split(" ").toTypedArray()
        if (textArray.size < 2) {
            if (textArray[0].length < 2) binding.initials.text =
                textArray[0][0].toString().uppercase(Locale.getDefault())
            else binding.initials.text = textArray[0].substring(0, 2).uppercase(Locale.getDefault())
        } else {
            binding.initials.text =
                "${textArray[0][0].uppercaseChar()}${textArray[1][0].uppercaseChar()}"
        }
    }

    private fun makeRoundedCorners(imageView: ImageView?, onlyTopCorners: Boolean) {
        val cornerRadius = 30f

        if (imageView != null) {
            imageView.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View?, outline: Outline?) {
                    if (onlyTopCorners) {
                        outline?.setRoundRect(
                            0,
                            0,
                            view!!.width,
                            (view.height + cornerRadius).toInt(),
                            cornerRadius
                        )
                    } else {
                        outline?.setRoundRect(0, 0, view!!.width, view.height, cornerRadius)
                    }
                }
            }
            imageView.clipToOutline = true
        }

    }

    private fun setMessageStatus(
        bindingSend: ListItemMessageRightBinding,
        chatMessage: QBChatMessage
    ) {
        val read = isRead(chatMessage)
        val delivered = isDelivered(chatMessage)
        if (read) {
            bindingSend.ivMessageStatus.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_status_read
                )
            )

        } else if (delivered) {
            bindingSend.ivMessageStatus.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_status_delivered
                )
            )
        } else {
            bindingSend.ivMessageStatus.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_status_sent
                )
            )
        }
    }


    private fun getRequestListener(holder: RecyclerView.ViewHolder): RequestListener<Drawable> {
        return ImageLoadListener(holder)
    }

    private inner class ImageLoadListener<P>(holder: RecyclerView.ViewHolder) : RequestListener<P> {

        val attachImage: ImageView
        val progressBar: ProgressBar

        init {

            if (holder is ReceiverVH) {
                progressBar = holder.bindingRecv.pbAttachImage
                attachImage = holder.bindingRecv.ivAttachImagePreview
            } else {
                progressBar = (holder as SenderVH).bindingSend.pbAttachImage
                attachImage = holder.bindingSend.ivAttachImagePreview
            }
            progressBar.visibility = View.VISIBLE
        }

        override fun onResourceReady(
            resource: P,
            model: Any?,
            target: Target<P>?,
            dataSource: DataSource?,
            isFirstResource: Boolean
        ): Boolean {
            attachImage.scaleType = ImageView.ScaleType.CENTER_CROP
            progressBar.visibility = View.GONE
            return false
        }

        override fun onLoadFailed(
            e: GlideException?,
            model: Any?,
            target: Target<P>?,
            isFirstResource: Boolean
        ): Boolean {
            Log.e(TAG, "ImageLoadListener Exception= $e")
            attachImage.scaleType = ImageView.ScaleType.CENTER_CROP
            progressBar.visibility = View.GONE
            return false
        }
    }

    private fun getSenderName(chatMessage: QBChatMessage): String {
        val sender = QbUsersDbManager.getUserById(chatMessage.senderId!!)
        var fullName = ""
        if (!TextUtils.isEmpty(sender?.fullName)) {
            fullName = sender!!.fullName
        }
        return fullName
    }

    private fun getQBUser(chatMessage: QBChatMessage): QBUser? {
        return QbUsersDbManager.getUserById(chatMessage.senderId!!)
    }


    private fun readMessage(chatMessage: QBChatMessage) {
        try {
            chatDialog.readMessage(chatMessage)
        } catch (e: XMPPException) {
            Log.w(TAG, e)
        } catch (e: SmackException.NotConnectedException) {
            Log.w(TAG, e)
        }
    }

    private fun isReadByCurrentUser(chatMessage: QBChatMessage): Boolean {
        val currentUserId = ChatHelper.getCurrentUser()!!.id
        return !CollectionsUtil.isEmpty(chatMessage.readIds) && chatMessage.readIds.contains(
            currentUserId
        )
    }

    private fun isIncoming(chatMessage: QBChatMessage): Boolean {
        val currentUser = ChatHelper.getCurrentUser()
        return chatMessage.senderId != null && chatMessage.senderId != currentUser!!.id
    }

    private fun getTime(seconds: Long): String {
        val dateFormat = SimpleDateFormat("hh:mm aa", Locale.getDefault())
        return dateFormat.format(Date(seconds * 1000))
    }

    fun setPaginationHistoryListener(paginationListener: PaginationHistoryListener) {
        this.paginationListener = paginationListener
    }

    private fun downloadMore(position: Int) {
        if (position == 0) {
            if (itemCount != previousGetCount) {
                paginationListener?.downloadMore()
                previousGetCount = itemCount
            }
        }
    }

    private fun getAttachment(position: Int): QBAttachment? {
        val chatMessage = chatMessages[position]
        if (chatMessage.attachments?.iterator() != null && chatMessage.attachments.iterator()
                .hasNext()
        ) {
            return chatMessage.attachments.iterator().next()
        }
        return null
    }

    fun updateStatusDelivered(messageID: String, userId: Int) {
        for (position in chatMessages.indices) {
            val message = chatMessages[position]
            if (message.id == messageID) {
                val deliveredIds = ArrayList<Int>()
                if (message.deliveredIds != null) {
                    deliveredIds.addAll(message.deliveredIds)
                }
                deliveredIds.add(userId)
                message.deliveredIds = deliveredIds
                notifyItemChanged(position)
            }
        }
    }

    fun updateStatusRead(messageID: String, userId: Int) {
        for (position in chatMessages.indices) {
            val message = chatMessages[position]
            if (message.id == messageID) {
                val readIds = ArrayList<Int>()
                if (message.readIds != null) {
                    readIds.addAll(message.readIds)
                }
                readIds.add(userId)
                message.readIds = readIds
                notifyItemChanged(position)
            }
        }
    }

    private fun isRead(chatMessage: QBChatMessage): Boolean {
        var read = false
        val recipientId = chatMessage.recipientId
        val currentUserId = ChatHelper.getCurrentUser()!!.id
        val readIds = chatMessage.readIds ?: return false

        Log.d(TAG, "isCheckRead: ${chatMessage.readIds}  ${chatMessage.id}")
        Log.d(TAG, "isCheckDialog: ${chatDialog.occupants.toString()}")



        return if (chatDialog.type != QBDialogType.PRIVATE) {
            if (chatMessage.readIds != null && chatDialog.occupants != null) {
                chatMessage.readIds.size >= chatDialog.occupants.size
            } else {
                false
            }
        } else {
            if (chatMessage.readIds != null && chatDialog.occupants != null) {
                chatMessage.readIds.containsAll(chatDialog.occupants)
            } else {
                false
            }
        }

//        if (recipientId != null && recipientId != currentUserId && readIds.contains(recipientId)) {
//            read = true
//        } else if (readIds.size == 1 && readIds.contains(currentUserId)) {
//            read = false
//        } else if (readIds.isNotEmpty()) {
//            read = true
//        }
//        return read
    }

    private fun isDelivered(chatMessage: QBChatMessage): Boolean {
        var delivered = false
        val recipientId = chatMessage.recipientId
        val currentUserId = ChatHelper.getCurrentUser()!!.id
        val deliveredIds = chatMessage.deliveredIds ?: return false
        if (recipientId != null && recipientId != currentUserId && deliveredIds.contains(recipientId)) {
            delivered = true
        } else if (deliveredIds.size == 1 && deliveredIds.contains(currentUserId)) {
            delivered = false
        } else if (deliveredIds.isNotEmpty()) {
            delivered = true
        }
        return delivered
    }

    fun setAttachImageClickListener(clickListener: AttachClickListener) {
        attachImageClickListener = clickListener
    }

    fun removeClickListeners() {
        attachImageClickListener = null
    }

    private fun setItemAttachClickListener(
        listener: AttachClickListener?,
        rootLayout: View,
        messageContainer: View,
        qbAttachment: QBAttachment?,
        position: Int
    ) {
        listener?.let {
            rootLayout.setOnClickListener(
                ItemClickListenerFilter(
                    getItemViewType(position),
                    it,
                    messageContainer,
                    position
                )
            )
        }
    }

    private inner class ItemClickListenerFilter(
        private var itemViewType: Int?,
        private var attachClickListener: AttachClickListener,
        private var messageContainer: View,
        private var position: Int
    ) : View.OnClickListener {


        override fun onClick(view: View) {
            messageContainer.let {
                val iterator = getItem(position)?.attachments?.iterator()
                if (iterator != null && iterator.hasNext()) {
                    val attachment = iterator.next()
                    if (isAttachmentValid(attachment)) {
                        attachClickListener.onAttachmentClicked(itemViewType, it, attachment)
                    } else {
                        shortToast(context.getString(R.string.error_attachment_corrupted))
                    }
                }
            }
        }
    }

    fun isAttachmentValid(attachment: QBAttachment?): Boolean {
        var result = false
        if (attachment != null && !attachment.name.isNullOrEmpty() && attachment.type.isNotEmpty()) {
            result = true
        }
        return result
    }


    private fun getAttachListenerByType(position: Int): AttachClickListener? {
        val attachment = getAttachment(position)

        if (attachment != null) {

            Log.e("20/01 attachment.type -=-=-=>", attachment.type)

            if (QBAttachment.PHOTO_TYPE.equals(attachment.type, ignoreCase = true)
                || QBAttachment.IMAGE_TYPE.equals(attachment.type, ignoreCase = true)
            ) {
                return attachImageClickListener
            } else if ("File".equals(attachment.type, ignoreCase = true)) {
                return attachImageClickListener
            } else {
                return null
            }
        } else {
            return null
        }
    }

    override fun getHeaderId(position: Int): Long {
        val chatMessage = chatMessages[position]
        return if (chatMessage != null) {
            getDateAsHeaderId(chatMessage.dateSent * 1000)
        } else {
            0L
        }
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup?): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent?.context)
            .inflate(R.layout.chat_message_header, parent, false)
        return object : RecyclerView.ViewHolder(view) {

        }
    }

    override fun onBindHeaderViewHolder(p0: RecyclerView.ViewHolder?, p1: Int) {
        val view = p0?.itemView
        val dateTextView = view?.findViewById<TextView>(R.id.txtHeader)

        val chatMessage = chatMessages[p1]
        chatMessage.let {
            var title: String
            val timeInMillis = chatMessage.dateSent * 1000
            val msgTime = Calendar.getInstance()
            msgTime.timeInMillis = timeInMillis

            if (timeInMillis == 0L) {
                title = ""
            }

            val now = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("d MMM", Locale.ENGLISH)
            val lastYearFormat = SimpleDateFormat("dd MMM, yyyy", Locale.ENGLISH)

            val sameDay = now.get(Calendar.DATE) == msgTime.get(Calendar.DATE)
            val sameMonth = now.get(Calendar.MONTH) == msgTime.get(Calendar.MONTH)
            val lastDay = now.get(Calendar.DAY_OF_YEAR) - msgTime.get(Calendar.DAY_OF_YEAR) == 1
            val sameYear = now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR)

            if (sameDay && sameYear && sameMonth) {
                title = context.getString(R.string.today)
            } else if (lastDay && sameYear) {
                title = context.getString(R.string.yesterday)
            } else if (sameYear) {
                title = dateFormat.format(Date(timeInMillis))
            } else {
                title = lastYearFormat.format(Date(timeInMillis))
            }

            if (dateTextView != null) {
                dateTextView.text = title
            }
        }

        val layoutParams = dateTextView?.layoutParams as LinearLayout.LayoutParams
        layoutParams.topMargin = 0
        dateTextView.layoutParams = layoutParams
    }

    fun LoadTask(
        file: File,
        inputStream: InputStream,
        videoProgressBar: ProgressBar,
        videoImageView: ImageView,
        position: Int
    ) {
        (context as ChatActivity).lifecycleScope.executeAsyncTask(onPreExecute = {

        },
            doInBackground = {
                Log.d(TAG, "Downloading File as InputStream")
                val output = FileOutputStream(file)

                try {
                    inputStream.use {
                        output.use {
                            inputStream.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, e.message!!)
                    return@executeAsyncTask false
                }

                return@executeAsyncTask true
            },
            onPostExecute = {
                if (it) {
                    Log.d(TAG, "File Downloaded")
                    fillVideoFileThumb(file, videoProgressBar, videoImageView, position)
                } else {
                    Log.d(TAG, "File Download Failed")
                }
            }
        )
    }

    private fun CoroutineScope.executeAsyncTask(
        onPreExecute: () -> Unit,
        doInBackground: () -> Boolean,
        onPostExecute: (Boolean) -> Unit
    ) = launch {
        onPreExecute()
        val result =
            withContext(Dispatchers.IO) { // runs in background thread without blocking the Main Thread
                doInBackground()
            }
        onPostExecute(result)
    }
}