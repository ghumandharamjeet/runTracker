package com.app.runtracker.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.app.runtracker.R
import com.app.runtracker.db.Run
import com.app.runtracker.others.TrackingUtility
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.run_item_view.view.*
import java.text.SimpleDateFormat
import java.util.*

class RunAdapter : RecyclerView.Adapter<RunAdapter.RunViewHolder>() {

    val differCallback = object : DiffUtil.ItemCallback<Run>(){
        override fun areItemsTheSame(oldItem: Run, newItem: Run): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Run, newItem: Run): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    }

    var asyncDiffer = AsyncListDiffer(this, differCallback)

    fun submitList(runList : List<Run>){
        asyncDiffer.submitList(runList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RunViewHolder {

        return RunViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.run_item_view, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RunViewHolder, position: Int) {

        holder.setView(asyncDiffer.currentList[position])
    }

    override fun getItemCount(): Int {

        return asyncDiffer.currentList.size
    }

    inner class RunViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){

        fun setView(run : Run){

            itemView.apply {

                Calendar.getInstance().apply {
                    timeInMillis = run.runTimeStamp
                    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    run_date.text = "${dateFormat.format(time)}"
                }

                run_distance.text = "${run.distanceInMeters / 1000f}"
                run_time.text = "${TrackingUtility().getFormattedTime(run.timeInMillis)}"
                Glide.with(itemView.context).load(run.img).into(map_snapshot)
                calories_burned.text = "${run.caloriesBurned} KCal"
                avg_speed.text = "${run.avgSpeedInKMH} Km/H"
            }
        }
    }
}