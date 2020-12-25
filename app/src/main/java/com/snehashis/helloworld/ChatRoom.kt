package com.snehashis.helloworld

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.snehashis.helloworld.notification.notificationChatRoomID
import kotlinx.android.synthetic.main.activity_chat_room.*
import kotlinx.android.synthetic.main.dialog_edit_text.view.*
import kotlinx.android.synthetic.main.dialog_progress.view.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.collections.HashMap

const val MESSAGE_COLLECTION_KEY = "Messages"
private const val ONLINE_COLLECTION_KEY = "Users"
private const val TYPING_COLLECTION_KEY = "User_Typing"
private const val STORAGE_IMAGE_KEY = "images"
const val KEY_USER = "user"
const val KEY_MESSAGE = "message"
const val KEY_IMAGE = "isImage"
const val KEY_IMAGE_URI = "imageUri"
const val KEY_TIME = "timeStamp"
const val KEY_UID = "uid"
const val KEY_EDITED = "isEdited"
const val KEY_REPLY = "isReply"
const val KEY_REPLY_ID = "replyID"
private const val KEY_TYPING = "isTyping"

private const val KEY_USERS_PEOPLE = "Users"
private const val KEY_NAME_PEOPLE = "Name"
private const val KEY_ONLINE_PEOPLE = "isOnline"
private const val KEY_PHOTO_PEOPLE =  "photoUri"
private const val KEY_TIME_PEOPLE = "lastSeen"
private const val KEY_BIO_PEOPLE = "bio"
private const val KEY_UID_PEOPLE = "uid"


private val FCM_API = "https://fcm.googleapis.com/fcm/send"
private val serverKey =
    "key=" + "AAAAumQ3uHw:APA91bHursLbOCzdKPxOl6z1sFTfUfweTxJARJZgQyfPca4krT06z57qnWcuUlW6AqFhry85VtciN7T_Axwuk2ucf1CbPqSO2RLsLTj32fHGSRqZKHO-wWK3HnMjdOHd2AXLTVf9Pii5"
private val contentType = "application/json"


private const val IMAGE_INTENT = 1001
const val MENU_IEM_INTENT = "MENU_ITEM"

private var IMG_URI : Uri? = null
private var SELECTION_MODE = false
private var REPLY_MODE = false
private var REPLY_ID = ""
private var REPLY_TO_UID =""
private var FIRST_DOCUMENT = true

var messageList = mutableListOf<Message>()
var selectedMessageList = mutableListOf<Message>()
var uidList = mutableListOf<String>()

private lateinit var mAuth : FirebaseAuth
private val fireStoreReference : FirebaseFirestore = FirebaseFirestore.getInstance()
private val messageCollection = fireStoreReference.collection(MESSAGE_COLLECTION_KEY)
private val onlineUsersCollection = fireStoreReference.collection(ONLINE_COLLECTION_KEY)
private val typingUsersCollection = fireStoreReference.collection(TYPING_COLLECTION_KEY)
private val usersCollection = fireStoreReference.collection(KEY_USERS_PEOPLE)
private val fireStorageReference : StorageReference = FirebaseStorage.getInstance().getReference(STORAGE_IMAGE_KEY)
private var uploadTask : UploadTask? = null // To ease handling upload tasks

class ChatRoom : AppCompatActivity(), MessageAdapter.MessageClickListener{


