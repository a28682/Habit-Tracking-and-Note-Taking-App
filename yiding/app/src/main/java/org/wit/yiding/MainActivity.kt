package org.wit.yiding

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
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
    }
}