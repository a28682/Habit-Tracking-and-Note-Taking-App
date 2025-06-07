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
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.SharedPreferences
import android.util.Log
import android.widget.Switch
import java.lang.ref.WeakReference

class thirdActivity : AppCompatActivity() {

    private lateinit var tableContainer: ConstraintLayout
    private lateinit var searchView: SearchView
    private var rowCount = 0
    private val selectedRows = mutableListOf<Int>()
    private val habitEntries = mutableListOf<HabitEntry>()
    private val allHabitEntries = mutableListOf<HabitEntry>()

    companion object {
        const val ADD_HABIT_REQUEST = 1001
        private const val TAG = "thirdActivity"
    }

    private data class HabitEntry(
        val id: Int,
        val name: String,
        val description: String,
        val imageUri: Uri?,
        val steps: List<String>,
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

        initViews()
        loadSavedHabits()
    }

    private fun initViews() {
        tableContainer = findViewById(R.id.table_container)
        searchView = findViewById(R.id.search_view)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                filterHabits(newText.orEmpty())
                return true
            }
        })

        findViewById<Button>(R.id.btn1).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        findViewById<Button>(R.id.btn2).setOnClickListener {
            startActivity(Intent(this, calendarActivity::class.java))
        }
        findViewById<Button>(R.id.btn4).setOnClickListener {
            startActivity(Intent(this, AnalyticsActivity::class.java))
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
    }

    private fun filterHabits(query: String) {
        if (query.isEmpty()) {
            habitEntries.clear()
            habitEntries.addAll(allHabitEntries)
            refreshTable()
        } else {
            val filteredList = allHabitEntries.filter { entry ->
                entry.name.contains(query, ignoreCase = true) ||
                        entry.description.contains(query, ignoreCase = true)
            }
            habitEntries.clear()
            habitEntries.addAll(filteredList)
            refreshTable()
        }
    }

    private fun refreshTable() {
        tableContainer.removeAllViews()
        rowCount = 0

        habitEntries.forEach { entry ->
            addHabitRowToTable(entry)
        }
    }

    private fun addHabitRowToTable(entry: HabitEntry) {
        LinearLayout(this).apply {
            id = entry.id
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 8.dpToPx(), 0, 8.dpToPx()) }
            background = ContextCompat.getDrawable(context, R.drawable.row_background_unselected)
            setOnClickListener { toggleRowSelection(entry.id) }

            TextView(this@thirdActivity).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    gravity = Gravity.START
                }
                val stepsText = if (entry.steps.isNotEmpty()) {
                    "\n步骤:\n${entry.steps.joinToString("\n• ", "• ")}"
                } else ""
                text = "${entry.name}: ${entry.description}$stepsText"
                textSize = 16f
                setPadding(16.dpToPx(), 0, 16.dpToPx(), 0)
                addView(this)
            }

            ImageView(this@thirdActivity).apply {
                layoutParams = LinearLayout.LayoutParams(48.dpToPx(), 48.dpToPx())
                scaleType = ImageView.ScaleType.CENTER_CROP
                if (entry.imageUri != null && isUriValid(entry.imageUri)) {
                    setImageURI(entry.imageUri)
                } else {
                    setImageResource(R.drawable.ic_default_image)
                }
                addView(this)
            }

            Switch(this@thirdActivity).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(16.dpToPx(), 0, 16.dpToPx(), 0) }
                setSwitchTextAppearance(this@thirdActivity, R.style.SwitchStyle)
                isChecked = entry.isEnabled
                entry.switchRef = WeakReference(this)

                setOnCheckedChangeListener { _, isChecked ->
                    entry.isEnabled = isChecked
                    saveHabitsToPrefs()
                    Toast.makeText(
                        this@thirdActivity,
                        "${entry.name} ${if (isChecked) "enable" else "disable"}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                addView(this)
            }

            tableContainer.addView(this)
        }

        ConstraintSet().apply {
            clone(tableContainer)
            connect(
                entry.id, ConstraintSet.TOP,
                if (rowCount == 0) tableContainer.id else tableContainer.getChildAt(rowCount - 1).id,
                if (rowCount == 0) ConstraintSet.TOP else ConstraintSet.BOTTOM
            )
            connect(entry.id, ConstraintSet.START, tableContainer.id, ConstraintSet.START)
            connect(entry.id, ConstraintSet.END, tableContainer.id, ConstraintSet.END)
            applyTo(tableContainer)
        }

        rowCount++
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
                    putStringArrayListExtra(habitsActivity.EXTRA_HABIT_STEPS, ArrayList(entry.steps))
                    startActivityForResult(this, ADD_HABIT_REQUEST)
                }
            }
        }
    }

    private fun loadSavedHabits() {
        val prefs = getSharedPreferences(SharedPrefsConstants.PREFS_NAME, MODE_PRIVATE)
        val habitCount = prefs.getInt(SharedPrefsConstants.KEY_HABIT_COUNT, 0)

        tableContainer.removeAllViews()
        habitEntries.clear()
        allHabitEntries.clear()
        rowCount = 0

        (0 until habitCount).forEach { i ->
            try {
                val name = prefs.getString("${SharedPrefsConstants.KEY_HABIT_PREFIX}${i}_name", "") ?: ""
                val desc = prefs.getString("${SharedPrefsConstants.KEY_HABIT_PREFIX}${i}_desc", "") ?: ""
                val isEnabled = prefs.getBoolean("${SharedPrefsConstants.KEY_HABIT_PREFIX}${i}_enabled", true)
                val uriString = prefs.getString("${SharedPrefsConstants.KEY_HABIT_PREFIX}${i}_uri", null)
                val steps = prefs.getStringSet("${SharedPrefsConstants.KEY_HABIT_PREFIX}${i}_steps", emptySet())?.toList() ?: emptyList()
                val uri = uriString?.let { Uri.parse(it) }

                if (name.isNotEmpty()) {
                    val entry = HabitEntry(View.generateViewId(), name, desc, uri, steps, isEnabled)
                    allHabitEntries.add(entry)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading habit $i", e)
            }
        }

        habitEntries.addAll(allHabitEntries)
        refreshTable()
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
                val steps = it.getStringArrayListExtra(habitsActivity.EXTRA_HABIT_STEPS) ?: emptyList()

                if (isEdit && originalId != -1) {
                    updateHabitEntry(originalId, habitName, description, imageUri, steps)
                } else {
                    val newEntry = HabitEntry(View.generateViewId(), habitName, description, imageUri, steps)
                    allHabitEntries.add(newEntry)
                    habitEntries.add(newEntry)
                    addHabitRowToTable(newEntry)
                }
                saveHabitsToPrefs()
                selectedRows.clear()
            }
        }
    }

    private fun updateHabitEntry(originalId: Int, name: String, desc: String, uri: Uri?, steps: List<String>) {
        allHabitEntries.indexOfFirst { it.id == originalId }.takeIf { it != -1 }?.let { index ->
            allHabitEntries[index] = HabitEntry(originalId, name, desc, uri, steps, allHabitEntries[index].isEnabled)
        }

        habitEntries.indexOfFirst { it.id == originalId }.takeIf { it != -1 }?.let { index ->
            habitEntries[index] = HabitEntry(originalId, name, desc, uri, steps, habitEntries[index].isEnabled)
            updateHabitRow(originalId, name, desc, uri, steps)
        }
    }

    private fun updateHabitRow(rowId: Int, habitName: String, description: String, imageUri: Uri?, steps: List<String>) {
        val row = tableContainer.findViewById<LinearLayout>(rowId)
        row?.let {
            (it.getChildAt(0) as? TextView)?.let { textView ->
                val stepsText = if (steps.isNotEmpty()) {
                    "\n步骤:\n${steps.joinToString("\n• ", "• ")}"
                } else ""
                textView.text = "$habitName: $description$stepsText"
            }

            (it.getChildAt(1) as? ImageView)?.let { imageView ->
                if (imageUri != null && isUriValid(imageUri)) {
                    imageView.setImageURI(imageUri)
                } else {
                    imageView.setImageResource(R.drawable.ic_default_image)
                }
            }

            habitEntries.firstOrNull { it.id == rowId }?.let { entry ->
                (it.getChildAt(2) as? Switch)?.let { switch ->
                    switch.isChecked = entry.isEnabled
                    entry.switchRef = WeakReference(switch)
                }
            }
        }
    }

    private fun isUriValid(uri: Uri): Boolean = try {
        contentResolver.openInputStream(uri)?.close()
        true
    } catch (e: Exception) {
        Log.e(TAG, "Invalid URI: $uri", e)
        false
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
            allHabitEntries.removeAll { it.id == rowId }
            habitEntries.removeAll { it.id == rowId }
            tableContainer.removeView(tableContainer.findViewById(rowId))
            rowCount--
        }

        resetConstraints()
        saveHabitsToPrefs()
        Toast.makeText(this, "delete ${selectedRows.size} habit", Toast.LENGTH_SHORT).show()
        selectedRows.clear()
    }

    private fun resetConstraints() {
        ConstraintSet().apply {
            clone(tableContainer)
            (0 until tableContainer.childCount).forEach { i ->
                connect(
                    tableContainer.getChildAt(i).id, ConstraintSet.TOP,
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
        val prefs = getSharedPreferences(SharedPrefsConstants.PREFS_NAME, MODE_PRIVATE)
        val editor = prefs.edit()

        val visibilityState = prefs.getString(SharedPrefsConstants.KEY_HABIT_VISIBILITY, "{}") ?: "{}"
        val clicksData = prefs.getString(SharedPrefsConstants.KEY_HABIT_CLICKS, "{}") ?: "{}"

        val oldHabitCount = prefs.getInt(SharedPrefsConstants.KEY_HABIT_COUNT, 0)
        (0 until oldHabitCount).forEach { i ->
            editor.remove("${SharedPrefsConstants.KEY_HABIT_PREFIX}${i}_name")
            editor.remove("${SharedPrefsConstants.KEY_HABIT_PREFIX}${i}_desc")
            editor.remove("${SharedPrefsConstants.KEY_HABIT_PREFIX}${i}_enabled")
            editor.remove("${SharedPrefsConstants.KEY_HABIT_PREFIX}${i}_uri")
            editor.remove("${SharedPrefsConstants.KEY_HABIT_PREFIX}${i}_steps")
        }

        editor.putInt(SharedPrefsConstants.KEY_HABIT_COUNT, allHabitEntries.size)
        allHabitEntries.forEachIndexed { index, entry ->
            with(entry) {
                editor.putString("${SharedPrefsConstants.KEY_HABIT_PREFIX}${index}_name", name)
                editor.putString("${SharedPrefsConstants.KEY_HABIT_PREFIX}${index}_desc", description)
                editor.putBoolean("${SharedPrefsConstants.KEY_HABIT_PREFIX}${index}_enabled", isEnabled)
                editor.putStringSet("${SharedPrefsConstants.KEY_HABIT_PREFIX}${index}_steps", steps.toSet())
                imageUri?.takeIf { uri -> isUriValid(uri) }?.let {
                    editor.putString("${SharedPrefsConstants.KEY_HABIT_PREFIX}${index}_uri", it.toString())
                }
            }
        }

        editor.putString(SharedPrefsConstants.KEY_HABIT_VISIBILITY, visibilityState)
        editor.putString(SharedPrefsConstants.KEY_HABIT_CLICKS, clicksData)

        editor.apply()
        MainActivity.notifyHabitsUpdated(this)
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}