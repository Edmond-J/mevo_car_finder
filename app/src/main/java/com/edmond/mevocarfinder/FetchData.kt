package com.edmond.mevocarfinder

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
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
                    connection.connectTimeout = 10000 // 设置连接超时时间
                    connection.readTimeout = 10000 // 设置读取超时时间
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
                        Log.d("edmond", "31: $response")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return response
        }


        fun parseJsonVehicle(jsonString: String?) {
            val jsonObject = JSONObject(jsonString)
            val featureCollection = jsonObject.getJSONObject("data").getJSONArray("features")
//featureCollection.length()
            for (i in 0 until featureCollection.length()) {
                val feature = featureCollection.getJSONObject(i)
                val coordinates = feature.getJSONObject("geometry").getJSONArray("coordinates")
                val latitude = coordinates.getString(0)
                val longitude = coordinates.getString(1)
                val iconUrl = feature.getJSONObject("properties").getString("iconUrl")
                Log.d("edmond", latitude)
                Log.d("edmond", longitude)
                Log.d("edmond", iconUrl)

            }
        }
    }
}