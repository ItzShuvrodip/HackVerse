package com.example.hackverse

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.hackverse.databinding.ChatRowBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(private val comments: MutableList<ChatData>, private val UID: String? = null) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(private val binding: ChatRowBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
        private val db = FirebaseFirestore.getInstance()
        private val user = firebaseAuth.currentUser
        private val userID = user?.uid

        fun bind(chat: ChatData, UID: String? = null, removeChatCallback: (String) -> Unit) {
            binding.commentUsername.text = chat.username
            binding.commentText.text = chat.text
            binding.designation.text=": "+chat.teamName
            var profileImageUrl = ""
            db.collection("Users").document(chat.userId).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    profileImageUrl = document.getString("Profile Picture URL") ?: ""
                    if (profileImageUrl.isNotEmpty()) {
                        Glide.with(binding.root.context).load(profileImageUrl)
                            .placeholder(R.drawable.defaultdp).into(binding.image)
                    }
                }
            }
            binding.root.setOnLongClickListener {
                if(userID==chat.userId) {
                    val dialog = Dialog(binding.root.context)
                    dialog.setContentView(R.layout.deletechat)
                    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    dialog.window?.setLayout(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    val params = dialog.window?.attributes
                    params?.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    params?.y = binding.root.top - 10
                    dialog.window?.attributes = params
                    val delete = dialog.findViewById<TextView>(R.id.delete)
                    delete.setOnClickListener {
                        dialog.dismiss()
                        if (UID != null) {
                            val commentRef =
                                db.collection("Hackathons").document(UID).collection("Chats")
                                    .document(chat.commentID)
                            commentRef.delete().addOnSuccessListener {
                                removeChatCallback(chat.commentID)
                            }
                        }
                    }
                    dialog.show()
                    true
                }else{
                    false
                }
            }
            binding.image.setOnClickListener {
                val context = it.context
                if (context is AppCompatActivity) {
                    val viewdp = viewdp(profileImageUrl)
                    viewdp.show(context.supportFragmentManager, "dp_popup")
                }
            }
            val dateFormat = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault())
            binding.commentTime.text = dateFormat.format(Date(chat.timestamp))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ChatRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(comments[position], UID) { commentID ->
            removeComment(commentID)
        }
    }

    fun removeComment(commentID: String) {
        val index = comments.indexOfFirst { it.commentID == commentID }
        if (index != -1) {
            comments.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    override fun getItemCount(): Int = comments.size
}