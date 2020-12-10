package com.snehashis.helloworld

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
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
        if (!user!!.isAnonymous) {
            Glide.with(this).load(user?.photoUrl).into(userImage)
            name_edit.visibility = View.GONE
        }
        else
            Glide.with(this).load(DEFAULT_PIC).into(userImage)

        name_edit.setOnClickListener {
            val newName : EditText = EditText(this)
            newName.inputType = InputType.TYPE_CLASS_TEXT
            val nameDialog = MaterialAlertDialogBuilder(this)
            nameDialog.setTitle("Enter Name")
            nameDialog.setView(newName)
            nameDialog.setPositiveButton("Ok") { _, _ ->
                if (newName.text.toString().isNotBlank()) {
                    val anonymousUserProfile = UserProfileChangeRequest.Builder().setDisplayName(newName.text.toString()).build()
                    mAuth.currentUser?.updateProfile(anonymousUserProfile)?.addOnCompleteListener {
                        Toast.makeText(this, "Changed name successfull\nNote: Previous Messages won't be affected", Toast.LENGTH_SHORT).show()
                        nameTv.text = newName.text
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