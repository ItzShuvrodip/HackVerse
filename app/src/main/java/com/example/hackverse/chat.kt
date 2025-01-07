package com.example.hackverse

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hackverse.databinding.ChatBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import java.util.Date

class chat : AppCompatActivity() {
    private lateinit var binding: ChatBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var chatList: MutableList<ChatData>
    private lateinit var adapter: ChatAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.my_primary)
        binding = ChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseAuth = FirebaseAuth.getInstance()
        val user = firebaseAuth.currentUser
        val userID = user?.uid
        val db = Firebase.firestore
        val intent = intent
        chatList = mutableListOf()
        val UID = intent.getStringExtra("UID")
        adapter = ChatAdapter(chatList, UID)
        if (userID == null) {
            val intent = Intent(this, opening::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }
        binding.back.setOnClickListener {
            finish()
        }
        binding.send.setOnClickListener {
            val text = binding.chatbox.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
                binding.chatbox.text.clear()
                binding.chatbox.clearFocus()
            }
        }
        getData()
        setupRecyclerView()
    }
    private fun setupRecyclerView() {
        val UID = intent.getStringExtra("UID")
        adapter = ChatAdapter(chatList,UID)
        binding.chats.layoutManager = LinearLayoutManager(this)
        binding.chats.adapter = adapter
    }
    private fun getData() {
        val db = Firebase.firestore
        val UID = intent.getStringExtra("UID")
        chatList.clear()

        if (UID != null) {
            db.collection("Hackathons").document(UID).collection("Chats")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        chatList.clear()
                        for (document in snapshot.documents) {
                            val userId = document.getString("userId") ?: ""
                            val username = document.getString("username") ?: "Anonymous"
                            val text = document.getString("text") ?: ""
                            val timestamp = document.getLong("timestamp") ?: 0L
                            val teamName = document.getString("teamName")?:""
                            chatList.add(
                                ChatData(
                                    document.id,
                                    userId,
                                    teamName,
                                    username,
                                    text,
                                    timestamp
                                )
                            )
                        }
                        adapter.notifyDataSetChanged()
                    }
                }
        }
    }

    fun sendMessage(text: String) {
        val UID=intent.getStringExtra("UID")
        val user = firebaseAuth.currentUser
        val userID = user?.uid
        val db = FirebaseFirestore.getInstance()
        val teamName = intent.getStringExtra("team")
        val message = mapOf(
            "userId" to userID,
            "username" to FirebaseAuth.getInstance().currentUser?.displayName,
            "timestamp" to System.currentTimeMillis(),
            "text" to text,
            "teamName" to teamName
        )
        db.collection("Hackathons").document(UID?:"").collection("Chats").add(message)
    }
}