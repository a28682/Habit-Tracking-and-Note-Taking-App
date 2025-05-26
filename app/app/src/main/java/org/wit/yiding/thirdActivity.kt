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

class thirdActivity : AppCompatActivity() {

    private lateinit var tableContainer: ConstraintLayout
    private var rowCount = 0

    companion object {
        const val ADD_HABIT_REQUEST = 1001
    }

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
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_HABIT_REQUEST && resultCode == RESULT_OK) {
            val habitName = data?.getStringExtra("habitName") ?: ""
            val description = data?.getStringExtra("description") ?: ""
            val imageUri = data?.getStringExtra("imageUri")?.let { Uri.parse(it) }
            addHabitRow(habitName, description, imageUri)
        }
    }

    private fun addHabitRow(habitName: String, description: String, imageUri: Uri?) {
        val rowId = View.generateViewId()

        // 创建水平布局的行
        val row = LinearLayout(this).apply {
            id = rowId
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8.dpToPx(), 0, 8.dpToPx())
            }
            background = ContextCompat.getDrawable(context, R.drawable.row_background)
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

    // dp转px的扩展函数
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}