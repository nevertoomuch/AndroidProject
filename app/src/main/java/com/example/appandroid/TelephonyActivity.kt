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
import android.telephony.*
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import android.os.Build

class TelephonyActivity : AppCompatActivity(), LocationListener {

    private lateinit var locationManager: LocationManager
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var tvLog: TextView
    private lateinit var toggleStart: ToggleButton

    private var zmqContext: ZContext? = null
    private val serverAddress = "tcp://172.24.0.121:1337"

    companion object {
        private const val REQ_CODE = 100
        private val PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_telephonedata)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        tvLog = findViewById(R.id.tvStatusLoger)
        toggleStart = findViewById(R.id.toggleButton)

        zmqContext = ZContext()

        toggleStart.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (hasPermissions()) {
                    startTracking()
                } else {
                    toggleStart.isChecked = false
                    ActivityCompat.requestPermissions(this, PERMISSIONS, REQ_CODE)
                }
            } else {
                stopTracking()
            }
        }
    }

    private fun startTracking() {
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            toggleStart.isChecked = false
            return
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 1f, this)
            log("Запуск...")
        }
    }

    private fun stopTracking() {
        locationManager.removeUpdates(this)
        log("Остановлено")
    }

    override fun onLocationChanged(location: Location) {
        val payload = JSONObject().apply {
            put("lat", location.latitude)
            put("lon", location.longitude)
            put("alt", location.altitude)
            put("accuracy", location.accuracy)
            put("time", location.time)
            put("cell_data", getCellularData())
        }.toString()

        Thread { sendZmq(payload) }.start()
        log("${"%.6f".format(location.latitude)}, ${"%.6f".format(location.longitude)}")
    }

    private fun getCellularData(): JSONObject {
        val result = JSONObject()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return result.put("error", "Permission Denied")
        }

        try {
            val cellInfoList = telephonyManager.allCellInfo
            if (cellInfoList.isNullOrEmpty()) return result.put("error", "Данные не найдены!")

            val cellInfo = cellInfoList.firstOrNull { it.isRegistered }
            if (cellInfo == null) return result.put("error", "Вышек Связи не найдено!")

            when (cellInfo) {
                is CellInfoLte -> {
                    val id = cellInfo.cellIdentity
                    val sig = cellInfo.cellSignalStrength
                    result.put("type", "LTE")

                    result.put("identity", JSONObject().apply {
                        put("band", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                            id.bands?.joinToString() else "N/A")

                        put("earfcn", id.earfcn)
                        put("mcc", id.mccString ?: "N/A")
                        put("mnc", id.mncString ?: "N/A")
                        put("pci", id.pci)
                        put("tac", id.tac)
                    })

                    result.put("signal", JSONObject().apply {
                        put("asuLevel", sig.asuLevel)
                        put("cqi", sig.cqi)
                        put("rsrp", sig.rsrp)
                        put("rsrq", sig.rsrq)
                        put("rssi", sig.rssi)
                        put("rssnr", sig.rssnr)
                        try {
                            val ta = sig.timingAdvance
                            put("timingAdvance", if (ta != Int.MIN_VALUE) ta else -1)
                        } catch (e: Exception) {
                            put("timingAdvance", -1)
                        }
                    })
                }

                is CellInfoGsm -> {
                    val id = cellInfo.cellIdentity
                    val sig = cellInfo.cellSignalStrength
                    result.put("type", "GSM")

                    result.put("identity", JSONObject().apply {
                        @Suppress("DEPRECATION")
                        put("ci", id.cid.toLong())
                        put("bsic", id.bsic)
                        put("arfcn", id.arfcn)
                        put("lac", id.lac)
                        put("mcc", id.mccString ?: "N/A")
                        put("mnc", id.mncString ?: "N/A")
                        put("psc", id.psc)
                    })

                    result.put("signal", JSONObject().apply {
                        put("dbm", sig.dbm)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            put("rssi", sig.rssi)
                        } else {
                            put("rssi", sig.dbm)
                        }
                        try {
                            val ta = sig.timingAdvance
                            put("timingAdvance", if (ta != Int.MIN_VALUE) ta else -1)
                        } catch (e: Exception) {
                            put("timingAdvance", -1)
                        }
                    })
                }

                is CellInfoNr -> {
                    val id = cellInfo.cellIdentity as CellIdentityNr
                    val sig = cellInfo.cellSignalStrength as CellSignalStrengthNr
                    result.put("type", "NR")

                    result.put("identity", JSONObject().apply {
                        put("band", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                            id.bands?.joinToString() else "N/A")
                        put("nci", id.nci)
                        put("pci", id.pci)
                        put("nrarfcn", id.nrarfcn)
                        put("tac", id.tac)
                        put("mcc", id.mccString ?: "N/A")
                        put("mnc", id.mncString ?: "N/A")
                    })

                    result.put("signal", JSONObject().apply {
                        put("ssRsrp", sig.ssRsrp)
                        put("ssRsrq", sig.ssRsrq)
                        put("ssSinr", sig.ssSinr)
                        put("asuLevel", sig.asuLevel)

                    })
                }
            }
        } catch (e: Exception) {
            result.put("error", e.message)
        }

        return result
    }

    private fun sendZmq(data: String) {
        try {
            zmqContext?.let { ctx ->
                val socket = ctx.createSocket(SocketType.REQ).apply {
                    connect(serverAddress)
                    sendTimeOut = 2000
                    receiveTimeOut = 2000
                    linger = 0
                }
                socket.send(data.toByteArray(ZMQ.CHARSET), 0)
                val reply = socket.recv(0)
                if (reply != null) {
                    val msg = String(reply, ZMQ.CHARSET)
                    runOnUiThread { log("Сервер: $msg") }
                }
                socket.close()
            }
        } catch (e: Exception) {
            runOnUiThread { log("ZMQ: ${e.message}") }
        }
    }

    private fun log(msg: String) {
        runOnUiThread { tvLog.append("$msg\n") }
    }

    private fun hasPermissions() = PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            toggleStart.isChecked = true
            startTracking()
        } else {
            log("Нет прав")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
        zmqContext?.close()
    }

    override fun onStatusChanged(p: String?, s: Int, b: Bundle?) {}
    override fun onProviderEnabled(p: String) {}
    override fun onProviderDisabled(p: String) {}
}




