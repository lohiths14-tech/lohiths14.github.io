package com.smartfind.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "object_locations")
data class ObjectLocation(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "latitude")
    val latitude: Double,

    @ColumnInfo(name = "longitude")
    val longitude: Double,

    @ColumnInfo(name = "accuracy")
    val accuracy: Float,

    @ColumnInfo(name = "address")
    val address: String?,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long
)
