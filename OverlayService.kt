package com.etour.app

import android.app.*
import android.content.*
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.*
import org.json.JSONArray

class OverlayService : Service() {

    data class Cred(val name: String, val id: String, val pass: String)

    companion object {
        const val EXTRA_JSON = "creds"
        @Volatile var running = false
    }

    private lateinit var wm: WindowManager
    private var root: FrameLayout? = null
    private lateinit var lp: WindowManager.LayoutParams
    private var expanded = false
    private var creds: List<Cred> = emptyList()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        parse(intent?.getStringExtra(EXTRA_JSON))
        if (root == null) createOverlay()
        render()
        running = true
        return START_NOT_STICKY
    }

    private fun parse(json: String?) {
        if (json.isNullOrBlank()) return
        try {
            val a = JSONArray(json)
            creds = (0 until a.length()).map {
                val o = a.getJSONObject(it)
                Cred(o.optString("name"), o.optString("roomId"), o.optString("roomPass"))
            }
        } catch (_: Exception) {}
    }

    private fun startAsForeground() {
        val chId = "etour_overlay"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26)
            nm.createNotificationChannel(NotificationChannel(chId, "Room Overlay", NotificationManager.IMPORTANCE_LOW))
        val n = (if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, chId) else Notification.Builder(this))
            .setContentTitle("Room ID overlay চালু আছে")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .build()
        if (Build.VERSION.SDK_INT >= 34)
            startForeground(1, n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
    }

    private fun createOverlay() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = dp(8); y = dp(140) }
        root = FrameLayout(this)
        wm.addView(root, lp)
    }

    private fun render() {
        val r = root ?: return
        r.removeAllViews()
        r.addView(if (expanded) panel() else bubble())
        try { wm.updateViewLayout(r, lp) } catch (_: Exception) {}
    }

    private fun bg(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = radius.toFloat()
    }

    private fun bubble(): View {
        val t = TextView(this).apply {
            text = "🔑"; textSize = 20f; gravity = Gravity.CENTER
            background = bg(Color.parseColor("#F59E0B"), dp(24))
            layoutParams = FrameLayout.LayoutParams(dp(48), dp(48))
        }
        draggable(t) { expanded = true; render() }
        return t
    }

    private fun panel(): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = bg(Color.parseColor("#F2111827"), dp(16))
            setPadding(dp(12), dp(8), dp(12), dp(12))
            layoutParams = FrameLayout.LayoutParams(dp(270), FrameLayout.LayoutParams.WRAP_CONTENT)
        }
        val head = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        head.addView(TextView(this).apply {
            text = "🔑 Room ID & Password"; setTextColor(Color.WHITE); textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        head.addView(iconBtn("—") { expanded = false; render() })
        head.addView(iconBtn("✕") { stopSelf() })
        draggable(head) {}
        box.addView(head)

        if (creds.isEmpty()) {
            box.addView(TextView(this).apply {
                text = "এখনো Room ID দেওয়া হয়নি"; setTextColor(Color.LTGRAY); textSize = 13f; setPadding(0, dp(10), 0, dp(4))
            })
        }
        creds.forEach { c ->
            if (c.name.isNotBlank()) box.addView(TextView(this).apply {
                text = c.name; setTextColor(Color.parseColor("#9CA3AF")); textSize = 11f; setPadding(0, dp(8), 0, 0)
            })
            box.addView(row("ID", c.id))
            box.addView(row("PASS", c.pass))
            if (c.id.isNotBlank() && c.pass.isNotBlank()) box.addView(Button(this).apply {
                text = "দুটোই একসাথে কপি"; textSize = 12f; setTextColor(Color.WHITE)
                background = bg(Color.parseColor("#0F766E"), dp(10)); isAllCaps = false
                setOnClickListener { copy(c.id + "\n" + c.pass) }
            }, LinearLayout.LayoutParams(-1, dp(40)).apply { topMargin = dp(6) })
        }
        return box
    }

    private fun row(label: String, value: String): View {
        val l = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(6), 0, 0) }
        l.addView(TextView(this).apply { text = label; setTextColor(Color.parseColor("#9CA3AF")); textSize = 10f },
            LinearLayout.LayoutParams(dp(36), -2))
        l.addView(TextView(this).apply {
            text = if (value.isBlank()) "—" else value; setTextColor(Color.WHITE); textSize = 17f
            typeface = android.graphics.Typeface.MONOSPACE
        }, LinearLayout.LayoutParams(0, -2, 1f))
        if (value.isNotBlank()) l.addView(Button(this).apply {
            text = "Copy"; textSize = 11f; setTextColor(Color.WHITE); isAllCaps = false
            background = bg(Color.parseColor("#4F46E5"), dp(9)); setPadding(dp(10), 0, dp(10), 0)
            setOnClickListener { copy(value) }
        }, LinearLayout.LayoutParams(-2, dp(34)))
        return l
    }

    private fun iconBtn(t: String, onClick: () -> Unit) = TextView(this).apply {
        text = t; setTextColor(Color.WHITE); textSize = 16f; gravity = Gravity.CENTER
        background = bg(Color.parseColor("#33FFFFFF"), dp(15))
        layoutParams = LinearLayout.LayoutParams(dp(30), dp(30)).apply { leftMargin = dp(6) }
        setOnClickListener { onClick() }
    }

    private fun copy(text: String) {
        val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("room", text))
        Toast.makeText(this, "কপি হয়েছে", Toast.LENGTH_SHORT).show()
    }

    private fun draggable(v: View, onTap: () -> Unit) {
        var sx = 0f; var sy = 0f; var ox = 0; var oy = 0; var moved = false
        v.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; ox = lp.x; oy = lp.y; moved = false; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - sx; val dy = e.rawY - sy
                    if (Math.abs(dx) > dp(6) || Math.abs(dy) > dp(6)) moved = true
                    if (moved) { lp.x = (ox + dx).toInt(); lp.y = (oy + dy).toInt(); try { wm.updateViewLayout(root, lp) } catch (_: Exception) {} }
                    true
                }
                MotionEvent.ACTION_UP -> { if (!moved) onTap(); true }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        running = false
        try { root?.let { wm.removeView(it) } } catch (_: Exception) {}
        root = null
        super.onDestroy()
    }
}
