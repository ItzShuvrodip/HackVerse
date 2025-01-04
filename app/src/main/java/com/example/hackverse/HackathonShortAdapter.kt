package com.example.hackverse

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.hackverse.databinding.HackathonShortBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class HackathonShortAdapter(private val hackathons: List<Hackathon>): RecyclerView.Adapter<HackathonShortAdapter.MyViewHolder>() {
    class MyViewHolder(private val binding: HackathonShortBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(hackathon: Hackathon){

                binding.apply {
                    title.text = hackathon.title
                    val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
                    val sdf2 = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    try {
                        val lastDateTime = sdf.parse(hackathon.lastdate)
                        val eventDateTime = sdf2.parse(hackathon.date)
                        val currentDateTime = Date()
                        val currentDate = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.time

                        indicator.backgroundTintList = when {
                            lastDateTime != null && lastDateTime.after(currentDateTime) && eventDateTime != null && eventDateTime.after(
                                currentDateTime
                            ) ->
                                ColorStateList.valueOf(Color.parseColor("#4CAF50"))

                            lastDateTime != null && lastDateTime.before(currentDateTime) && eventDateTime != null && (eventDateTime.after(
                                currentDateTime
                            ) || eventDateTime.equals(currentDate)) ->
                                ColorStateList.valueOf(Color.parseColor("#FFC107"))

                            lastDateTime != null && lastDateTime.before(currentDateTime) && eventDateTime != null && eventDateTime.before(
                                currentDateTime
                            ) && !eventDateTime.equals(currentDate) ->
                                ColorStateList.valueOf(Color.parseColor("#F44336"))

                            else -> ColorStateList.valueOf(Color.parseColor("#808080"))
                        }
                    } catch (e: Exception) {
                        indicator.backgroundTintList =
                            ColorStateList.valueOf(Color.parseColor("#808080"))
                    }
                    val eventDate = try {
                        sdf2.parse(hackathon.date)
                    } catch (e: Exception) {
                        null
                    }
                    val currentDate = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time

                    if (eventDate != null) {
                        val eventCalendar = Calendar.getInstance().apply {
                            time = eventDate
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val diffInMillies = eventCalendar.time.time - currentDate.time
                        val diffInDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS)

                        if(diffInDays==0L)
                        {
                            days.visibility=View.GONE
                            today.visibility= View.VISIBLE
                            today.text="is Today"
                            ready.visibility=View.GONE
                            hurry.visibility=View.VISIBLE
                        }else if(diffInDays==1L){
                            days.visibility=View.GONE
                            today.visibility= View.VISIBLE
                            today.text="is Tomorrow"
                            ready.visibility=View.GONE
                            hurry.visibility=View.VISIBLE
                        }else{
                            days.visibility=View.VISIBLE
                            today.visibility= View.GONE
                            days.text="${diffInDays} days left for"
                            ready.visibility=View.VISIBLE
                            hurry.visibility=View.GONE
                        }
                    }
                    root.setOnClickListener {
                        val intent = Intent(root.context, HackathonPage::class.java)
                        intent.putExtra("UID", hackathon.UID)
                        root.context.startActivity(intent)
                    }
                }
            }

        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = HackathonShortBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return hackathons.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(hackathons[position])
    }
}