package org.wit.yiding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HabitAdapter(private val habits: List<AnalyticsActivity.Habit>) :
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

    override fun getItemCount(): Int = habits.size
}