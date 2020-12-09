package com.snehashis.helloworld

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.message_layout.view.*
import java.util.*


class MessageAdapter(val context: Context, private val messageList: MutableList<Message>) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    class MessageViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        val userNameLabel : TextView = view.userNameLabel
        val textMessage : TextView = view.textMessage
        val time : TextView = view.time
        val messageBody : LinearLayout = view.messageBody
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        //TODO
        val adapterLayout = LayoutInflater.from(parent.context).inflate(R.layout.message_layout, parent, false)
        return MessageViewHolder(adapterLayout)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        //TODO
        val message = messageList[position]
        val currentUid = FirebaseAuth.getInstance().currentUser!!.uid
        if (currentUid != message.uid) {
            holder.userNameLabel.text = message.user
            holder.messageBody.background = context.getDrawable(R.drawable.chat_incoming)
        }
        else {
            holder.userNameLabel.text = "You"
            holder.messageBody.background = context.getDrawable(R.drawable.chat_outgoing)
            holder.userNameLabel.gravity = Gravity.RIGHT
        }
        holder.textMessage.text = message.text
        val calendar = Calendar.getInstance()
        calendar.time = message.timestamp!!.toDate()
        holder.time.text = formatTimeStamp(message.timestamp)
    }

    override fun getItemCount() = messageList.size

    private fun formatTimeStamp (timestamp: Timestamp?) : String {
        val calendar = Calendar.getInstance()
        calendar.time = timestamp!!.toDate()
        var time = ""
        time += calendar.get(Calendar.DATE).toString() + "/" + calendar.get(Calendar.MONTH) + "/" + calendar.get(Calendar.YEAR) + " "
        time += calendar.get(Calendar.HOUR).toString() + ":"
        if(calendar.get(Calendar.MINUTE) < 10)
            time += "0" + calendar.get(Calendar.MINUTE).toString()
        else
            time += calendar.get(Calendar.MINUTE).toString()
        time += when (calendar.get(Calendar.AM_PM)) {
            1 -> " PM"
            else -> " AM"
        }
        return time
    }

}