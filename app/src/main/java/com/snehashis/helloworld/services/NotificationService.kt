package com.snehashis.helloworld.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.util.TimeUtils
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.snehashis.helloworld.ChatRoom
import com.snehashis.helloworld.MainActivity
import com.snehashis.helloworld.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


private const val SERVICE_TAG = "NotificationService"

const val START_NOTIFICATION_SERVICE = 101
const val STOP_NOTIFICATION_SERVICE = 100

//Field keys for notification document
const val NOTIFICATION_KEY_NEW = "isNew"
const val NOTIFICATION_KEY_SENDER = "Sender"
const val NOTIFICATION_KEY_MESSAGE = "Message"
const val NOTIFICATION_KEY_TIME = "Time"
const val NOTIFICATION_IS_REPLY = "isReply"
const val NOTIFICATION_REPLY_UID = "replyTo"
const val NOTIFICATION_IS_IMAGE = "isImage"

private const val CHANNEL_MESSAGE_ID = "NEW MESSAGES CHANNEL"

private const val KEY_USERS_PEOPLE = "Users"
const val KEY_USER_NOTIFICATION = "Notifications"

class NotificationService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false
    private var isForeGround = true

    private val currentUser = FirebaseAuth.getInstance().currentUser
    private val fireStoreReference : FirebaseFirestore = FirebaseFirestore.getInstance()
    private val usersCollection = fireStoreReference.collection(KEY_USERS_PEOPLE)

    val contextForNotification = this

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(SERVICE_TAG, "onBind() called returning null")
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("NotificationService", "onStartCommand executed with startId: $startId")
        if (intent != null){
            isForeGround = intent.getBooleanExtra("isForeGround", true)
            val code = intent.getIntExtra("Action", START_NOTIFICATION_SERVICE)
            when(code){
                START_NOTIFICATION_SERVICE -> startNotificationService()
                STOP_NOTIFICATION_SERVICE -> stopNotificationService()
            }
        }
        else {
            Log.d(SERVICE_TAG, "Intent Null (Maybe restart from system)")
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(SERVICE_TAG,"Service onCreate()")
        createNotificationChannel(CHANNEL_MESSAGE_ID, "Chat Room Messages","The channel where all the messages from chat room appear", true)
        if (isForeGround) {
            var notification = createServiceNotification()
            startForeground(1, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e(SERVICE_TAG, "Service onDestroy()")
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        val restartServiceIntent = Intent(applicationContext, NotificationService::class.java).also {
            it.setPackage(packageName)
        };
        val restartServicePendingIntent: PendingIntent = PendingIntent.getService(this, 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT);
        applicationContext.getSystemService(Context.ALARM_SERVICE);
        val alarmService: AlarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager;
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, restartServicePendingIntent);
    }

    private fun startNotificationService() {
        if (isServiceStarted) return
        Log.d(SERVICE_TAG, "Starting notification service")
        isServiceStarted = true

        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                        newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NotificationService::lock").apply {
                            acquire()
                        }
                    }
        val chatRoomIntent: PendingIntent = Intent(this, ChatRoom::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, 0)
        }
        val notificationBuilder: NotificationCompat.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) NotificationCompat.Builder(
            contextForNotification,
            CHANNEL_MESSAGE_ID
        ) else NotificationCompat.Builder(contextForNotification)
        val notificationStyle = NotificationCompat.MessagingStyle("Me").setConversationTitle("Chat Room")
        GlobalScope.launch(Dispatchers.IO){
            if (isServiceStarted) {

                launch(Dispatchers.IO) {
                    Log.e(SERVICE_TAG, "Testing Service")
                    if (currentUser == null)
                        Log.e(SERVICE_TAG, "User Null F in chat")
                    else {
                        val userNotificationCollection = usersCollection.document(currentUser.uid).collection(
                            KEY_USER_NOTIFICATION)
                        userNotificationCollection.addSnapshotListener { value, error ->
                            if (error != null)
                                Log.e(SERVICE_TAG + "Fire", error.message!!)
                            else{
                                if (value != null){
                                    for (document in value){
                                        if (document.getBoolean(NOTIFICATION_KEY_NEW) != null) {
                                            Log.e( "NotificationFirebase", "${document.id} : ${document.getBoolean("isRead")}")
                                            if (document.getBoolean(NOTIFICATION_KEY_NEW)!!){
                                                //Create Notification here

                                                val sender = document.getString(NOTIFICATION_KEY_SENDER)!!
                                                var messageText = document.getString(NOTIFICATION_KEY_MESSAGE)!!
                                                if (document.getBoolean(NOTIFICATION_IS_IMAGE)!!)
                                                    messageText = "$sender sent an image tap to view"
                                                val idFromTime = document.getTimestamp(
                                                    NOTIFICATION_KEY_TIME)!!.toDate().time
                                                notificationStyle.addMessage(messageText, idFromTime, sender)
                                                val notification = notificationBuilder
                                                    .setContentTitle(sender)
                                                    .setContentText(messageText)
                                                    .setGroup(NOTIFICATION_KEY_MESSAGE)
                                                    .setContentIntent(chatRoomIntent)
                                                    .setSmallIcon(R.mipmap.ic_launcher)
                                                    .setTicker("New Message")
                                                    .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                                                    .setVibrate(longArrayOf(500, 500))
                                                    .setOngoing(false)
                                                    .setAutoCancel(true)
                                                    .setStyle(notificationStyle)
                                                    .setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
                                                    .build()

                                                NotificationManagerCompat.from(contextForNotification).notify(0 ,notification)

                                            }
                                            userNotificationCollection.document(document.id).update(
                                                NOTIFICATION_KEY_NEW, false)
                                        }
                                        else
                                            Log.e("NotificationFirebase", "No Document")
                                    }
                                }
                                else
                                    Log.e(SERVICE_TAG + "Fire", "Value null")
                            }
                        }
                    }
                }
                delay(10000)
            }
            Log.e(SERVICE_TAG,"End of the Loop for the Service")
        }
    }

    private fun stopNotificationService() {
        Log.d(SERVICE_TAG, "Stopping notification service")
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            Log.e(SERVICE_TAG,"Service stopped without being started: ${e.message}")
        }
        isServiceStarted = false

    }
    private fun createServiceNotification(): Notification {
        val notificationChannelId = "FOREGROUND SERVICE CHANNEL"

        // depending on the Android API that we're dealing with we will have
        // to use a specific method to create the notification
        createNotificationChannel(notificationChannelId, "Notification Service Channel", "Notification Foreground Service channel")

        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, 0)
        }

        val builder: Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(
                this,
                notificationChannelId
        ) else Notification.Builder(this)

        return builder
                .setContentTitle("Notification Service")
                .setContentText("IFF you can disable this category/channel do so it won't affect message notifications\nElse notifications won't come in looking for fixes meanwhile")
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker("Ticker text")
                .setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
                .build()
    }
    private fun createNotificationChannel(CHANNEL_ID : String, NAME : String, DESCRIPTION : String = NAME, vibrate : Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;
            val channel = NotificationChannel(
                CHANNEL_ID,
                NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).let {
                it.description = DESCRIPTION
                it.enableVibration(vibrate)
                it
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

}