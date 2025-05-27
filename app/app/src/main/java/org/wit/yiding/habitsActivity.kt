package org.wit.yiding

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
private val PREFS_NAME = "HabitPrefs"
private val KEY_HABIT_NAME = "habit_name"
private val KEY_DESCRIPTION = "description"
private val KEY_IMAGE_URI = "image_uri"
class habitsActivity : AppCompatActivity() {
    //初始化要保存的变量
    private lateinit var etHabitName: EditText
    private lateinit var etDescription: EditText
    private lateinit var ivHabitImage: ImageView
    private var selectedImageUri: Uri? = null

    companion object {
        const val PICK_IMAGE_REQUEST = 1002
        const val ADD_HABIT_REQUEST = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.habitsedit)

        // 初始化视图
        etHabitName = findViewById(R.id.etHabitName)
        etDescription = findViewById(R.id.etDescription)
        ivHabitImage = findViewById(R.id.ivHabitImage)
        val btnUploadImage = findViewById<Button>(R.id.btnUploadImage)
        val btnSave = findViewById<Button>(R.id.btnSave)

        // 上传图片按钮点击事件
        btnUploadImage.setOnClickListener {
            openImageChooser()
        }

        // 保存按钮点击事件
        btnSave.setOnClickListener {
            saveHabit()
        }
    }

    private fun openImageChooser() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
        }
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PICK_IMAGE_REQUEST -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    data.data?.let { uri ->
                        selectedImageUri = uri
                        ivHabitImage.setImageURI(uri)
                    }
                }
            }
        }
    }

    private fun saveHabit() {
        val habitName = etHabitName.text.toString()
        val description = etDescription.text.toString()

        if (habitName.isEmpty()) {
            Toast.makeText(this, "Habit name cannot be empty!", Toast.LENGTH_SHORT).show()
            return
        }
        //保存到变量
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_HABIT_NAME, habitName)
            putString(KEY_DESCRIPTION, description)
            selectedImageUri?.let { putString(KEY_IMAGE_URI, it.toString()) }
            apply()
        }
        val resultIntent = Intent().apply {
            putExtra("habitName", habitName)
            putExtra("description", description)
            selectedImageUri?.let { uri ->
                putExtra("imageUri", uri.toString())
            }
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}