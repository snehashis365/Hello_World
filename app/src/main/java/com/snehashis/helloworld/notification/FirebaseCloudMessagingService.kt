package com.snehashis.helloworld.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.snehashis.helloworld.MainActivity
import com.snehashis.helloworld.R

const val KEY_TOKEN = "Token"
const val notificationChatRoomID = 3650
class FirebaseCloudMessagingService : FirebaseMessagingService() {

    private val MESSAGE_CHANNEL_ID = "messages_channel"
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private val messageStyle : NotificationCompat.MessagingStyle = NotificationCompat.MessagingStyle(Person.Builder().setName("You").build()).setConversationTitle("Chat Room")


    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val sharedPreferences = getSharedPreferences("sharedPreferences", MODE_PRIVATE)
        val receiveAllNotifications = sharedPreferences.getBoolean("receiveAllNotifications", true)
        Log.e("ServiceUID", currentUser!!.uid)
        var isReply = remoteMessage.data["isReply"].toBoolean()
        if (isReply){
            isReply = (currentUser.uid == remoteMessage.data["replyUID"] )
            Log.e("UID CHECK",""+(remoteMessage.data["replyUID"]))
        }
        if (receiveAllNotifications || isReply) {
            val intent = Intent(this, MainActivity::class.java)
            val remoteInput = RemoteInput.Builder("chat_room_reply").setLabel("Your reply...").build()
            val replyIntent = Intent(this, NotificationReplyReceiver::class.java)
            val replyPendingIntent = PendingIntent.getBroadcast(this, 0, replyIntent, PendingIntent.FLAG_ONE_SHOT)
            val replyAction = NotificationCompat.Action.Builder(R.drawable.ic_reply,"Reply", replyPendingIntent).addRemoteInput(remoteInput).build()
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setupChannels(notificationManager)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pendingIntent =
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
            val largeIcon =
                BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_foreground)
            val notificationSoundUri =
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            var currentStyle = restoreMessagingStyle(this, notificationChatRoomID)
            val directReply = remoteMessage.data["directReply"]?.toInt()
            val currentPerson = if(directReply == 1 && isReply)  null else Person.Builder().setName(remoteMessage.data["title"]).build()
            if (currentStyle == null)
                currentStyle = messageStyle
            currentStyle.addMessage(
                remoteMessage.data["message"],
                remoteMessage.data["time"]!!.toLong(),
                currentPerson
            )
            val count = currentStyle.messages.size
            //Will display this when device is locked
            val publicVersion = NotificationCompat.Builder(this, MESSAGE_CHANNEL_ID).apply {
                setSmallIcon(R.drawable.ic_forum)
                setContentTitle("Hello from ChatRoom")
                setContentText("$count New Message")
                setAutoCancel(true)
                color = Color.BLACK
                priority = NotificationCompat.PRIORITY_HIGH
                setSound(notificationSoundUri)
            }.build()

            //The actual version
            val notificationBuilder = NotificationCompat.Builder(this, MESSAGE_CHANNEL_ID).apply {
                setSmallIcon(R.drawable.ic_forum)
                setContentTitle("ChatRoom")
                setContentText("$count New Message")
                setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                setPublicVersion(publicVersion)
                setStyle(currentStyle)
                addAction(replyAction)
                color = Color.BLACK
                priority = NotificationCompat.PRIORITY_HIGH
                setOnlyAlertOnce((currentPerson == null))
                setAutoCancel(true)
                setSound(notificationSoundUri)
                setContentIntent(pendingIntent)
            }

            notificationManager.notify(notificationChatRoomID, notificationBuilder.build())
        }
        
    }

    private fun restoreMessagingStyle(context: Context, notificationId: Int): NotificationCompat.MessagingStyle? {
        return (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .activeNotifications
                .find { it.id == notificationId }
                ?.notification
                ?.let { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(it) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupChannels(notificationManager: NotificationManager) {
        val channelName = "Cloud Message Notification Channel"
        val channelDescription = "Device to device notifications"

        val adminChannel: NotificationChannel
        adminChannel = NotificationChannel(MESSAGE_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH)
        adminChannel.description = channelDescription
        adminChannel.enableLights(true)
        adminChannel.enableVibration(true)
        notificationManager.createNotificationChannel(adminChannel)
    }

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
        Log.d("TokenUpdate", p0)
    }

}