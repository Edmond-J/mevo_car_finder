package com.edmond.mevocarfinder

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private var city = "wellington"
    private var vehicleInfoApi = "https://api.mevo.co.nz/public/vehicles/$city"
    private var boundaryInfoApi = "https://api.mevo.co.nz/public/parking/$city"
    private var vehicleList: MutableList<VehicleInfo>? = null
    private var boundaryPoints: List<List<Point>>? = null
    private lateinit var mapView: MapView
    private lateinit var satelliteMode: ImageView
    private lateinit var standardMode: ImageView
    private lateinit var trafficMode: ImageView
    private lateinit var darkModeSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        if (savedInstanceState == null) {
//            supportFragmentManager.beginTransaction()
//                .replace(R.id.container, DashNavigationFragment.newInstance())
//                .commitNow()
//        }
        mapView = findViewById(R.id.mapView)
        satelliteMode = findViewById(R.id.pic_satellite)
        standardMode = findViewById(R.id.pic_standard)
        trafficMode = findViewById(R.id.pic_traffic)
        darkModeSwitch = findViewById(R.id.switch_dark)
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                setImageBorder(trafficMode)
                mapView.mapboxMap.loadStyle(Style.TRAFFIC_NIGHT)
            } else {
                mapView.mapboxMap.loadStyle(Style.TRAFFIC_DAY)
            }
        }
        val findNearestCarButton: Button = findViewById(R.id.nearest_vehicle)
        findNearestCarButton.setOnClickListener {
            runBlocking { getDeviceLocation() }
            Log.d("edmond", "75: ${vehicleList?.size}")
        }
        val viewportOptions = FollowPuckViewportStateOptions.Builder()
            .pitch(0.0)
            .build()
        val findMyLocationButton = findViewById<ImageView>(R.id.my_location)
        findMyLocationButton.setOnClickListener {
            mapView.viewport.transitionTo(
                targetState = mapView.viewport.makeFollowPuckViewportState(viewportOptions)
            )
        }
        with(mapView) {
            location.locationPuck = createDefault2DPuck(withBearing = true)
            viewport.transitionTo(
                targetState = viewport.makeFollowPuckViewportState(viewportOptions),
                //This state syncs the map camera with the location puck.
                transition = viewport.makeImmediateViewportTransition()
                //The immediate viewport transition moves the camera to the target state at once without using animations.
            )
            location.pulsingEnabled = true
            if (loadSetting("style_mode") == "satellite") {
                mapboxMap.loadStyle(Style.SATELLITE)
                setImageBorder(satelliteMode)
            } else if (loadSetting("style_mode") == "traffic") {
                mapboxMap.loadStyle(Style.TRAFFIC_DAY)
                setImageBorder(trafficMode)
            } else {
                mapboxMap.loadStyle(Style.STANDARD)
                setImageBorder(standardMode)
            }
        }
        prepareMapAnnotation(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val gson = Gson()
        val vehicleListString = gson.toJson(vehicleList)
        outState.putString("vehicleList", vehicleListString)
        val boundaryPointsString = gson.toJson(boundaryPoints)
        outState.putString("boundaryPoints", boundaryPointsString)
    }

    private fun prepareMapAnnotation(savedInstanceState: Bundle?) {
        val gson = Gson()
        if (savedInstanceState?.getString("vehicleList") == null) {
            lifecycleScope.launch {
                val vehicleRawData =
                    FetchData.fetchData(vehicleInfoApi)
                vehicleList = FetchData.parseJsonVehicle(vehicleRawData)
                withContext(Dispatchers.Main) {
                    Log.d("edmond", "vehicle List: " + vehicleList!!.size)
                    addVehicleToMap(vehicleList!!)
                }
            }
        } else {
            val savedJson = savedInstanceState.getString("vehicleList")
            vehicleList =
                gson.fromJson(savedJson, object : TypeToken<MutableList<VehicleInfo>>() {}.type)
            Log.d("edmond", "vehicle List(from savedInstanceState)")
        }
        if (savedInstanceState?.getString("boundaryPoints") == null) {
            lifecycleScope.launch {
                val boundaryRawData = FetchData.fetchData(boundaryInfoApi)
                boundaryPoints = FetchData.parseJsonPolygon(boundaryRawData)
                withContext(Dispatchers.Main) {
                    drawFlexZoneBoundary(boundaryPoints!!)
                }
            }
        } else {
            val savedJson = savedInstanceState.getString("boundaryPoints")
            boundaryPoints =
                gson.fromJson(savedJson, object : TypeToken<List<List<Point>>>() {}.type)
            Log.d("edmond", "parking List(from savedInstanceState)")
        }
    }

    private fun drawFlexZoneBoundary(points: List<List<Point>>) {
        val polygonAnnotationManager =
            mapView.annotations.createPolygonAnnotationManager()
        val polygonAnnotationOptions = PolygonAnnotationOptions()
            .withPoints(points)
            .withFillColor(ContextCompat.getColor(this, R.color.accent))//#7AA0EB
            .withFillOpacity(0.4)
        polygonAnnotationManager.create(polygonAnnotationOptions)
    }

    private fun addVehicleToMap(vehicles: List<VehicleInfo>) {
        for (vehicle in vehicles) {
            val point = Point.fromLngLat(vehicle.longitude, vehicle.latitude)
//            val bitmap = vehicle.bitmap
            val bitmap =
                vehicle.bitmap ?: BitmapFactory.decodeResource(resources, R.drawable.red_marker)
            val pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
            val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(bitmap)
                .withDraggable(false)
            pointAnnotationManager.create(pointAnnotationOptions)
        }
    }

    private suspend fun getDeviceLocation(): Point? = coroutineScope {
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
        locationProvider?.getLastLocation { re ->
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
        val distanceMap = mutableListOf<Pair<VehicleInfo, Double>>()
        vehicleList?.forEach { vehicle ->
            val distance = vehicle.directDistance(userLongitude, userLatitude)
            distanceMap.add(vehicle to distance)
        }
        val nearestVehicle = distanceMap.minByOrNull { it.second }
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

    fun onLayerButtonClick(view: View) {
        val controlPanel: androidx.constraintlayout.widget.ConstraintLayout =
            findViewById(R.id.control_panel)
        if (controlPanel.visibility == View.VISIBLE) {
            controlPanel.visibility = View.INVISIBLE
        } else {
            controlPanel.visibility = View.VISIBLE
        }
    }

    fun onCloseButtonClick(view: View) {
        val controlPanel: androidx.constraintlayout.widget.ConstraintLayout =
            findViewById(R.id.control_panel)
        controlPanel.visibility = View.INVISIBLE
    }

    fun onSatelliteModeClick(view: View) {
        darkModeSwitch.isChecked = false
        mapView.mapboxMap.loadStyle(Style.SATELLITE)
        setImageBorder(satelliteMode)
        saveSetting("style_mode", "satellite")
        Toast.makeText(this@MainActivity, "Satellite Mode", Toast.LENGTH_SHORT).show()
    }

    fun onStandardModeClick(view: View) {
        darkModeSwitch.isChecked = false
        mapView.mapboxMap.loadStyle(Style.STANDARD)
        setImageBorder(standardMode)
        saveSetting("style_mode", "standard")
        Toast.makeText(this, "Standard Mode", Toast.LENGTH_SHORT).show()
    }

    fun onTrafficModeClick(view: View) {
        darkModeSwitch.isChecked = false
        mapView.mapboxMap.loadStyle(Style.TRAFFIC_DAY)
        setImageBorder(trafficMode)
        saveSetting("style_mode", "traffic")
        Toast.makeText(this, "Traffic Mode", Toast.LENGTH_SHORT).show()
    }

    private fun setImageBorder(imageView: ImageView) {
        satelliteMode.setBackgroundResource(0)
        standardMode.setBackgroundResource(0)
        trafficMode.setBackgroundResource(0)
        imageView.setBackgroundResource(R.drawable.border_round_corner_primary)
    }

    private fun saveSetting(key: String, value: String) {
        val sharedPreferences =
            applicationContext.getSharedPreferences("MySettings", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(key, value)
        editor.apply()
    }

    private fun loadSetting(key: String): String? {
        val sharedPreferences =
            applicationContext.getSharedPreferences("MySettings", Context.MODE_PRIVATE)
        return sharedPreferences.getString(key, null)
    }

}