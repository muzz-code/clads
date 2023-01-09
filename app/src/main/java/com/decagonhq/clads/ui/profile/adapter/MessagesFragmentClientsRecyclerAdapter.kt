package com.decagonhq.clads.ui.profile.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.decagonhq.clads.data.domain.ChatMessageModel
import com.decagonhq.clads.data.domain.MessagesNotificationModel
import com.decagonhq.clads.databinding.ChatRecyclerviewItemBinding
import com.decagonhq.clads.ui.messages.MessagesFragmentDirections
import com.decagonhq.clads.util.loadImage
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase

class MessagesFragmentClientsRecyclerAdapter(
    private var messageNotificationList: List<MessagesNotificationModel>,
    val email: String
) :
    RecyclerView.Adapter<MessagesFragmentClientsRecyclerAdapter.ViewHolder>() {

    val latestMessagesHashMap = HashMap<String, ChatMessageModel>()

    inner class ViewHolder(val binding: ChatRecyclerviewItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val clientName = binding.chatRecyclerViewItemClientNameTextView
        val notificationBody = binding.chatRecyclerViewItemMessageTextView
    }

    /*inflate the views*/
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ChatRecyclerviewItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    /*return the messageNotificationList*/
    override fun getItemCount(): Int {
        return messageNotificationList.size
    }

    /*bind the views with their holders*/
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.apply {
            with(holder) {
                with(messageNotificationList[position]) {
                    val clientFullName = "$firstName $lastName"
                    clientName.text = clientFullName
                    notificationBody.text = text

                    binding.chatRecyclerViewItemImageView.loadImage(userImage)
                    binding.chatRecyclerViewItemParentLayout.setOnClickListener {
                        val client = messageNotificationList[position]
                        val action =
                            MessagesFragmentDirections.actionNavMessagesToClientChatFragment2(client)
                        findNavController().navigate(action)
                    }
                    val reference =
                        FirebaseDatabase.getInstance().getReference("/latest-messages/$email")

                    reference.addChildEventListener(object : ChildEventListener {
                        override fun onChildAdded(
                            snapshot: DataSnapshot,
                            previousChildName: String?
                        ) {
                            val chatMessage =
                                snapshot.getValue(ChatMessageModel::class.java) ?: return
                            if (snapshot.key == fromEmail) {
                                binding.chatRecyclerViewItemMessageTextView.text = chatMessage.text
                                binding.chatRecyclerViewItemTimeTextView.text =
                                    chatMessage.timeStamp
                                latestMessagesHashMap[snapshot.key!!] = chatMessage
                            }
                        }

                        override fun onChildChanged(
                            snapshot: DataSnapshot,
                            previousChildName: String?
                        ) {}

                        override fun onChildRemoved(snapshot: DataSnapshot) {}
                        override fun onChildMoved(
                            snapshot: DataSnapshot,
                            previousChildName: String?
                        ) {}

                        override fun onCancelled(error: DatabaseError) {}
                    })
                }
            }
        }
    }
}
