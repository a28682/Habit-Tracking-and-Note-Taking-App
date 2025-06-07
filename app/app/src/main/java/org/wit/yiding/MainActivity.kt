package org.wit.yiding

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
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

    private lateinit var notesContainer: LinearLayout
    private lateinit var habitsContainer: LinearLayout
    private lateinit var btnAdd: Button
    private lateinit var btnEdit: Button
    private var isEditing = false
    private val habitVisibilityMap = mutableMapOf<String, Boolean>()

    companion object {
        const val ACTION_HABIT_UPDATED = "org.wit.yiding.ACTION_HABIT_UPDATED"
        const val KEY_HABIT_VISIBILITY = "habit_visibility"

        // 通知习惯更新
        fun notifyHabitsUpdated(context: Context) {
            LocalBroadcastManager.getInstance(context).sendBroadcast(
                Intent(ACTION_HABIT_UPDATED)
            )
        }
    }

    private val habitChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_HABIT_UPDATED) {
                loadAndDisplayHabits()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupButtonListeners()
        loadHabitVisibilityStates()
        loadAndDisplayNotes()
        loadAndDisplayHabits()
    }

    // 初始化视图组件
    private fun initViews() {
        notesContainer = findViewById(R.id.notes_container)
        habitsContainer = findViewById(R.id.habits_list)
        btnAdd = findViewById(R.id.btn_add)
        btnEdit = findViewById(R.id.btn_edit)
    }

    // 设置按钮点击事件监听器
    private fun setupButtonListeners() {
        btnAdd.setOnClickListener {
            showAddNoteDialog()
        }

        btnEdit.setOnClickListener {
            toggleEditMode()
        }

        findViewById<Button>(R.id.btn1).setOnClickListener {
        }

        findViewById<Button>(R.id.btn2).setOnClickListener {
            startActivity(Intent(this, calendarActivity::class.java))
        }

        findViewById<Button>(R.id.btn3).setOnClickListener {
            startActivity(Intent(this, thirdActivity::class.java))
        }
        findViewById<Button>(R.id.btn4).setOnClickListener {
            startActivity(Intent(this, AnalyticsActivity::class.java))
        }
    }

    private fun toggleEditMode() {
        isEditing = !isEditing
        btnEdit.text = if (isEditing) "Save" else "Edit"
    }

    // 显示添加笔记对话框
    private fun showAddNoteDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_note, null)
        val editTextNote = dialogView.findViewById<EditText>(R.id.editTextNote)

        AlertDialog.Builder(this)
            .setTitle("Add New Note")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val noteContent = editTextNote.text.toString().trim()
                if (noteContent.isNotEmpty()) {
                    saveNewNote(noteContent)
                    loadAndDisplayNotes()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // 保存新笔记到SharedPreferences
    private fun saveNewNote(content: String) {
        val prefs = getSharedPreferences(SharedPrefsConstants.PREFS_NAME, MODE_PRIVATE)
        val noteCount = prefs.getInt(SharedPrefsConstants.KEY_NOTE_COUNT, 0)

        prefs.edit().apply {
            putString("${SharedPrefsConstants.KEY_NOTE_PREFIX}${noteCount}_content", content)
            putBoolean("${SharedPrefsConstants.KEY_NOTE_PREFIX}${noteCount}_completed", false)
            putInt(SharedPrefsConstants.KEY_NOTE_COUNT, noteCount + 1)
            apply()
        }
    }

    // 从SharedPreferences加载并显示所有笔记
    private fun loadAndDisplayNotes() {
        notesContainer.removeAllViews()

        val prefs = getSharedPreferences(SharedPrefsConstants.PREFS_NAME, MODE_PRIVATE)
        val noteCount = prefs.getInt(SharedPrefsConstants.KEY_NOTE_COUNT, 0)

        for (i in 0 until noteCount) {
            val content = prefs.getString("${SharedPrefsConstants.KEY_NOTE_PREFIX}${i}_content", "") ?: ""
            if (content.isNotEmpty()) {
                addNoteView(content, i)
            }
        }
    }

    // 添加单个笔记视图到容器
    private fun addNoteView(content: String, noteId: Int) {
        val isCompleted = isNoteCompleted(noteId)

        val noteView = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 8.dpToPx())
            }
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.note_item_bg)
            setPadding(16.dpToPx(), 12.dpToPx(), 16.dpToPx(), 12.dpToPx())
        }

        TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            text = content
            textSize = 16f
            if (isCompleted) {
                paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                setTextColor(Color.GRAY)
            }
            noteView.addView(this)
        }

        TextView(this).apply {
            text = if (isCompleted) "✓" else "○"
            textSize = 18f
            setTextColor(if (isCompleted) Color.GREEN else Color.LTGRAY)
            noteView.addView(this)
        }

        noteView.setOnClickListener {
            val completed = toggleNoteCompletion(noteId)
            loadAndDisplayNotes()
        }

        notesContainer.addView(noteView)
    }

    // 检查笔记是否已完成
    private fun isNoteCompleted(noteId: Int): Boolean {
        val prefs = getSharedPreferences(SharedPrefsConstants.PREFS_NAME, MODE_PRIVATE)
        return prefs.getBoolean("${SharedPrefsConstants.KEY_NOTE_PREFIX}${noteId}_completed", false)
    }

    // 切换笔记完成状态并记录点击日期
    private fun toggleNoteCompletion(noteId: Int): Boolean {
        val isCompleted = !isNoteCompleted(noteId)
        val prefs = getSharedPreferences(SharedPrefsConstants.PREFS_NAME, MODE_PRIVATE)

        prefs.edit().putBoolean("${SharedPrefsConstants.KEY_NOTE_PREFIX}${noteId}_completed", isCompleted).apply()

        if (isCompleted) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val clicksJson = prefs.getString(SharedPrefsConstants.KEY_NOTE_CLICKS, "{}") ?: "{}"
            val noteClicks = try {
                JSONObject(clicksJson)
            } catch (e: Exception) {
                JSONObject()
            }

            val dateArray = noteClicks.optJSONArray(today) ?: JSONArray()
            if (!dateArray.toString().contains(noteId.toString())) {
                dateArray.put(noteId.toString())
                noteClicks.put(today, dateArray)
                prefs.edit().putString(SharedPrefsConstants.KEY_NOTE_CLICKS, noteClicks.toString()).apply()
            }
        }

        return isCompleted
    }

    // 加载并显示所有习惯
    private fun loadAndDisplayHabits() {
        habitsContainer.removeAllViews()

        val prefs = getSharedPreferences(SharedPrefsConstants.PREFS_NAME, MODE_PRIVATE)
        val habitCount = prefs.getInt(SharedPrefsConstants.KEY_HABIT_COUNT, 0)

        for (i in 0 until habitCount) {
            val name = prefs.getString("${SharedPrefsConstants.KEY_HABIT_PREFIX}${i}_name", "") ?: ""
            val isEnabled = prefs.getBoolean("${SharedPrefsConstants.KEY_HABIT_PREFIX}${i}_enabled", false)

            if (isEnabled && name.isNotEmpty()) {
                addHabitView(name)
            }
        }
    }

    // 添加单个习惯视图到容器
    private fun addHabitView(name: String) {
        val prefs = getSharedPreferences(SharedPrefsConstants.PREFS_NAME, MODE_PRIVATE)
        val habitCount = prefs.getInt(SharedPrefsConstants.KEY_HABIT_COUNT, 0)
        var imageUri: Uri? = null

        for (i in 0 until habitCount) {
            if (prefs.getString("${SharedPrefsConstants.KEY_HABIT_PREFIX}${i}_name", "") == name) {
                prefs.getString("${SharedPrefsConstants.KEY_HABIT_PREFIX}${i}_uri", null)?.let { uriString ->
                    imageUri = Uri.parse(uriString)
                }
                break
            }
        }

        val isVisible = habitVisibilityMap.getOrDefault(name, false)

        val habitView = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 8.dpToPx())
            }
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.habit_item_bg)
            setPadding(16.dpToPx(), 12.dpToPx(), 16.dpToPx(), 12.dpToPx())
        }

        TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            text = name
            textSize = 16f
            habitView.addView(this)
        }

        ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                24.dpToPx(),
                24.dpToPx()
            ).apply {
                marginEnd = 8.dpToPx()
            }
            scaleType = ImageView.ScaleType.CENTER_CROP

            if (isVisible && imageUri != null) {
                try {
                    setImageURI(imageUri)
                    visibility = View.VISIBLE
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to load image for $name", e)
                    setImageResource(R.drawable.ic_default_image)
                    visibility = View.GONE
                }
            } else {
                setImageResource(R.drawable.ic_default_image)
                visibility = if (isVisible) View.VISIBLE else View.GONE
            }
            habitView.addView(this)
        }

        habitView.setOnClickListener {
            val newVisibility = !isVisible
            habitVisibilityMap[name] = newVisibility
            saveHabitVisibilityStates()
            loadAndDisplayHabits()
            recordHabitClick(name)
        }

        habitsContainer.addView(habitView)
    }

    // 记录习惯点击并计算连续天数
    private fun recordHabitClick(habitName: String) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val prefs = getSharedPreferences(SharedPrefsConstants.PREFS_NAME, MODE_PRIVATE)

        val clicksJson = prefs.getString(SharedPrefsConstants.KEY_HABIT_CLICKS, "{}") ?: "{}"
        val habitClicks = try {
            JSONObject(clicksJson)
        } catch (e: Exception) {
            JSONObject()
        }

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        val dateArray = habitClicks.optJSONArray(today) ?: JSONArray()
        val yesterdayArray = habitClicks.optJSONArray(yesterday) ?: JSONArray()

        val streakKey = "${SharedPrefsConstants.KEY_HABIT_STREAK_PREFIX}$habitName"
        val currentStreak = prefs.getInt(streakKey, 0)

        if (yesterdayArray.toString().contains(habitName)) {
            prefs.edit().putInt(streakKey, currentStreak + 1).apply()
        } else if (!dateArray.toString().contains(habitName)) {
            prefs.edit().putInt(streakKey, 1).apply()
        }

        if (!dateArray.toString().contains(habitName)) {
            dateArray.put(habitName)
            habitClicks.put(today, dateArray)
            prefs.edit()
                .putString(SharedPrefsConstants.KEY_HABIT_CLICKS, habitClicks.toString())
                .apply()
        }
    }

    // 从SharedPreferences加载习惯可见性状态
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
        } catch (e: Exception) {
            Log.e("MainActivity", "Error loading visibility states", e)
        }
    }

    // 保存习惯可见性状态到SharedPreferences
    private fun saveHabitVisibilityStates() {
        val prefs = getSharedPreferences(SharedPrefsConstants.PREFS_NAME, MODE_PRIVATE)
        try {
            prefs.edit().apply {
                putString(SharedPrefsConstants.KEY_HABIT_VISIBILITY,
                    JSONObject().apply {
                        habitVisibilityMap.forEach { (k, v) -> put(k, v) }
                    }.toString()
                )
                commit()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Save failed", e)
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

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
    }
}