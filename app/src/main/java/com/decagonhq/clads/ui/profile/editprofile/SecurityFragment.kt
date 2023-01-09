package com.decagonhq.clads.ui.profile.editprofile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.decagonhq.clads.R
import com.decagonhq.clads.databinding.SecurityFragmentBinding
import com.decagonhq.clads.ui.BaseFragment
import com.decagonhq.clads.util.Resource
import com.decagonhq.clads.util.handleApiError
import com.decagonhq.clads.viewmodels.UserProfileViewModel

class SecurityFragment : BaseFragment() {
    private var _binding: SecurityFragmentBinding? = null
    private val userProfileViewModel: UserProfileViewModel by activityViewModels()

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = SecurityFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userProfileViewModel.userProfile.observe(
            viewLifecycleOwner,
            {
                if (it is Resource.Loading) {
                    progressDialog.showDialogFragment("Fetching profile data...")
                } else if (it is Resource.Error) {
                    progressDialog.hideProgressDialog()
                    handleApiError(it, mainRetrofit, requireView(), sessionManager, database)
                } else {
                    progressDialog.hideProgressDialog()
                    if (it.data?.phoneNumber.isNullOrEmpty()) {
                        binding.securityFragmentPhoneNumberInputTextView.hint = getString(R.string.input_phonenumber_security_fragment_hint)
                    } else {
                        binding.securityFragmentPhoneNumberInputTextView.hint = it.data?.phoneNumber
                    }
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
