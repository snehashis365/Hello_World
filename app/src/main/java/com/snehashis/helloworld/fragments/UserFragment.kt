package com.snehashis.helloworld.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.snehashis.helloworld.R
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.transition.TransitionInflater
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialContainerTransform
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.snehashis.helloworld.HelloWorldUser
import com.snehashis.helloworld.MainActivity
import kotlinx.android.synthetic.main.fragment_user.*
import kotlinx.android.synthetic.main.dialog_edit_text.view.*
import kotlinx.android.synthetic.main.fragment_people.view.*
import kotlinx.android.synthetic.main.fragment_user.view.*
import javax.security.auth.callback.Callback

private val mAuth : FirebaseAuth = FirebaseAuth.getInstance()

private const val ARGUMENT_USER = "userToLoad"
private const val ARGUMENT_IS_CURRENT = "isCurrentUser"

private const val KEY_NAME_PEOPLE = "Name"
private const val KEY_USERS_COLLECTION = "Users"
private const val KEY_BIO_PEOPLE = "bio"

private const val KEY_PROFILE_IMAGE = "profiles/images"
private const val IMAGE_INTENT = 1001

private val fireStorageReference  = FirebaseStorage.getInstance().getReference(KEY_PROFILE_IMAGE)
private val fireStoreReference = FirebaseFirestore.getInstance()
private val userCollection = fireStoreReference.collection(KEY_USERS_COLLECTION)
private var IMG_URI : Uri? = null
private const val DEFAULT_PIC = "https://itg.wfu.edu/wp-content/uploads/Cogn_mode-225x225.png"

class UserFragment : Fragment() {
    @SuppressLint("SetTextI18n")

