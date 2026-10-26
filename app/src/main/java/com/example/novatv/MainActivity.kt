package com.example.novatv

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.DateFormat
import java.util.Date

class MainActivity : Activity() {
    private class App(val label: String, val pkg: String, val icon: Drawable, val intent: Intent)
    private enum class Screen { HOME, DRAWER, SETTINGS }

    private val accents = intArrayOf(
        Color.parseColor("#4FC3F7"), Color.parseColor("#BA68C8"), Color.parseColor("#81C784"),
        Color.parseColor("#FFB74D"), Color.parseColor("#E57373"))
    private val walls = arrayOf(
        intArrayOf(Color.parseColor("#0B1020"), Color.parseColor("#1B2A4A")),
        intArrayOf(Color.parseColor("#120B1F"), Color.parseColor("#3A1F4A")),
        intArrayOf(Color.parseColor("#0A1A14"), Color.parseColor("#1F4A3A")),
        intArrayOf(Color.parseColor("#000000"), Color.parseColor("#202020")))

    private lateinit var prefs: SharedPreferences
    private var apps: List<App> = emptyList()
    private var screen = Screen.HOME
    private var query = ""
    private var status: TextView? = null
    private var searchField: EditText? = null
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() { updateStatus(); handler.postDelayed(this, 15000) }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        prefs = getSharedPreferences("nova", MODE_PRIVATE)
    }

    override fun onResume() {
        super.onResume()
        loadApps(); render(); handler.post(tick)
    }

    override fun onPause() { super.onPause(); handler.removeCallbacks(tick) }

    override fun onNewIntent(i: Intent) {
        super.onNewIntent(i)
        if (i.hasCategory(Intent.CATEGORY_HOME)) { screen = Screen.HOME; query = ""; render() }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (screen != Screen.HOME) { screen = Screen.HOME; query = ""; render() }
        // On the home screen a launcher ignores Back.
    }

    // ---------- data ----------
    private fun loadApps() {
        val found = LinkedHashMap<String, App>()
        // TV-optimised entries first, then ordinary launcher entries.
        for (cat in arrayOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val q = Intent(Intent.ACTION_MAIN).addCategory(cat)
            for (ri in packageManager.queryIntentActivities(q, 0)) {
                val pkg = ri.activityInfo.packageName
                if (pkg == packageName || found.containsKey(pkg)) continue
                val go = Intent(Intent.ACTION_MAIN).addCategory(cat)
                    .setClassName(pkg, ri.activityInfo.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                found[pkg] = App(ri.loadLabel(packageManager).toString(), pkg, ri.loadIcon(packageManager), go)
            }
        }
        apps = found.values.sortedBy { it.label.lowercase() }
    }

    private fun favs() = (prefs.getString("fav", "") ?: "").split(",").filter { it.isNotEmpty() }

    private fun toggleFav(a: App) {
        val l = favs().toMutableList()
        if (l.remove(a.pkg)) toast("Removed ${a.label}") else { l.add(a.pkg); toast("Pinned ${a.label}") }
        prefs.edit().putString("fav", l.joinToString(",")).apply()
    }

    private fun launch(a: App) {
        try { startActivity(a.intent) } catch (e: Exception) { toast("${a.label} can't be opened") }
    }

    private fun open(action: String) {
        try { startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (e: Exception) { toast("Not available on this device") }
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    @Suppress("DEPRECATION")
    private fun updateStatus() {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val n = cm.activeNetworkInfo
        val net = when {
            n == null || !n.isConnected -> "Offline"
            n.type == ConnectivityManager.TYPE_WIFI -> "Wi-Fi"
            n.type == ConnectivityManager.TYPE_ETHERNET -> "Ethernet"
            else -> "Online"
        }
        val t = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date())
        status?.text = "$t   |   $net"
    }

    // ---------- UI ----------
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun accent() = accents[prefs.getInt("accent", 0)]

    private fun shape(fill: Int, stroke: Int, w: Int) = GradientDrawable().apply {
        setColor(fill); cornerRadius = dp(14).toFloat(); if (w > 0) setStroke(dp(w), stroke)
    }

    private fun label(t: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false) =
        TextView(this).apply { text = t; textSize = size; setTextColor(color); if (bold) setTypeface(null, Typeface.BOLD) }

    private fun heading(t: String) =
        label(t, 20f, Color.parseColor("#9FB0D0")).apply { setPadding(dp(8), dp(20), 0, dp(4)) }

    // One focusable card; hold OK (long press) triggers onLong.
    private fun card(text: String, icon: Drawable?, w: Int, h: Int, onClick: () -> Unit, onLong: (() -> Unit)? = null): View {
        val ac = accent()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            isFocusable = true; isClickable = true
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_focused), shape(Color.parseColor("#2A3A5E"), ac, 3))
                addState(intArrayOf(), shape(Color.parseColor("#66101828"), 0, 0))
            }
            setOnClickListener { onClick() }
            if (onLong != null) setOnLongClickListener { onLong(); true }
            setOnFocusChangeListener { v, f ->
                val s = if (f) 1.08f else 1f
                v.animate().scaleX(s).scaleY(s).setDuration(120).start()
            }
            layoutParams = LinearLayout.LayoutParams(w, h).apply { setMargins(dp(8), dp(8), dp(8), dp(8)) }
        }
        if (icon != null) box.addView(ImageView(this).apply {
            setImageDrawable(icon); layoutParams = LinearLayout.LayoutParams(dp(56), dp(56))
        })
        box.addView(TextView(this).apply {
            this.text = text; textSize = 16f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            maxLines = 1; ellipsize = TextUtils.TruncateAt.END; setPadding(0, dp(6), 0, 0)
        })
        return box
    }

    private fun render() {
        val root = FrameLayout(this)
        root.background = GradientDrawable(GradientDrawable.Orientation.TL_BR, walls[prefs.getInt("wall", 0)])
        // Overscan-safe: keep 5% margins on every side for older TVs.
        val px = resources.displayMetrics.widthPixels * 5 / 100
        val py = resources.displayMetrics.heightPixels * 5 / 100
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(px, py, px, py) }
        val bar = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        bar.addView(label("Nova TV", 30f, accent(), true))
        bar.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        status = label("", 18f, Color.parseColor("#C8D3EA")); bar.addView(status)
        col.addView(bar); updateStatus()
        when (screen) { Screen.HOME -> home(col); Screen.DRAWER -> drawer(col); Screen.SETTINGS -> settings(col) }
        root.addView(col, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
        root.requestFocus()
    }

    private fun row(vararg v: View) = LinearLayout(this).apply { v.forEach { addView(it) } }

    private fun home(col: LinearLayout) {
        col.addView(heading("Favourites"))
        val fav = favs().mapNotNull { p -> apps.firstOrNull { it.pkg == p } }
        val r = LinearLayout(this)
        if (fav.isEmpty()) r.addView(label("Open All apps and hold OK on an app to pin it here.", 18f, Color.GRAY))
        fav.forEach { a -> r.addView(card(a.label, a.icon, dp(150), dp(130), { launch(a) }, { toggleFav(a); render() })) }
        col.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(r) })
        col.addView(heading("Navigate"))
        col.addView(row(
            card("All apps", null, dp(220), dp(100), { screen = Screen.DRAWER; render() }),
            card("Search", null, dp(220), dp(100), { screen = Screen.DRAWER; render(); searchField?.requestFocus() }),
            card("Settings", null, dp(220), dp(100), { screen = Screen.SETTINGS; render() })))
    }

    private fun drawer(col: LinearLayout) {
        val et = EditText(this).apply {
            hint = "Search apps"; setText(query); setSingleLine(); textSize = 20f
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY)
            background = shape(Color.parseColor("#66101828"), accent(), 2); setPadding(dp(16), dp(10), dp(16), dp(10))
        }
        searchField = et
        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        fun fill() {
            grid.removeAllViews()
            apps.filter { it.label.contains(query, true) }.chunked(6).forEach { chunk ->
                grid.addView(row(*chunk.map { a ->
                    card(a.label, a.icon, dp(150), dp(120), { launch(a) }, { toggleFav(a) })
                }.toTypedArray()))
            }
        }
        et.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { query = s.toString(); fill() }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
        fill()
        col.addView(et)
        col.addView(label("Hold OK on an app to pin or unpin it", 14f, Color.GRAY).apply { setPadding(dp(8), dp(6), 0, dp(6)) })
        col.addView(ScrollView(this).apply { addView(grid) })
    }

    private fun settings(col: LinearLayout) {
        col.addView(heading("Settings"))
        fun cycle(key: String, n: Int) { prefs.edit().putInt(key, (prefs.getInt(key, 0) + 1) % n).apply(); render() }
        col.addView(row(
            card("Accent colour", null, dp(240), dp(100), { cycle("accent", accents.size) }),
            card("Wallpaper", null, dp(240), dp(100), { cycle("wall", walls.size) })))
        col.addView(row(
            card("Android settings", null, dp(240), dp(100), { open(Settings.ACTION_SETTINGS) }),
            card("Network", null, dp(240), dp(100), { open(Settings.ACTION_WIRELESS_SETTINGS) }),
            card("Choose home app", null, dp(240), dp(100), { open(Settings.ACTION_HOME_SETTINGS) })))
    }
}