    private val requestQueue: RequestQueue by lazy {
        Volley.newRequestQueue(this.applicationContext)
    }
    private var receiveAllNotifications = true

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room)

        //Shared Preferences setup
        val sharedPreferences = getSharedPreferences("sharedPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        receiveAllNotifications = sharedPreferences.getBoolean("receiveAllNotifications", true)

        mAuth = FirebaseAuth.getInstance()
        val chatBoxAdapter = MessageAdapter(this, messageList)
        chatBoxView.adapter = chatBoxAdapter
        val chatBoxViewLayoutManager = LinearLayoutManager(this)
        chatBoxView.layoutManager = chatBoxViewLayoutManager
        chatBoxAdapter.notifyDataSetChanged()
        chatBoxView.addOnScrollListener(object : RecyclerView.OnScrollListener(){
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if(!chatBoxView.canScrollVertically(1) && newState == RecyclerView.SCROLL_STATE_IDLE)
                    scroll_btn.visibility = View.GONE
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy < 0)
                    scroll_btn.visibility = View.VISIBLE
                else if(dy == 0)
                    scroll_btn.visibility = View.GONE
            }
        })
        scroll_btn.setOnClickListener {
            chatBoxView.smoothScrollToPosition(chatBoxAdapter.itemCount - 1)
            scroll_btn.visibility = View.GONE
        }
        //Scroll when keyboard opens
        chatBoxView.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (bottom < oldBottom) {
                chatBoxView.post {
                    chatBoxView.smoothScrollToPosition(chatBoxAdapter.itemCount - 1)
                }
            }
        }

        imagePreview.visibility = View.GONE // Programmatically setting visibility will auto appear after image selection
        attachButton.setOnClickListener {
            openImage()
        }
        captionMsg.setOnClickListener {
            imagePreview.visibility = View.GONE
            IMG_URI = null
            if(messageInput.text.isBlank())
                sendButton.visibility = View.GONE
        }

        sendButton.visibility = View.GONE // Programmatically setting visibility will auto appear on typing
        //Send Message
        sendButton.setOnClickListener {
            //Moving to a separate method for better code readability
            sendMessage()
        }

        //More menu handle
        btn_more.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            val popupMenu = PopupMenu(this, it)
            popupMenu.inflate(R.menu.popup_menu)
            popupMenu.menu.getItem(0).isChecked = !receiveAllNotifications
            popupMenu.setOnMenuItemClickListener { item->
                val index = when(item.itemId){
                    R.id.people_popup -> 0
                    R.id.user_popup -> 1
                    R.id.about_popup -> 2
                    else -> 3
                }
                if (index < 3){
                    val bottomNavigationIntent = Intent(this, BottomNavigation::class.java)
                    bottomNavigationIntent.putExtra(MENU_IEM_INTENT, index)
                    bottomNavigationIntent.putExtra("uid", mAuth.currentUser?.uid)
                    startActivity(bottomNavigationIntent)
                }
                else {
                    receiveAllNotifications = !receiveAllNotifications
                    editor.putBoolean("receiveAllNotifications", receiveAllNotifications)
                    editor.apply()
                    popupMenu.menu.getItem(0).isChecked = !receiveAllNotifications
                    return@setOnMenuItemClickListener false
                }
                true
            }
            popupMenu.show()
        }
        messageInput.afterTextChangedDelayed {
            val map = HashMap<String, Boolean>()
            map["isTyping"] = false
            val user = mAuth.currentUser
            typingUsersCollection.document(user!!.uid).set(map)
        }
        selectionBar.visibility = View.GONE
        btn_back.setOnClickListener {
            exitSelectionMode()
        }

        btn_copy.setOnClickListener {
            copySelectedMessages()
            exitSelectionMode()
        }

        btn_delete.setOnClickListener {
            val confirmDialogBuilder = MaterialAlertDialogBuilder(this)
            confirmDialogBuilder.setTitle("Permanently Delete Messages")
            confirmDialogBuilder.setMessage("You are about to delete ${selectedMessageList.size} messages. This action cannot be undone do you want to continue?")
            val dialog = confirmDialogBuilder.setPositiveButton("Confirm") { _, _ ->
                deleteSelectedMessages()
                exitSelectionMode()
            }
            .setNegativeButton("Cancel") { _, _ ->
                exitSelectionMode()
            }.create()
            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.secondaryColor))
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor(R.color.secondaryColor))
            }
            dialog.setCancelable(false)
            dialog.show()
        }

        btn_edit.setOnClickListener {
            val selectedMessage = selectedMessageList[0]
            exitSelectionMode()
            val editMessage = layoutInflater.inflate(R.layout.dialog_edit_text, null)
            val editDialog = MaterialAlertDialogBuilder(this)
            editDialog.setTitle("Edit Message")
            editMessage.textInDialog.isSingleLine = false
            editMessage.textInDialog.setText(selectedMessage.text)
            editDialog.setView(editMessage)
            val dialog = editDialog.setPositiveButton("Done") { _, _ ->
                if (editMessage.textInDialog.text.isNotBlank() && editMessage.textInDialog.text.toString() != selectedMessage.text){
                    val selectedMessageDocument = messageCollection.document(selectedMessage.msgID)
                    selectedMessageDocument.update(KEY_MESSAGE, editMessage.textInDialog.text.toString())
                    selectedMessageDocument.update(KEY_EDITED, true)
                }
                else if (editMessage.textInDialog.text.toString() == selectedMessage.text)
                    Toast.makeText(this, "Message is Same", Toast.LENGTH_SHORT).show()
                else
                    Toast.makeText(this, "Message can't be blank", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel") {_, _ ->
                //Do Nothing
            }.create()
            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.secondaryColor))
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor(R.color.secondaryColor))
            }
            dialog.setCancelable(false)
            dialog.show()
        }
        replyPreviewDialog.visibility = View.GONE // Hiding initially will reappear when replying
        btn_reply.setOnClickListener {
            val selectedMessage = selectedMessageList[0]
            REPLY_MODE =true
            replyPreviewDialog.visibility = View.VISIBLE
            captionReplyMsg.text = "Replying to ${selectedMessage.user}. Tap here to cancel"
            if (selectedMessage.isImage) {
                Glide.with(this)
                    .load(selectedMessage.imageUri)
                    .placeholder(R.drawable.ic_image_search)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .transition(DrawableTransitionOptions.withCrossFade(DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()))
                    .into(selectedReplyImagePreview)
            }
            else {
                Glide.with(this)
                    .load(R.drawable.ic_reply)
                    .into(selectedReplyImagePreview)
            }
            REPLY_ID = selectedMessage.msgID
            REPLY_TO_UID = selectedMessage.uid
            exitSelectionMode()
        }
        captionReplyMsg.setOnClickListener {
            REPLY_MODE = false
            REPLY_ID = ""
            REPLY_TO_UID = ""
            replyPreviewDialog.visibility = View.GONE
        }
    }

    private fun sendNotification(notification: JSONObject) {
        Log.e("TAG", "sendNotification")
        val jsonObjectRequest = object : JsonObjectRequest(FCM_API, notification,
            Response.Listener<JSONObject> { response ->
                Log.i("TAG", "onResponse: $response")
            },
            Response.ErrorListener {
                Toast.makeText(this@ChatRoom, "Request error", Toast.LENGTH_LONG).show()
                Log.i("TAG", "onErrorResponse: Didn't work")
            }) {

            override fun getHeaders(): Map<String, String> {
                val params = HashMap<String, String>()
                params["Authorization"] = serverKey
                params["Content-Type"] = contentType
                return params
            }
        }
        requestQueue.add(jsonObjectRequest)
    }

    @SuppressLint("SetTextI18n")
    private fun sendMessage() {
        if (messageInput.text.isNotBlank() || IMG_URI != null){
            sendButton.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            val map = HashMap<String, Any>()
            val topic = "/topics/ChatRoom"
            val notification = JSONObject()
            val notificationBody = JSONObject()
            val currentMessageReference = messageCollection.document()
            //Basic fields setup
            map[KEY_USER] = mAuth.currentUser!!.displayName!!
            notificationBody.put("title",mAuth.currentUser!!.displayName!!)
            map[KEY_MESSAGE] = "" + messageInput.text.toString().trim()
            if (IMG_URI != null)
                notificationBody.put("message", "Sent an image")
            else
                notificationBody.put("message", ""+messageInput.text.toString().trim())
            map[KEY_TIME] = Timestamp.now()
            notificationBody.put("time",Timestamp.now().toDate().time)
            map[KEY_UID] = mAuth.currentUser!!.uid
            //Setup for Replying
            notificationBody.put("isReply", REPLY_MODE)
            if (REPLY_MODE){
                map[KEY_REPLY] = true
                map[KEY_REPLY_ID] = REPLY_ID
                notificationBody.put("replyUID", REPLY_TO_UID)
                REPLY_MODE = false
                REPLY_ID = ""
                REPLY_TO_UID = ""
                replyPreviewDialog.visibility = View.GONE
            }
            else {
                map[KEY_REPLY] = false
            }
            notificationBody.put("directReply", 0)
            notification.put("to", topic)
            notification.put("data", notificationBody)
            //Setup for image in message
            if (imagePreview.isVisible && IMG_URI != null){
                val alertDialog = MaterialAlertDialogBuilder(this).create()
                val progressDialog = layoutInflater.inflate(R.layout.dialog_progress, null)
                alertDialog.setView(progressDialog)
                if(Build.VERSION.SDK_INT >= 24)
                    progressDialog.progressBar.setProgress(0, true)
                else
                    progressDialog.progressBar.progress = 0
                progressDialog.progressNumeric.text = "0%"
                alertDialog.setCancelable(false)
                alertDialog.show()
                val fileName = currentMessageReference.id + "." + getFileExtension(IMG_URI!!)
                val currentImageReference = fireStorageReference.child(fileName)
                if (getFileExtension(IMG_URI!!) == "jpg" || getFileExtension(IMG_URI!!) == "jpeg") { //Compress if JPEG
                    val bytesData = compressJpegToByteArray(IMG_URI!!)
                    uploadTask = currentImageReference.putBytes(bytesData)
                }
                else
                    uploadTask = currentImageReference.putFile(IMG_URI!!)
                uploadTask!!.addOnSuccessListener {
                    progressDialog.uploadingLabel.text = "Fetching link..."
                    currentImageReference.downloadUrl.addOnSuccessListener { uri ->
                        map[KEY_IMAGE] = true
                        map[KEY_IMAGE_URI] = uri.toString()
                        currentMessageReference.set(map).addOnSuccessListener {
                            sendNotification(notification)
                        }
                        captionMsg.performClick()
                        messageInput.text.clear()
                        alertDialog.dismiss()
                    }.addOnFailureListener { exception ->
                        Toast.makeText(this, exception.message, Toast.LENGTH_SHORT).show()
                        alertDialog.dismiss()
                    }
                }.addOnFailureListener { exception ->
                    Toast.makeText(this, "Upload Failed", Toast.LENGTH_SHORT).show()
                    exception.printStackTrace()
                }.addOnProgressListener { progressSnapshot ->
                    val progress = (100.0 * progressSnapshot.bytesTransferred /  progressSnapshot.totalByteCount).toInt()
                    if(Build.VERSION.SDK_INT >= 24)
                        progressDialog.progressBar.setProgressCompat(progress, true)
                    else
                        progressDialog.progressBar.progress = progress
                    progressDialog.progressNumeric.text = "$progress%"
                }.addOnCanceledListener {
                    currentImageReference.delete()
                }
            }
            else {
                map[KEY_IMAGE] = false
                currentMessageReference.set(map).addOnSuccessListener {
                    sendNotification(notification)
                }
                captionMsg.performClick()
                messageInput.text.clear()
            }
        }
    }

    private fun TextView.afterTextChangedDelayed(afterTextChanged: (String) -> Unit) {
        this.addTextChangedListener(object : TextWatcher {
            var timer: CountDownTimer? = null

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(editable: Editable?) {
                if (editable!!.isNotBlank()){
                    sendButton.visibility = View.VISIBLE
                }
                else{
                    sendButton.visibility = View.GONE
                }
                val map = HashMap<String, Boolean>()
                map["isTyping"] = true
                val user = mAuth.currentUser
                typingUsersCollection.document(user!!.uid).set(map)
                timer?.cancel()
                timer = object : CountDownTimer(3000, 1500) {
                    override fun onTick(millisUntilFinished: Long) {}
                    override fun onFinish() {
                        afterTextChanged.invoke(editable.toString())
                    }
                }.start()
            }
        })
    }

    @Suppress("DEPRECATION")
    private fun getImageBitmap(selectedPhotoUri: Uri): Bitmap  =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            MediaStore.Images.Media.getBitmap(contentResolver, selectedPhotoUri)
        else {
            val source = ImageDecoder.createSource(contentResolver, selectedPhotoUri)
            ImageDecoder.decodeBitmap(source)
        }

    private fun compressJpegToByteArray(uri: Uri, quality: Int = 40) : ByteArray {
        val bitmap = getImageBitmap(uri)
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    private fun getFileExtension (uri: Uri) : String = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentResolver.getType(uri))!!

    private fun openImage() {
        val imageOpenIntent = Intent()
        imageOpenIntent.type = "image/*"
        imageOpenIntent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(imageOpenIntent, IMAGE_INTENT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            IMAGE_INTENT -> {
                if (resultCode == RESULT_OK){
                    IMG_URI = data!!.data
                    Glide.with(this).load(IMG_URI).into(selectedImagePreview)
                    imagePreview.visibility = View.VISIBLE
                    sendButton.visibility = View.VISIBLE
                }
            }
        }
    }
    //Handling Message Item Clicks
    override fun onMessageItemClick(position: Int, isImage: Boolean, selectionMode: Boolean) {
        if(isImage && !SELECTION_MODE){
            val openImageIntent = Intent()
            openImageIntent.action = Intent.ACTION_VIEW
            openImageIntent.setDataAndType(Uri.parse(messageList[position].imageUri), "image/*")
            startActivity(openImageIntent)
        }
        else if (SELECTION_MODE && selectionMode){
            toggleSelection(position)
        }
        else if (!isImage && !selectionMode && !SELECTION_MODE && messageList[position].replyingToMessage!!.msgID.isNotBlank()){
            val index = messageList.indexOf(messageList[position].replyingToMessage)
            if (index >= 0)
                chatBoxView.smoothScrollToPosition(index)
        }
    }

    override fun onMessageItemLongClick(position: Int) {
        if (!SELECTION_MODE){
            selectedMessageList.clear()
            SELECTION_MODE = true
            selectionBar.visibility = View.VISIBLE
            toggleSelection(position)
        }
    }

    private fun toggleSelection(position: Int){
        messageList[position].isSelected = !messageList[position].isSelected
        if(messageList[position].isSelected)
            selectedMessageList.add(messageList[position])
        else
            selectedMessageList.remove(messageList[position])
        chatBoxView.adapter?.notifyDataSetChanged()
        notifySelectionListChange()
    }

    @SuppressLint("SetTextI18n")
    private fun notifySelectionListChange() { //Adjust the UI as per the updated list
        selectedCount.text = "Selected : ${selectedMessageList.size}"
        var flag = false
        if(selectedMessageList.isNotEmpty()){
            for (message in selectedMessageList) {
                if (message.uid != mAuth.currentUser?.uid) {
                    flag = true
                    break
                }
            }
            btn_delete.visibility = if (flag) View.GONE else View.VISIBLE
            btn_edit.visibility = if (selectedMessageList.size > 1 || flag) View.GONE else View.VISIBLE
            btn_reply.visibility = if (selectedMessageList.size > 1) View.GONE else View.VISIBLE
        }
        else
            exitSelectionMode()
    }

    private fun exitSelectionMode() {
        SELECTION_MODE = false
        btn_delete.visibility = View.VISIBLE
        btn_edit.visibility = View.VISIBLE
        selectionBar.visibility = View.GONE
        for (message in selectedMessageList){
            message.isSelected = false
        }
        chatBoxView.adapter?.notifyDataSetChanged()
    }

    override fun onBackPressed() {
        if (SELECTION_MODE)
            exitSelectionMode()
        else
            super.onBackPressed()
    }

    private fun deleteSelectedMessages() {
        for (message in selectedMessageList)
            deleteMessage(message)
    }

    private fun deleteMessage(message: Message) {
        val messageReference = messageCollection.document(message.msgID)
        if (message.isImage){
            val imageReference = fireStorageReference.child(formatUrlToFileName(message.imageUri))
            imageReference.delete()
        }
        messageReference.delete()
        chatBoxView.adapter?.notifyDataSetChanged()
    }

    private fun formatUrlToFileName(url : String) : String {
        val lastIndex = url.indexOf('?')
        val firstIndex = url.indexOf("%2F") + 3
        return url.substring(firstIndex, lastIndex)
    }

    private fun copySelectedMessages () {
        var copiedMessage = ""
        if(selectedMessageList.size == 1)
            copiedMessage = copyMessage(selectedMessageList[0])
        else{
            selectedMessageList.sortBy {it.timestamp}
            for (message in selectedMessageList){
                copiedMessage += copyMessage(message, false) + "\n" //Set to true to enable labels
            }
        }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData: ClipData = ClipData.newPlainText("Hello_World Messages", copiedMessage)
        clipboard.setPrimaryClip(clipData)
    }
    private fun copyMessage(message: Message, format: Boolean = false): String {

        return if (format)
            generateMessageLabel(message) + "\n" + message.text
        else
            message.text
    }

    private fun generateMessageLabel(message: Message): String {
        val timestamp = formatTimeStamp(message.timestamp)
        return "> ${message.user} [$timestamp] :"
    }


    private fun buildMessage(document : QueryDocumentSnapshot, isNewM: Boolean): Message {
        val currentUser = ""+document.getString(KEY_USER)
        val currentMessage = ""+document.getString(KEY_MESSAGE)
        val timeStamp = document.getTimestamp(KEY_TIME)
        val uid = ""+document.getString(KEY_UID)
        val msgID = document.id
        var isImagePresent = document.getBoolean(KEY_IMAGE)
        var imageUri = ""
        if (isImagePresent == null)
            isImagePresent = false
        else if (isImagePresent)
            imageUri = ""+document.getString(KEY_IMAGE_URI)
        var isMessageEdited = document.getBoolean(KEY_EDITED)
        isMessageEdited = when(isMessageEdited){
            null -> false
            else -> isMessageEdited
        }
        var isMessageReply = document.getBoolean(KEY_REPLY)
        var replyMessage : Message? = null
        if(isMessageReply == null)
            isMessageReply = false
        else if (isMessageReply){
            val replyMsgID = document.getString(KEY_REPLY_ID)
            for (msg in messageList){
                when (msg.msgID) {
                    replyMsgID -> replyMessage = msg
                }
            }
        }
        if (replyMessage == null)
            replyMessage = Message("Deleted", "Message", false, "",null, "","")
        return Message(currentUser, currentMessage, isImagePresent, imageUri, timeStamp, uid, msgID, isMessageEdited, isMessageReply, replyMessage, isNew = isNewM)
    }

    @SuppressLint("SetTextI18n")
    override fun onStart() {
        super.onStart()
        val user = mAuth.currentUser
        if(user == null)
            finish()
        else {
            //Storing User info to be retrieved in PeopleFragment.kt
            val currentUserDocument = fireStoreReference.collection("Users").document(user.uid)

            currentUserDocument.update(KEY_NAME_PEOPLE, user.displayName!!)
            currentUserDocument.update(KEY_PHOTO_PEOPLE, user.photoUrl.toString())
            currentUserDocument.update(KEY_ONLINE_PEOPLE, true)
            currentUserDocument.update(KEY_TIME_PEOPLE,Timestamp.now())
            currentUserDocument.update(KEY_UID_PEOPLE, user.uid)
        }
        messageCollection.orderBy(KEY_TIME, Query.Direction.ASCENDING).addSnapshotListener(this,EventListener { value, error ->
                if (error != null){
                    Log.e("SnapshotListener Error","Exception", error)
                    return@EventListener
                }
                else if (value != null){
                    //Plan to optimize this by not clearing the list in upcoming version
                    messageList.clear()
                    for (document in value) {
                        val message = buildMessage(document, !FIRST_DOCUMENT)
                        messageList.add(message)
                    }
                    FIRST_DOCUMENT = false
                    chatBoxView.adapter?.notifyDataSetChanged()
                    chatBoxView.scrollToPosition(chatBoxView.adapter!!.itemCount - 1)
                }
            })
        onlineUsersCollection.addSnapshotListener(this, EventListener { value, error ->
            if (error != null) {
                Log.e("SnapshotListener Error", "Exception", error)
                return@EventListener
            } else if (value != null) {
                var count = -1
                for (document in value) {
                    if (document.getBoolean(KEY_ONLINE_PEOPLE) != null){
                        if (document.getBoolean(KEY_ONLINE_PEOPLE)!!) count++
                    }

                }
                onlineUserCount.text = when (count) {
                    1 -> "You are alone"
                    else -> "Online : $count"
                }
            }
        })

        typingUsersCollection.addSnapshotListener(this) { value, error ->
            if (error != null) {
                Log.e("SnapshotListener Error", "Exception", error)
            } else if (value != null) {
                var count = 0
                for (document in value) {
                    val isTyping = document.getBoolean(KEY_TYPING)
                    if (document.id == user!!.uid)
                        continue
                    if (isTyping!!)
                        count++
                }
                when (count) {
                    0 -> typingView.visibility = View.GONE
                    1 -> {
                        typingView.visibility = View.VISIBLE
                        typingView.text = "Someone is typing..."
                    }
                    else -> {
                        typingView.visibility = View.VISIBLE
                        typingView.text = "Many People are typing..."
                    }
                }
            }
        }

    }

    private fun updateStatus(isOnline : Boolean){
        usersCollection.document(mAuth.currentUser!!.uid).update(KEY_ONLINE_PEOPLE, isOnline)
        usersCollection.document(mAuth.currentUser!!.uid).update(KEY_TIME_PEOPLE, Timestamp.now())
    }

    override fun onResume() {
        FIRST_DOCUMENT = true
        //Clearing pending notifications from the app
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationChatRoomID)
        Handler().postDelayed({
            updateStatus(true)
        },1000)
        FirebaseMessaging.getInstance().unsubscribeFromTopic("ChatRoom").addOnSuccessListener {
            Log.e("TopicSubscription", "Success")
        }.addOnFailureListener {
            Log.e("TopicSubscription", "Failed")
        }
        super.onResume()
    }

    override fun onPause() {
        FirebaseMessaging.getInstance().subscribeToTopic("ChatRoom").addOnSuccessListener {
            Log.e("TopicSubscription", "Success")
        }.addOnFailureListener {
            Log.e("TopicSubscription", "Failed")
        }
        super.onPause()
    }

    override fun onStop() {
        exitSelectionMode()
        val user = mAuth.currentUser
        typingUsersCollection.document(user!!.uid).delete()
        updateStatus(false)
        if (uploadTask != null){
            if (uploadTask!!.isInProgress)
                uploadTask!!.cancel()
        }
        super.onStop()
    }
}