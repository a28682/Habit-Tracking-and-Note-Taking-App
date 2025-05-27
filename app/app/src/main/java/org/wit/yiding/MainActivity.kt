package org.wit.yiding

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var txtLab: TextView
    private lateinit var txtHomework: TextView
    private lateinit var btnEdit: Button
    private lateinit var tableContainer: ConstraintLayout
    private var isEditing = false
    private var rowCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 处理边缘到边缘布局
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 初始化视图
        txtLab = findViewById(R.id.txt_lab)
        txtHomework = findViewById(R.id.txt_homework)
        btnEdit = findViewById(R.id.btn_edit)
        tableContainer = findViewById(R.id.table_container)

        // 设置按钮点击事件
        findViewById<Button>(R.id.btn2).setOnClickListener {
            startActivity(Intent(this, calendarActivity::class.java))
        }

        findViewById<Button>(R.id.btn3).setOnClickListener {
            startActivity(Intent(this, thirdActivity::class.java))
        }

        // 设置Edit按钮点击事件
        btnEdit.setOnClickListener {
            toggleEditMode()
        }

        // 默认设置为不可编辑
        txtLab.isEnabled = false
        txtHomework.isEnabled = false
    }

    private fun toggleEditMode() {
        isEditing = !isEditing
        if (isEditing) {
            btnEdit.text = "Save"
            txtLab.isEnabled = true
            txtHomework.isEnabled = true
        } else {
            btnEdit.text = "Edit"
            txtLab.isEnabled = false
            txtHomework.isEnabled = false
        }
    }

}