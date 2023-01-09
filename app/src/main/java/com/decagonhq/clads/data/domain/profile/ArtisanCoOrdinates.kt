package com.decagonhq.clads.data.domain.profile

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ArtisanCoOrdinates(
    val artisanLatitude: Double = 0.0,
    val artisanLongitude: Double = 0.0,
) : Parcelable
