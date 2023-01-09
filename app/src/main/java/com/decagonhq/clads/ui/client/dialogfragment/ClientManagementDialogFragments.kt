package com.decagonhq.clads.ui.client.dialogfragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.os.bundleOf
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import com.decagonhq.clads.R
import com.decagonhq.clads.data.domain.client.DeliveryAddress
import com.decagonhq.clads.data.domain.client.Measurement
import com.decagonhq.clads.data.domain.location.LocationJson
import com.decagonhq.clads.databinding.AddAddressFragmentBinding
import com.decagonhq.clads.databinding.AddMeasurementDialogFragmentBinding
import com.decagonhq.clads.databinding.EditMeasurementDialogFragmentBinding
import com.decagonhq.clads.ui.client.DeliveryAddressFragment.Companion.CURRENT_DELIVERY_ADDRESS_BUNDLE_KEY
import com.decagonhq.clads.ui.client.DeliveryAddressFragment.Companion.DELIVERY_ADDRESS_BUNDLE_KEY
import com.decagonhq.clads.ui.client.DeliveryAddressFragment.Companion.DELIVERY_ADDRESS_REQUEST_KEY
import com.decagonhq.clads.ui.client.MeasurementsFragment.Companion.EDIT_MEASUREMENT_BUNDLE_KEY
import com.decagonhq.clads.ui.client.MeasurementsFragment.Companion.EDIT_MEASUREMENT_BUNDLE_POSITION
import com.decagonhq.clads.viewmodels.ClientsRegisterViewModel
import com.google.gson.Gson
import java.io.InputStream
import java.util.Locale

