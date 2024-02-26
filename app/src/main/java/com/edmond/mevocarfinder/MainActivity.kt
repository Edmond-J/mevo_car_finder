package com.edmond.mevocarfinder

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
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
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private var city = "wellington"
    private var vehicleInfoApi = "https://api.mevo.co.nz/public/vehicles/$city"
    private var boundaryInfoApi = "https://api.mevo.co.nz/public/parking/$city"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val mapView = findViewById<MapView>(R.id.mapView)
        val viewportOptions = FollowPuckViewportStateOptions.Builder()
            .pitch(0.0)
            .build()
        with(mapView) {
            location.locationPuck = createDefault2DPuck(withBearing = true)
//            location.enabled = true
//            location.puckBearing = PuckBearing.HEADING
            viewport.transitionTo(
                targetState = viewport.makeFollowPuckViewportState(viewportOptions)
                //This state syncs the map camera with the location puck.
//                transition = viewport.makeImmediateViewportTransition()
                //The immediate viewport transition moves the camera to the target state at once without using animations.
            )
            location.pulsingEnabled = true
            mapboxMap.loadStyle(Style.STANDARD)
        }
        val myLocation = findViewById<ImageView>(R.id.my_location)
        myLocation.setOnClickListener {
            mapView.viewport.transitionTo(
                targetState = mapView.viewport.makeFollowPuckViewportState(viewportOptions)
            )
        }
        GlobalScope.launch {
            val vehicleRawData =
                FetchData.fetchData(vehicleInfoApi)
            val vehicleCollection: MutableList<VehicleInfo> =
                FetchData.parseJsonVehicle(vehicleRawData)
            Log.d("edmond", "59: "+vehicleCollection.size.toString())
            addVehicleToMap(mapView, vehicleCollection)
        }
        GlobalScope.launch {
            val boundaryRawData=FetchData.fetchData(boundaryInfoApi)
            val boundaryPoints: List<List<Point>> =FetchData.parseJsonPolygon(boundaryRawData)
            Log.d("edmond", "71: "+ boundaryPoints[0][0].longitude())
            drawFlexZoneBoundary(mapView, boundaryPoints)
        }
    }

    private fun drawFlexZoneBoundary(mapView: MapView, points: List<List<Point>>) {
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

    private suspend fun addVehicleToMap(mapView: MapView, vehicles: List<VehicleInfo>) {
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

//    private fun addAnnotationToMap(mapView: MapView, drawable: Int, points: List<Point>) {
//        for (point in points) {
//            bitmapFromDrawableRes(
//                this@MainActivity,
//                drawable
//            )?.let {
//                val pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
//                val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
//                    .withPoint(point)
//                    .withIconImage(it)
//                    .withDraggable(false)
//                Log.d("edmond", pointAnnotationManager.toString())
//                pointAnnotationManager.create(pointAnnotationOptions)
//            }
//        }
//    }
//
//    private fun bitmapFromDrawableRes(context: Context, @DrawableRes resourceId: Int) =
//        convertDrawableToBitmap(AppCompatResources.getDrawable(context, resourceId))
//
//    private fun convertDrawableToBitmap(sourceDrawable: Drawable?): Bitmap? {
//        if (sourceDrawable == null) {
//            return null
//        }
//        return if (sourceDrawable is BitmapDrawable) {
//            sourceDrawable.bitmap
//        } else {
//// copying drawable object to not manipulate on the same reference
//            val constantState = sourceDrawable.constantState ?: return null
//            val drawable = constantState.newDrawable().mutate()
//            val bitmap: Bitmap = Bitmap.createBitmap(
//                drawable.intrinsicWidth, drawable.intrinsicHeight,
//                Bitmap.Config.ARGB_8888
//            )
//            val canvas = Canvas(bitmap)
//            drawable.setBounds(0, 0, canvas.width, canvas.height)
//            drawable.draw(canvas)
//            bitmap
//        }
//    }

}