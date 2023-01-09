package com.decagonhq.clads.data.domain.profile

import androidx.room.ColumnInfo

data class WorkshopAddress(
    @ColumnInfo(name = "workshop_address_street")
    val street: String? = null,
    @ColumnInfo(name = "workshop_address_city")
    val city: String? = null,
    @ColumnInfo(name = "workshop_address_state")
    val state: String? = null,
    @ColumnInfo(name = "longitude")
    val longitude: String? = null,
    @ColumnInfo(name = "latitude")
    val latitude: String? = null
)
