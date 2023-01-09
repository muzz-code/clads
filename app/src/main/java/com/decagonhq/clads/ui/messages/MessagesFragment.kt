package com.decagonhq.clads.ui.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.decagonhq.clads.data.domain.MessagesNotificationModel
import com.decagonhq.clads.databinding.MessagesFragmentBinding
import com.decagonhq.clads.ui.BaseFragment
import com.decagonhq.clads.ui.profile.adapter.MessagesFragmentClientsRecyclerAdapter
import com.decagonhq.clads.ui.profile.updateToolbarTitleListener
import com.decagonhq.clads.util.EncodeEmail
import com.decagonhq.clads.viewmodels.UserProfileViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import timber.log.Timber

class MessagesFragment : BaseFragment() {

    private var _binding: MessagesFragmentBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!
    private lateinit var notificationRecyclerView: RecyclerView
    private lateinit var notificationAdapter: MessagesFragmentClientsRecyclerAdapter
    private var userArrayList: ArrayList<MessagesNotificationModel> = arrayListOf()
    private val userProfileViewModel: UserProfileViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = MessagesFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userProfileViewModel.userProfile.observe(viewLifecycleOwner) {
            val userEmail = EncodeEmail.encodeUserEmail(it.data?.email)
            getClient(userEmail)
            it.data?.let { it1 ->
                (activity as updateToolbarTitleListener).updateUserName(
                    it1.firstName
                )
            }
            it.data?.let {
                (activity as updateToolbarTitleListener).profileImage(it.thumbnail)
            }
        }

        notificationRecyclerView = binding.messagesFragmentClientMessagesRecyclerView
        notificationRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
    }

    private fun getClient(userEmail: String?) {

        val firebaseRef = FirebaseDatabase.getInstance().getReference("/users")

        firebaseRef.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                userArrayList.clear()
                Timber.e(userEmail)
                if (snapshot.exists()) {
                    val userList =
                        snapshot.children.mapNotNull {
                            it.getValue(MessagesNotificationModel::class.java)
                        }.filter {
                            it.fromEmail != userEmail && !it.firstName.isNullOrEmpty()
                        }
                    userProfileViewModel.userProfile.observe(
                        viewLifecycleOwner,
                        {
                            it.data?.let { userProfile ->
                                notificationAdapter = EncodeEmail.encodeUserEmail(userProfile.email)?.let { it1 ->
                                    MessagesFragmentClientsRecyclerAdapter(userList, it1)
                                }!!
                            }
                        }
                    )
                    notificationRecyclerView.adapter = notificationAdapter
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
