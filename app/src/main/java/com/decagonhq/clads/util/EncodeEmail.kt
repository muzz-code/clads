package com.decagonhq.clads.util

object EncodeEmail {

    fun encodeUserEmail(userEmail: String?): String? {
        return userEmail?.replace(".", ",")
    }
}
