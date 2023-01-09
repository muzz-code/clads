package com.decagonhq.clads.ui.profile.editprofile

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.decagonhq.clads.R
import com.decagonhq.clads.data.domain.images.UserProfileImage
import com.decagonhq.clads.data.domain.profile.Union
import com.decagonhq.clads.data.domain.profile.UserProfile
import com.decagonhq.clads.data.domain.profile.WorkshopAddress
import com.decagonhq.clads.data.local.UserProfileEntity
import com.decagonhq.clads.databinding.AccountFragmentBinding
import com.decagonhq.clads.ui.BaseFragment
import com.decagonhq.clads.ui.profile.dialogfragment.ProfileManagementDialogFragments.Companion.createProfileDialogFragment
import com.decagonhq.clads.util.Constants
import com.decagonhq.clads.util.Resource
import com.decagonhq.clads.util.handleApiError
import com.decagonhq.clads.util.loadImage
import com.decagonhq.clads.util.observeOnce
import com.decagonhq.clads.util.saveBitmap
import com.decagonhq.clads.util.uriToBitmap
import com.decagonhq.clads.viewmodels.ArtisanLocationViewModel
import com.decagonhq.clads.viewmodels.ImageUploadViewModel
import com.decagonhq.clads.viewmodels.UserProfileViewModel
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.theartofdev.edmodo.cropper.CropImage
import dagger.hilt.android.AndroidEntryPoint
import id.zelory.compressor.Compressor
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber

@AndroidEntryPoint
class AccountFragment : BaseFragment() {

    private lateinit var _binding: AccountFragmentBinding
    private val binding get() = _binding
    private val artisanAddressViewModel: ArtisanLocationViewModel by activityViewModels()
    private val imageUploadViewModel: ImageUploadViewModel by activityViewModels()
    private val userProfileViewModel: UserProfileViewModel by activityViewModels()
    private lateinit var cropActivityResultLauncher: ActivityResultLauncher<Any?>
    private var artisanLatitude: String = ""
    private var artisanLongitude: String = ""
    private var artisanStreet: String = ""
    private var artisanCity: String = ""
    private var artisanState: String = ""
    private lateinit var locationManager: LocationManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        _binding = AccountFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* register broadcast receivers */
        val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        filter.addAction(Intent.ACTION_PROVIDER_CHANGED)
        requireContext().registerReceiver(broadcastReceiver, filter)

        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        /*Dialog fragment functions*/
        accountFirstNameEditDialog()
        accountGenderSelectDialog()
        accountUnionStateDialogFragment()
        accountUnionLGADialogFragment()
        accountUnionWardDialogFragment()
        accountUnionNameDialogFragment()
        accountLastNameDialogFragment()
        accountEmployeeNumberDialogFragment()
        accountOtherNameEditDialog()
        accountLegalStatusDialog()
        requestPermissions()

        /*Initialize Image Cropper*/
        cropActivityResultLauncher = registerForActivityResult(cropActivityResultContract) {
            it?.let { uri ->
                binding.accountFragmentEditProfileIconImageView.imageAlpha = 140
                uploadImageToServer(uri)
            }
        }

        // inflate bottom sheet
        binding.accountFragmentWorkshopAddressValueTextView.setOnClickListener {

            if (binding.accountFragmentWorkshopAddressValueTextView.text.isNullOrEmpty()) {

                val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogStyle)

                val bottomSheetView: View = layoutInflater.inflate(
                    R.layout.account_fragment_add_address_bottom_sheet,
                    view.findViewById(R.id.account_fragment_location_bottom_sheet) as LinearLayout?
                )

                val setLocationLaterRadioButton: RadioButton? =
                    bottomSheetView.findViewById(R.id.account_fragment_location_bottom_sheet_set_location_later_radio_button)

                val setLocationNowRadioButton: RadioButton? =
                    bottomSheetView.findViewById(R.id.account_fragment_location_bottom_sheet_set_location_now_radio_button)

                setLocationNowRadioButton?.setOnClickListener {
                    dialog.dismiss()
                    if (binding.accountFragmentWorkshopAddressValueTextView.text.isNullOrEmpty()) {
                        dialog.dismiss()
                        initiateMapLunch()
                    }
                }

                setLocationLaterRadioButton?.setOnClickListener {
                    dialog.dismiss()
                    setLocationLaterDialog()
                }

                dialog.setContentView(bottomSheetView)
                dialog.show()
            } else {

                val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogStyle)

                val bottomSheetView: View = layoutInflater.inflate(
                    R.layout.account_fragment_edit_location_bottom_sheet,
                    view.findViewById(R.id.account_fragment_edit_location_bottom_sheet) as LinearLayout?
                )

