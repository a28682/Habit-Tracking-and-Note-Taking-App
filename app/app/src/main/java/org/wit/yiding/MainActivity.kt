package org.wit.yiding

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.core.content.ContextCompat
import android.net.Uri
import android.widget.FrameLayout
import android.widget.ImageView
class MainActivity : AppCompatActivity() {

    private lateinit var txtLab: TextView
    private lateinit var txtHomework: TextView
    private lateinit var btnEdit: Button
    private lateinit var tableContainer: ConstraintLayout
    private var isEditing = false
    private var rowCount = 0
    /**
     * 广播接收器：监听习惯数据更新通知
     */
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
        loadAndDisplayHabits()
    }
    /**
     * Activity恢复可见时注册广播接收器并刷新数据
     */
    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            habitChangeReceiver,
            IntentFilter(ACTION_HABIT_UPDATED)
        )
        loadAndDisplayHabits()
    }
    /**
     * Activity不可见时注销广播接收器
     */
    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(habitChangeReceiver)
    }
    /**
     * 初始化所有界面控件引用
     */
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
    /**
     * 切换编辑/保存模式（控制文本框的可编辑状态）
     */
    private fun toggleEditMode() {
        isEditing = !isEditing
        btnEdit.text = if (isEditing) "Save" else "Edit"
        txtLab.isEnabled = isEditing
        txtHomework.isEnabled = isEditing

        if (!isEditing) {
            Toast.makeText(this, "Changes saved", Toast.LENGTH_SHORT).show()
        }
    }
    /**
     * 从SharedPreferences加载习惯数据并显示在列表中
     */
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
    /**
     * 向主界面添加带图片显示功能的习惯条目行
     * @param habitName 习惯名称
     * @param description 习惯描述
     */
    private fun addHabitRowToMain(habitName: String, description: String) {
        val rowId = View.generateViewId()
        val prefs = getSharedPreferences(SharedPrefsConstants.PREFS_NAME, MODE_PRIVATE)
        val habitCount = prefs.getInt(SharedPrefsConstants.KEY_HABIT_COUNT, 0)
        var imageUri: Uri? = null

        // 查找当前习惯对应的图片URI
        for (i in 0 until habitCount) {
            if (prefs.getString("${SharedPrefsConstants.KEY_HABIT_PREFIX}${i}_name", "") == habitName) {
                val uriString = prefs.getString("${SharedPrefsConstants.KEY_HABIT_PREFIX}${i}_uri", null)
                imageUri = uriString?.let { Uri.parse(it) }
                break
            }
        }

        // 创建行容器（水平布局）
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

        // 文本部分（左侧）
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

        // 图片部分（右侧）
        val imageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                24.dpToPx(),
                24.dpToPx()
            ).apply {
                marginEnd = 8.dpToPx()
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            visibility = if (imageUri != null) View.VISIBLE else View.GONE

            imageUri?.let { uri ->
                setImageURI(uri)
            } ?: run {
                setImageResource(R.drawable.ic_default_image)
            }

            // 点击图片可以关闭
            setOnClickListener {
                visibility = View.GONE
            }
        }
        rowContainer.addView(imageView)

        // 图片显示控制按钮
        val toggleBtn = TextView(this).apply {
            text = if (imageUri != null) "" else "×"
            textSize = 16f
            setOnClickListener {
                if (imageView.visibility == View.VISIBLE) {
                    imageView.visibility = View.GONE
                    text = ""
                } else {
                    imageView.visibility = View.VISIBLE
                    text = "×"
                }
            }
        }
        rowContainer.addView(toggleBtn)

        // 添加点击整行也可以切换图片显示
        rowContainer.setOnClickListener {
            if (imageView.visibility == View.VISIBLE) {
                imageView.visibility = View.GONE
                toggleBtn.text = ""
            } else {
                imageView.visibility = View.VISIBLE
                toggleBtn.text = "×"
            }
        }

        tableContainer.addView(rowContainer)

        // 约束布局设置
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
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    companion object {
        const val ACTION_HABIT_UPDATED = "org.wit.yiding.ACTION_HABIT_UPDATED"
        /**
         * 发送习惯数据更新广播
         * @param context 上下文对象
         */
        fun notifyHabitsUpdated(context: Context) {
            LocalBroadcastManager.getInstance(context).sendBroadcast(
                Intent(ACTION_HABIT_UPDATED)
            )
        }
    }
}