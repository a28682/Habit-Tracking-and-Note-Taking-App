package org.wit.yiding

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class habitsActivity : AppCompatActivity() {
    private lateinit var etHabitName: EditText
    private lateinit var etDescription: EditText
    private lateinit var ivHabitImage: ImageView
    private lateinit var stepsContainer: LinearLayout
    private var selectedImageUri: Uri? = null
    private var isEditMode = false
    private var originalHabitId = -1
    private val stepViews = mutableListOf<EditText>()

    companion object {
        const val PICK_IMAGE_REQUEST = 1002
        const val ADD_HABIT_REQUEST = 1001
        const val EXTRA_IS_EDIT_MODE = "isEditMode"
        const val EXTRA_ORIGINAL_HABIT_ID = "originalHabitId"
        const val EXTRA_HABIT_NAME = "habitName"
        const val EXTRA_DESCRIPTION = "description"
        const val EXTRA_IMAGE_URI = "imageUri"
        const val EXTRA_HABIT_STEPS = "habitSteps"
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
        stepsContainer = findViewById(R.id.steps_container)
        addStepView()
    }

    private fun checkEditMode() {
        isEditMode = intent.getBooleanExtra(EXTRA_IS_EDIT_MODE, false)
        if (isEditMode) {
            originalHabitId = intent.getIntExtra(EXTRA_ORIGINAL_HABIT_ID, -1)
            etHabitName.setText(intent.getStringExtra(EXTRA_HABIT_NAME))
            etDescription.setText(intent.getStringExtra(EXTRA_DESCRIPTION))

            val steps = intent.getStringArrayListExtra(EXTRA_HABIT_STEPS)
            steps?.forEach { step ->
                addStepView(step)
            }

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

        findViewById<Button>(R.id.btnAddStep).setOnClickListener {
            addStepView()
        }

        findViewById<Button>(R.id.btnDeleteStep).setOnClickListener {
            removeStepView()
        }
    }

    private fun addStepView(prefilledText: String = "") {
        val stepView = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8.dpToPx(), 0, 0)
            }
            hint = "步骤 ${stepViews.size + 1}"
            textSize = 16f
            setPadding(12.dpToPx(), 12.dpToPx(), 12.dpToPx(), 12.dpToPx())
            setText(prefilledText)
        }

        stepsContainer.addView(stepView)
        stepViews.add(stepView)
    }

    private fun removeStepView() {
        if (stepViews.size > 1) {
            val lastStep = stepViews.last()
            stepsContainer.removeView(lastStep)
            stepViews.removeAt(stepViews.size - 1)
        } else {
            Toast.makeText(this, "至少需要保留一个步骤", Toast.LENGTH_SHORT).show()
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
        val steps = stepViews.map { it.text.toString().trim() }.filter { it.isNotEmpty() }

        if (habitName.isEmpty()) {
            Toast.makeText(this, "习惯名称不能为空!", Toast.LENGTH_SHORT).show()
            return
        }

        if (steps.isEmpty()) {
            Toast.makeText(this, "请至少添加一个步骤", Toast.LENGTH_SHORT).show()
            return
        }

        Intent().apply {
            putExtra(EXTRA_IS_EDIT_MODE, isEditMode)
            if (isEditMode) {
                putExtra(EXTRA_ORIGINAL_HABIT_ID, originalHabitId)
            }
            putExtra(EXTRA_HABIT_NAME, habitName)
            putExtra(EXTRA_DESCRIPTION, description)
            putStringArrayListExtra(EXTRA_HABIT_STEPS, ArrayList(steps))

            selectedImageUri?.let { uri ->
                val copiedUri = copyImageToAppStorage(uri)
                putExtra(EXTRA_IMAGE_URI, copiedUri.toString())
            }

            setResult(Activity.RESULT_OK, this)
            finish()
        }
    }

    private fun copyImageToAppStorage(uri: Uri): Uri {
        val inputStream = contentResolver.openInputStream(uri)
        val file = File(filesDir, "images/${System.currentTimeMillis()}.jpg")
        file.parentFile?.mkdirs()

        FileOutputStream(file).use { output ->
            inputStream?.copyTo(output)
        }

        return Uri.fromFile(file)
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}