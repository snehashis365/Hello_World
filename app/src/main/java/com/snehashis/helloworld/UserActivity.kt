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
import kotlinx.android.synthetic.main.dialog_edit_name.view.*

private lateinit var mAuth : FirebaseAuth

private const val DEFAULT_PIC = "https://itg.wfu.edu/wp-content/uploads/Cogn_mode-225x225.png"

class UserActivity : AppCompatActivity() {
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)
        mAuth = FirebaseAuth.getInstance()
        val user = mAuth.currentUser

        uidTv.text = "UID: ${user?.uid}"
        emailTv.text = "Email: ${user?.email}"
        nameTv.text = "Name: ${user?.displayName}"
        if (!user!!.isAnonymous) {
            Glide.with(this).load(user.photoUrl).circleCrop().into(userImage)
            name_edit.visibility = View.GONE
        }
        else
            Glide.with(this).load(DEFAULT_PIC).into(userImage)

        name_edit.setOnClickListener {
            val newName = layoutInflater.inflate(R.layout.dialog_edit_name,null)
            newName.textPersonName.setText(user.displayName)
            val nameDialog = AlertDialog.Builder(this)
            nameDialog.setTitle("Enter Name")
            nameDialog.setView(newName)
            nameDialog.setPositiveButton("Ok") {_, _ ->
                if (newName.textPersonName.text.toString().isNotBlank()) {
                    val anonymousUserProfile = UserProfileChangeRequest.Builder().setDisplayName(newName.textPersonName.text.toString()).build()
                    mAuth.currentUser?.updateProfile(anonymousUserProfile)?.addOnCompleteListener {
                        Toast.makeText(this, "Changed name successfull\nNote: Previous Messages won't be affected", Toast.LENGTH_SHORT).show()
                        nameTv.text = newName.textPersonName.text
                    }
                }
            }
            nameDialog.setNegativeButton("Cancel") {_, _ ->
                //Nothing
            }
            nameDialog.setCancelable(false)
            nameDialog.show()
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