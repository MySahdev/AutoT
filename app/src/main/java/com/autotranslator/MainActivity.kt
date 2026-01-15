package com.autotranslator

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.autotranslator.databinding.ActivityMainBinding
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Use modern Activity Result API instead of deprecated startActivityForResult
    private lateinit var screenCaptureLauncher: ActivityResultLauncher<Intent>
    private lateinit var overlayPermissionLauncher: ActivityResultLauncher<Intent>

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupActivityResultLaunchers()
        setupUI()
        downloadTranslationModels()
    }

    private fun setupActivityResultLaunchers() {
        // Modern Activity Result API for screen capture
        screenCaptureLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                startServices(result.resultCode, result.data!!)
            } else {
                Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        // Modern Activity Result API for overlay permission
        overlayPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { _ ->
            if (checkOverlayPermission()) {
                startScreenCapture()
            } else {
                Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupUI() {
        binding.btnStart.setOnClickListener {
            if (checkOverlayPermission()) {
                startScreenCapture()
            } else {
                requestOverlayPermission()
            }
        }

        binding.btnStop.setOnClickListener {
            stopServices()
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        updateButtonStates(false)
    }

    private fun checkOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        overlayPermissionLauncher.launch(intent)
    }

    private fun startScreenCapture() {
        val mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        screenCaptureLauncher.launch(captureIntent)
    }

    private fun startServices(resultCode: Int, data: Intent) {
        // Start screen capture service using startForegroundService for Android 15+
        val captureIntent = Intent(this, ScreenCaptureService::class.java).apply {
            putExtra("resultCode", resultCode)
            putExtra("data", data)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(captureIntent)
        } else {
            startService(captureIntent)
        }

        // Start floating window service
        val floatingIntent = Intent(this, FloatingWindowService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(floatingIntent)
        } else {
            startService(floatingIntent)
        }

        updateButtonStates(true)
        Toast.makeText(this, "Auto Translator Started", Toast.LENGTH_SHORT).show()
    }

    private fun stopServices() {
        stopService(Intent(this, ScreenCaptureService::class.java))
        stopService(Intent(this, FloatingWindowService::class.java))
        updateButtonStates(false)
        Toast.makeText(this, "Auto Translator Stopped", Toast.LENGTH_SHORT).show()
    }

    private fun updateButtonStates(isRunning: Boolean) {
        binding.btnStart.isEnabled = !isRunning
        binding.btnStop.isEnabled = isRunning
        binding.statusText.text = if (isRunning) "Running" else "Stopped"
    }

    private fun downloadTranslationModels() {
        binding.statusText.text = "Downloading translation models..."

        // Download common language models
        val languages = listOf(
            TranslateLanguage.SPANISH,
            TranslateLanguage.FRENCH,
            TranslateLanguage.GERMAN,
            TranslateLanguage.CHINESE,
            TranslateLanguage.JAPANESE,
            TranslateLanguage.KOREAN,
            TranslateLanguage.HINDI,
            TranslateLanguage.ARABIC
        )

        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        var downloadedCount = 0
        languages.forEach { lang ->
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(lang)
                .setTargetLanguage(TranslateLanguage.ENGLISH)
                .build()
            val translator = Translation.getClient(options)

            translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener {
                    downloadedCount++
                    if (downloadedCount == languages.size) {
                        binding.statusText.text = "Ready to start"
                    }
                }
                .addOnFailureListener {
                    // Model will download on demand
                }
        }
    }
}
