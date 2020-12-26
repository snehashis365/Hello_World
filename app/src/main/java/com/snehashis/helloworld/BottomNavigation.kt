package com.snehashis.helloworld

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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
        val requestedFragment = intent.getIntExtra(MENU_IEM_INTENT, 1)

        expandable_bottom_bar.onItemSelectedListener = { _, menuItem ->
            when(menuItem.itemId) {
                R.id.user -> switchToFragment(userFragment)
                R.id.about -> switchToFragment(aboutFragment)
                R.id.people -> switchToFragment(peopleFragment)
            }
        }
        //Will get the value of the string field from ChatRoom.kt made it public for this usage
        expandable_bottom_bar.select(when(requestedFragment) {
            0 -> R.id.people
            1 -> R.id.user
            else -> R.id.about
        })
        switchToFragment(when(requestedFragment){
            0 -> peopleFragment
            1 -> userFragment
            else -> aboutFragment
        })
    }
    private fun switchToFragment(fragment : Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.frameLayout, fragment)
            commit()
        }
    }
}