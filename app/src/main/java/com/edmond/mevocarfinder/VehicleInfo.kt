package com.edmond.mevocarfinder

import android.graphics.Bitmap
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

class VehicleInfo(
    val longitude: Double,
    val latitude: Double,
    val iconUrl: String,
    val bitmap: Bitmap?
) {

    fun directDistance(userLon: Double, userLat: Double): Double {
        val theta = userLon - longitude
        var dist = sin(Math.toRadians(userLat)) * sin(Math.toRadians(latitude)) +
                cos(Math.toRadians(userLat)) * cos(Math.toRadians(latitude)) *
                cos(Math.toRadians(theta))
        dist = acos(dist)
        dist = Math.toDegrees(dist)
        dist *= 60 * 1.1515 * 1.609344 * 1000 // Convert to meters
//        Log.d("edmond", "distance: $dist , lat: $latitude , lon: $longitude")
        return dist
    }
}

