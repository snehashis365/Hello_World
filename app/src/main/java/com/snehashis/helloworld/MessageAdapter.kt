package com.snehashis.helloworld

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.message_layout.view.*
import java.util.*


class MessageAdapter(private val context: Context, private val messageList: MutableList<Message>) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private val messageClickListener = context as MessageClickListener
    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userNameLabel : TextView = view.userNameLabel
        val attachedImage : ImageView = view.attachedImage
        val textMessage : TextView = view.textMessage
        val time : TextView = view.time
        val messageBody : LinearLayout = view.messageBody
        val messageBodyHolder : LinearLayout = view.messageBodyHolder
    }

    interface MessageClickListener {
        fun onMessageItemClick(position: Int, isImage: Boolean, selectionMode: Boolean)
        fun onMessageItemLongClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context).inflate(R.layout.message_layout, parent, false)
        return MessageViewHolder(adapterLayout)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messageList[position]
        val currentUid = FirebaseAuth.getInstance().currentUser!!.uid
        if (currentUid != message.uid) {
            holder.userNameLabel.visibility = View.VISIBLE
            holder.userNameLabel.text = message.user
            holder.messageBody.background = ContextCompat.getDrawable(context, R.drawable.chat_incoming)
            holder.userNameLabel.gravity = Gravity.START
            holder.messageBodyHolder.gravity = Gravity.START
        }
        else {
            holder.userNameLabel.visibility = View.GONE
            holder.messageBody.background = ContextCompat.getDrawable(context, R.drawable.chat_outgoing)
            holder.userNameLabel.gravity = Gravity.END
            holder.messageBodyHolder.gravity = Gravity.END
        }
        if (message.isImage) {

            Glide.with(context)
                    .load(message.imageUri)
                    .placeholder(R.drawable.ic_image_search)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .transition(DrawableTransitionOptions.withCrossFade(DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build())) // Have to build Cross fade to avoid transparent images bug with placeholder
                    .into(holder.attachedImage)
            holder.attachedImage.visibility = View.VISIBLE
            holder.setIsRecyclable(false)
            if (message.isSelected){
                holder.attachedImage.setOnClickListener {
                    messageClickListener.onMessageItemClick(position, isImage = false, selectionMode = true)
                }
            }
            else {
                holder.attachedImage.setOnClickListener {
                    messageClickListener.onMessageItemClick(position, isImage = true, selectionMode = false)
                }
            }
        }
        else {
            Glide.with(context).clear(holder.attachedImage)
            holder.attachedImage.setImageDrawable(null)
            holder.attachedImage.visibility = View.GONE
        }
        if (message.text!!.isNotBlank()) {
            holder.textMessage.visibility = View.VISIBLE
            holder.textMessage.text = message.text
        }
        else{
            holder.textMessage.visibility = View.GONE
        }
        holder.time.text = formatTimeStamp(message.timestamp)
        holder.messageBodyHolder.setOnClickListener {
            messageClickListener.onMessageItemClick(position, isImage = false, selectionMode = true)
        }
        holder.messageBodyHolder.setOnLongClickListener {
            messageClickListener.onMessageItemLongClick(position)
            notifyDataSetChanged()
            return@setOnLongClickListener true
        }
        if (message.isSelected)
            holder.messageBodyHolder.foreground = ContextCompat.getDrawable(context, R.drawable.selected_bg)
        else
            holder.messageBodyHolder.foreground = null
    }

    override fun getItemCount() = messageList.size
}

fun formatTimeStamp (timestamp: Timestamp?) : String {
    val calendar = Calendar.getInstance()
    calendar.time = timestamp!!.toDate()
    var time = ""
    time += calendar.get(Calendar.DATE).toString() + "/" + calendar.get(Calendar.MONTH) + "/" + calendar.get(Calendar.YEAR).toString().substring(2) + " "
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