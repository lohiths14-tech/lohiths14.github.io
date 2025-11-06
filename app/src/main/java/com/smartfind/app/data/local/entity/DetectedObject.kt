package com.smartfind.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "detected_objects",
    indices = [Index(value = ["timestamp"]), Index(value = ["object_name"])]
)
data class DetectedObject(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "object_name")
    val objectName: String,

    @ColumnInfo(name = "confidence")
    val confidence: Float,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "image_path")
    val imagePath: String?,

    @ColumnInfo(name = "thumbnail_path")
    val thumbnailPath: String?,

    @ColumnInfo(name = "location_id")
    val locationId: Long?
)
