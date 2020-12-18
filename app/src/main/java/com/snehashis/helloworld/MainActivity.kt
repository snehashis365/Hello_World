package com.snehashis.helloworld

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.android.synthetic.main.activity_main.*

private lateinit var mAuth : FirebaseAuth
private lateinit var googleSignInClient : GoogleSignInClient
const val RC_SIGN_IN = 111
const val TAG = "SignInWithFirebase"

class MainActivity : AppCompatActivity() {
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mAuth = FirebaseAuth.getInstance()
        val user = mAuth.currentUser
        if (user!=null){
            googleSignInButton.text = "Welcome Back"
            googleSignInButton.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,0,0)
            googleSignInButton.isClickable = false
            btn_login.isClickable = false
            btn_login.background = null
            btn_login.setTextColor(getColor(R.color.primaryTextColor))
            btn_login.text = "Just a second"
            btn_login.textSize = 0.3f * btn_login.textSize
        }
        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this,gso)
        googleSignInButton.setOnClickListener {
            signIn()
        }
        btn_login.setOnClickListener {
            val snackBar = Snackbar.make(it, "Creating temporary account...", Snackbar.LENGTH_INDEFINITE)
                    .setBackgroundTint(getColor(R.color.primaryDarkColor))
                    .setTextColor(getColor(R.color.primaryTextColor))
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
            snackBar.show()
            mAuth.signInAnonymously()
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) { // Sign in success, update UI with the signed-in user's information
                        val anonymousUserProfile = UserProfileChangeRequest.Builder().setDisplayName("user_"+ mAuth.currentUser?.uid?.substring(0, 4)).build()
                        snackBar.setText("Generating username...")
                        mAuth.currentUser?.updateProfile(anonymousUserProfile)?.addOnCompleteListener {
                            snackBar.dismiss()
                            startActivity(Intent(this, ChatRoom::class.java))
                            finish()
                        }
                        Log.d(TAG, "signInAnonymously:success")
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInAnonymously:failure", task.exception)
                        Toast.makeText(this, "Authentication failed.",Toast.LENGTH_SHORT).show()
                    }
                }
        }


        Handler().postDelayed({
            if (user != null) {
                startActivity(Intent(this, ChatRoom::class.java))
                finish()
            }
        }, 3300) // Delay for the animation to finish

    }
    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            if (task.isSuccessful){
                try {
                    // Google Sign In was successful, authenticate with Firebase
                    val account = task.getResult(ApiException::class.java)!!
                    Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    // Google Sign In failed, update UI appropriately
                    Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                    Log.w(TAG, "Google sign in failed", e)
                    // ...
                }
            }
            else{
                Log.e(TAG,task.exception.toString())
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val snackBar = Snackbar.make(startPage, "Signing in with Google...", Snackbar.LENGTH_INDEFINITE)
                .setBackgroundTint(getColor(R.color.primaryDarkColor))
                .setTextColor(getColor(R.color.primaryTextColor))
                .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
        snackBar.show()
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    snackBar.setText("Sign in successful")
                    Log.d(TAG, "signInWithCredential:success")
                    startActivity(Intent(this, ChatRoom::class.java))
                    finish()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    snackBar.setText("Sign in failed")
                    snackBar.duration = Snackbar.LENGTH_SHORT
                    // ...
                }

                // ...
            }
    }
}