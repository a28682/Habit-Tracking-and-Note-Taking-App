package org.wit.yiding

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.SharedPreferences
import android.util.Log
import android.widget.Switch
import java.lang.ref.WeakReference

object SharedPrefsConstants {
    const val PREFS_NAME = "HabitTrackerPrefs"
    const val KEY_HABIT_COUNT = "habit_count"
    const val KEY_HABIT_PREFIX = "habit_"
}

class thirdActivity : AppCompatActivity() {

    private lateinit var tableContainer: ConstraintLayout
    private var rowCount = 0
    private val selectedRows = mutableListOf<Int>()
    private val habitEntries = mutableListOf<HabitEntry>()

    companion object {
        const val ADD_HABIT_REQUEST = 1001
    }

    private data class HabitEntry(
        val id: Int,
        val name: String,
        val description: String,
        val imageUri: Uri?,
        var isEnabled: Boolean = true,
        var switchRef: WeakReference<Switch>? = null
    ) {
        override fun equals(other: Any?): Boolean = (other as? HabitEntry)?.id == id
        override fun hashCode(): Int = id
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.thirdactivity)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tableContainer = findViewById(R.id.table_container)

        findViewById<Button>(R.id.btn1).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        findViewById<Button>(R.id.btn2).setOnClickListener {
            startActivity(Intent(this, calendarActivity::class.java))
        }

        findViewById<Button>(R.id.btn_add_habits).setOnClickListener {
            startActivityForResult(Intent(this, habitsActivity::class.java), ADD_HABIT_REQUEST)
        }

        findViewById<Button>(R.id.btn_delete_habits).setOnClickListener {
            deleteSelectedHabits()
        }

        findViewById<Button>(R.id.btn_edit_habits).setOnClickListener {
            editHabits()
        }

