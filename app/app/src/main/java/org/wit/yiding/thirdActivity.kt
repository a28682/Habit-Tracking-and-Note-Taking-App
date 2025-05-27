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
object SharedPrefsConstants {
    const val PREFS_NAME = "HabitTrackerPrefs"  // SharedPreferences 文件名
    const val KEY_HABIT_COUNT = "habit_count"   // 存储习惯数量的键
    const val KEY_HABIT_PREFIX = "habit_"       // 习惯条目前缀
}
class thirdActivity : AppCompatActivity() {

    private lateinit var tableContainer: ConstraintLayout
    private var rowCount = 0
    private val selectedRows = mutableListOf<Int>() // 存储选中行的ID
    private val habitEntries = mutableListOf<HabitEntry>() // 存储习惯条目

    companion object {
        const val ADD_HABIT_REQUEST = 1001
    }

    // 内部数据类，存储习惯信息
    private data class HabitEntry(
        val id: Int,
        val name: String,
        val description: String,
        val imageUri: Uri?
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.thirdactivity)

        // 处理边缘到边缘布局
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 初始化视图
        tableContainer = findViewById(R.id.table_container)

        // 设置按钮点击事件
        findViewById<Button>(R.id.btn1).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        findViewById<Button>(R.id.btn2).setOnClickListener {
            startActivity(Intent(this, calendarActivity::class.java))
        }

        findViewById<Button>(R.id.btn_add_habits).setOnClickListener {
            val intent = Intent(this, habitsActivity::class.java)
            startActivityForResult(intent, ADD_HABIT_REQUEST)
        }

