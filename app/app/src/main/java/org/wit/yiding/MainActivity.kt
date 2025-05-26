package org.wit.yiding

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet

import android.view.inputmethod.InputMethodManager

import android.widget.TextView

import androidx.core.widget.doAfterTextChanged

class MainActivity : AppCompatActivity() {

    private lateinit var txtLab: TextView
    private lateinit var txtHomework: TextView
    private lateinit var btnEdit: Button
    private var isEditing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 获取按钮引用并设置点击事件
        findViewById<Button>(R.id.btn2).setOnClickListener {
            // 跳转到SecondActivity
            startActivity(Intent(this, calendarActivity::class.java))
        }
        findViewById<Button>(R.id.btn3).setOnClickListener {
            // 跳转到SecondActivity
            startActivity(Intent(this, thirdActivity::class.java))
        }

        // 初始化视图
        txtLab = findViewById(R.id.txt_lab)
        txtHomework = findViewById(R.id.txt_homework)
        btnEdit = findViewById(R.id.btn_edit)

        // 设置编辑按钮点击事件
        btnEdit.setOnClickListener {
            if (isEditing) {
                // 结束编辑状态
                btnEdit.text = "Edit"
                txtLab.isEnabled = false
                txtHomework.isEnabled = false
                isEditing = false
            } else {
                // 进入编辑状态
                btnEdit.text = "Save"
                txtLab.isEnabled = true
                txtHomework.isEnabled = true
                isEditing = true
            }
        }

        // 默认设置为不可编辑
        txtLab.isEnabled = false
        txtHomework.isEnabled = false
    }
}