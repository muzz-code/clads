package com.decagonhq.clads.data.domain

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MessagesNotificationModel(
    var text: String?,
    var firstName: String?,
    var lastName: String?,
    var fromEmail: String?,
    var userId: Int?,
    var userImage: String?

) : Parcelable {
    constructor() : this("", "", "", "", 1, "")
}
