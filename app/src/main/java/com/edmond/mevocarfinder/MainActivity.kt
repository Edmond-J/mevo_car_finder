package com.edmond.mevocarfinder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
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
        val myLocation=findViewById<ImageView>(R.id.my_location)
        myLocation.setOnClickListener {
            mapView.viewport.transitionTo(
                targetState =  mapView.viewport.makeFollowPuckViewportState(viewportOptions)
            )
        }
        addAnnotationToMap(mapView, R.drawable.red_marker)
        placePolygon(mapView)
        GlobalScope.launch {
            var vehicleData =
                FetchData.fetchData("https://api.mevo.co.nz/public/vehicles/wellington")
        FetchData.parseJsonVehicle(vehicleData)
        }
    }

    private fun placePolygon(mapView: MapView) {
        val polygonAnnotationManager =
            mapView.annotations.createPolygonAnnotationManager()
// Define a list of geographic coordinates to be connected.
        val points = listOf(
            listOf(
                Point.fromLngLat(174.7222, -41.3005),
                Point.fromLngLat(174.7039, -41.2755),
                Point.fromLngLat(174.7315, -41.2875),
            )
        )
// Set options for the resulting fill layer.
        val polygonAnnotationOptions = PolygonAnnotationOptions()
            .withPoints(points)
            // Style the polygon that will be added to the map.
            .withFillColor("#7AA0EB")
            .withFillOpacity(0.4)
// Add the resulting polygon to the map.
        polygonAnnotationManager.create(polygonAnnotationOptions)
    }

    private fun addAnnotationToMap(mapView: MapView, drawable: Int) {
        bitmapFromDrawableRes(
            this@MainActivity,
            drawable
        )?.let {
            val pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
            val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
                .withPoint(Point.fromLngLat(174.7258, -41.2941))
                .withIconImage(it)
                .withDraggable(true)
            Log.d("edmond", pointAnnotationManager.toString())
            pointAnnotationManager.create(pointAnnotationOptions)
        }
    }

    private fun bitmapFromDrawableRes(context: Context, @DrawableRes resourceId: Int) =
        convertDrawableToBitmap(AppCompatResources.getDrawable(context, resourceId))

    private fun convertDrawableToBitmap(sourceDrawable: Drawable?): Bitmap? {
        if (sourceDrawable == null) {
            return null
        }
        return if (sourceDrawable is BitmapDrawable) {
            sourceDrawable.bitmap
        } else {
// copying drawable object to not manipulate on the same reference
            val constantState = sourceDrawable.constantState ?: return null
            val drawable = constantState.newDrawable().mutate()
            val bitmap: Bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth, drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }
}