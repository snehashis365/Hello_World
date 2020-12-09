package com.snehashis.helloworld

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.activity_chat_room.*
import java.util.*
import kotlin.collections.HashMap

private const val MESSAGE_COLLECTION_KEY = "Messages"
private const val KEY_USER = "user"
private const val KEY_MESSAGE = "message"
private const val KEY_TIME = "timeStamp"
private const val KEY_UID = "uid"

var messageList = mutableListOf<Message>()

private lateinit var mAuth : FirebaseAuth
private val fireStoreReference : FirebaseFirestore = FirebaseFirestore.getInstance()
private val messageCollection = fireStoreReference.collection(MESSAGE_COLLECTION_KEY)

class ChatRoom : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room)
        mAuth = FirebaseAuth.getInstance()
        if (mAuth.currentUser!!.isAnonymous)
            userNameHeader.text = "Anonymous: " + mAuth.currentUser!!.displayName
        else
            userNameHeader.text = mAuth.currentUser!!.displayName
        val chatBoxAdapter = MessageAdapter(this, messageList)
        chatBoxView.adapter = chatBoxAdapter
        val chatBoxViewLayoutManager = LinearLayoutManager(this)
        chatBoxView.layoutManager = chatBoxViewLayoutManager
        chatBoxAdapter.notifyDataSetChanged()

        //Send Message
        sendButton.setOnClickListener {
            if (messageInput.text.isNotBlank()){
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                val map = HashMap<String, Any>()
                map[KEY_USER] = mAuth.currentUser!!.displayName!!
                map[KEY_MESSAGE] = messageInput.text.toString().trim()
                map[KEY_TIME] = Timestamp.now()
                map[KEY_UID] = mAuth.currentUser!!.uid
                messageCollection.document().set(map)
                messageInput.text.clear()
            }
        }

        //More menu handle
        btn_more.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            val popupMenu = PopupMenu(this, it)
            popupMenu.inflate(R.menu.popup_menu)
            popupMenu.setOnMenuItemClickListener { item->
                when(item.itemId){
                    R.id.btn_user ->
                        startActivity(Intent(this, UserActivity::class.java))
                    R.id.btn_about -> Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show()
                }
                true
            }
            popupMenu.show()
        }

    }

    override fun onStart() {
        super.onStart()
        if(mAuth.currentUser == null)
            finish()
        messageCollection.orderBy(KEY_TIME, Query.Direction.ASCENDING).addSnapshotListener(this, EventListener { value, error ->
            if (error != null){
                Log.e("SnapshotListener Error","Exception", error)
                return@EventListener
            }
            else if (value != null){
                messageList.clear()
                for (document in value){
                    val currentUser = document.getString(KEY_USER)
                    val currentMessage = document.getString(KEY_MESSAGE)
                    val timeStamp = document.getTimestamp(KEY_TIME)
                    val uid = document.getString(KEY_UID)
                    val message = Message(currentUser, currentMessage, timestamp = timeStamp, uid = uid)
                    messageList.add(message)
                    chatBoxView.adapter?.notifyDataSetChanged()
                    chatBoxView.scrollToPosition(chatBoxView.adapter!!.itemCount - 1)
                }
            }
        })
    }
}