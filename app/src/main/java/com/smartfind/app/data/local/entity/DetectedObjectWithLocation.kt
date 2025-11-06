package com.smartfind.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class DetectedObjectWithLocation(
    @Embedded val detectedObject: DetectedObject,
    @Relation(
        parentColumn = "location_id",
        entityColumn = "id"
    )
    val location: ObjectLocation?
)
