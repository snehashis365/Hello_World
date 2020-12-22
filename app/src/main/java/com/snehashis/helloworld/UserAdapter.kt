package com.snehashis.helloworld

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.google.firebase.Timestamp
import kotlinx.android.synthetic.main.user_layout.view.*
import java.util.concurrent.TimeUnit

@SuppressLint("SetTextI18n")
class UserAdapter(private val listener : UserClickListener, private val userList : MutableList<HelloWorldUser>) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {


    private val userClickListener = listener
    class UserViewHolder(view : View, val context: Context) : RecyclerView.ViewHolder(view) {
        //Declaring the view widgets needed
        val userPhoto : ImageView = view.userPhoto
        val userDisplayName : TextView = view.userDisplayName
        val userStatus : TextView = view.userStatus
        val userStatusLabel : TextView = view.userStatusLabel

        //Declaring the layouts as well if needed in future
        val userConstraintLayout : ConstraintLayout = view.userConstraintLayout
        val userLayoutHolder : LinearLayout = view.userLayoutHolder
    }
    interface UserClickListener {
        fun onUserClick(position : Int, clickedUser: HelloWorldUser, clickedUserView: LinearLayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context).inflate(R.layout.user_layout, parent, false)
        return UserViewHolder(adapterLayout, parent.context)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.userDisplayName.text = user.displayName
        if (user.isOnline){
            holder.userStatusLabel.text = "Status : "
            holder.userStatus.text = "Online"
            holder.userStatus.setTextColor(holder.context.getColor(R.color.green))
        }
        else{
            holder.userStatusLabel.text = "Last Seen "
            holder.userStatus.text = generateLastSeen(user.lastSeen!!)
            holder.userStatus.setTextColor(holder.context.getColor(R.color.grey))
        }
        Glide.with(holder.context)
            .load(user.photoUrl)
            .circleCrop()
            .placeholder(R.drawable.ic_user)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .transition(DrawableTransitionOptions.withCrossFade(DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()))
            .into(holder.userPhoto)
        ViewCompat.setTransitionName(holder.userPhoto, user.uid)
        holder.itemView.setOnClickListener {
            userClickListener.onUserClick(position, user, holder.userLayoutHolder)
        }
    }

    override fun getItemCount() = userList.size

}
fun generateLastSeen(timestamp: Timestamp) : String {
    val currentTimeStamp = Timestamp.now()
    val elapsedTime = currentTimeStamp.toDate().time - timestamp.toDate().time
    val days = TimeUnit.MILLISECONDS.toDays(elapsedTime)
    val hours = TimeUnit.MILLISECONDS.toHours(elapsedTime)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTime)
    return when {
        days > 0 ->  "$days days ago"
        hours > 0 ->  "$hours hours ago"
        minutes > 0 ->  "$minutes minutes ago"
        else -> "A few seconds ago"
    }
}