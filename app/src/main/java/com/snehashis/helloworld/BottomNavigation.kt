package com.snehashis.helloworld

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.auth.User
import com.snehashis.helloworld.fragments.UserFragment
import kotlinx.android.synthetic.main.activity_bottom_navigation.*

class BottomNavigation : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bottom_navigation)

        val userFragment = UserFragment()
        val userFragment2 = UserFragment()

        switchToFragment(userFragment)

        bottomNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.btn_user -> switchToFragment(userFragment)
                R.id.btn_about -> switchToFragment(userFragment2)
            }
            true
        }

    }
    private fun switchToFragment(fragment : Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.frameLayout, fragment)
            commit()
        }
    }
}