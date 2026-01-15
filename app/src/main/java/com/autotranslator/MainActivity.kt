package com.autotranslator

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.autotranslator.databinding.ActivityMainBinding
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val SCREEN_CAPTURE_REQUEST_CODE = 100
    private val OVERLAY_PERMISSION_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        downloadTranslationModels()
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
        startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
    }

    private fun startScreenCapture() {
        val mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(captureIntent, SCREEN_CAPTURE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            SCREEN_CAPTURE_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    startServices(resultCode, data)
                } else {
                    Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            OVERLAY_PERMISSION_REQUEST_CODE -> {
                if (checkOverlayPermission()) {
                    startScreenCapture()
                } else {
                    Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startServices(resultCode: Int, data: Intent) {
        // Start screen capture service
        val captureIntent = Intent(this, ScreenCaptureService::class.java).apply {
            putExtra("resultCode", resultCode)
            putExtra("data", data)
        }
        startService(captureIntent)

        // Start floating window service
        val floatingIntent = Intent(this, FloatingWindowService::class.java)
        startService(floatingIntent)

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
