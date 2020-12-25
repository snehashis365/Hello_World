package com.snehashis.helloworld.notification

import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.snehashis.helloworld.*
import org.json.JSONObject

class NotificationReplyReceiver : BroadcastReceiver() {


    private val FCM_API = "https://fcm.googleapis.com/fcm/send"
    private val serverKey =
        "key=" + "AAAAumQ3uHw:APA91bHursLbOCzdKPxOl6z1sFTfUfweTxJARJZgQyfPca4krT06z57qnWcuUlW6AqFhry85VtciN7T_Axwuk2ucf1CbPqSO2RLsLTj32fHGSRqZKHO-wWK3HnMjdOHd2AXLTVf9Pii5"
    private val contentType = "application/json"

    override fun onReceive(context: Context, intent: Intent) {

        val requestQueue: RequestQueue by lazy {
            Volley.newRequestQueue(context.applicationContext)
        }
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null){
            val userName = currentUser.displayName!!
            val uid = currentUser.uid
            val currentTime = Timestamp.now()
            val bundle = RemoteInput.getResultsFromIntent(intent)
            if (bundle != null){
                val replyMessage = bundle.getCharSequence("chat_room_reply").toString()
                if (replyMessage.isNotBlank())
                    sendChatRoomMessage(context, userName, uid, currentTime, replyMessage, requestQueue)
            }
        }
    }

    private fun sendChatRoomMessage(context: Context, userName: String, uid: String, currentTime: Timestamp, replyMessage: String, requestQueue: RequestQueue) {
        //Begin message setup
        val isImage = false
        val isReply = false
        val replyID = ""
        val imageUri = ""
        val directReplyMap = HashMap<String, Any>()

        //Setting necessary fields
        directReplyMap[KEY_USER] = userName
        directReplyMap[KEY_UID] = uid
        directReplyMap[KEY_TIME] = currentTime
        directReplyMap[KEY_MESSAGE] = replyMessage
        //Setting all fields to maintain template convention
        directReplyMap[KEY_IMAGE] = isImage
        directReplyMap[KEY_IMAGE_URI] = imageUri
        directReplyMap[KEY_REPLY] = isReply
        directReplyMap[KEY_REPLY_ID] = replyID

        //Begin Notification json setup
        val topic = "/topics/ChatRoom"
        val notification = JSONObject()
        val notificationBody = JSONObject()

        //Setting fields
        notificationBody.put("title",userName)
        notificationBody.put("message",replyMessage)
        notificationBody.put("time",currentTime.toDate().time)
        notificationBody.put("isReply",true)
        notificationBody.put("replyUID", uid)
        notificationBody.put("directReply", 1)
        notification.put("to", topic)
        notification.put("data", notificationBody)

        val fireStoreReference : FirebaseFirestore = FirebaseFirestore.getInstance()
        val messageCollection = fireStoreReference.collection(MESSAGE_COLLECTION_KEY)
        val currentMessageReference = messageCollection.document()
        currentMessageReference.set(directReplyMap).addOnSuccessListener {
            sendNotification(context, notification, requestQueue)
        }
    }


    private fun sendNotification(context: Context, notification: JSONObject, requestQueue: RequestQueue) {
        Log.e("TAG", "sendNotification")
        val jsonObjectRequest = object : JsonObjectRequest(
            FCM_API, notification,
            Response.Listener<JSONObject> { response ->
                Log.i("TAG", "onResponse: $response")
            },
            Response.ErrorListener {
                Toast.makeText(context, "Request error", Toast.LENGTH_LONG).show()
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
}