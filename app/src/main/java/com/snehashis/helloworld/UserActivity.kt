package com.snehashis.helloworld

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.android.synthetic.main.activity_user.*
import kotlinx.android.synthetic.main.dialog_edit_text.view.*

private lateinit var mAuth : FirebaseAuth

private const val DEFAULT_PIC = "https://itg.wfu.edu/wp-content/uploads/Cogn_mode-225x225.png"

class UserActivity : AppCompatActivity() {
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)
        mAuth = FirebaseAuth.getInstance()
        val user = mAuth.currentUser

        uidTv.text = user?.uid
        emailTv.text = user?.email
        nameTv.text = user?.displayName
        if (!user!!.isAnonymous) {
            Glide.with(this).load(user.photoUrl).circleCrop().into(userImage)
        }
        else
            Glide.with(this).load(DEFAULT_PIC).into(userImage)

        nameTv.setOnClickListener {
            val newName = layoutInflater.inflate(R.layout.dialog_edit_text,null)
            newName.textInDialog.setText(user.displayName)
            val nameDialog = AlertDialog.Builder(this)
            nameDialog.setTitle("Enter Name")
            nameDialog.setView(newName)
            nameDialog.setPositiveButton("Ok") {_, _ ->
                if (newName.textInDialog.text.toString().isNotBlank()) {
                    val anonymousUserProfile = UserProfileChangeRequest.Builder().setDisplayName(newName.textInDialog.text.toString()).build()
                    mAuth.currentUser?.updateProfile(anonymousUserProfile)?.addOnCompleteListener {
                        Toast.makeText(this, "Changed name successful\nNote: Previous Messages won't be affected", Toast.LENGTH_SHORT).show()
                        nameTv.text = newName.textInDialog.text
                    }
                }
            }
            nameDialog.setNegativeButton("Cancel") {_, _ ->
                //Nothing
            }
            val dialog = nameDialog.create()
            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.secondaryColor))
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor(R.color.secondaryColor))
            }
            dialog.setCancelable(false)
            dialog.show()
        }
        user_back.setOnClickListener {
            finish()
        }
        btn_logout.setOnClickListener {
            if (user.isAnonymous)
                user.delete()
            mAuth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}