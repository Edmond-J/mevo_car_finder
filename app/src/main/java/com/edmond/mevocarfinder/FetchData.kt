package com.edmond.mevocarfinder

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.mapbox.geojson.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class FetchData {
    companion object {
        //"https://api.mevo.co.nz/public/vehicles/wellington"
        suspend fun fetchData(urlString: String): String? {
            var response: String? = null
            try {
                withContext(Dispatchers.IO) {
                    val url = URL(urlString)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    val inputStream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val stringBuffer = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stringBuffer.append(line)
                    }
                    reader.close()
                    inputStream.close()
                    connection.disconnect()
                    response = stringBuffer.toString()
                    // 在协程中打印 Log 时需要在 Dispatchers.Main 上下文中执行
                    withContext(Dispatchers.Main) {
//                        Log.d("edmond", "42: $response")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return response
        }

        fun parseJsonVehicle(jsonString: String?): MutableList<VehicleInfo> {
            val jsonObject = JSONObject(jsonString)
            val featureCollection = jsonObject.getJSONObject("data").getJSONArray("features")
            val vehicleCollection: MutableList<VehicleInfo> = mutableListOf()
            for (i in 0 until featureCollection.length()) {
                val feature = featureCollection.getJSONObject(i)
                val coordinates = feature.getJSONObject("geometry").getJSONArray("coordinates")
                val longitude = coordinates.getString(0).toDouble()
                val latitude = coordinates.getString(1).toDouble()
                val iconUrl = feature.getJSONObject("properties").getString("iconUrl")
                vehicleCollection.add(VehicleInfo(longitude, latitude, iconUrl))
            }
            return vehicleCollection
        }

        fun parseJsonPolygon(jsonString: String?): List<List<Point>>{
            val jsonObject = JSONObject(jsonString)
            val dataObject = jsonObject.getJSONObject("data")
            val geometryObject = dataObject.getJSONObject("geometry")
            val coordinatesArray = geometryObject.getJSONArray("coordinates")
            val polygons = mutableListOf<List<Point>>()
            for (i in 1 until coordinatesArray.length()) {
                val coordinateList = coordinatesArray.getJSONArray(i)
                val points = mutableListOf<Point>()
                for (j in 0 until coordinateList.length()) {
                    val coordinatePair = coordinateList.getJSONArray(j)
                    val longitude = coordinatePair.getDouble(0)
                    val latitude = coordinatePair.getDouble(1)
                    val point = Point.fromLngLat(longitude, latitude)
                    points.add(point)
                }
                polygons.add(points)
            }
            return polygons
        }

        suspend fun loadImageBitmap(resource: Resources, urlString: String): Bitmap {
           return withContext(Dispatchers.IO) {
                var bitmap: Bitmap?=null
                var urlConnection: HttpURLConnection? = null
                try {
                    val url = URL(urlString)
                    urlConnection = url.openConnection() as HttpURLConnection
                    urlConnection.connect()
                    val inputStream = BufferedInputStream(urlConnection.inputStream)
                    bitmap = if(inputStream!=null)
                        BitmapFactory.decodeStream(inputStream)
                    else
                        BitmapFactory.decodeResource(resource, R.drawable.red_marker)
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    urlConnection?.disconnect()
                }
                bitmap !!
            }
        }


    }
}