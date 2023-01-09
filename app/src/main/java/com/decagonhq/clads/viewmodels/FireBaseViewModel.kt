package com.decagonhq.clads.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decagonhq.clads.data.domain.ChatMessageModel
import com.decagonhq.clads.data.domain.MessagesNotificationModel
import com.decagonhq.clads.repository.UserProfileRepository
import com.decagonhq.clads.ui.messages.ChatReceiverItem
import com.decagonhq.clads.ui.messages.ChatSenderItem
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FireBaseViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    lateinit var dataSenderBaseRef: DatabaseReference
    lateinit var dataReceiverBaseRef: DatabaseReference

    private var _chatSenderItem = MutableLiveData<ChatSenderItem>()
    var chatSenderItem: LiveData<ChatSenderItem> = _chatSenderItem

    private var _chatReceiverItem = MutableLiveData<ChatReceiverItem>()
    var chatReceiverItem: LiveData<ChatReceiverItem> = _chatReceiverItem

    fun sendMessages(chatMessage: ChatMessageModel?, toId: String, fromEmail: String) {

        viewModelScope.launch {

            // set value to firebase
            // reference to database
            // chatMessage.id = dataReceiverBaseRef.key

            dataSenderBaseRef = FirebaseDatabase.getInstance().getReference("/text-messages/$toId/$fromEmail").push()
            dataReceiverBaseRef = FirebaseDatabase.getInstance().getReference("/text-messages/$fromEmail/$toId").push()

            // set value to the node
            dataReceiverBaseRef.setValue(chatMessage)
            dataSenderBaseRef.setValue(chatMessage)

            // set last sent/received messages
            val latestMessagesSender =
                FirebaseDatabase.getInstance().getReference("/latest-messages/$toId/$fromEmail")
            latestMessagesSender.setValue(chatMessage)

            val latestMessagesReceiver =
                FirebaseDatabase.getInstance().getReference("/latest-messages/$fromEmail/$toId")
            latestMessagesReceiver.setValue(chatMessage)
        }
    }

    // get chat messages
    fun receiveMessages(toId: String, fromEmail: String, args: MessagesNotificationModel) {

        viewModelScope.launch {
            val reference = FirebaseDatabase.getInstance().getReference("/text-messages/$toId/$fromEmail")
            reference.addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {

                    val chatMessage = snapshot.getValue(ChatMessageModel::class.java)

                    if (chatMessage != null) {
                        if (chatMessage.toId == args.fromEmail) {
                            ChatSenderItem(
                                chatMessage.text,
                                chatMessage.timeStamp
                            ).let {
                                _chatSenderItem.value = it
                            }
                        } else {
                            ChatReceiverItem(
                                chatMessage.text,
                                chatMessage.timeStamp
                            ).let {
                                _chatReceiverItem.value = it
                            }
                        }
                    }
                }
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }
}
