package com.snehashis.helloworld

import com.google.firebase.Timestamp

class Message(val user: String?, val text : String?, val isImage: Boolean = false, val imageUri: String = "", val timestamp: Timestamp?, val uid: String?, val msgID: String, var isSelected: Boolean = false) {

}