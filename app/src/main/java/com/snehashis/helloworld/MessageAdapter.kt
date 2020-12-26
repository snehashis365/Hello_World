package com.snehashis.helloworld

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.message_layout.view.*
import java.util.*


class MessageAdapter(private val context: Context, private val messageList: MutableList<Message>) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private val fireStoreReference : FirebaseFirestore = FirebaseFirestore.getInstance()
    private val usersCollection = fireStoreReference.collection("Users")
    private var lastColor : ColorStateList? = ContextCompat.getColorStateList(context, R.color.amber_a400)
    private val messageClickListener = context as MessageClickListener
    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userNameLabel : TextView = view.userNameLabel
        val attachedImage : ImageView = view.attachedImage
        val textMessage : TextView = view.textMessage
        val time : TextView = view.time
        val messageBody : LinearLayout = view.messageBody
        val messageProfileImage : ImageView = view.messageProfileImage
        val messageBodyHolder : LinearLayout = view.messageBodyHolder
        val editLabel : TextView = view.editLabel
        val replyLayout : LinearLayout = view.replyLayout
        val replyUser : TextView = view.replyUser
        val replyMessage : TextView = view.replyMessage
        val replyImage : ImageView = view.replyImage
    }

    interface MessageClickListener {
        fun onMessageItemClick(position: Int, isImage: Boolean, selectionMode: Boolean)
        fun onMessageItemLongClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context).inflate(R.layout.message_layout, parent, false)
        return MessageViewHolder(adapterLayout)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messageList[position]
        val currentUid = FirebaseAuth.getInstance().currentUser!!.uid
        holder.userNameLabel.text = message.user
        if (!message.loadMeta){
            holder.messageBody.backgroundTintList = lastColor
            holder.messageProfileImage.visibility = View.GONE
            holder.userNameLabel.visibility = View.GONE
            Glide.with(context).clear(holder.messageProfileImage)
            holder.messageProfileImage.setImageDrawable(null)
        }
        else {
            lastColor = getRandomColor(context)
            holder.messageBody.backgroundTintList = lastColor
            holder.messageProfileImage.visibility = View.VISIBLE
            holder.userNameLabel.visibility = View.VISIBLE
            val dpUri = usersCollection.document(message.uid).get()
            dpUri.addOnSuccessListener {
                Glide.with(context)
                    .load(it.getString("photoUri"))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_user)
                    .transition(DrawableTransitionOptions.withCrossFade(DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()))
                    .circleCrop()
                    .into(holder.messageProfileImage)
            }
        }
        holder.messageBodyHolder.gravity = if (currentUid != message.uid) Gravity.START else Gravity.END
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
        if (message.text.isNotBlank()) {
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

        if(message.isEdited)
            holder.editLabel.visibility = View.VISIBLE
        else
            holder.editLabel.visibility = View.GONE
        // Setup if replying to someone
        if (message.isReply){
            holder.replyLayout.visibility = View.VISIBLE
            if (message.replyingToMessage?.uid == currentUid)
                holder.replyUser.text = "You"
            else
                holder.replyUser.text = message.replyingToMessage?.user
            if (message.replyingToMessage!!.isImage) {
                holder.replyImage.visibility = View.VISIBLE
                Glide.with(context)
                        .load(message.replyingToMessage!!.imageUri)
                        .placeholder(R.drawable.ic_image_search)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .transition(DrawableTransitionOptions.withCrossFade(DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()))
                        .into(holder.replyImage)
            }
            else{
                Glide.with(context).clear(holder.replyImage)
                holder.replyImage.setImageDrawable(null)
                holder.replyImage.visibility = View.GONE
            }
            holder.replyMessage.text = message.replyingToMessage!!.text
            holder.replyLayout.setOnClickListener {
                messageClickListener.onMessageItemClick(position, isImage = false, selectionMode = false)

            }
        }
        else
            holder.replyLayout.visibility = View.GONE
        if (position == messageList.size - 1 && message.isNew) {
            holder.messageBody.animation =AnimationUtils.loadAnimation(context, when(currentUid){
                message.uid ->  R.anim.outgoing_new_message_anim
                else -> R.anim.incoming_new_message_anim
            })
            messageList[position].isNew = false
        }
    }

    override fun getItemCount() = messageList.size
}

fun getRandomColor(context: Context) : ColorStateList?{
    val a400Colors = mutableListOf<ColorStateList?>()
    a400Colors.add(ContextCompat.getColorStateList(context, R.color.red_a400))
    a400Colors.add(ContextCompat.getColorStateList(context, R.color.pink_a400))
    a400Colors.add(ContextCompat.getColorStateList(context, R.color.purple_a400))
    a400Colors.add(ContextCompat.getColorStateList(context, R.color.deepPurple_a400))
    a400Colors.add(ContextCompat.getColorStateList(context, R.color.indigo_a400))
    a400Colors.add(ContextCompat.getColorStateList(context, R.color.blue_a400))
    a400Colors.add(ContextCompat.getColorStateList(context, R.color.lightBlue_a400))
    a400Colors.add(ContextCompat.getColorStateList(context, R.color.teal_a400))
    a400Colors.add(ContextCompat.getColorStateList(context, R.color.green_a400))
    a400Colors.add(ContextCompat.getColorStateList(context, R.color.amber_a400))
    a400Colors.add(ContextCompat.getColorStateList(context, R.color.orange_a400))
    a400Colors.add(ContextCompat.getColorStateList(context, R.color.deepOrange_a400))
    return a400Colors.random()
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