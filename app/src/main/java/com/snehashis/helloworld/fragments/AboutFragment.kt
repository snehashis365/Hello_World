package com.snehashis.helloworld.fragments

import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.snehashis.helloworld.BuildConfig
import com.snehashis.helloworld.R
import kotlinx.android.synthetic.main.fragment_about.view.*


class AboutFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_about, container, false)

        var count = 0
        root.version_layout.setOnClickListener {
            count += 1
            Handler().postDelayed({
                count = 0
            }, 5000)
            if (count >= 8) {
                Toast.makeText(activity, "No easter egg yet 👷‍♂️", Toast.LENGTH_SHORT).show()
            }
        }
        root.version_name.text = BuildConfig.VERSION_NAME



        return root
    }

}