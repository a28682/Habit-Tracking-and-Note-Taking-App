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
        val clicksJson = prefs.getString(SharedPrefsConstants.KEY_HABIT_CLICKS, "{}") ?: "{}"

        try {
            val habitClicks = JSONObject(clicksJson)
            val habitsArray = habitClicks.optJSONArray(date) ?: JSONArray()

            val habits = mutableListOf<String>()
            for (i in 0 until habitsArray.length()) {
                habits.add(habitsArray.getString(i))
            }

            if (habits.isNotEmpty()) {
                txtHabits.text = "$date 点击的习惯:\n${habits.joinToString("\n")}"
            } else {
                txtHabits.text = "$date 没有习惯点击记录"
            }
        } catch (e: Exception) {
            Log.e("calendarActivity", "Error parsing habit clicks", e)
            txtHabits.text = "加载数据出错"
        }
    }
}