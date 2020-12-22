package com.snehashis.helloworld.fragments

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialElevationScale
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.snehashis.helloworld.HelloWorldUser
import com.snehashis.helloworld.R
import com.snehashis.helloworld.TAG
import com.snehashis.helloworld.UserAdapter
import kotlinx.android.synthetic.main.fragment_people.view.*

private const val KEY_USERS_PEOPLE = "Users"
private const val KEY_NAME_PEOPLE = "Name"
private const val KEY_ONLINE_PEOPLE = "isOnline"
private const val KEY_PHOTO_PEOPLE =  "photoUri"
private const val KEY_TIME_PEOPLE = "lastSeen"
private const val KEY_BIO_PEOPLE = "bio"
private const val KEY_UID_PEOPLE = "uid"

private val fireStoreReference : FirebaseFirestore = FirebaseFirestore.getInstance()
private val usersCollection = fireStoreReference.collection(KEY_USERS_PEOPLE)

private var userList = mutableListOf<HelloWorldUser>()



class PeopleFragment : Fragment(), UserAdapter.UserClickListener {

    lateinit var peopleGridView : RecyclerView
    private val mUser = FirebaseAuth.getInstance().currentUser!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Not Working ATM will fix or remove later
        /*val transition = MaterialContainerTransform()
        transition.duration = 300
        transition.fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
        transition.fadeProgressThresholds = MaterialContainerTransform.ProgressThresholds(0f, 1f)
        sharedElementEnterTransition = transition*/
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_people, container, false)

        val peopleGridAdapter = UserAdapter(this, userList)
        root.peopleGridView.adapter = peopleGridAdapter
        val peopleGridLayoutManager = GridLayoutManager(activity!!, 2,GridLayoutManager.VERTICAL, false)
        root.peopleGridView.layoutManager = peopleGridLayoutManager
        peopleGridAdapter.notifyDataSetChanged()
        peopleGridView = root.peopleGridView
        return root
    }
    override fun onUserClick(position: Int,clickedUser: HelloWorldUser,clickedUserView: LinearLayout){
        clickedUserView.transitionName = clickedUser.uid
        val userProfileFragment = UserFragment.newInstance(clickedUser)
        fragmentManager!!.beginTransaction().apply {
            addSharedElement(clickedUserView, clickedUser.uid)
            addToBackStack(TAG)
            replace(R.id.frameLayout, userProfileFragment)
            commit()
        }

    }
    private fun updateStatus(isOnline : Boolean){
        usersCollection.document(mUser.uid).update(KEY_ONLINE_PEOPLE, isOnline)
        usersCollection.document(mUser.uid).update(KEY_TIME_PEOPLE, Timestamp.now())
    }

    private fun buildUser(document : QueryDocumentSnapshot) : HelloWorldUser {
        val userDisplayName = document.getString(KEY_NAME_PEOPLE)!!
        val userUID = document.getString(KEY_UID_PEOPLE)!!
        val userIsOnline = when(userUID) {
            mUser.uid -> true
            else -> document.getBoolean(KEY_ONLINE_PEOPLE)!!
        }
        var userPhotoUri = document.getString(KEY_PHOTO_PEOPLE)!!
        if (userPhotoUri.isBlank()) userPhotoUri = "https://itg.wfu.edu/wp-content/uploads/Cogn_mode-225x225.png"
        val userLastSeen = document.getTimestamp(KEY_TIME_PEOPLE)
        var userBio = document.getString(KEY_BIO_PEOPLE)
        if (userBio == null) userBio = "Hello_World"
        return HelloWorldUser(userDisplayName, userUID, userPhotoUri, userIsOnline, userLastSeen, userBio)
    }

    override fun onStart() {
        super.onStart()
        usersCollection.orderBy(KEY_TIME_PEOPLE, Query.Direction.DESCENDING).addSnapshotListener{ value, error ->
            if (value != null) {
                userList.clear()
                for (document in value) {
                    if (document.id != "Default") {
                        val user = buildUser(document)
                        userList.add(user)
                    }
                }
                peopleGridView.adapter?.notifyDataSetChanged()

            }
            else {
                error?.printStackTrace()
                Log.e("FireStore", error?.message, error)
            }
        }
    }

    override fun onResume() {
        Handler().postDelayed({
            updateStatus(true)
        },1000)
        super.onResume()
    }

    override fun onStop() {
        updateStatus(false)
        super.onStop()
    }
}