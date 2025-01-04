package com.example.hackverse

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.hackverse.databinding.HackathonBoxBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar

class HackathonBoxAdapter(private val hackathons: List<Hackathon>) : RecyclerView.Adapter<HackathonBoxAdapter.MyViewHolder>() {

    class MyViewHolder(private val binding: HackathonBoxBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(hackathon: Hackathon) {
            binding.apply {
                title.text = hackathon.title
                organiser.text = hackathon.organisingevent

                if (!hackathon.ImURL.isNullOrEmpty()) {
                    Glide.with(root.context).load(hackathon.ImURL).placeholder(R.drawable.placeholder2).centerCrop().into(image)
                }

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
                        lastDateTime != null && lastDateTime.after(currentDateTime) && eventDateTime != null && eventDateTime.after(currentDateTime) ->
                            ColorStateList.valueOf(Color.parseColor("#4CAF50"))

                        lastDateTime != null && lastDateTime.before(currentDateTime) && eventDateTime != null && (eventDateTime.after(currentDateTime)||eventDateTime.equals(currentDate)) ->
                            ColorStateList.valueOf(Color.parseColor("#FFC107"))

                        lastDateTime != null && lastDateTime.before(currentDateTime) && eventDateTime != null && eventDateTime.before(currentDateTime) && !eventDateTime.equals(currentDate) ->
                            ColorStateList.valueOf(Color.parseColor("#F44336"))

                        else -> ColorStateList.valueOf(Color.parseColor("#808080"))
                    }
                } catch (e: Exception) {
                    indicator.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#808080"))
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
        val binding = HackathonBoxBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return hackathons.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(hackathons[position])
    }
}
