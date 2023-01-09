package com.decagonhq.clads.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.decagonhq.clads.data.domain.client.ClientReg
import com.decagonhq.clads.data.domain.client.DeliveryAddress
import com.decagonhq.clads.data.domain.client.Measurement
import com.decagonhq.clads.repository.ClientsRepository
import com.decagonhq.clads.util.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ClientsRegisterViewModel @Inject constructor(
    private val clientsRepository: ClientsRepository
) : ViewModel() {

    private val _clientData = MutableLiveData<ClientReg>()
    val clientData: LiveData<ClientReg> get() = _clientData

    private var list = mutableListOf<Measurement>()

    private val _measurementData = MutableLiveData<MutableList<Measurement>>()
    val measurementData: LiveData<MutableList<Measurement>> get() = _measurementData

    private val _clientAddress = MutableLiveData<DeliveryAddress>()
    val clientAddress: LiveData<DeliveryAddress> get() = _clientAddress

    private val _shouldEdit = SingleLiveEvent<Boolean>()
    val shouldEdit: LiveData<Boolean> = _shouldEdit

    fun setEdit(value: Boolean) {
        _shouldEdit.value = value
    }

    fun setClient(client: ClientReg) {
        _clientData.value = client
    }

    fun addMeasurements(measurement: Measurement) {
        list.add(measurement)
        _measurementData.value = list
    }

    fun setList(newList: List<Measurement>) {
        list = newList.toMutableList()
        _measurementData.value = list
    }

    fun editMeasurement(position: Int, measurement: Measurement) {
        list[position] = measurement
        _measurementData.value = list
    }

    fun deleteMeasurement(position: Int) {
        list.removeAt(position)
        _measurementData.value = list
    }

    fun clearMeasurement() {
        list.clear()
        _measurementData.value = list
    }

    /**client address is added */
    fun clientNewAddress(address: DeliveryAddress) {
        _clientAddress.value = address
    }

    fun clearAddress() {
        _clientAddress.value = DeliveryAddress()
    }
}
