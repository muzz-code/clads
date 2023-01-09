package com.decagonhq.clads.data.domain

class ChatMessageModel(val text: String, val toId: String, val timeStamp: String, val fromId: String) {
    constructor() : this("", "", "", "")
}
