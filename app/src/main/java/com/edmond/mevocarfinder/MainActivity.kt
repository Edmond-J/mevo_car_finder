package com.edmond.mevocarfinder

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mapbox.common.location.AccuracyLevel
import com.mapbox.common.location.IntervalSettings
import com.mapbox.common.location.LocationProviderRequest
import com.mapbox.common.location.LocationService
import com.mapbox.common.location.LocationServiceFactory
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.MapAnimationOptions.Companion.mapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolygonAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateOptions
import com.mapbox.maps.plugin.viewport.viewport
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private var city = "wellington"
    private var vehicleInfoApi = "https://api.mevo.co.nz/public/vehicles/$city"
    private var boundaryInfoApi = "https://api.mevo.co.nz/public/parking/$city"
    private var vehicleList: MutableList<VehicleInfo>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        if (savedInstanceState == null) {
//            supportFragmentManager.beginTransaction()
//                .replace(R.id.container, DashNavigationFragment.newInstance())
//                .commitNow()
//        }
        val findCarButton: Button = findViewById(R.id.nearest_vehicle)
        findCarButton.setOnClickListener {
            Log.d("edmond", "find car button")
            val nearestCar = runBlocking { findMyLocation() }
            Log.d("edmond", "56: nearestCar: ${nearestCar?.latitude()}")
        }
        mapView = findViewById(R.id.mapView)
        val viewportOptions = FollowPuckViewportStateOptions.Builder()
            .pitch(0.0)
            .build()
        with(mapView) {
            location.locationPuck = createDefault2DPuck(withBearing = true)
            viewport.transitionTo(
                targetState = viewport.makeFollowPuckViewportState(viewportOptions),
                //This state syncs the map camera with the location puck.
                transition = viewport.makeImmediateViewportTransition()
                //The immediate viewport transition moves the camera to the target state at once without using animations.
            )
            location.pulsingEnabled = true
            mapboxMap.loadStyle(Style.STANDARD)
        }
        val myLocation = findViewById<ImageView>(R.id.my_location)
        myLocation.setOnClickListener {
            mapView.location.onStart()
            mapView.viewport.transitionTo(
                targetState = mapView.viewport.makeFollowPuckViewportState(viewportOptions)
            )
        }
        GlobalScope.launch {
            val vehicleRawData =
                FetchData.fetchData(vehicleInfoApi)
            vehicleList = FetchData.parseJsonVehicle(vehicleRawData)
            if (vehicleList != null) {
                Log.d("edmond", "59: " + vehicleList!!.size)
                addVehicleToMap(vehicleList!!)
            }
        }
        GlobalScope.launch {
            val boundaryRawData = FetchData.fetchData(boundaryInfoApi)
            val boundaryPoints: List<List<Point>> = FetchData.parseJsonPolygon(boundaryRawData)
            Log.d("edmond", "71: " + boundaryPoints[0][0].longitude())
            drawFlexZoneBoundary(boundaryPoints)
        }
    }

    private fun drawFlexZoneBoundary(points: List<List<Point>>) {
        val polygonAnnotationManager =
            mapView.annotations.createPolygonAnnotationManager()
        val polygonAnnotationOptions = PolygonAnnotationOptions()
            .withPoints(points)
            // Style the polygon that will be added to the map.
            .withFillColor(ContextCompat.getColor(this, R.color.accent))//#7AA0EB
            .withFillOpacity(0.4)
        // Add the resulting polygon to the map.
        polygonAnnotationManager.create(polygonAnnotationOptions)
    }

    private suspend fun addVehicleToMap(vehicles: List<VehicleInfo>) {
        for (vehicle in vehicles) {
            val point = Point.fromLngLat(vehicle.longitude, vehicle.latitude)
            val bitmap = FetchData.loadImageBitmap(resources, vehicle.iconUrl)
            val pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
            val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(bitmap)
                .withDraggable(false)
            pointAnnotationManager.create(pointAnnotationOptions)
        }
    }

    private suspend fun findMyLocation(): Point? = coroutineScope {
        val locationService: LocationService = LocationServiceFactory.getOrCreate()
        val resultDeferred = async {
            val request = LocationProviderRequest.Builder()
                .interval(
                    IntervalSettings.Builder().interval(1000L).minimumInterval(500L)
                        .maximumInterval(2000L).build()
                )
                .displacement(5.0F)
                .accuracy(AccuracyLevel.HIGHEST)
                .build()
            val result = locationService.getDeviceLocationProvider(request)
            result.value
        }
        val locationProvider = resultDeferred.await()
        var myLocation: Point? = null
        val lastLocationCancelable = locationProvider?.getLastLocation { re ->
            if (re != null) {
                myLocation = Point.fromLngLat(re.longitude, re.latitude)
                Log.d("edmond", "Latitude: ${re.latitude}, Longitude: ${re.longitude}")
//                findNearestVehicle(174.757759, -41.336273)
                findNearestVehicle(re.longitude, re.latitude)
            } else {
                Log.d("edmond", "result null")
            }
        }
        myLocation
    }

    private fun findNearestVehicle(userLongitude: Double?, userLatitude: Double?) {
        if (userLatitude == null || userLongitude == null) {
            return
        }
        val distances = mutableListOf<Pair<VehicleInfo, Double>>()
        // 计算所有车辆与用户位置的直线距离
        vehicleList?.forEach { vehicle ->
            val distance = vehicle.directDistance(userLongitude, userLatitude)
            distances.add(vehicle to distance)
        }
        val nearestVehicle = distances.minByOrNull { it.second }
        if (nearestVehicle != null) {
            flyToDest(nearestVehicle.first.longitude, nearestVehicle.first.latitude)
        }
    }


    private fun flyToDest(longitude: Double, latitude: Double) {
        val cameraOptions = CameraOptions.Builder()
            .center(
                Point.fromLngLat(longitude, latitude)
            )
            .bearing(-30.0)
            .pitch(45.0)
            .zoom(18.0)
            .build()
        mapView.location.onStop()
        mapView.mapboxMap.flyTo(cameraOptions, mapAnimationOptions { duration(6_000) })
    }


}