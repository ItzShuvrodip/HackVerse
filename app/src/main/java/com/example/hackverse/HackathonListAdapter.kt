package com.example.hackverse

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.hackverse.databinding.HackathonRowBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class HackathonListAdapter(private val hackathons: List<Hackathon>): RecyclerView.Adapter<HackathonListAdapter.MyViewHolder>() {
    class MyViewHolder(private val binding: HackathonRowBinding) : RecyclerView.ViewHolder(binding.root){
        private val firebaseAuth = FirebaseAuth.getInstance()
        private val db = Firebase.firestore
        fun bind(hackathon: Hackathon){

                binding.apply{
                    title.text = hackathon.title
                    organiser.text = hackathon.organisingevent
                    venue.text = hackathon.organisingauthority
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

                        if (lastDateTime != null && lastDateTime.after(currentDateTime) && eventDateTime != null && eventDateTime.after(currentDateTime)) {
                            rstatus.text = "Registration Open"
                            rstatus.setTextColor(Color.parseColor("#012106"))
                            rstatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#7EFB81"))
                        } else if(lastDateTime != null && lastDateTime.before(currentDateTime) && eventDateTime != null && (eventDateTime.after(currentDateTime)||eventDateTime.equals(currentDate))){
                            rstatus.text = "Registration Closed"
                            rstatus.setTextColor(Color.parseColor("#6E0101"))
                            rstatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFBBBB"))
                        }
                        else if(lastDateTime != null && lastDateTime.before(currentDateTime) && eventDateTime != null && eventDateTime.before(currentDateTime) && !eventDateTime.equals(currentDate)){
                            rstatus.text = "Event Closed"
                            rstatus.setTextColor(Color.parseColor("#640101"))
                            rstatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF6969"))
                        }
                    }catch (e: Exception){
                        rstatus.text = "Invalid Date"
                        rstatus.setTextColor(Color.parseColor("#808080"))
                        rstatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
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

                        date.text = when {
                            diffInDays == 0L -> "Today"
                            diffInDays == 1L -> "Tomorrow"
                            diffInDays > 1 -> "$diffInDays Days Left"
                            else -> "Date : " + hackathon.date
                        }
                        binding.date.textSize = when {
                            diffInDays == 0L -> 13f
                            diffInDays == 1L -> 13f
                            diffInDays > 1 -> 13f
                            else -> 11f
                        }
                            val user = firebaseAuth.currentUser
                            val userID=user?.uid
                    } else {
                        date.text = "Date : " + hackathon.date
                        binding.date.textSize = 11f
                    }
                    if (!hackathon.ImURL.isNullOrEmpty()) {
                        Glide.with(root.context).load(hackathon.ImURL).placeholder(R.drawable.placeholder2).centerCrop().into(binding.image)
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
        val binding = HackathonRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return hackathons.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(hackathons[position])
    }
}