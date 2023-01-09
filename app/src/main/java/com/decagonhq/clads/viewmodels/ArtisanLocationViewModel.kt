package com.decagonhq.clads.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decagonhq.clads.data.domain.profile.ArtisanCoOrdinates
import kotlinx.coroutines.cancel

class ArtisanLocationViewModel : ViewModel() {
    private var _artisanLocation = MutableLiveData<ArtisanCoOrdinates> ()
    val artisanLocation: LiveData<ArtisanCoOrdinates> get() = _artisanLocation

    private var _artisanLocationString = MutableLiveData<String> ()
    val artisanLocationString: LiveData<String> get() = _artisanLocationString

    fun setArtisanCoOrdinates(coOrdinates: ArtisanCoOrdinates) {
        _artisanLocation.value = coOrdinates
    }

    fun setArtisanAddressString(address: String) {
        _artisanLocationString.value = address
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }
}
