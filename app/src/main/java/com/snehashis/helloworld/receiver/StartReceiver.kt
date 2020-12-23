package com.snehashis.helloworld.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.snehashis.helloworld.services.NotificationService
import com.snehashis.helloworld.services.START_NOTIFICATION_SERVICE

class StartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ) {
            Intent(context, NotificationService::class.java).also {
                it.putExtra("Action", START_NOTIFICATION_SERVICE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Log.e("Hello_World Receiver","Starting the service in >=26 Mode from a BroadcastReceiver")
                    context.startForegroundService(it)
                    return
                }
                Log.e("Hello_World Receiver","Starting the service in < 26 Mode from a BroadcastReceiver")
                context.startService(it)
            }
        }
    }
}