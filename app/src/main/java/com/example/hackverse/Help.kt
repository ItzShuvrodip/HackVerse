package com.example.hackverse

import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.hackverse.databinding.HelpBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

class Help : AppCompatActivity() {
    private lateinit var binding: HelpBinding
    private lateinit var firebaseAuth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.my_primary)
        binding = HelpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseAuth = FirebaseAuth.getInstance()
        val db = Firebase.firestore
        val user = firebaseAuth.currentUser
        val userID = user?.uid
        if (userID == null) {
            val intent = Intent(this, opening::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }
        binding.submit.setOnClickListener {
            val feedback=binding.feedbackBox.text.toString()
            if(feedback.isEmpty()){
                binding.feedbackBox.error="Please enter your feedback"
            }
            else{
                db.collection("Feedbacks").document(userID.toString()).set(mapOf("Feedback ${System.currentTimeMillis()}" to feedback))
                    .addOnSuccessListener {
                        binding.feedbackBox.setText("")
                        binding.feedbackBox.clearFocus()
                        binding.feedbackBox.error = null
                        binding.message.visibility= View.VISIBLE
                    }
            }
        }
        binding.feedbackBox.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.message.visibility = View.GONE
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
        binding.back.setOnClickListener {
            finish()
        }
    }
}