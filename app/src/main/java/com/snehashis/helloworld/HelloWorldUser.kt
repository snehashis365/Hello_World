package com.snehashis.helloworld

import com.google.firebase.Timestamp

class HelloWorldUser(val displayName : String,
                     val uid : String,
                     val photoUrl : String,
                     var isOnline : Boolean,
                     var lastSeen : Timestamp? = null,
                     var bio : String = "Hello World") {
}