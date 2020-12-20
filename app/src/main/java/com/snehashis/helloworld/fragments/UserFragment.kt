package com.snehashis.helloworld.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.snehashis.helloworld.R
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.snehashis.helloworld.MainActivity
import kotlinx.android.synthetic.main.fragment_user.*
import kotlinx.android.synthetic.main.dialog_edit_text.view.*
import kotlinx.android.synthetic.main.fragment_user.view.*
import java.io.ByteArrayOutputStream

private lateinit var mAuth : FirebaseAuth

private const val KEY_PROFILE_IMAGE = "profiles/images"
private const val IMAGE_INTENT = 1001

private val fireStorageReference : StorageReference = FirebaseStorage.getInstance().getReference(KEY_PROFILE_IMAGE)
private var IMG_URI : Uri? = null
private const val DEFAULT_PIC = "https://itg.wfu.edu/wp-content/uploads/Cogn_mode-225x225.png"

/**
 * A simple [Fragment] subclass.
 * Use the [UserFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class UserFragment : Fragment() {
    @SuppressLint("SetTextI18n")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_user, container, false)
        mAuth = FirebaseAuth.getInstance()
        val user = mAuth.currentUser
        val imageUploadProgress = root.imageUploadProgress
        val uidTv = root.uidTv
        val emailTv = root.emailTv
        val nameTv = root.nameTv
        val userImage = root.userImage
        val editProfilePic = root.editProfilePic
        val btn_logout = root.btn_logout

        imageUploadProgress.visibility = View.GONE
        uidTv.text = user?.uid
        emailTv.text = user?.email
        nameTv.text = user?.displayName
        if (user!!.photoUrl != null) {
            Glide.with(this).load(user.photoUrl).circleCrop().into(userImage)
        }
        else
            Glide.with(this).load(DEFAULT_PIC).into(userImage)

        nameTv.setOnClickListener {
            val newName = layoutInflater.inflate(R.layout.dialog_edit_text,null)
            newName.textInDialog.setText(user.displayName)
            val nameDialog = AlertDialog.Builder(activity!!)
            nameDialog.setTitle("Enter Name")
            nameDialog.setView(newName)
            nameDialog.setPositiveButton("Ok") {_, _ ->
                if (newName.textInDialog.text.toString().isNotBlank()) {
                    val anonymousUserProfile = UserProfileChangeRequest.Builder().setDisplayName(newName.textInDialog.text.toString()).build()
                    mAuth.currentUser?.updateProfile(anonymousUserProfile)?.addOnCompleteListener {
                        Toast.makeText(activity, "Changed name successful\nNote: Previous Messages won't be affected", Toast.LENGTH_SHORT).show()
                        nameTv.text = newName.textInDialog.text
                    }
                }
            }
            nameDialog.setNegativeButton("Cancel") {_, _ ->
                //Nothing
            }
            val dialog = nameDialog.create()
            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(activity!!.getColor(R.color.secondaryColor))
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(activity!!.getColor(R.color.secondaryColor))
            }
            dialog.setCancelable(false)
            dialog.show()
        }
        editProfilePic.setOnClickListener {
            openImage()
        }
        btn_logout.setOnClickListener {
            if (user.isAnonymous)
                user.delete()
            mAuth.signOut()
            startActivity(Intent(activity, MainActivity::class.java))
            activity!!.finish()
        }

        return root
    }

    private fun setProfilePicture() {
        val currentImageReference = fireStorageReference.child(mAuth.currentUser!!.uid)
        imageUploadProgress.visibility = View.VISIBLE
        val compressedImage = compressJpegToByteArray(IMG_URI!!)
        currentImageReference.putBytes(compressedImage).addOnSuccessListener {
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

    @Suppress("DEPRECATION")
    private fun getImageBitmap(selectedPhotoUri: Uri): Bitmap =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            MediaStore.Images.Media.getBitmap(activity?.contentResolver, selectedPhotoUri)
        else {
            val source = ImageDecoder.createSource(activity!!.contentResolver, selectedPhotoUri)
            ImageDecoder.decodeBitmap(source)
        }

    private fun compressJpegToByteArray(uri: Uri, quality: Int = 40) : ByteArray {
        val bitmap = getImageBitmap(uri)
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    private fun openImage() {
        val imageOpenIntent = Intent()
        imageOpenIntent.type = "image/*"
        imageOpenIntent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(imageOpenIntent, IMAGE_INTENT)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            IMAGE_INTENT -> {
                if (resultCode == AppCompatActivity.RESULT_OK){
                    IMG_URI = data!!.data
                    setProfilePicture()
                }
            }
        }
    }
}