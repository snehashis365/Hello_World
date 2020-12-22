package com.snehashis.helloworld

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.snehashis.helloworld.fragments.AboutFragment
import com.snehashis.helloworld.fragments.PeopleFragment
import com.snehashis.helloworld.fragments.UserFragment
import kotlinx.android.synthetic.main.activity_bottom_navigation.*

class BottomNavigation : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bottom_navigation)
        val userFragment = UserFragment.newInstance()
        val aboutFragment = AboutFragment()
        val peopleFragment = PeopleFragment()

        bottomNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.user -> switchToFragment(userFragment)
                R.id.about -> switchToFragment(aboutFragment)
                R.id.people -> switchToFragment(peopleFragment)
            }
            true
        }
        //Will get the value of the string field from ChatRoom.kt made it public for this usage
        bottomNavigationView.selectedItemId = when(intent.getIntExtra(MENU_IEM_INTENT, 1)) {
            0 -> R.id.people
            1 -> R.id.user
            else -> R.id.about
        }

    }
    private fun switchToFragment(fragment : Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.frameLayout, fragment)
            commit()
        }
    }
}