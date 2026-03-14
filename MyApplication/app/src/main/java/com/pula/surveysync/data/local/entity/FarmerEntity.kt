package com.pula.surveysync.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.pula.surveysync.data.local.converter.DateConverter
import java.util.Date
import java.util.UUID

/**
 * Entity representing a farmer in the pre-loaded local database.
 * This data is typically synced down from the server before field work begins.
 */
@Entity(tableName = "farmers")
@TypeConverters(DateConverter::class)
data class FarmerEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val firstName: String,
    val lastName: String,
    val phoneNumber: String? = null,
    val village: String? = null,
    val district: String? = null,
    val region: String? = null,
    val farmSize: Double? = null, // in hectares
    val farmSizeUnit: String? = "hectares",
    val registrationDate: Date,
    val lastUpdated: Date,
    val photoUri: String? = null,
    val isActive: Boolean = true,
    val notes: String? = null
) {
    val fullName: String
        get() = "$firstName $lastName"

    val location: String
        get() = listOfNotNull(village, district, region).joinToString(", ")
}