    companion object{
        @JvmStatic
        fun newInstance(userToLoad : HelloWorldUser? = null) = UserFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARGUMENT_USER, userToLoad)
                putBoolean(ARGUMENT_IS_CURRENT, (userToLoad == null))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Not working ATM will fix or remove later
        /*val transition = MaterialContainerTransform()
        transition.duration = 300
        transition.fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
        transition.fadeProgressThresholds = MaterialContainerTransform.ProgressThresholds(0f, 1f)
        sharedElementEnterTransition = transition*/
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_user, container, false)
        val user = mAuth.currentUser
        val imageUploadProgress = root.imageUploadProgress
        val uidTv = root.uidTv
        val emailTv = root.emailTv
        val nameTv = root.nameTv
        val userImage = root.userImage
        val editProfilePic = root.editProfilePic
        val details = root.details
        val bioTv = root.bioTv
        val profileConstraintLayout = root.profileConstraintLayout
        val profileFrameLayout = root.profileFrameLayout
        //Retrieving passed arguments from bundled arguments
        val isCurrentUser = arguments?.getBoolean(ARGUMENT_IS_CURRENT)!!
        val receivedUser  = arguments?.getSerializable(ARGUMENT_USER) as HelloWorldUser?
        imageUploadProgress.visibility = View.GONE

        if (isCurrentUser){
            uidTv.text = user?.uid
            emailTv.text = user?.email
            nameTv.text = user?.displayName
            userCollection.document(user!!.uid).addSnapshotListener { value, error ->
                if (error == null)
                    bioTv.text = when (value?.getString(KEY_BIO_PEOPLE)){
                        null -> "Hello_World"
                        else -> value.getString(KEY_BIO_PEOPLE)
                    }
            }
            if (user!!.photoUrl != null) {
                Glide.with(this).load(user.photoUrl).circleCrop().into(userImage)
            } else
                Glide.with(this).load(DEFAULT_PIC).into(userImage)

            nameTv.setOnClickListener {
                editUserDetails(nameTv)
            }
            bioTv.setOnClickListener {
                editUserDetails(bioTv)
            }
            editProfilePic.setOnClickListener {
                openImage()
            }
            root.btn_logout.setOnClickListener {
                if (user.isAnonymous)
                    user.delete()
                mAuth.signOut()
                startActivity(Intent(activity, MainActivity::class.java))
                activity!!.finish()
            }
        }
        else if (receivedUser != null){
            details.visibility = View.GONE
            root.btn_logout.visibility = View.GONE
            editProfilePic.visibility = View.GONE
            nameTv.text = receivedUser.displayName
            bioTv.text = receivedUser.bio
            profileConstraintLayout.transitionName = receivedUser.uid
            Glide.with(this).load(receivedUser.photoUrl).circleCrop().into(userImage)
        }

        return root
    }

    private fun editUserDetails(textView: TextView) {
        val newLayout = layoutInflater.inflate(R.layout.dialog_edit_text, null)
        newLayout.textInDialog.setText(textView.text)
        val nameDialog = MaterialAlertDialogBuilder(activity!!)
        nameDialog.setTitle(when(textView.id){
            R.id.nameTv -> "Enter Name"
            R.id.bioTv -> "Enter custom Bio"
            else -> "<PlaceHolder>"
        })
        nameDialog.setView(newLayout)
        nameDialog.setPositiveButton("Ok") { _, _ ->
            if (newLayout.textInDialog.text.toString().isNotBlank()) {
                if (textView.id == R.id.nameTv){
                    val userProfile = UserProfileChangeRequest.Builder()
                        .setDisplayName(newLayout.textInDialog.text.toString()).build()
                    mAuth.currentUser?.updateProfile(userProfile)
                        ?.addOnCompleteListener {
                            userCollection.document(mAuth.currentUser!!.uid).update(KEY_NAME_PEOPLE, newLayout.textInDialog.text.toString()).addOnSuccessListener {
                                Toast.makeText(
                                    activity,
                                    "Changed name successful\nNote: Previous Messages won't be affected",
                                    Toast.LENGTH_SHORT).show()
                            }
                            textView.text = newLayout.textInDialog.text
                        }
                }
                else if (textView.id == R.id.bioTv){
                    userCollection.document(mAuth.currentUser!!.uid).update(KEY_BIO_PEOPLE, newLayout.textInDialog.text.toString())
                }
            }
        }
        nameDialog.setNegativeButton("Cancel") { _, _ ->
            //Nothing
        }
        val dialog = nameDialog.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(activity!!.getColor(R.color.secondaryColor))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(activity!!.getColor(R.color.secondaryColor))
        }
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun setProfilePicture() {
        val currentImageReference = fireStorageReference.child(mAuth.currentUser!!.uid)
        imageUploadProgress.visibility = View.VISIBLE
        currentImageReference.putFile(IMG_URI!!).addOnSuccessListener {
            Toast.makeText(activity, "Upload Successful", Toast.LENGTH_SHORT).show()
            currentImageReference.downloadUrl.addOnSuccessListener { storage_link ->
                val userProfileChangeRequest = UserProfileChangeRequest.Builder().setPhotoUri(storage_link).build()
                mAuth.currentUser!!.updateProfile(userProfileChangeRequest).addOnSuccessListener {
                    imageUploadProgress.visibility = View.GONE
                    Glide.with(this)
                        .load(storage_link)
                        .circleCrop()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .transition(DrawableTransitionOptions.withCrossFade(DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()))
                        .into(userImage)
                    Toast.makeText(activity, "Profile Updated Successfully", Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnProgressListener {  progressSnapshot ->
            val progress = (100.0 * progressSnapshot.bytesTransferred /  progressSnapshot.totalByteCount).toInt()
            if(Build.VERSION.SDK_INT >= 24)
                imageUploadProgress.setProgressCompat(progress, true)
            else
                imageUploadProgress.progress = progress
        }
    }

    private fun openImage() {
        ImagePicker.with(this)
            .crop()
            .galleryOnly()
            .compress(320)
            .start(IMAGE_INTENT)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            IMAGE_INTENT -> {
                if (resultCode == AppCompatActivity.RESULT_OK){
                    IMG_URI = data!!.data
                    val confirmDialog = MaterialAlertDialogBuilder(activity!!)
                    confirmDialog.setTitle("Confirm profile picture?")
                    confirmDialog.setPositiveButton("Confirm") {_, _ ->
                        setProfilePicture()
                    }.setNegativeButton("Cancel", null)
                    val dialog = confirmDialog.create()
                    dialog.setOnShowListener {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(activity!!.getColor(R.color.secondaryColor))
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(activity!!.getColor(R.color.secondaryDarkColor))
                    }
                    dialog.setCancelable(false)
                    dialog.show()
                }
            }
        }
    }
}