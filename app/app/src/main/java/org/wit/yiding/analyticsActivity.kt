package org.wit.yiding

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

class AnalyticsActivity : AppCompatActivity() {
    private var scrollView: ScrollView? = null
    private var refreshHeader: LinearLayout? = null
    private var refreshText: TextView? = null
    private var refreshProgress: ProgressBar? = null
    private lateinit var sharedPrefs: SharedPreferences
    private var isRefreshing = false
    private var lastY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.analyticsactivity)
        sharedPrefs = getSharedPreferences("HabitPrefs", MODE_PRIVATE)

        // 初始化所有视图
        scrollView = findViewById(R.id.scrollView)
        refreshHeader = findViewById(R.id.refreshHeader)
        refreshText = findViewById(R.id.refreshText)
        refreshProgress = findViewById(R.id.refreshProgress)

        initPullToRefresh()
        setupBottomNavigation()
        initData()
    }

    private fun initPullToRefresh() {
        scrollView?.viewTreeObserver?.addOnScrollChangedListener {
            val scrollY = scrollView?.scrollY ?: 0
            when {
                scrollY < -150 && !isRefreshing -> startRefresh()
                scrollY < -30 && !isRefreshing -> {
                    refreshText?.text = "松开刷新"
                    refreshHeader?.visibility = View.VISIBLE
                }
                scrollY >= 0 -> refreshHeader?.visibility = View.GONE
            }
        }

        scrollView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> lastY = event.y
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = event.y - lastY
                    if ((scrollView?.scrollY ?: 0) == 0 && deltaY > 0) {
                        refreshHeader?.translationY = deltaY / 2 - (refreshHeader?.height ?: 0)
                        return@setOnTouchListener true
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (!isRefreshing) {
                        resetRefreshHeader()
                    }
                }
            }
            false
        }
    }

    private fun initData() {
        val habitStreaks = getHabitStreakData()
        val habitClicks = getHabitClickDaysData()

        // 设置表格数据
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewTable)
        recyclerView?.layoutManager = LinearLayoutManager(this)
        recyclerView?.adapter = HabitAdapter(habitStreaks.map { (name, streak) ->
            Habit(name, "$streak 天", habitClicks[name] ?: 0)
        })

        // 设置柱状图数据
        val chartView = findViewById<HorizontalBarChartView>(R.id.barChartView)
        chartView?.setData(habitClicks)
    }

    private fun getHabitStreakData(): Map<String, Int> {
        val sharedPrefs = getSharedPreferences(SharedPrefsConstants.PREFS_NAME, MODE_PRIVATE)
        val habitCount = sharedPrefs.getInt(SharedPrefsConstants.KEY_HABIT_COUNT, 0)
        val streaks = mutableMapOf<String, Int>()

        for (i in 0 until habitCount) {
            val name = sharedPrefs.getString("${SharedPrefsConstants.KEY_HABIT_PREFIX}${i}_name", "") ?: ""
            if (name.isNotEmpty()) {
                val streak = sharedPrefs.getInt("${SharedPrefsConstants.KEY_HABIT_STREAK_PREFIX}$name", 0)
                streaks[name] = streak
            }
        }
        return streaks
    }

    private fun getHabitClickDaysData(): Map<String, Int> {
        val sharedPrefs = getSharedPreferences(SharedPrefsConstants.PREFS_NAME, MODE_PRIVATE)
        val json = sharedPrefs.getString(SharedPrefsConstants.KEY_HABIT_CLICKS, "{}") ?: "{}"
        val habitClicks = try {
            JSONObject(json)
        } catch (e: Exception) {
            JSONObject()
        }

        val habitCount = sharedPrefs.getInt(SharedPrefsConstants.KEY_HABIT_COUNT, 0)
        val result = mutableMapOf<String, Int>()

        for (i in 0 until habitCount) {
            val name = sharedPrefs.getString("${SharedPrefsConstants.KEY_HABIT_PREFIX}${i}_name", "") ?: ""
            if (name.isNotEmpty()) {
                var count = 0
                val keys = habitClicks.keys()
                while (keys.hasNext()) {
                    val date = keys.next()
                    val array = habitClicks.getJSONArray(date)
                    for (j in 0 until array.length()) {
                        if (array.getString(j) == name) {
                            count++
                            break
                        }
                    }
                }
                result[name] = count
            }
        }
        return result
    }

    data class Habit(val name: String, val streak: String, val totalDays: Int)

    private fun startRefresh() {
        isRefreshing = true
        refreshText?.text = "正在刷新..."
        refreshProgress?.visibility = View.VISIBLE
        refreshHeader?.animate()
            ?.translationY(0f)
            ?.setDuration(200)
            ?.start()

        Handler(Looper.getMainLooper()).postDelayed({
            resetRefreshHeader()
            loadData()
        }, 1500)
    }

    private fun loadData() {
        initData()
        Toast.makeText(this, "数据已刷新", Toast.LENGTH_SHORT).show()
    }

    private fun resetRefreshHeader() {
        refreshHeader?.animate()
            ?.translationY(-(refreshHeader?.height?.toFloat() ?: 0f))
            ?.setDuration(200)
            ?.withEndAction {
                refreshHeader?.visibility = View.GONE
                isRefreshing = false
            }
            ?.start()
    }

    private fun setupBottomNavigation() {
        findViewById<Button>(R.id.btn1)?.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        findViewById<Button>(R.id.btn2)?.setOnClickListener {
            startActivity(Intent(this, calendarActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        findViewById<Button>(R.id.btn3)?.setOnClickListener {
            startActivity(Intent(this, thirdActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    class HabitAdapter(private val habits: List<Habit>) :
        RecyclerView.Adapter<HabitAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val nameView: TextView = view.findViewById(R.id.tvHabitName)
            val streakView: TextView = view.findViewById(R.id.tvStreak)
            val totalDaysView: TextView = view.findViewById(R.id.tvTotalDays)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_table_row, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = habits[position]
            holder.nameView.text = item.name

            holder.totalDaysView.text = "总计: ${item.totalDays}天"
        }

        override fun getItemCount() = habits.size
    }

    class HorizontalBarChartView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : View(context, attrs, defStyleAttr) {
        private val barPaint = Paint().apply {
            color = Color.parseColor("#6200EE")
            style = Paint.Style.FILL
        }
        private val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 36f
            textAlign = Paint.Align.LEFT
        }
        private var data: Map<String, Int> = emptyMap()

        fun setData(data: Map<String, Int>) {
            this.data = data
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (data.isEmpty()) return

            val barHeight = 50f
            val startX = 150f
            var startY = 80f
            val maxValue = data.values.maxOrNull() ?: 1
            val scale = (width - 200f) / maxValue

            // 绘制坐标轴
            canvas.drawLine(100f, 50f, 100f, startY + data.size * 80f, Paint().apply {
                color = Color.BLACK
                strokeWidth = 3f
            })

            // 绘制标题
            canvas.drawText("习惯点击天数统计", 20f, 40f, textPaint.apply {
                textSize = 40f
                color = Color.DKGRAY
            })

            data.forEach { (habitName, days) ->
                // 绘制习惯名称
                canvas.drawText(habitName, 20f, startY + barHeight / 2 + 10, textPaint.apply {
                    textSize = 32f
                })

                // 绘制柱状条
                val barLength = days * scale
                canvas.drawRect(
                    startX, startY,
                    startX + barLength, startY + barHeight,
                    barPaint
                )

                // 绘制天数
                canvas.drawText(
                    "$days 天",
                    startX + barLength + 20f,
                    startY + barHeight / 2 + 10,
                    textPaint.apply { color = Color.DKGRAY }
                )

                startY += 80f
            }
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            setMeasuredDimension(
                MeasureSpec.getSize(widthMeasureSpec),
                (data.size * 80 + 100).toInt().coerceAtLeast(300)
            )
        }
    }
}