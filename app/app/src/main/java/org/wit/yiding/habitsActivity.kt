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

class habitsActivity : AppCompatActivity() {
    // 视图变量
    private lateinit var etHabitName: EditText
    private lateinit var etDescription: EditText
    private lateinit var ivHabitImage: ImageView
    private var selectedImageUri: Uri? = null
    private var isEditMode = false
    private var originalHabitId = -1  // 原始习惯ID，用于编辑时识别

    companion object {
        const val PICK_IMAGE_REQUEST = 1002
        const val ADD_HABIT_REQUEST = 1001
        const val EXTRA_IS_EDIT_MODE = "isEditMode"
        const val EXTRA_ORIGINAL_HABIT_ID = "originalHabitId"
        const val EXTRA_HABIT_NAME = "habitName"
        const val EXTRA_DESCRIPTION = "description"
        const val EXTRA_IMAGE_URI = "imageUri"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.habitsedit)

        initViews()
        checkEditMode()
        setupButtonListeners()
    }

    private fun initViews() {
        etHabitName = findViewById(R.id.etHabitName)
        etDescription = findViewById(R.id.etDescription)
        ivHabitImage = findViewById(R.id.ivHabitImage)
    }

    private fun checkEditMode() {
        isEditMode = intent.getBooleanExtra(EXTRA_IS_EDIT_MODE, false)
        if (isEditMode) {
            originalHabitId = intent.getIntExtra(EXTRA_ORIGINAL_HABIT_ID, -1)
            etHabitName.setText(intent.getStringExtra(EXTRA_HABIT_NAME))
            etDescription.setText(intent.getStringExtra(EXTRA_DESCRIPTION))

            intent.getStringExtra(EXTRA_IMAGE_URI)?.let { uriString ->
                selectedImageUri = Uri.parse(uriString)
                ivHabitImage.setImageURI(selectedImageUri)
            }
        }
    }

    private fun setupButtonListeners() {
        findViewById<Button>(R.id.btnUploadImage).setOnClickListener {
            openImageChooser()
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            saveHabit()
        }
    }

    private fun openImageChooser() {
        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
            startActivityForResult(this, PICK_IMAGE_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                selectedImageUri = uri
                ivHabitImage.setImageURI(uri)
            }
        }
    }

    private fun saveHabit() {
        val habitName = etHabitName.text.toString().trim()
        val description = etDescription.text.toString().trim()

        if (habitName.isEmpty()) {
            Toast.makeText(this, "Habit name cannot be empty!", Toast.LENGTH_SHORT).show()
            return
        }

        Intent().apply {
            putExtra(EXTRA_IS_EDIT_MODE, isEditMode)
            if (isEditMode) {
                putExtra(EXTRA_ORIGINAL_HABIT_ID, originalHabitId)  // 返回原始ID用于替换
            }
            putExtra(EXTRA_HABIT_NAME, habitName)
            putExtra(EXTRA_DESCRIPTION, description)
            selectedImageUri?.let { putExtra(EXTRA_IMAGE_URI, it.toString()) }
            setResult(Activity.RESULT_OK, this)
            finish()
        }
    }
}