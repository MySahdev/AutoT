package com.autotranslator

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView

class FloatingWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var translationText: TextView? = null

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateTranslation()
        }
    }

    companion object {
        private const val TAG = "FloatingWindowService"
    }

    override fun onCreate() {
        super.onCreate()

        // Android 13+ requires explicit export flag for BroadcastReceiver
        val filter = IntentFilter("com.autotranslator.UPDATE_TRANSLATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(updateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(updateReceiver, filter)
        }

        createFloatingWindow()
    }

    private fun createFloatingWindow() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_window, null)
        translationText = floatingView?.findViewById(R.id.translationText)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 100

        windowManager.addView(floatingView, params)

        setupDraggable(params)
    }

    private fun setupDraggable(params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        floatingView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(floatingView, params)
                    true
                }
                else -> false
            }
        }
    }

    private fun updateTranslation() {
        translationText?.text = ScreenCaptureService.currentTranslation
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(updateReceiver)
        floatingView?.let { windowManager.removeView(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
