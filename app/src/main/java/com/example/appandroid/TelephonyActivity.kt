package com.example.appandroid


import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.appandroid.services.TrackingService

class TelephonyActivity : AppCompatActivity() {

    private lateinit var tvLog: TextView
    private lateinit var toggleStart: ToggleButton

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.getStringExtra(TrackingService.EXTRA_MESSAGE)?.let { msg ->
                tvLog.append("$msg\n")
            }
        }
    }

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

        tvLog = findViewById(R.id.tvStatusLoger)
        toggleStart = findViewById(R.id.toggleButton)

        LocalBroadcastManager.getInstance(this).registerReceiver(
            broadcastReceiver,
            IntentFilter(TrackingService.ACTION_UPDATE)
        )

        toggleStart.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (hasPermissions()) {
                    startService()
                } else {
                    toggleStart.isChecked = false
                    ActivityCompat.requestPermissions(this, PERMISSIONS, REQ_CODE)
                }
            } else {
                stopService()
            }
        }

        findViewById<Button>(R.id.btnExport).setOnClickListener {
            exportTelemetryFile()
        }

        findViewById<Button>(R.id.btnTestConnection).setOnClickListener {
            tvLog.append("Тест соединения...\n")
            Intent(this, TrackingService::class.java).apply {
                action = TrackingService.ACTION_TEST_CONNECTION
                startService(this)
            }
        }
    }

    private fun exportTelemetryFile() {
        if (!checkStoragePermission()) {
            tvLog.append("Предоставьте доступ к файлам\n")
            return
        }
        try {
            val src = java.io.File(filesDir, "telemetry.jsonl")
            if (!src.exists()) {
                tvLog.append("Файл не найден. Сначала запустите сбор данных.\n")
                return
            }
            if (src.length() == 0L) {
                tvLog.append("Файл пустой\n")
                return
            }
            val recordCount = src.readLines().size
            val exportFileName = "telemetry_${DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").format(LocalDateTime.now())}.jsonl"
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, exportFileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/jsonl")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
                    android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = contentResolver.insert(
                android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            )
            uri?.let {
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    src.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                tvLog.append("Экспортировано $recordCount записей\n")
                tvLog.append("Файл: $exportFileName\n")
                tvLog.append("Папка: Downloads\n")
            } ?: run {
                tvLog.append("Ошибка: не удалось создать файл\n")
            }
        } catch (e: Exception) {
            tvLog.append("Ошибка экспорта: ${e.message}\n")
            e.printStackTrace()
        }
    }

    private fun startService() {
        Intent(this, TrackingService::class.java).apply {
            action = TrackingService.ACTION_START
            startService(this)
        }
        tvLog.append("Запуск сервиса...\n")
    }

    private fun stopService() {
        Intent(this, TrackingService::class.java).apply {
            action = TrackingService.ACTION_STOP
            startService(this)
        }
        tvLog.append("Остановка сервиса...\n")
    }

    private fun checkStoragePermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
                return false
            }
        }
        return true
    }

    private fun hasPermissions() = PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            toggleStart.isChecked = true
            startService()
        } else {
            tvLog.append("Нет прав\n")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }
}