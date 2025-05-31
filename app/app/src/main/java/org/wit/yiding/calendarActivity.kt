package org.wit.yiding

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CalendarView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

class calendarActivity : AppCompatActivity() {

    private lateinit var txtHabits: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.calendaractivity)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        txtHabits = findViewById(R.id.txtHabits)
        val calendarView = findViewById<CalendarView>(R.id.calendarView)

        findViewById<Button>(R.id.btn1).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        findViewById<Button>(R.id.btn3).setOnClickListener {
            startActivity(Intent(this, thirdActivity::class.java))
        }
        findViewById<Button>(R.id.btn4).setOnClickListener {
            startActivity(Intent(this, AnalyticsActivity::class.java))
        }
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }.time

            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate)
            showHabitsForDate(dateStr)
        }
    }



    private fun showHabitsForDate(date: String) {
        val prefs = getSharedPreferences(SharedPrefsConstants.PREFS_NAME, MODE_PRIVATE)

        // 获取习惯数据
        val habitClicksJson = prefs.getString(SharedPrefsConstants.KEY_HABIT_CLICKS, "{}") ?: "{}"
        val completedHabitsJson = prefs.getString(SharedPrefsConstants.KEY_COMPLETED_HABITS, "{}") ?: "{}"

        // 获取note数据
        val noteClicksJson = prefs.getString(SharedPrefsConstants.KEY_NOTE_CLICKS, "{}") ?: "{}"
        val completedNotesJson = prefs.getString(SharedPrefsConstants.KEY_COMPLETED_NOTES, "{}") ?: "{}"

        val result = StringBuilder()
        result.append("$date 的活动记录:\n\n")

        try {
            // 处理习惯
            val habitClicks = JSONObject(habitClicksJson)
            val completedHabits = JSONObject(completedHabitsJson)

            val habitsArray = habitClicks.optJSONArray(date) ?: JSONArray()
            val completedHabitsArray = completedHabits.optJSONArray(date) ?: JSONArray()

            val completedHabitSet = mutableSetOf<String>()
            for (i in 0 until completedHabitsArray.length()) {
                completedHabitSet.add(completedHabitsArray.getString(i))
            }

            if (habitsArray.length() > 0) {
                result.append("习惯:\n")
                for (i in 0 until habitsArray.length()) {
                    val habitName = habitsArray.getString(i)
                    result.append(if (completedHabitSet.contains(habitName)) "  ✓ $habitName\n" else "  ○ $habitName\n")
                }
                result.append("\n")
            }

            // 处理笔记
            val noteClicks = JSONObject(noteClicksJson)
            val completedNotes = JSONObject(completedNotesJson)

            val notesArray = noteClicks.optJSONArray(date) ?: JSONArray()
            val completedNotesArray = completedNotes.optJSONArray(date) ?: JSONArray()

            val completedNoteSet = mutableSetOf<String>()
            for (i in 0 until completedNotesArray.length()) {
                completedNoteSet.add(completedNotesArray.getString(i))
            }

            // 笔记查询
            if (notesArray.length() > 0) {
                result.append("笔记:\n")
                for (i in 0 until notesArray.length()) {
                    val noteId = notesArray.getString(i)
                    val noteContent = prefs.getString("${SharedPrefsConstants.KEY_NOTE_PREFIX}${noteId}_content", "未知笔记") ?: "未知笔记"
                    val isCompleted = prefs.getBoolean("${SharedPrefsConstants.KEY_NOTE_PREFIX}${noteId}_completed", false)
                    result.append(if (isCompleted) "  ✓ $noteContent\n" else "  ○ $noteContent\n")
                }
            }

            if (habitsArray.length() == 0 && notesArray.length() == 0) {
                result.append("当天没有活动记录")
            }

            txtHabits.text = result.toString()
        } catch (e: Exception) {
            Log.e("calendarActivity", "Error parsing data", e)
            txtHabits.text = "加载数据出错"
        }
    }
}