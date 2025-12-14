package com.example.appandroid

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.widget.Button
import java.util.TimeZone

private val NOVOSIBIRSK_TZ = TimeZone.getTimeZone("Asia/Novosibirsk")

class LocationActivity : LocationListener, AppCompatActivity() {

    private lateinit var locationManager: LocationManager
    private lateinit var tvLat: TextView
    private lateinit var tvLon: TextView
    private lateinit var tvAlt: TextView
    private lateinit var tvTime: TextView

    companion object {
        private const val LOCATION_PERMISSION_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        tvLat = findViewById(R.id.tv_lat)
        tvLon = findViewById(R.id.tv_lon)
        tvAlt = findViewById(R.id.tv_alt)
        tvTime = findViewById(R.id.tv_time)

        val btnClearLog = findViewById<Button>(R.id.btn_clear_log)
        btnClearLog.setOnClickListener {
            clearLocationLog()
        }
    }

    override fun onResume() {
        super.onResume()
        updateLocation()
    }

    private fun updateLocation() {
        if (!hasPermissions()) {
            requestPermissions()
            return
        }
        if (!isLocationEnabled()) {
            Toast.makeText(this, "Включите определение местоположения", Toast.LENGTH_SHORT).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, this)
    }

    private fun hasPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            LOCATION_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            updateLocation()
        } else {
            Toast.makeText(this, "Разрешение отклонено", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    override fun onLocationChanged(location: Location) {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("Asia/Novosibirsk")
        }

        val localTime = formatter.format(Date(location.time))

        tvLat.text = "Широта: ${location.latitude}"
        tvLon.text = "Долгота: ${location.longitude}"
        tvAlt.text = "Высота: ${if (location.hasAltitude()) location.altitude else -1.0} м"
        tvTime.text = "Дата и время: $localTime"

        saveLocationToFile(location, formatter)
    }

    private fun saveLocationToFile(location: Location, timeFormatter: SimpleDateFormat? = null) {
        try {
            val formatter = timeFormatter ?: SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("Asia/Novosibirsk")
            }

            val json = JSONObject().apply {
                put("latitude", location.latitude)
                put("longitude", location.longitude)
                put("altitude", if (location.hasAltitude()) location.altitude else null)
                put("time", location.time)
                put("time_formatted", formatter.format(Date(location.time)))
            }

            val file = File(filesDir, "location_log.json")
            val content = if (file.exists()) org.json.JSONArray(file.readText()) else org.json.JSONArray()
            content.put(json)
            file.writeText(content.toString(2))
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка записи: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    private fun clearLocationLog() {
        try {
            val file = File(filesDir, "location_log.json")
            file.writeText("[]")
            Toast.makeText(this, "Лог очищен", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка очистки: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}