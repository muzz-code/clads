package com.decagonhq.clads.ui.profile.editprofile

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.decagonhq.clads.R
import com.decagonhq.clads.data.domain.profile.ArtisanCoOrdinates
import com.decagonhq.clads.databinding.FragmentMapBinding
import com.decagonhq.clads.databinding.LocationPickerBinding
import com.decagonhq.clads.ui.BaseFragment
import com.decagonhq.clads.viewmodels.ArtisanLocationViewModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.io.IOException
import java.util.Locale

class MapFragment : BaseFragment() {

    /* Initialize variables */
    private lateinit var binding: FragmentMapBinding
    private val client by lazy {
        LocationServices.getFusedLocationProviderClient(requireContext())
    }
    private val artisanLocationViewModel: ArtisanLocationViewModel by activityViewModels()
    private lateinit var userCurrentLocation: Location
    private lateinit var nowLocation: LatLng
    private lateinit var location: LatLng

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getLastLocation()

//        activity?.onBackPressedDispatcher?.addCallback(
//            viewLifecycleOwner,
//            object : OnBackPressedCallback(true) {
//
//                override fun handleOnBackPressed() {
//                    // in here you can do logic when backPress is clicked
//                    findNavController().popBackStack()
//                }
//            }
//        )
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        client.lastLocation.addOnCompleteListener { task ->
            val location = task.result
            if (location == null) {
                requestNewLocationData()
            } else {
                userCurrentLocation = location
                startLocationService()
            }
        }
    }

    /*
    * GEts the result after calling a location
    * */
    private val mLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation
            userCurrentLocation = mLastLocation
            startLocationService()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        // Initializing LocationRequest
        // object with appropriate methods
        val mLocationRequest = LocationRequest.create()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 5
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1

        client.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())
    }

    /**
     * If every permission is satisfied open the dialog and load map,
     * and set the marker at the user's current location
     */
    @SuppressLint("MissingPermission")
    private fun startLocationService() {
        val dialog = Dialog(requireContext())
        val b: LocationPickerBinding =
            LocationPickerBinding.inflate(LayoutInflater.from(requireContext()))
        dialog.apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(b.root)
            val lp = WindowManager.LayoutParams()
            lp.copyFrom(this.window!!.attributes)
            lp.width = WindowManager.LayoutParams.MATCH_PARENT
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
            val window = this.window
            window?.attributes = lp
            show()
        }
        MapsInitializer.initialize(requireContext())
        b.mapView.onCreate(dialog.onSaveInstanceState())
        b.mapView.onResume()
        b.mapView.getMapAsync { googleMap -> // storing location to temporary variable
            nowLocation = LatLng(
                userCurrentLocation.latitude,
                userCurrentLocation.longitude
            ) // your lat lng
            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(nowLocation)
                    .title("Marker")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
            // Enable GPS marker in Map
            googleMap.isMyLocationEnabled = true
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(nowLocation))
            googleMap.uiSettings.isZoomControlsEnabled = true
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(15f), 1000, null)
            googleMap.setOnCameraMoveListener {
                val midLatLng = googleMap.cameraPosition.target
                if (marker != null) {
                    marker.position = midLatLng
                    nowLocation = marker.position
                }
            }
        }

        dialog.setCancelable(false)

        b.saveLocation.setOnClickListener {

            val cm = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
            val isConnected: Boolean = activeNetwork?.isConnectedOrConnecting == true

            if (isConnected) {

                location = LatLng(nowLocation.latitude, nowLocation.longitude)
                val address = getAddressText(location)
                showToast("$address")
                val artisanAddress = ArtisanCoOrdinates(
                    nowLocation.latitude,
                    nowLocation.longitude
                )

                artisanLocationViewModel.setArtisanCoOrdinates(artisanAddress)
                artisanLocationViewModel.setArtisanAddressString(address!!)
                dialog.dismiss()
                findNavController().navigate(R.id.action_mapFragment_to_editProfileFragment)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Poor Network service. Check your network connection.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun getAddressText(location: LatLng): String? {
        var addresses: List<Address>? = null
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        try {
            addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            // Here 1 represent max location result to returned, by documents it recommended 1 to 5
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return addresses?.get(0)!!.getAddressLine(0)
    }
}
