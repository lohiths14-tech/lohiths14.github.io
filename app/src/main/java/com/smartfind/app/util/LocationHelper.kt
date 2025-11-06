package com.smartfind.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.smartfind.app.data.local.entity.ObjectLocation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.*
import kotlin.coroutines.resume

class LocationHelper(private val context: Context) {
    
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    private val geocoder: Geocoder = Geocoder(context, Locale.getDefault())
    
    companion object {
        private const val TAG = "LocationHelper"
        private const val LOCATION_TIMEOUT_MS = 5000L
        private const val HIGH_ACCURACY_THRESHOLD = 50f
        private const val LOW_ACCURACY_THRESHOLD = 100f
    }
    
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted")
            return null
        }
        
        return try {
            withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
                val cancellationTokenSource = CancellationTokenSource()
                
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).await()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting location", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location", e)
            null
        }
    }
    
    suspend fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) {
            return null
        }
        
        return try {
            fusedLocationClient.lastLocation.await()
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting last location", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting last location", e)
            null
        }
    }
    
    suspend fun getAddressFromLocation(latitude: Double, longitude: Double): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Use the new API for Android 13+
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        val address = addresses.firstOrNull()?.let { formatAddress(it) }
                        continuation.resume(address)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                addresses?.firstOrNull()?.let { formatAddress(it) }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error getting address from location", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in geocoding", e)
            null
        }
    }
    
    private fun formatAddress(address: Address): String {
        val parts = mutableListOf<String>()
        
        address.featureName?.let { parts.add(it) }
        address.thoroughfare?.let { parts.add(it) }
        address.subLocality?.let { parts.add(it) }
        address.locality?.let { parts.add(it) }
        address.adminArea?.let { parts.add(it) }
        address.postalCode?.let { parts.add(it) }
        
        return if (parts.isNotEmpty()) {
            parts.joinToString(", ")
        } else {
            "Address unavailable"
        }
    }
    
    suspend fun createObjectLocation(location: Location?): ObjectLocation? {
        if (location == null) {
            return null
        }
        
        // Only store location if accuracy is acceptable
        if (location.accuracy > HIGH_ACCURACY_THRESHOLD) {
            Log.d(TAG, "Location accuracy too low: ${location.accuracy}m")
            if (location.accuracy > LOW_ACCURACY_THRESHOLD) {
                return null
            }
        }
        
        val address = getAddressFromLocation(location.latitude, location.longitude)
        val finalAddress = when {
            address != null -> address
            location.accuracy > LOW_ACCURACY_THRESHOLD -> "Indoor location — GPS unavailable"
            else -> "Coordinates: ${String.format(Locale.US, "%.6f", location.latitude)}, ${String.format(Locale.US, "%.6f", location.longitude)}"
        }
        
        return ObjectLocation(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            address = finalAddress,
            timestamp = System.currentTimeMillis()
        )
    }
    
    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
    }
}