        loadSavedHabits()
    }

    override fun onResume() {
        super.onResume()
        refreshAllSwitchStates()
    }

    private fun refreshAllSwitchStates() {
        habitEntries.forEach { entry ->
            entry.switchRef?.get()?.isChecked = entry.isEnabled
        }
    }

    private fun editHabits() {
        when {
            selectedRows.isEmpty() -> Toast.makeText(this, "请先选择要编辑的习惯", Toast.LENGTH_SHORT).show()
            selectedRows.size > 1 -> Toast.makeText(this, "一次只能编辑一个习惯", Toast.LENGTH_SHORT).show()
            else -> habitEntries.firstOrNull { it.id == selectedRows.first() }?.let { entry ->
                Intent(this, habitsActivity::class.java).apply {
                    putExtra(habitsActivity.EXTRA_IS_EDIT_MODE, true)
                    putExtra(habitsActivity.EXTRA_ORIGINAL_HABIT_ID, entry.id)
                    putExtra(habitsActivity.EXTRA_HABIT_NAME, entry.name)
                    putExtra(habitsActivity.EXTRA_DESCRIPTION, entry.description)
                    entry.imageUri?.let { putExtra(habitsActivity.EXTRA_IMAGE_URI, it.toString()) }
                    startActivityForResult(this, ADD_HABIT_REQUEST)
                }
            }
        }
    }

    private fun loadSavedHabits() {
        val prefs = getSharedPreferences(SharedPrefsConstants.PREFS_NAME, MODE_PRIVATE)
        val habitCount = prefs.getInt(SharedPrefsConstants.KEY_HABIT_COUNT, 0)

        (0 until habitCount).forEach { i ->
            try {
                val name = prefs.getString("${SharedPrefsConstants.KEY_HABIT_PREFIX}${i}_name", "") ?: ""
                val desc = prefs.getString("${SharedPrefsConstants.KEY_HABIT_PREFIX}${i}_desc", "") ?: ""
                val isEnabled = prefs.getBoolean("${SharedPrefsConstants.KEY_HABIT_PREFIX}${i}_enabled", true)
                val uriString = prefs.getString("${SharedPrefsConstants.KEY_HABIT_PREFIX}${i}_uri", null)
                val uri = uriString?.takeIf { isUriValid(Uri.parse(it)) }?.let { Uri.parse(it) }

                if (name.isNotEmpty()) addHabitRow(name, desc, uri, isEnabled)
            } catch (e: Exception) {
                Log.e("HabitTracker", "Error loading habit $i", e)
            }
        }
    }

    private fun isUriValid(uri: Uri): Boolean = try {
        contentResolver.openInputStream(uri)?.close()
        true
    } catch (e: Exception) {
        false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_HABIT_REQUEST && resultCode == RESULT_OK) {
            data?.let {
                val isEdit = it.getBooleanExtra(habitsActivity.EXTRA_IS_EDIT_MODE, false)
                val originalId = it.getIntExtra(habitsActivity.EXTRA_ORIGINAL_HABIT_ID, -1)
                val habitName = it.getStringExtra(habitsActivity.EXTRA_HABIT_NAME) ?: ""
                val description = it.getStringExtra(habitsActivity.EXTRA_DESCRIPTION) ?: ""
                val imageUri = it.getStringExtra(habitsActivity.EXTRA_IMAGE_URI)?.let { uri -> Uri.parse(uri) }

                if (isEdit && originalId != -1) {
                    habitEntries.indexOfFirst { it.id == originalId }.takeIf { it != -1 }?.let { index ->
                        habitEntries[index] = HabitEntry(originalId, habitName, description, imageUri)
                        updateHabitRow(originalId, habitName, description, imageUri)
                    }
                } else {
                    addHabitRow(habitName, description, imageUri)
                }
                saveHabitsToPrefs()
                selectedRows.clear()
            }
        }
    }

    private fun updateHabitRow(rowId: Int, habitName: String, description: String, imageUri: Uri?) {
        val row = tableContainer.findViewById<LinearLayout>(rowId)
        row?.let {
            // 更新文本
            (it.getChildAt(0) as? TextView)?.text = "$habitName: $description"

            // 更新图片
            (it.getChildAt(1) as? ImageView)?.let { imageView ->
                if (imageUri != null) {
                    imageView.setImageURI(imageUri)
                } else {
                    // 确保R.drawable.ic_default_image资源存在
                    imageView.setImageResource(R.drawable.ic_default_image)
                }
            }

            // 更新开关状态
            habitEntries.firstOrNull { it.id == rowId }?.let { entry ->
                (it.getChildAt(2) as? Switch)?.let { switch ->
                    switch.isChecked = entry.isEnabled
                    entry.switchRef = WeakReference(switch)
                }
            }
        }
    }

    private fun addHabitRow(habitName: String, description: String, imageUri: Uri?, isEnabled: Boolean = true) {
        val rowId = View.generateViewId()
        val entry = HabitEntry(rowId, habitName, description, imageUri, isEnabled)
        habitEntries.add(entry)

        LinearLayout(this).apply {
            id = rowId
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 8.dpToPx(), 0, 8.dpToPx()) }
            background = ContextCompat.getDrawable(context, R.drawable.row_background_unselected)
            setOnClickListener { toggleRowSelection(rowId) }

            // Text View
            TextView(this@thirdActivity).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    gravity = Gravity.START
                }
                text = "$habitName: $description"
                textSize = 16f
                setPadding(16.dpToPx(), 0, 16.dpToPx(), 0)
                addView(this)
            }

            // Image View
            ImageView(this@thirdActivity).apply {
                layoutParams = LinearLayout.LayoutParams(48.dpToPx(), 48.dpToPx())
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageURI(imageUri ?: run {
                    setImageResource(R.drawable.ic_default_image)
                    null
                })
                addView(this)
            }

            // Switch
            Switch(this@thirdActivity).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(16.dpToPx(), 0, 16.dpToPx(), 0) }
                setSwitchTextAppearance(this@thirdActivity, R.style.SwitchStyle)
                isChecked = isEnabled
                entry.switchRef = WeakReference(this)

                setOnCheckedChangeListener { _, isChecked ->
                    entry.isEnabled = isChecked
                    saveHabitsToPrefs()
                    Toast.makeText(
                        this@thirdActivity,
                        "${entry.name} ${if (isChecked) "已启用" else "已禁用"}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                addView(this)
            }

            tableContainer.addView(this)
        }

        ConstraintSet().apply {
            clone(tableContainer)
            connect(rowId, ConstraintSet.TOP,
                if (rowCount == 0) tableContainer.id else tableContainer.getChildAt(rowCount - 1).id,
                if (rowCount == 0) ConstraintSet.TOP else ConstraintSet.BOTTOM
            )
            connect(rowId, ConstraintSet.START, tableContainer.id, ConstraintSet.START)
            connect(rowId, ConstraintSet.END, tableContainer.id, ConstraintSet.END)
            applyTo(tableContainer)
        }

        rowCount++
    }

    private fun toggleRowSelection(rowId: Int) {
        if (selectedRows.contains(rowId)) {
            selectedRows.remove(rowId)
            tableContainer.findViewById<LinearLayout>(rowId)?.background =
                ContextCompat.getDrawable(this, R.drawable.row_background_unselected)
        } else {
            selectedRows.add(rowId)
            tableContainer.findViewById<LinearLayout>(rowId)?.background =
                ContextCompat.getDrawable(this, R.drawable.row_background_selected)
        }
    }

    private fun deleteSelectedHabits() {
        if (selectedRows.isEmpty()) {
            Toast.makeText(this, "请先选择要删除的习惯", Toast.LENGTH_SHORT).show()
            return
        }

        selectedRows.sortedDescending().forEach { rowId ->
            tableContainer.removeView(tableContainer.findViewById(rowId))
            habitEntries.removeAll { it.id == rowId }
            rowCount--
        }

        resetConstraints()
        saveHabitsToPrefs()
        selectedRows.clear()
        Toast.makeText(this, "已删除${selectedRows.size}个习惯", Toast.LENGTH_SHORT).show()
    }

    private fun resetConstraints() {
        ConstraintSet().apply {
            clone(tableContainer)
            (0 until tableContainer.childCount).forEach { i ->
                connect(tableContainer.getChildAt(i).id, ConstraintSet.TOP,
                    if (i == 0) tableContainer.id else tableContainer.getChildAt(i - 1).id,
                    if (i == 0) ConstraintSet.TOP else ConstraintSet.BOTTOM
                )
                connect(tableContainer.getChildAt(i).id, ConstraintSet.START, tableContainer.id, ConstraintSet.START)
                connect(tableContainer.getChildAt(i).id, ConstraintSet.END, tableContainer.id, ConstraintSet.END)
            }
            applyTo(tableContainer)
        }
    }

    private fun saveHabitsToPrefs() {
        getSharedPreferences(SharedPrefsConstants.PREFS_NAME, MODE_PRIVATE).edit().apply {
            clear()
            putInt(SharedPrefsConstants.KEY_HABIT_COUNT, habitEntries.size)
            habitEntries.forEachIndexed { index, entry ->
                putString("${SharedPrefsConstants.KEY_HABIT_PREFIX}${index}_name", entry.name)
                putString("${SharedPrefsConstants.KEY_HABIT_PREFIX}${index}_desc", entry.description)
                putBoolean("${SharedPrefsConstants.KEY_HABIT_PREFIX}${index}_enabled", entry.isEnabled)
                entry.imageUri?.let { putString("${SharedPrefsConstants.KEY_HABIT_PREFIX}${index}_uri", it.toString()) }
            }
        }.apply()
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}