class ClientManagementDialogFragments(
    private var dialogLayoutId: Int,
    private var bundle: Bundle? = null

) : DialogFragment() {

    private val registerClientViewModel: ClientsRegisterViewModel by activityViewModels()
    private val gson by lazy { Gson() }
    private lateinit var locations: LocationJson
    private var statePicked = ""
    private lateinit var stateSelectorDropdown: AutoCompleteTextView
    private lateinit var cityAddress: AutoCompleteTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_Light_Dialog_MinWidth)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(dialogLayoutId, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /*Inflate Dialog Fragment Layouts based on Id*/
        when (dialogLayoutId) {

            /*Add Measurement Dialog Fragment*/
            R.layout.add_measurement_dialog_fragment -> {

                /*Initialise binding*/
                val binding = AddMeasurementDialogFragmentBinding.bind(view)
                /*Initializing Views*/
                val measurementNameEditText = binding.addAddressFragmentMeasurementNameEditText
                val measurementEditText = binding.addMeasurementFragmentAddMeasureEditText
                val addMeasurementButton = binding.addMeasurementFragmentAddMeasurementButton

                /*Add new measurement*/
                addMeasurementButton.setOnClickListener {
                    val measurementName = measurementNameEditText.text
                    val measurement = measurementEditText.text
                    when {
                        measurementName.toString().trim().isEmpty() -> {
                            binding.addAddressFragmentMeasurementNameEditTextLayout.error =
                                getString(
                                    R.string.required
                                )
                            binding.addAddressFragmentMeasurementNameEditTextLayout.errorIconDrawable =
                                null
                            return@setOnClickListener
                        }

                        measurementName.toString().trim().length < 3 && measurementName.toString()
                            .trim().isNotEmpty() -> {
                            binding.addAddressFragmentMeasurementNameEditTextLayout.error =
                                getString(R.string.must_contain_3_or_more)

                            binding.addAddressFragmentMeasurementNameEditTextLayout.errorIconDrawable =
                                null
                            return@setOnClickListener
                        }

                        measurement.toString().isEmpty() -> {
                            binding.addMeasurementFragmentAddMeasurementEditTextLayout.error =
                                getString(
                                    R.string.required
                                )
                            binding.addMeasurementFragmentAddMeasurementEditTextLayout.errorIconDrawable =
                                null
                            return@setOnClickListener
                        }
                        else -> {
                            registerClientViewModel.addMeasurements(
                                Measurement(
                                    measurementName.toString(),
                                    measurement.toString().toInt()
                                )
                            )
                            dismiss()
                        }
                    }
                }
                /*Validate Dialog Fields onTextChange*/
                measurementNameEditText.doOnTextChanged { _, _, _, _ ->
                    when {
                        measurementNameEditText.text.toString().trim().isEmpty() -> {
                            binding.addAddressFragmentMeasurementNameEditTextLayout.error =
                                getString(
                                    R.string.required
                                )
                            binding.addAddressFragmentMeasurementNameEditTextLayout.errorIconDrawable =
                                null
                        }

                        measurementNameEditText.text.toString()
                            .trim().length < 3 && measurementNameEditText.text.toString().trim()
                            .isNotEmpty() -> {
                            binding.addAddressFragmentMeasurementNameEditTextLayout.error =
                                getString(R.string.must_contain_3_or_more)

                            binding.addAddressFragmentMeasurementNameEditTextLayout.errorIconDrawable =
                                null
                        }

                        else -> {
                            binding.addAddressFragmentMeasurementNameEditTextLayout.error = null
                        }
                    }
                }
                measurementEditText.doOnTextChanged { _, _, _, _ ->
                    when {
                        measurementEditText.text.toString().trim().isEmpty() -> {
                            binding.addMeasurementFragmentAddMeasurementEditTextLayout.error =
                                getString(
                                    R.string.required
                                )
                            binding.addMeasurementFragmentAddMeasurementEditTextLayout.errorIconDrawable =
                                null
                        }
                        else -> {
                            binding.addMeasurementFragmentAddMeasurementEditTextLayout.error = null
                        }
                    }
                }
            }

            /*Edit address Dialog Fragment*/
            R.layout.add_address_fragment -> {

                /*Initialise binding*/
                val binding = AddAddressFragmentBinding.bind(view)

                locations = gson.fromJson(readJson(requireActivity()), LocationJson::class.java)

                // Read state and lga from
                val states: MutableList<String> = mutableListOf()

                for (data in locations.data) {
                    states.add(data.state)
                }

                // set state view binding
                stateSelectorDropdown = binding.addAddressFragmentStateAddressEditTextView

                // set view data
                val adapterState =
                    ArrayAdapter(requireContext(), R.layout.list_item, states.sorted())
                stateSelectorDropdown.setAdapter(adapterState)

                // lga
                val lga: MutableList<String> = mutableListOf()
                val adapterLGA = ArrayAdapter(requireContext(), R.layout.list_item, lga)

                cityAddress = binding.addAddressFragmentLgaAddressEditTextView
                cityAddress.setAdapter(adapterLGA)

                val cityEditText = binding.addAddressFragmentLgaAddressEditTextView

                // Get State Picked
                val adapterStateObject =
                    AdapterView.OnItemClickListener { parent, _, position, _ ->

                        statePicked = parent.getItemAtPosition(position).toString()
                        for (state in locations.data) {
                            if (state.state == statePicked) {
                                lga.clear()
                                for (data in state.lgas) {
                                    lga.add(data)
                                }
                                cityEditText.setText(lga[0], false)
                            }
                        }
                    }

                stateSelectorDropdown.onItemClickListener = adapterStateObject

                /*Initializing Views*/
                val enterAddressEditText = binding.addAddressFragmentEnterDeliveryAddressEditText

                val stateEditText = binding.addAddressFragmentStateAddressEditTextView
                val saveAddressButton = binding.addAddressFragmentSaveAddressButton

                // get the arguments
                val retrievedArgs = bundle?.getString(CURRENT_DELIVERY_ADDRESS_BUNDLE_KEY)
                val splitAddress = retrievedArgs?.split("~")

                /*Attaching the data*/
                if (splitAddress != null && splitAddress.size >= 3) {

                    /* Set dialogue title*/
                    binding.addAddressFragmentTitleAddAddressTextView.text =
                        getString(R.string.Edit_delivery_address)

                    /* get current user location*/
                    val currentState = splitAddress[2]
                    val currentLga = splitAddress[1].trim()
                    val currentStreet = splitAddress[0].trim()

                    /* populate state array */
                    for (state in locations.data) {
                        states.add(state.state)
                    }

                    stateEditText.setText(currentState, false)

                    /* populate LGA arary*/
                    for (state in locations.data) {
                        if (state.state.trim() == currentState.trim()) {
                            for (data in state.lgas) {
                                lga.add(data)
                            }
                        }
                    }

                    val lgaPicked = lga[lga.indexOf(currentLga)]

                    /* set fields */
                    cityEditText.setText(lgaPicked, false)
                    enterAddressEditText.setText(currentStreet)
                }

                /*Saving the changes for the address*/
                saveAddressButton.setOnClickListener {

                    val addressState = binding.addAddressFragmentStateAddressEditTextView.text.toString().trim()
                    val addressName = binding.addAddressFragmentEnterDeliveryAddressEditText.text.toString().trim()
                    val addressCity = binding.addAddressFragmentLgaAddressEditTextView.text.toString().trim()

                    when {

                        addressName.isEmpty() -> {

                            binding.addAddressFragmentEnterDeliveryAddressEditTextLayout.error =
                                getString(
                                    R.string.required
                                )
                            binding.addAddressFragmentEnterDeliveryAddressEditTextLayout.errorIconDrawable =
                                null
                            return@setOnClickListener
                        }

                        addressName.length < 3 && addressName.isNotEmpty() -> {
                            binding.addAddressFragmentEnterDeliveryAddressEditTextLayout.error =
                                getString(R.string.must_contain_3_or_more)

                            binding.addAddressFragmentEnterDeliveryAddressEditTextLayout.errorIconDrawable =
                                null
                            return@setOnClickListener
                        }

                        addressCity.isEmpty() -> {
                            binding.addAddressFragmentLgaAddressEditTextLayout.error =
                                getString(
                                    R.string.required
                                )
                            binding.addAddressFragmentLgaAddressEditTextLayout.errorIconDrawable =
                                null

                            return@setOnClickListener
                        }

                        addressCity.length < 3 && addressCity.isNotEmpty() -> {
                            binding.addAddressFragmentLgaAddressEditTextLayout.error =
                                getString(R.string.must_contain_3_or_more)

                            binding.addAddressFragmentLgaAddressEditTextLayout.errorIconDrawable =
                                null
                            return@setOnClickListener
                        }
                        addressState.isEmpty() -> {
                            binding.addAddressFragmentStateAddressEditTextLayout.error =
                                getString(
                                    R.string.required
                                )
                            binding.addAddressFragmentStateAddressEditTextLayout.errorIconDrawable =
                                null
                            return@setOnClickListener
                        }
                        else -> {

                            val addressBundle = DeliveryAddress(
                                addressName.capitalize(Locale.ROOT),
                                addressCity.capitalize(Locale.ROOT),
                                addressState
                            )

                            /*Save Temporal Client Address*/
                            registerClientViewModel.clientNewAddress(addressBundle)

                            setFragmentResult(
                                DELIVERY_ADDRESS_REQUEST_KEY,
                                bundleOf(DELIVERY_ADDRESS_BUNDLE_KEY to addressBundle)
                            )
                            dismiss()
                        }
                    }
                }

                /*Validate Dialog Fields onTextChange*/
                enterAddressEditText.doOnTextChanged { _, _, _, _ ->
                    when {
                        enterAddressEditText.text.toString().trim().isEmpty() -> {
                            binding.addAddressFragmentEnterDeliveryAddressEditTextLayout.error =
                                getString(
                                    R.string.required
                                )
                            binding.addAddressFragmentEnterDeliveryAddressEditTextLayout.errorIconDrawable =
                                null
                        }
                        enterAddressEditText.text.toString()
                            .trim().length < 3 && enterAddressEditText.text.toString().trim()
                            .isNotEmpty() -> {
                            binding.addAddressFragmentEnterDeliveryAddressEditTextLayout.error =
                                getString(R.string.must_contain_3_or_more)

                            binding.addAddressFragmentEnterDeliveryAddressEditTextLayout.errorIconDrawable =
                                null
                        }
                        else -> {
                            binding.addAddressFragmentEnterDeliveryAddressEditTextLayout.error =
                                null
                        }
                    }
                }
                cityEditText.doOnTextChanged { _, _, _, _ ->
                    when {
                        cityEditText.text.toString().trim().isEmpty() -> {
                            binding.addAddressFragmentLgaAddressEditTextLayout.error =
                                getString(
                                    R.string.required
                                )
                            binding.addAddressFragmentLgaAddressEditTextLayout.errorIconDrawable =
                                null
                        }
                        else -> {
                            binding.addAddressFragmentLgaAddressEditTextLayout.error = null
                        }
                    }
                }
                stateEditText.doOnTextChanged { _, _, _, _ ->
                    when {
                        stateEditText.text.toString().trim().isEmpty() -> {
                            binding.addAddressFragmentStateAddressEditTextLayout.error =
                                getString(R.string.required)
                            binding.addAddressFragmentStateAddressEditTextLayout.errorIconDrawable =
                                null
                        }
                        else -> {
                            binding.addAddressFragmentStateAddressEditTextLayout.error = null
                        }
                    }
                }
            }

            /*Edit Measurement Dialog Fragment*/
            R.layout.edit_measurement_dialog_fragment -> {
                /*Initialise binding*/
                val binding = EditMeasurementDialogFragmentBinding.bind(view)
                /*Initializing Views*/
                val measurementNameEditText = binding.editAddressFragmentMeasurementNameEditText
                val measurementEditText = binding.editMeasurementFragmentAddMeasureEditText
                val editMeasurementButton = binding.editMeasurementFragmentSaveButton

                val itemPosition = bundle?.getInt(EDIT_MEASUREMENT_BUNDLE_POSITION)
                val retrievedMeasurement =
                    bundle?.getParcelable<Measurement>(EDIT_MEASUREMENT_BUNDLE_KEY)

                /*Attaching the data*/
                measurementNameEditText.setText(retrievedMeasurement?.title)
                measurementEditText.setText(retrievedMeasurement?.value.toString())

                /*Saving the changes for the measurement*/
                editMeasurementButton.setOnClickListener {
                    val measurementName = binding.editAddressFragmentMeasurementNameEditText.text
                    val measurement = binding.editMeasurementFragmentAddMeasureEditText.text

                    when {
                        measurementName.toString().trim().isEmpty() -> {
                            binding.editAddressFragmentMeasurementNameEditTextLayout.error =
                                getString(
                                    R.string.required
                                )
                        }
                        measurementName.toString().trim().length < 3 && measurementName.toString()
                            .trim().isNotEmpty() -> {
                            binding.editAddressFragmentMeasurementNameEditTextLayout.error =
                                getString(R.string.must_contain_3_or_more)

                            binding.editAddressFragmentMeasurementNameEditTextLayout.errorIconDrawable =
                                null
                            return@setOnClickListener
                        }
                        measurement.toString().isEmpty() -> {
                            binding.editMeasurementFragmentAddMeasurementEditTextLayout.error =
                                getString(
                                    R.string.required
                                )
                            binding.editMeasurementFragmentAddMeasurementEditTextLayout.errorIconDrawable =
                                null
                        }
                        else -> {
                            val editedDataModel =
                                Measurement(
                                    measurementName.toString(),
                                    measurement.toString().toInt()
                                )

                            if (itemPosition != null) {
                                registerClientViewModel.editMeasurement(
                                    itemPosition,
                                    editedDataModel
                                )
                            }
                            dismiss()
                        }
                    }
                }

                /*Validate Dialog Fields onTextChange*/
                measurementNameEditText.doOnTextChanged { _, _, _, _ ->
                    when {
                        measurementNameEditText.text!!.trim().isEmpty() -> {
                            binding.editAddressFragmentMeasurementNameEditTextLayout.error =
                                getString(
                                    R.string.required
                                )
                            binding.editAddressFragmentMeasurementNameEditTextLayout.errorIconDrawable =
                                null
                        }
                        else -> {
                            binding.editAddressFragmentMeasurementNameEditTextLayout.error = null
                        }
                    }
                }
                measurementEditText.doOnTextChanged { _, _, _, _ ->
                    when {
                        measurementEditText.text!!.toString().trim().isEmpty() -> {
                            binding.editMeasurementFragmentAddMeasurementEditTextLayout.error =
                                getString(
                                    R.string.required
                                )
                            binding.editMeasurementFragmentAddMeasurementEditTextLayout.errorIconDrawable =
                                null
                        }
                        else -> {
                            binding.editMeasurementFragmentAddMeasurementEditTextLayout.error = null
                        }
                    }
                }
            }
        }
    }

    private fun readJson(context: Context): String? {
        var inputStream: InputStream? = null

        val jsonString: String

        try {

            inputStream = context.assets.open("location.json")

            val size = inputStream.available()

            val buffer = ByteArray(size)

            inputStream.read(buffer)

            jsonString = String(buffer)

            return jsonString
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inputStream?.close()
        }

        return null
    }

    companion object {

        fun createClientDialogFragment(
            layoutId: Int,
            bundle: Bundle? = null
        ): ClientManagementDialogFragments {
            return ClientManagementDialogFragments(layoutId, bundle)
        }
    }
}
