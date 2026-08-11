package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object LocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return coarseGranted || fineGranted
    }

    suspend fun getCurrentLocationDescription(context: Context): String? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission(context)) return@withContext null
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return@withContext null

            val providers = locationManager.getProviders(true)
            var lastLocation: Location? = null

            for (provider in providers) {
                try {
                    val loc = locationManager.getLastKnownLocation(provider) ?: continue
                    if (lastLocation == null || loc.time > lastLocation.time) {
                        lastLocation = loc
                    }
                } catch (e: SecurityException) {
                    // Permission revoked mid-check
                }
            }

            if (lastLocation == null) return@withContext null

            try {
                if (Geocoder.isPresent()) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lastLocation.latitude, lastLocation.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea
                        val country = addr.countryName
                        if (!city.isNullOrBlank() && !country.isNullOrBlank()) {
                            return@withContext "$city, $country"
                        } else if (!city.isNullOrBlank()) {
                            return@withContext city
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback to coordinates
            }

            return@withContext "Coordinates: ${String.format(Locale.US, "%.3f, %.3f", lastLocation.latitude, lastLocation.longitude)}"
        } catch (e: Exception) {
            return@withContext null
        }
    }
}