                val editLocationOnMapRadioButton: RadioButton? =
                    bottomSheetView.findViewById(R.id.account_fragment_location_bottom_sheet_edit_location_on_map_now_radio_button)

                val editLocationManuallyRadioButton: RadioButton? =
                    bottomSheetView.findViewById(R.id.account_fragment_location_bottom_sheet_edit_location_manually_radio_button)

                editLocationOnMapRadioButton?.setOnClickListener {
                    dialog.dismiss()
                    initiateMapLunch()
                }

                editLocationManuallyRadioButton?.setOnClickListener {
                    dialog.dismiss()
                    accountStreetAddressDialogFragment()
                }

                dialog.setContentView(bottomSheetView)
                dialog.show()
            }
        }

        /*Select profile image*/
        binding.accountFragmentEditProfileIconImageView.setOnClickListener {
            Manifest.permission.READ_EXTERNAL_STORAGE.checkForPermission(NAME, READ_IMAGE_STORAGE)
        }

        /* Update User Profile */
        binding.accountFragmentSaveChangesButton.setOnClickListener {
            updateUserProfile()
        }

        /*Get users profile*/
        userProfileViewModel.getLocalDatabaseUserProfile()
        getUserProfile()

        /* extract address */
        artisanAddressViewModel.artisanLocation.observe(

            viewLifecycleOwner,
            {
                artisanLatitude = it.artisanLatitude.toString()
                artisanLongitude = it.artisanLongitude.toString()
            }
        )

        artisanAddressViewModel.artisanLocationString.observe(
            viewLifecycleOwner,
            {
                binding.accountFragmentWorkshopAddressValueTextView.text = it

                val splittedAddress = it.split(",")
                artisanStreet = splittedAddress[0]
                artisanCity = splittedAddress[1]
                artisanState = "$splittedAddress[2], $splittedAddress[3]"
            }
        )
    }

    /*Get User Profile*/
    private fun getUserProfile() {
        userProfileViewModel.userProfile.observe(
            viewLifecycleOwner,
            Observer {
                if (it is Resource.Loading && it.data?.firstName.isNullOrEmpty()) {
                    progressDialog.showDialogFragment("Updating..")
                } else if (it is Resource.Error) {
                    progressDialog.hideProgressDialog()
                    handleApiError(it, mainRetrofit, requireView(), sessionManager, database)
                } else {
                    progressDialog.hideProgressDialog()
                    it.data?.let { userProfile ->
                        binding.apply {
                            accountFragmentFirstNameValueTextView.text = userProfile.firstName
                            accountFragmentLastNameValueTextView.text = userProfile.lastName
                            accountFragmentPhoneNumberValueTextView.text = userProfile.phoneNumber
                            accountFragmentGenderValueTextView.text = userProfile.gender
                            if (userProfile.workshopAddress?.street == null) {
                                accountFragmentWorkshopAddressValueTextView.hint =
                                    "Tap to enter shop address"
                            } else {
                                accountFragmentWorkshopAddressValueTextView.text =
                                    "${userProfile.workshopAddress.street}, ${userProfile.workshopAddress.city}, ${userProfile.workshopAddress?.state}."
                            }
                            accountFragmentNameOfUnionValueTextView.text =
                                userProfile.union?.name ?: "Tap to enter Union name"
                            accountFragmentWardValueTextView.text =
                                userProfile.union?.ward ?: "Tap to enter Union ward"
                            accountFragmentLocalGovtAreaValueTextView.text =
                                userProfile.union?.lga ?: "Tap to enter Union LGA"
                            accountFragmentStateValueTextView.text =
                                userProfile.union?.state ?: "Tap to enter Union State"
                            /*Load Profile Picture with Glide*/
                            binding.accountFragmentEditProfileIconImageView.loadImage(userProfile.thumbnail)
                        }
                    }
                }
            }
        )
    }

    private fun setLocationLaterDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.set_location_later))
            .setMessage("Location needs to be set to be visible to clients")
            .setCancelable(false)
            .setNegativeButton("Cancel") { _, _ ->
            }
            .setPositiveButton("Set Now") { _, _ ->
                initiateMapLunch()
            }
            .show()
    }

    /*Update User Profile*/
    private fun updateUserProfile() {

        userProfileViewModel.userProfile.observeOnce(
            viewLifecycleOwner,
            {
                if (it is Resource.Loading && it.data?.firstName.isNullOrEmpty()) {
                    progressDialog.showDialogFragment("Fetching profile data...")
                } else if (it is Resource.Error) {
                    progressDialog.hideProgressDialog()
                    handleApiError(it, mainRetrofit, requireView(), sessionManager, database)
                } else {

                    progressDialog.hideProgressDialog()
                    showToast("Update successful")

                    it.data?.let { profile ->

                        val userProfile = UserProfile(
                            country = profile.country,
                            deliveryTime = profile.deliveryTime,
                            email = profile.email,
                            firstName = binding.accountFragmentFirstNameValueTextView.text.toString(),
                            gender = binding.accountFragmentGenderValueTextView.text.toString(),
                            genderFocus = profile.genderFocus,
                            lastName = binding.accountFragmentLastNameValueTextView.text.toString(),
                            measurementOption = profile.measurementOption,
                            phoneNumber = binding.accountFragmentPhoneNumberValueTextView.text.toString(),
                            role = profile.role,
                            workshopAddress = WorkshopAddress(
                                artisanStreet,
                                artisanCity,
                                artisanState,
                                artisanLongitude,
                                artisanLatitude
                            ),

                            specialties = profile.specialties,
                            thumbnail = profile.thumbnail,
                            trained = profile.trained,
                            union = Union(
                                name = binding.accountFragmentNameOfUnionValueTextView.text.toString(),
                                ward = binding.accountFragmentWardValueTextView.text.toString(),
                                lga = binding.accountFragmentLocalGovtAreaValueTextView.text.toString(),
                                state = binding.accountFragmentStateValueTextView.text.toString(),
                            ),
                            paymentTerms = profile.paymentTerms,
                            paymentOptions = profile.paymentOptions
                        )

                        userProfileViewModel.updateUserProfile(userProfile)
                    }
                }
            }
        )
    }

    /*Update User Profile Picture*/
    private fun updateUserProfilePicture(downloadUri: String) {
        userProfileViewModel.userProfile.observeOnce(
            viewLifecycleOwner,
            Observer {
                if (it is Resource.Loading<UserProfileEntity>) {
                    progressDialog.showDialogFragment("Uploading...")
                } else if (it is Resource.Error) {
                    progressDialog.hideProgressDialog()
                    handleApiError(it, mainRetrofit, requireView(), sessionManager, database)
                } else {
                    it.data?.let { profile ->
                        val userProfile = UserProfile(
                            country = profile.country,
                            deliveryTime = profile.deliveryTime,
                            email = profile.email,
                            firstName = profile.firstName,
                            gender = profile.gender,
                            genderFocus = profile.genderFocus,
                            lastName = profile.lastName,
                            measurementOption = profile.measurementOption,
                            phoneNumber = profile.phoneNumber,
                            role = profile.role,
                            workshopAddress = profile.workshopAddress,
                            showroomAddress = profile.showroomAddress,
                            specialties = profile.specialties,
                            thumbnail = downloadUri,
                            trained = profile.trained,
                            union = profile.union,
                            paymentTerms = profile.paymentTerms,
                            paymentOptions = profile.paymentOptions
                        )
                        userProfileViewModel.updateUserProfile(userProfile)
                    }
                }
            }
        )
    }

    /*Check for Gallery Permission*/
    private fun String.checkForPermission(name: String, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    this
                ) == PackageManager.PERMISSION_GRANTED -> {
                    cropActivityResultLauncher.launch(null)
                }
                shouldShowRequestPermissionRationale(this) -> showDialog(this, name, requestCode)
                else -> ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(this),
                    requestCode
                )
            }
        }
    }

    // check for permission and make call
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        when (requestCode) {

            READ_IMAGE_STORAGE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    showToast("permission refused")
                } else {
                    showToast("Permission granted")
                    cropActivityResultLauncher.launch(null)
                }
            }
        }
    }

    // Show dialog for permission dialog
    private fun showDialog(permission: String, name: String, requestCode: Int) {
        // Alert dialog box
        val builder = AlertDialog.Builder(requireContext())
        builder.apply {
            // setting alert properties
            setMessage(getString(R.string.permision_to_access) + name + getString(R.string.is_required_to_use_this_app))
            setTitle("Permission required")
            setPositiveButton("Ok") { _, _ ->
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(permission),
                    requestCode
                )
            }
        }
        val dialog = builder.create()
        dialog.show()
    }

    /*function to crop picture*/
    private val cropActivityResultContract = object : ActivityResultContract<Any?, Uri>() {
        override fun createIntent(context: Context, input: Any?): Intent {
            return CropImage.activity()
                .setCropMenuCropButtonTitle("Done")
                .setAspectRatio(1, 1)
                .getIntent(context)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            var imageUri: Uri? = null
            try {
                imageUri = CropImage.getActivityResult(intent).uri
            } catch (e: Exception) {
                Timber.d(e.localizedMessage)
            }
            return imageUri
        }
    }

    /*Upload Profile Picture*/
    private fun uploadImageToServer(uri: Uri) {

        // create RequestBody instance from file
        val convertedImageUriToBitmap = uriToBitmap(uri)
        val bitmapToFile = saveBitmap(convertedImageUriToBitmap)

        /*Compress Image then Upload Image*/
        lifecycleScope.launch {
            val compressedImage = Compressor.compress(requireContext(), bitmapToFile!!)
            val imageBody = compressedImage.asRequestBody("image/jpg".toMediaTypeOrNull())
            val image = MultipartBody.Part.createFormData("file", bitmapToFile?.name, imageBody!!)
            imageUploadViewModel.mediaImageUpload(image)
        }

        /*Handling the response from the retrofit*/
        imageUploadViewModel.userProfileImage.observe(
            viewLifecycleOwner,
            Observer {
                if (it is Resource.Loading<UserProfileImage>) {
                    progressDialog.showDialogFragment("Uploading...")
                } else if (it is Resource.Error) {
                    progressDialog.hideProgressDialog()
                    handleApiError(it, mainRetrofit, requireView(), sessionManager, database)
                } else {
                    //  progressDialog.hideProgressDialog()
                    showToast("Upload Successful")
                    it.data?.downloadUri?.let { imageUrl ->
                        updateUserProfilePicture(imageUrl)
                        /*Load Profile Picture with Glide*/
                        binding.accountFragmentEditProfileIconImageView.loadImage(imageUrl)
                    }
                }
            }
        )
    }

    private fun accountLegalStatusDialog() {
        childFragmentManager.setFragmentResultListener(
            ACCOUNT_LEGAL_STATUS_REQUEST_KEY,
            requireActivity()
        ) { key, bundle ->
            // collect input values from dialog fragment and update the firstname text of user
            val legalStatus = bundle.getString(ACCOUNT_LEGAL_STATUS_BUNDLE_KEY)
            binding.accountFragmentLegalStatusValueTextView.text = legalStatus
        }

        // when employee number name value is clicked
        binding.accountFragmentLegalStatusValueTextView.setOnClickListener {
            val currentLegalStatus =
                binding.accountFragmentLegalStatusValueTextView.text.toString()
            val bundle = bundleOf(CURRENT_ACCOUNT_LEGAL_STATUS_BUNDLE_KEY to currentLegalStatus)
            createProfileDialogFragment(
                R.layout.account_legal_status_dialog_fragment,
                bundle
            ).show(
                childFragmentManager, AccountFragment::class.java.simpleName
            )
        }
    }

    // firstName Dialog
    private fun accountFirstNameEditDialog() {
        // when first name value is clicked
        childFragmentManager.setFragmentResultListener(
            ACCOUNT_FIRST_NAME_REQUEST_KEY,
            requireActivity()
        ) { key, bundle ->
            // collect input values from dialog fragment and update the firstname text of user
            val firstName = bundle.getString(ACCOUNT_FIRST_NAME_BUNDLE_KEY)
            binding.accountFragmentFirstNameValueTextView.text = firstName
        }

        // when first name value is clicked
        binding.accountFragmentFirstNameValueTextView.setOnClickListener {
            val currentFirstName = binding.accountFragmentFirstNameValueTextView.text.toString()
            val bundle = bundleOf(CURRENT_ACCOUNT_FIRST_NAME_BUNDLE_KEY to currentFirstName)
            createProfileDialogFragment(
                R.layout.account_first_name_dialog_fragment,
                bundle
            ).show(
                childFragmentManager, getString(R.string.frstname_dialog_fragment)
            )
        }
    }

    private fun accountLastNameDialogFragment() {
        // when last name value is clicked
        childFragmentManager.setFragmentResultListener(
            ACCOUNT_LAST_NAME_REQUEST_KEY,
            requireActivity()
        ) { key, bundle ->
            // collect input values from dialog fragment and update the firstname text of user
            val lastName = bundle.getString(ACCOUNT_LAST_NAME_BUNDLE_KEY)
            binding.accountFragmentLastNameValueTextView.text = lastName
        }

        // when last Name name value is clicked
        binding.accountFragmentLastNameValueTextView.setOnClickListener {
            val currentLastName = binding.accountFragmentLastNameValueTextView.text.toString()
            val bundle = bundleOf(CURRENT_ACCOUNT_LAST_NAME_BUNDLE_KEY to currentLastName)
            createProfileDialogFragment(
                R.layout.account_last_name_dialog_fragment,
                bundle
            ).show(
                childFragmentManager, AccountFragment::class.java.simpleName
            )
        }
    }

    // Other name Dialog
    private fun accountOtherNameEditDialog() {
        // when other name name value is clicked
        childFragmentManager.setFragmentResultListener(
            ACCOUNT_OTHER_NAME_REQUEST_KEY,
            requireActivity()
        ) { key, bundle ->
            // collect input values from dialog fragment and update the otherName text of user
            val otherName = bundle.getString(ACCOUNT_OTHER_NAME_BUNDLE_KEY)
            binding.accountFragmentPhoneNumberValueTextView.text = otherName
        }

        // when last Name name value is clicked
        binding.accountFragmentPhoneNumberValueTextView.setOnClickListener {
            val currentOtherName =
                binding.accountFragmentPhoneNumberValueTextView.text.toString()
            val bundle = bundleOf(CURRENT_ACCOUNT_OTHER_NAME_BUNDLE_KEY to currentOtherName)
            createProfileDialogFragment(
                R.layout.account_phone_number_dialog_fragment,
                bundle
            ).show(
                childFragmentManager, AccountFragment::class.java.simpleName
            )
        }
    }

    private fun accountStreetAddressDialogFragment() {
        childFragmentManager.setFragmentResultListener(
            ACCOUNT_WORKSHOP_STREET_REQUEST_KEY,
            requireActivity()
        ) { key, bundle ->
            // collect input values from dialog fragment and update the street value  and address of user
            val streetAddress = bundle.getString(ACCOUNT_WORKSHOP_STREET_BUNDLE_KEY)
            val currentStreetAddress =
                binding.accountFragmentWorkshopAddressValueTextView.text.toString().trim()
                    .split(",").takeLast(3)
            val editedStreetAddress =
                "${streetAddress?.trim()}, ${currentStreetAddress[0].trim()}, ${currentStreetAddress[1].trim()}, ${currentStreetAddress[2].trim()}"
            binding.accountFragmentWorkshopAddressValueTextView.text = editedStreetAddress
        }

        val currentStreetAddress = binding.accountFragmentWorkshopAddressValueTextView.text.toString().split(",").take(1)
        val stringStreetAddress = currentStreetAddress[0].trim()
        val bundle = bundleOf(CURRENT_ACCOUNT_WORKSHOP_STREET_BUNDLE_KEY to stringStreetAddress)
        createProfileDialogFragment(
            R.layout.account_workshop_street_dialog_fragment,
            bundle
        ).show(
            childFragmentManager,
            getString(R.string.tag_employee_address_dialog_fragment)
        )
    }

    private fun accountEmployeeNumberDialogFragment() {
        childFragmentManager.setFragmentResultListener(
            ACCOUNT_EMPLOYEE_REQUEST_KEY,
            requireActivity()
        ) { key, bundle ->
            // collect input values from dialog fragment and update the employee number text of user
            val employeeNumber = bundle.getString(ACCOUNT_EMPLOYEE_BUNDLE_KEY)
            binding.accountFragmentNumberOfEmployeeApprenticeValueTextView.text = employeeNumber
        }

        // when employee number name value is clicked
        binding.accountFragmentNumberOfEmployeeApprenticeValueTextView.setOnClickListener {
            val currentEmployeeNumber =
                binding.accountFragmentNumberOfEmployeeApprenticeValueTextView.text.toString()
            val bundle = bundleOf(CURRENT_ACCOUNT_EMPLOYEE_BUNDLE_KEY to currentEmployeeNumber)
            createProfileDialogFragment(
                R.layout.account_employee_number_dialog_fragment,
                bundle
            ).show(
                childFragmentManager,
                getString(R.string.tag_employee_number_dialog_fragment)
            )
        }
    }

    private fun accountUnionNameDialogFragment() {
        // when union name value is clicked
        childFragmentManager.setFragmentResultListener(
            ACCOUNT_UNION_NAME_REQUEST_KEY,
            requireActivity()
        ) { key, bundle ->
            // collect input values from dialog fragment and update the union name text of user
            val unionName = bundle.getString(ACCOUNT_UNION_NAME_BUNDLE_KEY)
            binding.accountFragmentNameOfUnionValueTextView.text = unionName
        }

        // when union name value is clicked
        binding.accountFragmentNameOfUnionValueTextView.setOnClickListener {
            val currentUnionName =
                binding.accountFragmentNameOfUnionValueTextView.text.toString()
            val bundle = bundleOf(CURRENT_ACCOUNT_UNION_NAME_BUNDLE_KEY to currentUnionName)
            createProfileDialogFragment(
                R.layout.account_union_name_dialog_fragment,
                bundle
            ).show(
                childFragmentManager,
                AccountFragment::class.java.simpleName
            )
        }
    }

    private fun accountUnionWardDialogFragment() {
        // when ward name value is clicked
        childFragmentManager.setFragmentResultListener(
            ACCOUNT_UNION_WARD_REQUEST_KEY,
            requireActivity()
        ) { key, bundle ->
            // collect input values from dialog fragment and update the union name text of user
            val unionWard = bundle.getString(ACCOUNT_UNION_WARD_BUNDLE_KEY)
            binding.accountFragmentWardValueTextView.text = unionWard
        }

        // when union ward value is clicked
        binding.accountFragmentWardValueTextView.setOnClickListener {
            val currentUnionWard =
                binding.accountFragmentWardValueTextView.text.toString()
            val bundle = bundleOf(CURRENT_ACCOUNT_UNION_WARD_BUNDLE_KEY to currentUnionWard)
            createProfileDialogFragment(
                R.layout.account_union_ward_dialog_fragment,
                bundle
            ).show(
                childFragmentManager,
                AccountFragment::class.java.simpleName
            )
        }
    }

    private fun accountUnionLGADialogFragment() {
        // when lga name value is clicked
        childFragmentManager.setFragmentResultListener(
            ACCOUNT_UNION_LGA_REQUEST_KEY,
            requireActivity()
        ) { key, bundle ->
            // collect input values from dialog fragment and update the union lga text of user
            val unionLga = bundle.getString(ACCOUNT_UNION_LGA_BUNDLE_KEY)
            binding.accountFragmentLocalGovtAreaValueTextView.text = unionLga
        }

        // when lga value is clicked
        binding.accountFragmentLocalGovtAreaValueTextView.setOnClickListener {
            val currentUnionState =
                binding.accountFragmentLocalGovtAreaValueTextView.text.toString()
            val bundle = bundleOf(CURRENT_ACCOUNT_UNION_LGA_BUNDLE_KEY to currentUnionState)
            createProfileDialogFragment(
                R.layout.account_union_lga_dialog_fragment,
                bundle
            ).show(
                childFragmentManager,
                AccountFragment::class.java.simpleName
            )
        }
    }

    private fun accountUnionStateDialogFragment() {
        // when state name value is clicked
        childFragmentManager.setFragmentResultListener(
            ACCOUNT_UNION_STATE_REQUEST_KEY,
            requireActivity()
        ) { key, bundle ->
            // collect input values from dialog fragment and update the union state text of user
            val unionState = bundle.getString(ACCOUNT_UNION_STATE_BUNDLE_KEY)
            binding.accountFragmentStateValueTextView.text = unionState
        }

        // when state value is clicked
        binding.accountFragmentStateValueTextView.setOnClickListener {
            val currentUnionState =
                binding.accountFragmentStateValueTextView.text.toString()
            val bundle = bundleOf(CURRENT_ACCOUNT_STATE_NAME_BUNDLE_KEY to currentUnionState)
            createProfileDialogFragment(
                R.layout.account_union_state_dialog_fragment,
                bundle
            ).show(
                childFragmentManager,
                AccountFragment::class.java.simpleName
            )
        }
    }

    // Gender Dialog
    private fun accountGenderSelectDialog() {
        // when gender value is clicked
        childFragmentManager.setFragmentResultListener(
            ACCOUNT_GENDER_REQUEST_KEY,
            requireActivity()
        ) { key, bundle ->
            // collect input values from dialog fragment and update the text of user
            val gender = bundle.getString(ACCOUNT_GENDER_BUNDLE_KEY)
            binding.accountFragmentGenderValueTextView.text = gender
        }

        // when employee number name value is clicked
        binding.accountFragmentGenderValueTextView.setOnClickListener {
            val currentGender = binding.accountFragmentGenderValueTextView.text.toString()
            val bundle = bundleOf(CURRENT_ACCOUNT_GENDER_BUNDLE_KEY to currentGender)
            createProfileDialogFragment(R.layout.account_gender_dialog_fragment, bundle).show(
                childFragmentManager, AccountFragment::class.java.simpleName
            )
        }
    }

    private fun requestPermissions() {

        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            Constants.PERMISSION_ID
        )
    }

    private fun checkPermissions(): Boolean {

        return ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            requireActivity(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        // If we want background location
        // on Android 10.0 and higher,
        // use:
        // ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Ask for GPS Location and get current location
     */
    private fun buildAlertMessageNoGps() {

        val locationRequest: LocationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 30 * 1000
        locationRequest.fastestInterval = 5 * 1000

        val builder: LocationSettingsRequest.Builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        builder.setAlwaysShow(true) // this is the key ingredient
        val result: Task<LocationSettingsResponse> = LocationServices.getSettingsClient(requireContext()).checkLocationSettings(builder.build())
        result.addOnCompleteListener { task ->
            try {
                val response: LocationSettingsResponse = task.getResult(ApiException::class.java)
                /**
                 * All location settings are satisfied. The client can initialize location requests here.
                 */
            } catch (exception: ApiException) {
                when (exception.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED ->
                        // Location settings are not satisfied. But could be fixed by showing the user a dialog.
                        try {
                            // Cast to a resolvable exception.
                            val resolvable = exception as ResolvableApiException
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            resolvable.startResolutionForResult(
                                requireActivity(),
                                Constants.REQUEST_CHECK_SETTINGS
                            )
                        } catch (e: IntentSender.SendIntentException) {
                            // Ignore the error.
                        } catch (e: ClassCastException) {
                            // Ignore, should be an impossible error.
                        }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                    }
                }
            }
        }
    }

    //    /* set broadcast receiver go detect GPS changes */
    var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            /* listen for changes in cell broadcast */
            if (LocationManager.PROVIDERS_CHANGED_ACTION == intent.action) {
                val locationManager =
                    context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
//                val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                if (isGpsEnabled) {
                    // Handle Location turned ON
                    Toast.makeText(requireContext(), "LOCATION ENABLED", Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.mapFragment)
                } else {
                    Toast.makeText(requireContext(), "LOCATION DISABLED", Toast.LENGTH_LONG).show()
                    // Handle Location turned OFF
                }
            }
        }
    }

    /**
     * Method to check if location is enabled
     * @return true || false
     */
    private fun isLocationEnabled(): Boolean {

        val locationManager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
            locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
    }

    // logic to lunch map if all equirements are met
    private fun initiateMapLunch() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                findNavController().navigate(R.id.mapFragment)
            } else {
                buildAlertMessageNoGps()
            }
        } else {
            requestPermissions()
        }
    }

    companion object {
        const val ACCOUNT_EMPLOYEE_REQUEST_KEY = "ACCOUNT EMPLOYEE REQUEST KEY"
        const val ACCOUNT_EMPLOYEE_BUNDLE_KEY = "ACCOUNT EMPLOYEE BUNDLE KEY"
        const val CURRENT_ACCOUNT_EMPLOYEE_BUNDLE_KEY = "CURRENT ACCOUNT EMPLOYEE BUNDLE KEY"

        const val ACCOUNT_FIRST_NAME_REQUEST_KEY = "ACCOUNT FIRST NAME REQUEST KEY"
        const val ACCOUNT_FIRST_NAME_BUNDLE_KEY = "ACCOUNT FIRST NAME BUNDLE KEY"
        const val CURRENT_ACCOUNT_FIRST_NAME_BUNDLE_KEY = "CURRENT ACCOUNT FIRST NAME BUNDLE KEY"

        const val ACCOUNT_LAST_NAME_REQUEST_KEY = "ACCOUNT LAST NAME REQUEST KEY"
        const val ACCOUNT_LAST_NAME_BUNDLE_KEY = "ACCOUNT LAST NAME BUNDLE KEY"
        const val CURRENT_ACCOUNT_LAST_NAME_BUNDLE_KEY = "CURRENT ACCOUNT LAST NAME BUNDLE KEY"

        const val ACCOUNT_OTHER_NAME_REQUEST_KEY = "ACCOUNT OTHER NAME REQUEST KEY"
        const val ACCOUNT_OTHER_NAME_BUNDLE_KEY = "ACCOUNT OTHER NAME BUNDLE KEY"
        const val CURRENT_ACCOUNT_OTHER_NAME_BUNDLE_KEY = "CURRENT ACCOUNT OTHER NAME BUNDLE KEY"

        const val ACCOUNT_GENDER_REQUEST_KEY = "ACCOUNT GENDER REQUEST KEY"
        const val ACCOUNT_GENDER_BUNDLE_KEY = "ACCOUNT GENDER BUNDLE KEY"
        const val CURRENT_ACCOUNT_GENDER_BUNDLE_KEY = "CURRENT ACCOUNT GENDER BUNDLE KEY"

        const val ACCOUNT_WORKSHOP_STATE_REQUEST_KEY = "ACCOUNT WORKSHOP STATE REQUEST KEY"
        const val ACCOUNT_WORKSHOP_STATE_BUNDLE_KEY = "ACCOUNT WORKSHOP STATE BUNDLE KEY"
        const val CURRENT_ACCOUNT_WORKSHOP_STATE_BUNDLE_KEY =
            "CURRENT ACCOUNT WORKSHOP STATE BUNDLE KEY"

        const val ACCOUNT_WORKSHOP_CITY_REQUEST_KEY = "ACCOUNT WORKSHOP CITY REQUEST KEY"
        const val ACCOUNT_WORKSHOP_CITY_BUNDLE_KEY = "ACCOUNT WORKSHOP CITY BUNDLE KEY"
        const val CURRENT_ACCOUNT_WORKSHOP_CITY_BUNDLE_KEY =
            "CURRENT ACCOUNT WORKSHOP CITY BUNDLE KEY"

        const val ACCOUNT_WORKSHOP_STREET_REQUEST_KEY = "ACCOUNT WORKSHOP STREET REQUEST KEY"
        const val ACCOUNT_WORKSHOP_STREET_BUNDLE_KEY = "ACCOUNT WORKSHOP STREET BUNDLE KEY"
        const val CURRENT_ACCOUNT_WORKSHOP_STREET_BUNDLE_KEY =
            "CURRENT ACCOUNT WORKSHOP STREET BUNDLE KEY"

        const val ACCOUNT_SHOWROOM_ADDRESS_REQUEST_KEY = "ACCOUNT SHOWROOM ADDRESS REQUEST KEY"
        const val ACCOUNT_SHOWROOM_ADDRESS_BUNDLE_KEY = "ACCOUNT SHOWROOM ADDRESS BUNDLE KEY"
        const val CURRENT_ACCOUNT_SHOWROOM_ADDRESS_BUNDLE_KEY =
            "CURRENT ACCOUNT SHOWROOM ADDRESS BUNDLE KEY"

        const val ACCOUNT_UNION_NAME_REQUEST_KEY = "ACCOUNT UNION NAME REQUEST KEY"
        const val ACCOUNT_UNION_NAME_BUNDLE_KEY = "ACCOUNT UNION NAME BUNDLE KEY"
        const val CURRENT_ACCOUNT_UNION_NAME_BUNDLE_KEY = "CURRENT ACCOUNT UNION NAME BUNDLE KEY"

        const val ACCOUNT_UNION_STATE_REQUEST_KEY = "ACCOUNT UNION STATE REQUEST KEY"
        const val ACCOUNT_UNION_STATE_BUNDLE_KEY = "ACCOUNT UNION STATE BUNDLE KEY"
        const val CURRENT_ACCOUNT_STATE_NAME_BUNDLE_KEY = "CURRENT ACCOUNT UNION STATE BUNDLE KEY"

        const val ACCOUNT_UNION_WARD_REQUEST_KEY = "ACCOUNT UNION WARD REQUEST KEY"
        const val ACCOUNT_UNION_WARD_BUNDLE_KEY = "ACCOUNT UNION WARD BUNDLE KEY"
        const val CURRENT_ACCOUNT_UNION_WARD_BUNDLE_KEY = "CURRENT ACCOUNT UNION WARD BUNDLE KEY"

        const val ACCOUNT_UNION_LGA_REQUEST_KEY = "ACCOUNT UNION LGA REQUEST KEY"
        const val ACCOUNT_UNION_LGA_BUNDLE_KEY = "ACCOUNT UNION LGA BUNDLE KEY"
        const val CURRENT_ACCOUNT_UNION_LGA_BUNDLE_KEY = "CURRENT ACCOUNT UNION LGA BUNDLE KEY"

        const val ACCOUNT_LEGAL_STATUS_REQUEST_KEY = "ACCOUNT LEGAL STATUS REQUEST KEY"
        const val ACCOUNT_LEGAL_STATUS_BUNDLE_KEY = "ACCOUNT LEGAL STATUS BUNDLE KEY"
        const val CURRENT_ACCOUNT_LEGAL_STATUS_BUNDLE_KEY =
            "CURRENT ACCOUNT LEGAL STATUS BUNDLE KEY"

        const val RENAME_DESCRIPTION_REQUEST_KEY = "ACCOUNT FIRST NAME REQUEST KEY"
        const val RENAME_DESCRIPTION_BUNDLE_KEY = "ACCOUNT FIRST NAME BUNDLE KEY"
        const val CURRENT_ACCOUNT_RENAME_DESCRIPTION_BUNDLE_KEY =
            "CURRENT ACCOUNT FIRST NAME BUNDLE KEY"

        const val READ_IMAGE_STORAGE = 102
        const val NAME = "CLads"

        const val GPS_KEY = "false"
    }
}
