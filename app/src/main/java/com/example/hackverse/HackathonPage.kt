package com.example.hackverse

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.hackverse.databinding.HackathonpageBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.dynamicLinks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class HackathonPage : AppCompatActivity() {
            private lateinit var binding: HackathonpageBinding
            private lateinit var firebaseAuth: FirebaseAuth
            private lateinit var CommentList: MutableList<Comment>
            private lateinit var adapter: CommentsAdapter
            var iurl:String?=null
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = ContextCompat.getColor(this, R.color.my_primary)
                binding = HackathonpageBinding.inflate(layoutInflater)
                setContentView(binding.root)
                firebaseAuth = FirebaseAuth.getInstance()
                val user = firebaseAuth.currentUser
                CommentList = mutableListOf()
                val intent = intent
                val UID = intent.getStringExtra("UID")
                adapter = CommentsAdapter(CommentList,UID)
                val userID = user?.uid
                val scope = CoroutineScope(Dispatchers.Main)
                val db = Firebase.firestore
                var nameh: String? = null
                if (UID != null) {
                    if (userID != null) {
                        db.collection("Users").document(userID).collection("Recents").document(UID).get()
                            .addOnSuccessListener { documentSnapshot ->
                                if (documentSnapshot.exists()) {
                                    documentSnapshot.reference.update("timestamp", System.currentTimeMillis())
                                } else {
                                    db.collection("Users").document(userID).collection("Recents").document(UID)
                                        .set(hashMapOf<String, Any>())
                                        .addOnSuccessListener {
                                            documentSnapshot.reference.update("timestamp", System.currentTimeMillis())
                                        }
                                }
                            }
                    db.collection("Hackathons").document(UID).collection("Likes").document(userID).get()
                        .addOnSuccessListener {
                            if(it.exists()){
                                binding.like.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.like))
                                binding.like.imageTintList = ColorStateList.valueOf(Color.parseColor("#FC1239"))
                            }else{
                                binding.like.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.unlike))
                                binding.like.imageTintList = ColorStateList.valueOf(Color.parseColor("#1A1919"))
                            }
                        }
                        }
                    db.collection("Hackathons").document(UID)
                        .collection("Likes").get()
                        .addOnSuccessListener { querySnapshot ->
                            val likes = querySnapshot.size()
                            binding.likeCount.text = likes.toString()
                        }
                    val userRef =
                        FirebaseFirestore.getInstance().collection("Hackathons").document(UID)
                    userRef.get().addOnSuccessListener { documentSnapshot ->
                        if (documentSnapshot.exists()) {
                            binding.header.text = documentSnapshot.getString("Title")
                            nameh = documentSnapshot.getString("Title")
                            binding.title.text = documentSnapshot.getString("Title")
                            binding.organisingauthority.text =
                                documentSnapshot.getString("Organising Authority")
                            binding.organisingevent.text =
                                documentSnapshot.getString("Organising Event")
                            binding.date.text = "Event Date : " + documentSnapshot.getString("Date")
                            binding.eligibility.text = documentSnapshot.getString("Eligibility")
                            binding.eligibilityd.text =
                                documentSnapshot.getString("Eligibility Details")
                            binding.size.text = documentSnapshot.getString("Size") + " Members"
                            binding.lastdate.text = documentSnapshot.getString("Last Date")
                            binding.venue.text = documentSnapshot.getString("Venue")
                            binding.overview.text = documentSnapshot.getString("Overview")
                            binding.eventstructure.text = documentSnapshot.getString("Structure")
                            binding.judging.text = documentSnapshot.getString("Judging")
                            binding.date2.text = documentSnapshot.getString("Date")
                            binding.lastdate2.text = documentSnapshot.getString("Last Date")
                            binding.reward.text = documentSnapshot.getString("Reward")
                            binding.email.text = documentSnapshot.getString("Email")
                            binding.number.text = documentSnapshot.getString("Number")
                            binding.name.text = documentSnapshot.getString("Name")
                            iurl=documentSnapshot.getString("Image URL")
                            if (!documentSnapshot.getString("Image URL").isNullOrEmpty()) {
                                Glide.with(this).load(documentSnapshot.getString("Image URL")).placeholder(R.drawable.placeholder2).centerCrop().into(binding.display)
                            }

                            val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
                            val sdf2 = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                            val lastDateString = documentSnapshot.getString("Last Date")
                            val eventDateString = documentSnapshot.getString("Date")

                            val lastDateTime = lastDateString?.let { sdf.parse(it) }
                            val eventDateTime = eventDateString?.let { sdf2.parse(it) }
                            val currentDateTime = Date()
                            val currentDate = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.time

                            if (lastDateTime != null && eventDateTime != null) {
                                scope.launch {

                                    if (lastDateTime.after(currentDateTime) && eventDateTime.after(
                                            currentDateTime
                                        )
                                    ) {
                                        val participatedDoc = userID?.let {
                                            db.collection("Users")
                                                .document(it)
                                                .collection("Participated Hackathons")
                                                .document(UID).get().await()
                                        }

                                        if (participatedDoc != null) {
                                            if (participatedDoc.exists()) {
                                                binding.register.isEnabled = true
                                                binding.register.isClickable = true
                                                binding.register.text = "Check Team Details"
                                                binding.chat.visibility = View.VISIBLE
                                                binding.register.setTextColor(Color.parseColor("#FFFFFF"))
                                                binding.register.backgroundTintList =
                                                    ColorStateList.valueOf(Color.parseColor("#4CAF50"))
                                            } else {
                                                binding.register.text = "Register Now"
                                                binding.register.setTextColor(Color.parseColor("#036261"))
                                                binding.register.backgroundTintList =
                                                    ColorStateList.valueOf(Color.parseColor("#BAF0EF"))
                                                binding.register.isEnabled = true
                                                binding.register.isClickable = true
                                            }
                                        }
                                    } else if (lastDateTime != null && lastDateTime.before(currentDateTime) && eventDateTime != null && (eventDateTime.after(currentDateTime)||eventDateTime.equals(currentDate))){
                                        val participatedDoc = userID?.let {
                                            db.collection("Users")
                                                .document(it)
                                                .collection("Participated Hackathons")
                                                .document(UID).get().await()
                                        }
                                        if (participatedDoc != null) {
                                            if (participatedDoc.exists()) {
                                                binding.register.isEnabled = true
                                                binding.register.isClickable = true
                                                binding.chat.visibility = View.VISIBLE
                                                binding.register.text = "Check Team Details"
                                                binding.register.setTextColor(Color.parseColor("#FFFFFF"))
                                                binding.register.backgroundTintList =
                                                    ColorStateList.valueOf(Color.parseColor("#4CAF50"))

                                            } else {
                                                binding.register.text = "Registration Closed"
                                                binding.register.setTextColor(Color.parseColor("#6E0101"))
                                                binding.register.backgroundTintList =
                                                    ColorStateList.valueOf(Color.parseColor("#FFBBBB"))
                                                binding.register.isEnabled = false
                                                binding.register.isClickable = false
                                            }
                                        }
                                    } else if (lastDateTime != null && lastDateTime.before(currentDateTime) && eventDateTime != null && eventDateTime.before(currentDateTime) && !eventDateTime.equals(currentDate)){
                                        binding.register.text = "Event Closed"
                                        binding.register.setTextColor(Color.parseColor("#640101"))
                                        binding.register.backgroundTintList =
                                            ColorStateList.valueOf(Color.parseColor("#FF6969"))
                                        binding.register.isEnabled = false
                                        binding.register.isClickable = false
                                    }
                                }
                            }



                            if (documentSnapshot.getString("UserID") == userID) {
                                binding.edit.visibility = View.VISIBLE
                                binding.delete.visibility = View.VISIBLE
                                binding.chat.visibility = View.VISIBLE
                                binding.info.visibility = View.VISIBLE
                            } else {
                                binding.edit.visibility = View.GONE
                                binding.delete.visibility = View.GONE
                                binding.info.visibility = View.GONE
                            }
                            if (documentSnapshot.getString("Fee") == "Free" || documentSnapshot.getString(
                                    "Fee"
                                ) == "0"
                            ) {
                                binding.registrationfee.text = "Free"
                                binding.registrationfee.textSize = 15f
                                binding.registrationfee.setTextColor(
                                    ContextCompat.getColor(
                                        this,
                                        R.color.my_primary
                                    )
                                )
                            } else {
                                binding.registrationfee.text =
                                    "₹" + documentSnapshot.getString("Fee")
                                binding.registrationfee.textSize = 13f
                            }
                        }
                    }
                }

                if (userID == null) {
                    val intent = Intent(this, opening::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    finish()
                }
                binding.chat.setOnClickListener {
                    val intent = Intent(this, chat::class.java)
                    intent.putExtra("UID", UID)
                    startActivity(intent)
                }
                binding.like.setOnClickListener {
                    val currentTintColor = binding.like.imageTintList?.defaultColor
                    if(UID!=null){
                    if (currentTintColor == Color.parseColor("#FC1239")) {
                        db.collection("Hackathons").document(UID).collection("Likes").document(userID!!).delete()
                            .addOnSuccessListener {
                                db.collection("Hackathons").document(UID)
                                    .collection("Likes").get()
                                    .addOnSuccessListener { querySnapshot ->
                                        val likes = querySnapshot.size()
                                        binding.likeCount.text = likes.toString()
                                    }
                            }
                                binding.like.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.unlike))
                                binding.like.imageTintList = ColorStateList.valueOf(Color.parseColor("#1A1919"))
                            } else {
                                db.collection("Hackathons").document(UID).collection("Likes").document(userID!!).set(hashMapOf<String, Any>())
                                    .addOnSuccessListener {
                                        db.collection("Hackathons").document(UID)
                                            .collection("Likes").get()
                                            .addOnSuccessListener { querySnapshot ->
                                                val likes = querySnapshot.size()
                                                binding.likeCount.text = likes.toString()
                                            }
                                    }
                            binding.like.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.like))
                            binding.like.imageTintList = ColorStateList.valueOf(Color.parseColor("#FC1239"))
                        }
                    }
                }
                binding.email.setOnClickListener {
                    binding.email.setTextColor(Color.parseColor("#551A8B"))
                    binding.email.paintFlags = binding.email.paintFlags or Paint.UNDERLINE_TEXT_FLAG
                    val intent = Intent(Intent.ACTION_SENDTO)
                    intent.data = Uri.parse("mailto:")
                    intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(binding.email.text.toString()))
                    startActivity(intent)
                }
                binding.number.setOnClickListener {
                    binding.number.setTextColor(Color.parseColor("#551A8B"))
                    binding.number.paintFlags = binding.number.paintFlags or Paint.UNDERLINE_TEXT_FLAG
                    val intent = Intent(Intent.ACTION_DIAL)
                    intent.data = Uri.parse("tel:"+binding.number.text.toString())
                    startActivity(intent)
                }
                binding.back.setOnClickListener {
                    finish()
                }
                binding.info.setOnClickListener{
                    val intent = Intent(this, Information::class.java)
                    intent.putExtra("UID", UID)
                    startActivity(intent)
                }
                binding.delete.setOnClickListener {
                    val dialogView = LayoutInflater.from(this).inflate(R.layout.deleteddialogueh, null)
                    val builder = AlertDialog.Builder(this)
                        .setView(dialogView)
                        .setCancelable(true)
                    val alertDialog = builder.create()
                    val btnYes = dialogView.findViewById<Button>(R.id.btn_yes)
                    val btnNo = dialogView.findViewById<Button>(R.id.btn_no)
                    btnYes.setOnClickListener {
                        if (UID != null) {
                            db.collection("Hackathons").document(UID).delete()
                            Toast.makeText(this, "Hackathon deleted successfully!", Toast.LENGTH_SHORT).show()
                            finish()
                        }else{
                            Toast.makeText(this, "Failed to delete hackathon", Toast.LENGTH_SHORT).show()
                        }
                        alertDialog.dismiss()
                    }

                    btnNo.setOnClickListener {
                        alertDialog.dismiss()
                    }
                    alertDialog.show()
                }
                binding.share.setOnClickListener {
                    val link = Uri.parse("https://hackverse.page.link/hackathon?uid=$UID")
                    val dynamicLink = Firebase.dynamicLinks
                        .createDynamicLink()
                        .setLink(link)
                        .setDomainUriPrefix("https://hackverse.page.link")
                        .setAndroidParameters(
                            DynamicLink.AndroidParameters.Builder("com.example.hackverse")
                                .build()
                        )
                        .setSocialMetaTagParameters(
                            DynamicLink.SocialMetaTagParameters.Builder()
                                .setTitle("Join the Hackathon! - $nameh")
                                .setDescription("Register Now!")
                                .setImageUrl(Uri.parse(iurl))
                                .build()
                        )
                        .buildShortDynamicLink()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val shortLink = task.result.shortLink
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Check out this Hackathon: $shortLink")
                                    type = "text/plain"
                                }
                                startActivity(Intent.createChooser(shareIntent, "Share via"))
                            }
                        }

                }
                binding.edit.setOnClickListener {
                    val intent = Intent(this, EditHackathon::class.java)
                    intent.putExtra("UID", UID)
                    startActivity(intent)
                    finish()
                }
                binding.nav1.setOnClickListener {
                    binding.scrollView.smoothScrollTo(0, binding.section6.top)
                }
                binding.nav2.setOnClickListener {
                    binding.scrollView.smoothScrollTo(0, binding.nav2d.top+binding.section6.top)
                }
                binding.nav3.setOnClickListener {
                    binding.scrollView.smoothScrollTo(0, binding.nav3d.top+binding.section6.top)
                }
                binding.nav4.setOnClickListener {
                    binding.scrollView.smoothScrollTo(0, binding.section7.top)
                }
                binding.nav5.setOnClickListener {
                    binding.scrollView.smoothScrollTo(0, binding.section8.top)
                }
                binding.register.setOnClickListener {
                    val intent = Intent(this, DetailsRegistration::class.java)
                    intent.putExtra("UID", UID)
                    startActivity(intent)
                    finish()
                }
                binding.comment.setOnClickListener {
                    val commentText = binding.commentInput.text.toString()
                    if (commentText.isNotBlank()) {
                        val comment = mapOf(
                            "userId" to userID,
                            "username" to FirebaseAuth.getInstance().currentUser?.displayName,
                            "timestamp" to System.currentTimeMillis(),
                            "text" to binding.commentInput.text.toString()
                        )
                        if (UID != null) {
                            db.collection("Hackathons").document(UID).collection("Comments").add(comment).addOnSuccessListener {
                                binding.commentInput.text?.clear()
                                getData()
                                setupRecyclerView()
                                Toast.makeText(this, "Comment added", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                getData()
                setupRecyclerView()

    }
            private fun setupRecyclerView() {
                val UID = intent.getStringExtra("UID")
                adapter = CommentsAdapter(CommentList,UID)
                binding.commentsRecyclerView.layoutManager = LinearLayoutManager(this)
                binding.commentsRecyclerView.adapter = adapter
            }
            private fun getData() {
                val db = Firebase.firestore
                val UID = intent.getStringExtra("UID")
                CommentList.clear()
                if (UID != null) {
                    db.collection("Hackathons").document(UID).collection("Comments")
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                val userId = document.getString("userId") ?: ""
                                val username = document.getString("username") ?: "Anonymous"
                                val text = document.getString("text") ?: ""
                                val timestamp = document.getLong("timestamp") ?: 0L
                                CommentList.add(Comment(document.id, userId, username, text, timestamp))
                            }
                            CommentList.sortByDescending { it.timestamp }
                            adapter.notifyDataSetChanged()
                        }
                }
            }

}