        // 设置Delete按钮点击事件
        findViewById<Button>(R.id.btn_delete_habits).setOnClickListener {
            deleteSelectedHabits()
        }
        findViewById<Button>(R.id.btn_edit_habits).setOnClickListener{
            editHabits()
        }
        // 加载已保存的习惯
        loadSavedHabits()
    }
    private fun editHabits() {
        if (selectedRows.isEmpty()) {
            Toast.makeText(this, "请先选择要编辑的习惯", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedRows.size > 1) {
            Toast.makeText(this, "一次只能编辑一个习惯", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedRowId = selectedRows.first()
        val habitEntry = habitEntries.firstOrNull { it.id == selectedRowId }

        habitEntry?.let { entry ->
            val intent = Intent(this, habitsActivity::class.java).apply {
                putExtra(habitsActivity.EXTRA_IS_EDIT_MODE, true)
                putExtra(habitsActivity.EXTRA_ORIGINAL_HABIT_ID, entry.id)  // 传递原始ID
                putExtra(habitsActivity.EXTRA_HABIT_NAME, entry.name)
                putExtra(habitsActivity.EXTRA_DESCRIPTION, entry.description)
                entry.imageUri?.let { uri ->
                    putExtra(habitsActivity.EXTRA_IMAGE_URI, uri.toString())
                }
            }
            startActivityForResult(intent, ADD_HABIT_REQUEST)
        }
    }
    private fun loadSavedHabits() {
        val prefs = getSharedPreferences(SharedPrefsConstants.PREFS_NAME, MODE_PRIVATE)
        val habitCount = prefs.getInt(SharedPrefsConstants.KEY_HABIT_COUNT, 0)

        for (i in 0 until habitCount) {
            val name = prefs.getString("${SharedPrefsConstants.KEY_HABIT_PREFIX}${i}_name", "") ?: ""
            val desc = prefs.getString("${SharedPrefsConstants.KEY_HABIT_PREFIX}${i}_desc", "") ?: ""
            val uriString = prefs.getString("${SharedPrefsConstants.KEY_HABIT_PREFIX}${i}_uri", null)
            val uri = uriString?.let { Uri.parse(it) }

            if (name.isNotEmpty()) {
                addHabitRow(name, desc, uri)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_HABIT_REQUEST && resultCode == RESULT_OK) {
            val isEdit = data?.getBooleanExtra(habitsActivity.EXTRA_IS_EDIT_MODE, false) ?: false
            val originalId = data?.getIntExtra(habitsActivity.EXTRA_ORIGINAL_HABIT_ID, -1) ?: -1

            val habitName = data?.getStringExtra(habitsActivity.EXTRA_HABIT_NAME) ?: ""
            val description = data?.getStringExtra(habitsActivity.EXTRA_DESCRIPTION) ?: ""
            val imageUri = data?.getStringExtra(habitsActivity.EXTRA_IMAGE_URI)?.let { Uri.parse(it) }

            if (isEdit && originalId != -1) {
                // 替换原有习惯
                val index = habitEntries.indexOfFirst { it.id == originalId }
                if (index != -1) {
                    // 保留原始ID，只更新内容
                    habitEntries[index] = HabitEntry(originalId, habitName, description, imageUri)
                    updateHabitRow(originalId, habitName, description, imageUri)
                }
            } else {
                // 添加新习惯
                addHabitRow(habitName, description, imageUri)
            }
            saveHabitsToPrefs()
            selectedRows.clear()  // 清空选择
        }
    }
    private fun updateHabitRow(rowId: Int, habitName: String, description: String, imageUri: Uri?) {
        val row = tableContainer.findViewById<LinearLayout>(rowId)
        row?.let {
            val textView = it.getChildAt(0) as TextView
            textView.text = "$habitName: $description"

            val imageView = it.getChildAt(1) as ImageView
            imageUri?.let { uri ->
                imageView.setImageURI(uri)
            } ?: run {
                imageView.setImageResource(R.drawable.ic_default_image)
            }
        }
    }
    private fun addHabitRow(habitName: String, description: String, imageUri: Uri?) {
        val rowId = View.generateViewId()

        // 创建习惯条目对象
        val entry = HabitEntry(rowId, habitName, description, imageUri)
        habitEntries.add(entry)

        // 创建水平布局的行
        val row = LinearLayout(this).apply {
            id = rowId
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8.dpToPx(), 0, 8.dpToPx())
            }
            background = ContextCompat.getDrawable(context, R.drawable.row_background_unselected)

            // 设置点击事件
            setOnClickListener {
                toggleRowSelection(rowId)
            }
        }

        // 左侧文本部分
        val textView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f // 使用权重填满剩余空间
            ).apply {
                gravity = Gravity.START
            }
            text = "$habitName: $description"
            textSize = 16f
            setPadding(16.dpToPx(), 0, 16.dpToPx(), 0)
        }

        // 右侧图片部分
        val imageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                48.dpToPx(),
                48.dpToPx()
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            imageUri?.let { uri ->
                setImageURI(uri)
            } ?: run {
                setImageResource(R.drawable.ic_default_image) // 默认图片
            }
        }

        // 添加视图到行
        row.addView(textView)
        row.addView(imageView)
        tableContainer.addView(row)

        // 设置约束布局
        val constraintSet = ConstraintSet()
        constraintSet.clone(tableContainer)

        // 根据是否是第一行设置不同的顶部约束
        if (rowCount == 0) {
            constraintSet.connect(rowId, ConstraintSet.TOP, tableContainer.id, ConstraintSet.TOP)
        } else {
            val prevRowId = tableContainer.getChildAt(rowCount - 1).id
            constraintSet.connect(rowId, ConstraintSet.TOP, prevRowId, ConstraintSet.BOTTOM)
        }

        // 设置左右约束
        constraintSet.connect(rowId, ConstraintSet.START, tableContainer.id, ConstraintSet.START)
        constraintSet.connect(rowId, ConstraintSet.END, tableContainer.id, ConstraintSet.END)
        constraintSet.applyTo(tableContainer)

        rowCount++
    }

    private fun toggleRowSelection(rowId: Int) {
        if (selectedRows.contains(rowId)) {
            // 取消选中
            selectedRows.remove(rowId)
            tableContainer.findViewById<LinearLayout>(rowId)?.background =
                ContextCompat.getDrawable(this, R.drawable.row_background_unselected)
        } else {
            // 选中
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

        // 从后往前删除，避免索引问题
        selectedRows.sortedDescending().forEach { rowId ->
            // 从视图中删除
            tableContainer.removeView(tableContainer.findViewById(rowId))

            // 从数据中删除
            habitEntries.removeAll { it.id == rowId }

            rowCount--
        }

        // 重新设置约束
        resetConstraints()

        // 更新SharedPreferences
        saveHabitsToPrefs()

        // 清空选择
        selectedRows.clear()

        Toast.makeText(this, "已删除${selectedRows.size}个习惯", Toast.LENGTH_SHORT).show()
    }

    private fun resetConstraints() {
        val constraintSet = ConstraintSet()
        constraintSet.clone(tableContainer)

        // 重新设置所有行的约束
        for (i in 0 until tableContainer.childCount) {
            val child = tableContainer.getChildAt(i)

            if (i == 0) {
                constraintSet.connect(child.id, ConstraintSet.TOP, tableContainer.id, ConstraintSet.TOP)
            } else {
                constraintSet.connect(child.id, ConstraintSet.TOP,
                    tableContainer.getChildAt(i-1).id, ConstraintSet.BOTTOM)
            }

            constraintSet.connect(child.id, ConstraintSet.START, tableContainer.id, ConstraintSet.START)
            constraintSet.connect(child.id, ConstraintSet.END, tableContainer.id, ConstraintSet.END)
        }

        constraintSet.applyTo(tableContainer)
    }

    private fun saveHabitsToPrefs() {
        val prefs = getSharedPreferences(SharedPrefsConstants.PREFS_NAME, MODE_PRIVATE)
        val editor = prefs.edit()

        // 清除旧数据
        editor.clear()

        // 保存新数据
        editor.putInt(SharedPrefsConstants.KEY_HABIT_COUNT, habitEntries.size)

        habitEntries.forEachIndexed { index, entry ->
            editor.putString("${SharedPrefsConstants.KEY_HABIT_PREFIX}${index}_name", entry.name)
            editor.putString("${SharedPrefsConstants.KEY_HABIT_PREFIX}${index}_desc", entry.description)
            entry.imageUri?.let {
                editor.putString("${SharedPrefsConstants.KEY_HABIT_PREFIX}${index}_uri", it.toString())
            }
        }

        editor.apply()
    }

    // dp转px的扩展函数
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}