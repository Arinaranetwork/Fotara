// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.note.clock

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.arinara.fotara.MainActivity
import com.arinara.fotara.R
import java.util.Locale

class TimerFloatingPillService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingPillView: View? = null
    private var isRunning = false
    private var elapsedSeconds = 0L
    private val handler = Handler(Looper.getMainLooper())

    private val tickerRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                elapsedSeconds++
                updateDisplay()
                handler.postDelayed(this, 1000L)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Timer Running", "00:00"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                elapsedSeconds = intent.getLongExtra(EXTRA_ELAPSED_SECONDS, 0L)
                isRunning = true
                handler.removeCallbacks(tickerRunnable)
                handler.post(tickerRunnable)
                showFloatingPill()
            }
            ACTION_TOGGLE_PAUSE -> {
                isRunning = !isRunning
                if (isRunning) {
                    handler.post(tickerRunnable)
                } else {
                    handler.removeCallbacks(tickerRunnable)
                }
                updateDisplay()
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun formatTime(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format(Locale.US, "%02d:%02d", m, s)
    }

    private fun updateDisplay() {
        val formatted = formatTime(elapsedSeconds)
        floatingPillView?.let { pill ->
            val tv = pill.findViewById<TextView>(android.R.id.text1)
            tv?.text = formatted
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification("Timer Active", formatted))
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showFloatingPill() {
        if (!Settings.canDrawOverlays(this)) {
            return
        }
        if (floatingPillView != null) return

        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 100
                y = 200
            }

            // Create programmatically styled floating pill view
            val context = this
            val pill = android.widget.LinearLayout(context).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                setPadding(32, 16, 32, 16)
                gravity = Gravity.CENTER_VERTICAL
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(0xF0161C26.toInt())
                    cornerRadius = 64f
                    setStroke(3, 0xFF7A5AF8.toInt())
                }
            }

            val timerText = TextView(context).apply {
                id = android.R.id.text1
                text = formatTime(elapsedSeconds)
                setTextColor(0xFFF8FAFC.toInt())
                textSize = 15f
                typeface = android.graphics.Typeface.MONOSPACE
            }
            pill.addView(timerText)

            val spacer = View(context).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(24, 1)
            }
            pill.addView(spacer)

            val closeBtn = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                setColorFilter(0xFF94A3B8.toInt())
                setOnClickListener { stopSelf() }
            }
            pill.addView(closeBtn)

            // Touch Dragging
            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f

            pill.setOnTouchListener { _, event ->
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
                        windowManager?.updateViewLayout(pill, params)
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val dx = Math.abs(event.rawX - initialTouchX)
                        val dy = Math.abs(event.rawY - initialTouchY)
                        if (dx < 10 && dy < 10) {
                            // Tapped: Bring Fotara to front
                            val appIntent = Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                putExtra("OPEN_CLOCK_SCREEN", true)
                            }
                            startActivity(appIntent)
                        }
                        true
                    }
                    else -> false
                }
            }

            floatingPillView = pill
            windowManager?.addView(pill, params)
        } catch (_: Exception) {
            // Gracefully handled if overlay permission fails
        }
    }

    private fun buildNotification(title: String, timeText: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(timeText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java).apply {
                        putExtra("OPEN_CLOCK_SCREEN", true)
                    },
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Fotara Floating Timer Pill",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacks(tickerRunnable)
        floatingPillView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
        }
        floatingPillView = null
    }

    companion object {
        const val CHANNEL_ID = "fotara_timer_pill_channel"
        const val NOTIFICATION_ID = 4040
        const val ACTION_START = "com.arinara.fotara.TIMER_START"
        const val ACTION_TOGGLE_PAUSE = "com.arinara.fotara.TIMER_TOGGLE"
        const val ACTION_STOP = "com.arinara.fotara.TIMER_STOP"
        const val EXTRA_ELAPSED_SECONDS = "extra_elapsed_seconds"

        fun startService(context: Context, elapsedSeconds: Long = 0L) {
            val intent = Intent(context, TimerFloatingPillService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ELAPSED_SECONDS, elapsedSeconds)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, TimerFloatingPillService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }
}
