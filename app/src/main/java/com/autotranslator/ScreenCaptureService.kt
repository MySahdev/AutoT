package com.autotranslator

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.translate.TranslateLanguage
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import kotlin.math.abs

class ScreenCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val languageIdentifier = LanguageIdentification.getClient()

    // Use background thread for captures to avoid blocking main thread
    private val handlerThread = HandlerThread("ScreenCapture").apply { start() }
    private val backgroundHandler = Handler(handlerThread.looper)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var captureInterval = 2000L // Configurable
    private var isProcessing = false // Prevent overlapping captures

    private var lastTranslatedText = ""
    private var lastTextHash = 0 // For faster text comparison

    // Cache translators to avoid recreating
    private val translatorCache = mutableMapOf<String, Translator>()

    // Downscale resolution for faster OCR
    private val maxWidth = 1080
    private val maxHeight = 1920

    companion object {
        const val CHANNEL_ID = "ScreenCaptureChannel"
        const val TAG = "ScreenCaptureService"
        var currentTranslation: String = ""
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("resultCode", -1) ?: -1
        val data = intent?.getParcelableExtra<Intent>("data")

        if (resultCode != -1 && data != null) {
            startForeground(1, createNotification())
            startCapture(resultCode, data)
        }

        return START_STICKY
    }

    private fun startCapture(resultCode: Int, data: Intent) {
        val mediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)

        val metrics = DisplayMetrics()
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(metrics)

        // Optimize: Use lower resolution for faster processing
        val width = minOf(metrics.widthPixels, maxWidth)
        val height = minOf(metrics.heightPixels, maxHeight)
        val density = metrics.densityDpi

        // Use RGB_565 for lower memory usage (half of ARGB_8888)
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        startPeriodicCapture()
    }

    private fun startPeriodicCapture() {
        backgroundHandler.postDelayed(object : Runnable {
            override fun run() {
                // Skip if previous capture is still processing
                if (!isProcessing) {
                    captureScreen()
                }
                backgroundHandler.postDelayed(this, captureInterval)
            }
        }, captureInterval)
    }

    private fun captureScreen() {
        imageReader?.let { reader ->
            val image = reader.acquireLatestImage()
            image?.let {
                processCapturedImage(it)
                it.close()
            }
        }
    }

    private fun processCapturedImage(image: Image) {
        isProcessing = true
        serviceScope.launch {
            try {
                val bitmap = imageToBitmap(image)
                bitmap?.let {
                    recognizeText(it)
                    // Recycle bitmap to free memory
                    it.recycle()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing image", e)
            } finally {
                isProcessing = false
            }
        }
    }

    private fun imageToBitmap(image: Image): Bitmap? {
        return try {
            val planes = image.planes
            val buffer: ByteBuffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width

            // Use RGB_565 for 50% memory reduction
            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.RGB_565
            )
            bitmap.copyPixelsFromBuffer(buffer)

            Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height).also {
                bitmap.recycle() // Recycle temp bitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting image to bitmap", e)
            null
        }
    }

    private fun recognizeText(bitmap: Bitmap) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val text = visionText.text.trim()

                // Fast comparison using hash to avoid string comparison
                val textHash = text.hashCode()

                if (text.isNotEmpty() && textHash != lastTextHash) {
                    lastTextHash = textHash
                    detectLanguageAndTranslate(text)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Text recognition failed", e)
            }
    }

    private fun detectLanguageAndTranslate(text: String) {
        // Use language identifier to auto-detect source language
        languageIdentifier.identifyLanguage(text)
            .addOnSuccessListener { languageCode ->
                if (languageCode == "und") {
                    Log.d(TAG, "Can't identify language")
                    return@addOnSuccessListener
                }

                // Skip translation if already in English
                if (languageCode == "en") {
                    Log.d(TAG, "Text is already in English")
                    return@addOnSuccessListener
                }

                // Map language code to TranslateLanguage
                val sourceLanguage = mapLanguageCode(languageCode)
                if (sourceLanguage != null) {
                    translateText(text, sourceLanguage)
                } else {
                    Log.d(TAG, "Unsupported language: $languageCode")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Language identification failed", e)
            }
    }

    private fun mapLanguageCode(code: String): String? {
        return when (code) {
            "es" -> TranslateLanguage.SPANISH
            "fr" -> TranslateLanguage.FRENCH
            "de" -> TranslateLanguage.GERMAN
            "zh" -> TranslateLanguage.CHINESE
            "ja" -> TranslateLanguage.JAPANESE
            "ko" -> TranslateLanguage.KOREAN
            "hi" -> TranslateLanguage.HINDI
            "ar" -> TranslateLanguage.ARABIC
            "pt" -> TranslateLanguage.PORTUGUESE
            "ru" -> TranslateLanguage.RUSSIAN
            "it" -> TranslateLanguage.ITALIAN
            "th" -> TranslateLanguage.THAI
            "vi" -> TranslateLanguage.VIETNAMESE
            else -> null
        }
    }

    private fun translateText(text: String, sourceLanguage: String) {
        serviceScope.launch {
            try {
                // Get or create cached translator
                val translatorKey = "$sourceLanguage-en"
                val translator = translatorCache.getOrPut(translatorKey) {
                    val options = TranslatorOptions.Builder()
                        .setSourceLanguage(sourceLanguage)
                        .setTargetLanguage(TranslateLanguage.ENGLISH)
                        .build()
                    Translation.getClient(options)
                }

                translator.translate(text)
                    .addOnSuccessListener { translatedText ->
                        lastTranslatedText = text
                        currentTranslation = "[$sourceLanguage → en]\n\n$translatedText"

                        // Update floating window
                        sendBroadcast(Intent("com.autotranslator.UPDATE_TRANSLATION"))
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Translation failed", e)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Translation error", e)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Capture Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Auto Translator")
        .setContentText("Monitoring screen for text...")
        .setSmallIcon(android.R.drawable.ic_menu_camera)
        .build()

    override fun onDestroy() {
        super.onDestroy()

        // Cancel all coroutines
        serviceScope.cancel()

        // Clean up handlers
        backgroundHandler.removeCallbacksAndMessages(null)
        mainHandler.removeCallbacksAndMessages(null)
        handlerThread.quitSafely()

        // Release resources
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()

        // Close ML Kit clients
        textRecognizer.close()
        languageIdentifier.close()

        // Close all cached translators
        translatorCache.values.forEach { it.close() }
        translatorCache.clear()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
