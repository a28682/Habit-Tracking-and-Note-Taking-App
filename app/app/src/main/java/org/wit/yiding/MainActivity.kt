package org.wit.yiding

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var txtLab: TextView
    private lateinit var txtHomework: TextView
    private lateinit var btnEdit: Button
    private lateinit var tableContainer: ConstraintLayout
    private var isEditing = false
    private var rowCount = 0
    private val habitVisibilityMap = mutableMapOf<String, Boolean>()

    companion object {
        const val ACTION_HABIT_UPDATED = "org.wit.yiding.ACTION_HABIT_UPDATED"
        const val KEY_HABIT_VISIBILITY = "habit_visibility"
        private const val VISIBILITY_STATE_KEY = "visibility_state"

        fun notifyHabitsUpdated(context: Context) {
            LocalBroadcastManager.getInstance(context).sendBroadcast(
                Intent(ACTION_HABIT_UPDATED)
            )
        }
    }

    private val habitChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_HABIT_UPDATED) {
                Log.d("MainActivity", "Received habit update broadcast")
                loadAndDisplayHabits()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupButtonListeners()

        if (savedInstanceState == null) {
            loadHabitVisibilityStates()
        }
        loadAndDisplayHabits()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(VISIBILITY_STATE_KEY, HashMap(habitVisibilityMap))
        Log.d("MainActivity", "Saved instance state with ${habitVisibilityMap.size} items")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        @Suppress("UNCHECKED_CAST")
        val savedMap = savedInstanceState.getSerializable(VISIBILITY_STATE_KEY) as? HashMap<String, Boolean>
        savedMap?.let {
            habitVisibilityMap.clear()
            habitVisibilityMap.putAll(it)
            Log.d("MainActivity", "Restored ${it.size} visibility states from instance state")
        }
    }

    override fun onPause() {
        super.onPause()
        saveHabitVisibilityStates()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(habitChangeReceiver)
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            habitChangeReceiver,
            IntentFilter(ACTION_HABIT_UPDATED)
        )
        loadHabitVisibilityStates()
        loadAndDisplayHabits()
    }

    private fun initViews() {
        txtLab = findViewById(R.id.txt_lab)
        txtHomework = findViewById(R.id.txt_homework)
        btnEdit = findViewById(R.id.btn_edit)
        tableContainer = findViewById(R.id.table_container)

        txtLab.isEnabled = false
        txtHomework.isEnabled = false
    }

    private fun setupButtonListeners() {
        findViewById<Button>(R.id.btn2).setOnClickListener {
            startActivity(Intent(this, calendarActivity::class.java))
        }

        findViewById<Button>(R.id.btn3).setOnClickListener {
            startActivity(Intent(this, thirdActivity::class.java))
        }

        btnEdit.setOnClickListener {
            toggleEditMode()
        }
    }

    private fun toggleEditMode() {
        isEditing = !isEditing
        btnEdit.text = if (isEditing) "Save" else "Edit"
        txtLab.isEnabled = isEditing
        txtHomework.isEnabled = isEditing

        if (!isEditing) {
            Toast.makeText(this, "Changes saved", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadAndDisplayHabits() {
        val prefs = getSharedPreferences(SharedPrefsConstants.PREFS_NAME, MODE_PRIVATE)
        val habitCount = prefs.getInt(SharedPrefsConstants.KEY_HABIT_COUNT, 0)

        tableContainer.removeAllViews()
        rowCount = 0

        for (i in 0 until habitCount) {
            try {
                val name = prefs.getString("${SharedPrefsConstants.KEY_HABIT_PREFIX}${i}_name", "") ?: ""
                val desc = prefs.getString("${SharedPrefsConstants.KEY_HABIT_PREFIX}${i}_desc", "") ?: ""
                val isEnabled = prefs.getBoolean("${SharedPrefsConstants.KEY_HABIT_PREFIX}${i}_enabled", false)

                if (isEnabled && name.isNotEmpty()) {
                    addHabitRowToMain(name, desc)
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading habit $i", e)
            }
        }
    }

    private fun addHabitRowToMain(habitName: String, description: String) {
        val rowId = View.generateViewId()
        val prefs = getSharedPreferences(SharedPrefsConstants.PREFS_NAME, MODE_PRIVATE)
        val habitCount = prefs.getInt(SharedPrefsConstants.KEY_HABIT_COUNT, 0)
        var imageUri: Uri? = null

        for (i in 0 until habitCount) {
            if (prefs.getString("${SharedPrefsConstants.KEY_HABIT_PREFIX}${i}_name", "") == habitName) {
                val uriString = prefs.getString("${SharedPrefsConstants.KEY_HABIT_PREFIX}${i}_uri", null)
                imageUri = uriString?.let { Uri.parse(it) }
                break
            }
        }

        val isImageVisible = habitVisibilityMap.getOrPut(habitName) { true }

        val rowContainer = LinearLayout(this).apply {
            id = rowId
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 0)
            }
            background = ContextCompat.getDrawable(context, R.drawable.table_row_border)
            setPadding(16.dpToPx(), 8.dpToPx(), 16.dpToPx(), 8.dpToPx())
        }

        TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                gravity = Gravity.START
            }
            text = "$habitName: $description"
            textSize = 16f
            setPadding(0, 0, 16.dpToPx(), 0)
            rowContainer.addView(this)
        }

        val imageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                24.dpToPx(),
                24.dpToPx()
            ).apply {
                marginEnd = 8.dpToPx()
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            visibility = if (isImageVisible && imageUri != null) View.VISIBLE else View.GONE

            imageUri?.let { uri ->
                setImageURI(uri)
            } ?: run {
                setImageResource(R.drawable.ic_default_image)
            }

            setOnClickListener {
                toggleHabitVisibility(habitName, this)
            }
        }
        rowContainer.addView(imageView)

        val toggleBtn = TextView(this).apply {
            text = if (imageView.visibility == View.VISIBLE) "×" else ""
            textSize = 16f
            setOnClickListener {
                toggleHabitVisibility(habitName, imageView)
                text = if (imageView.visibility == View.VISIBLE) "×" else ""
            }
        }
        rowContainer.addView(toggleBtn)

        rowContainer.setOnClickListener {
            val newVisibility = imageView.visibility != View.VISIBLE
            imageView.visibility = if (newVisibility) View.VISIBLE else View.GONE
            toggleBtn.text = if (newVisibility) "×" else ""
            habitVisibilityMap[habitName] = newVisibility
            saveHabitVisibilityStates()
            recordHabitClick(habitName)
        }

        tableContainer.addView(rowContainer)

        ConstraintSet().apply {
            clone(tableContainer)
            connect(
                rowId, ConstraintSet.TOP,
                if (rowCount == 0) tableContainer.id else tableContainer.getChildAt(rowCount - 1).id,
                if (rowCount == 0) ConstraintSet.TOP else ConstraintSet.BOTTOM
            )
            connect(rowId, ConstraintSet.START, tableContainer.id, ConstraintSet.START)
            connect(rowId, ConstraintSet.END, tableContainer.id, ConstraintSet.END)
            applyTo(tableContainer)
        }

        rowCount++
    }

    private fun toggleHabitVisibility(habitName: String, imageView: ImageView) {
        val newVisibility = imageView.visibility != View.VISIBLE
        imageView.visibility = if (newVisibility) View.VISIBLE else View.GONE
        habitVisibilityMap[habitName] = newVisibility
        saveHabitVisibilityStates()
        Log.d("MainActivity", "Toggled visibility for $habitName to $newVisibility")
    }

    private fun loadHabitVisibilityStates() {
        val prefs = getSharedPreferences(SharedPrefsConstants.PREFS_NAME, MODE_PRIVATE)
        try {
            val jsonString = prefs.getString(SharedPrefsConstants.KEY_HABIT_VISIBILITY, "{}") ?: "{}"
            val jsonObject = JSONObject(jsonString)

            val currentHabits = mutableSetOf<String>()
            val habitCount = prefs.getInt(SharedPrefsConstants.KEY_HABIT_COUNT, 0)
            for (i in 0 until habitCount) {
                prefs.getString("${SharedPrefsConstants.KEY_HABIT_PREFIX}${i}_name", "")?.let {
                    if (it.isNotEmpty()) currentHabits.add(it)
                }
            }

            habitVisibilityMap.clear()
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (currentHabits.contains(key)) {
                    habitVisibilityMap[key] = jsonObject.getBoolean(key)
                }
            }
            Log.d("MainActivity", "Loaded ${habitVisibilityMap.size} visibility states from prefs")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error loading visibility states", e)
        }
    }

    private fun saveHabitVisibilityStates() {
        val prefs = getSharedPreferences(SharedPrefsConstants.PREFS_NAME, MODE_PRIVATE)
        try {
            val jsonObject = JSONObject().apply {
                habitVisibilityMap.forEach { (key, value) ->
                    put(key, value)
                }
            }
            prefs.edit()
                .putString(SharedPrefsConstants.KEY_HABIT_VISIBILITY, jsonObject.toString())
                .apply()
            Log.d("MainActivity", "Saved ${habitVisibilityMap.size} visibility states to prefs")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error saving visibility states", e)
        }
    }

    private fun recordHabitClick(habitName: String) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val prefs = getSharedPreferences(SharedPrefsConstants.PREFS_NAME, MODE_PRIVATE)

        val clicksJson = prefs.getString(SharedPrefsConstants.KEY_HABIT_CLICKS, "{}") ?: "{}"
        val habitClicks = try {
            JSONObject(clicksJson)
        } catch (e: Exception) {
            JSONObject()
        }

        val dateArray = habitClicks.optJSONArray(today) ?: JSONArray()
        if (!dateArray.toString().contains(habitName)) {
            dateArray.put(habitName)
            habitClicks.put(today, dateArray)

            prefs.edit()
                .putString(SharedPrefsConstants.KEY_HABIT_CLICKS, habitClicks.toString())
                .apply()
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}