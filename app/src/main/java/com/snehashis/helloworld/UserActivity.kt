package com.snehashis.helloworld

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_user.*

private lateinit var mAuth : FirebaseAuth

val DEFAULT_PIC = "https://itg.wfu.edu/wp-content/uploads/Cogn_mode-225x225.png"

class UserActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)
        mAuth = FirebaseAuth.getInstance()
        val user = mAuth.currentUser

        uidTv.text = "UID: " + user?.uid
        emailTv.text = "Email: " + user?.email
        nameTv.text ="Name: " + user?.displayName
        if (!user!!.isAnonymous)
            Glide.with(this).load(user?.photoUrl).into(userImage)
        else
            Glide.with(this).load(DEFAULT_PIC).into(userImage)

        btn_logout.setOnClickListener {
            if (user != null) {
                if (user.isAnonymous)
                    user.delete()
                    mAuth.signOut()
            }
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}