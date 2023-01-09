package com.decagonhq.clads.ui.client

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.decagonhq.clads.R
import com.decagonhq.clads.data.domain.client.Client
import com.decagonhq.clads.data.domain.client.ClientReg
import com.decagonhq.clads.data.domain.client.DeliveryAddress
import com.decagonhq.clads.data.domain.client.Measurement
import com.decagonhq.clads.databinding.AddClientFragmentBinding
import com.decagonhq.clads.ui.BaseFragment
import com.decagonhq.clads.ui.client.adapter.AddClientPagerAdapter
import com.decagonhq.clads.ui.profile.updateToolbarTitleListener
import com.decagonhq.clads.util.Resource
import com.decagonhq.clads.util.handleApiError
import com.decagonhq.clads.util.hideView
import com.decagonhq.clads.util.showSnackBar
import com.decagonhq.clads.util.showView
import com.decagonhq.clads.viewmodels.ClientViewModel
import com.decagonhq.clads.viewmodels.ClientsRegisterViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class AddClientFragment : BaseFragment() {
    private var _binding: AddClientFragmentBinding? = null
    val args: AddClientFragmentArgs by navArgs()
    var editClient: Client? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!
    private lateinit var viewPager2: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var nextAndSaveButton: TextView
    private lateinit var previousButton: TextView
    private lateinit var clientBio: ClientReg
    private var clientMeasurements: List<Measurement>? = null
    private lateinit var clientDeliveryAddress: DeliveryAddress
    private var isClientAccountFragmentSaved: Boolean = false
    private var pagePosition = 0

    private val clientsRegisterViewModel: ClientsRegisterViewModel by activityViewModels()
    private val clientViewModel: ClientViewModel by activityViewModels()

    override fun onStart() {
        super.onStart()
        if (args.client != null) {
            (activity as updateToolbarTitleListener).updateTitle("Edit Client")
        } else {
            (activity as updateToolbarTitleListener).updateTitle("Add Client")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editClient = args.client

        if (editClient == null) {
            clientsRegisterViewModel.clearAddress()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = AddClientFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()
        setEventListeners()
        setObservers()
    }

    override fun onResume() {
        super.onResume()
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setEventListeners() {

        nextAndSaveButton.setOnClickListener {

            saveChildFragmentData(pagePosition)

            if (pagePosition == 0 && isClientAccountFragmentSaved) {
                viewPager2.currentItem = 1
            } else if (pagePosition == 1 && clientMeasurements?.isNotEmpty() == true) {
                viewPager2.currentItem = 2

                setFragmentResult("requestKey", bundleOf("data" to editClient))
            } else if (pagePosition == 2 && this::clientDeliveryAddress.isInitialized) {

                if (this::clientBio.isInitialized && clientMeasurements?.isNotEmpty() == true) {
                    if (editClient == null) {
                        /*Add Client*/
                        clientViewModel.addClient(
                            Client(
                                fullName = clientBio.fullName,
                                phoneNumber = clientBio.phoneNumber,
                                email = clientBio.email,
                                gender = clientBio.gender,
                                deliveryAddresses = arrayListOf(clientDeliveryAddress),
                                measurements = clientMeasurements
                            )
                        )
                    } else {
                        /*Update Client*/
                        editClient!!.id?.let { it1 ->
                            clientViewModel.updateClient(
                                it1,
                                Client(
                                    // id = clientBio.id, // ------ //
                                    artisanId = editClient!!.artisanId,
                                    fullName = editClient!!.fullName,
                                    phoneNumber = editClient!!.phoneNumber,
                                    email = editClient!!.email,
                                    gender = editClient!!.gender,
                                    deliveryAddresses = arrayListOf(clientDeliveryAddress),
                                    measurements = clientMeasurements
                                ).also {
                                    Log.d("ALSO_CLIENT", " $it")
                                }
                            )
                        }
                    }

                    clientViewModel.client.observe(
                        viewLifecycleOwner,
                        Observer {
                            when (it) {
                                is Resource.Loading -> {
                                    editClient?.let {
                                        progressDialog.showDialogFragment("Updating profile... Please wait")
                                    } ?: progressDialog.showDialogFragment("Saving... Please wait")
                                }
                                is Resource.Error -> {
                                    progressDialog.hideProgressDialog()
                                    handleApiError(
                                        it,
                                        mainRetrofit,
                                        requireView(),
                                        sessionManager,
                                        database
                                    )
                                }
                                is Resource.Success -> {
                                    progressDialog.hideProgressDialog()
                                    it.data?.let {
                                        editClient?.let {
                                            showToast("Updated Successfully")
                                            findNavController().popBackStack(R.id.clientFragment, false)
                                        } ?: showToast("Saved Successfully")
                                    }

                                    clientsRegisterViewModel.clearMeasurement()
                                    findNavController().popBackStack(R.id.clientFragment, false)
                                }
                            }
                        }
                    )
                } else {
                    nextAndSaveButton.showSnackBar("Something Went Wrong, Retry")
                }
            } else {
                nextAndSaveButton.showSnackBar("Some Fields have not been entered")
            }
        }

        viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                pagePosition = position
                when (position) {
                    0 -> {
                        binding.addClientFragmentNextButton.text = getString(R.string.all_next)
                        binding.addClientFragmentPreviousButton.hideView()
                    }
                    1 -> {
                        binding.addClientFragmentNextButton.text = getString(R.string.all_next)
                        binding.addClientFragmentPreviousButton.showView()
                    }
                    2 -> {
                        if (editClient != null) {
                            binding.addClientFragmentNextButton.text =
                                getString(R.string.all_update)
                        } else {
                            binding.addClientFragmentNextButton.text = getString(R.string.all_save)
                        }
                        binding.addClientFragmentPreviousButton.showView()
                    }
                }
            }
        })

        previousButton.setOnClickListener {
            when (pagePosition) {
                2 -> {
                    viewPager2.currentItem = 1
                }
                1 -> {
                    viewPager2.currentItem = 0
                }
            }
        }
    }

    private fun init() {

        viewPager2 = binding.addClientViewPager
        tabLayout = binding.addClientTabLayout
        nextAndSaveButton = binding.addClientFragmentNextButton
        previousButton = binding.addClientFragmentPreviousButton

        val adapter = AddClientPagerAdapter(childFragmentManager, lifecycle)

        viewPager2.apply {
            this.adapter = adapter
            this.isUserInputEnabled = false
        }

        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
            when (position) {
                0 -> tab.text = getString(R.string.client_account)
                1 -> tab.text = getString(R.string.measurements)
                2 -> tab.text = getString(R.string.delivery_addresses)
            }
        }.attach()

        /*Set Edit Client Variables*/
        editClient?.let {

            val clientReg =
                ClientReg(it.id, it.artisanId, it.email, it.fullName, it.gender, it.phoneNumber)
            clientsRegisterViewModel.setClient(clientReg)

            it.measurements?.let { measurementList ->
                clientsRegisterViewModel.setList(
                    measurementList
                )
            }

            it.deliveryAddresses?.let { deliveryAddresses ->
                deliveryAddresses.firstOrNull()?.let { it1 ->
                    clientsRegisterViewModel.clientNewAddress(
                        it1
                    )
                }
            }
        }
        /*Disable tab change on tapping tab-layout container*/
        tabLayout.touchables.forEach { it.isClickable = false }
    }

    private fun setObservers() {
        clientsRegisterViewModel.clientData.observe(
            viewLifecycleOwner,
            Observer {
                clientBio = it
            }
        )

        clientsRegisterViewModel.measurementData.observe(
            viewLifecycleOwner,
            Observer {
                clientMeasurements = it
            }
        )

        clientsRegisterViewModel.clientAddress.observe(
            viewLifecycleOwner,
            Observer { deliveryAddress ->
                // Only proceed if the event has never been handled
                clientDeliveryAddress = deliveryAddress
            }
        )
    }

    private fun saveChildFragmentData(position: Int) {
        when (val fragment = childFragmentManager.fragments[position]) {
            is ClientAccountFragment -> {
                isClientAccountFragmentSaved = fragment.saveToViewModel()
            }

            is MeasurementsFragment -> {
                fragment.saveToViewModel()
            }

            is DeliveryAddressFragment -> {
                fragment.saveToViewModel()
            }
        }
    }
}
