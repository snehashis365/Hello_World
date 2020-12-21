package com.snehashis.helloworld.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.snehashis.helloworld.R


class PeopleFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_people, container, false)

        Toast.makeText(activity, "Coming Soon 😉", Toast.LENGTH_LONG).show()

        return root
    }

}