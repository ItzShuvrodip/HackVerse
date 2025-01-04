package com.example.hackverse

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.hackverse.databinding.CommentsRowBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class CommentsAdapter(private val comments: MutableList<Comment>, private val UID: String? = null) :
    RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    class CommentViewHolder(private val binding: CommentsRowBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
        private val db = FirebaseFirestore.getInstance()
        private val user = firebaseAuth.currentUser
        private val userID = user?.uid

        fun bind(comment: Comment, UID: String? = null, removeCommentCallback: (String) -> Unit) {
            binding.commentUsername.text = comment.username
            binding.commentText.text = comment.text
            var profileImageUrl = ""
            db.collection("Users").document(comment.userId).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    profileImageUrl = document.getString("Profile Picture URL") ?: ""
                    if (profileImageUrl.isNotEmpty()) {
                        Glide.with(binding.root.context).load(profileImageUrl)
                            .placeholder(R.drawable.defaultdp).into(binding.image)
                    }
                }
            }
            if (userID == comment.userId) {
                binding.delete.visibility = View.VISIBLE
            } else {
                binding.delete.visibility = View.GONE
            }
            binding.delete.setOnClickListener {
                if (UID != null) {
                    val commentRef =
                        db.collection("Hackathons").document(UID).collection("Comments")
                            .document(comment.commentID)

                    commentRef.delete().addOnSuccessListener {
                        removeCommentCallback(comment.commentID)
                    }

                }
            }
            binding.image.setOnClickListener {
                val context = it.context
                if (context is AppCompatActivity) {
                    val viewdp = viewdp(profileImageUrl)
                    viewdp.show(context.supportFragmentManager, "dp_popup")
                }
            }
            binding.commentTime.text = getRelativeTime(comment.timestamp)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = CommentsRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
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

    companion object {
        fun getRelativeTime(timestamp: Long): String {
            val currentTime = System.currentTimeMillis()
            val diff = currentTime - timestamp
            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "${TimeUnit.MILLISECONDS.toSeconds(diff)} seconds ago"
                diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)} minutes ago"
                diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)} hours ago"
                diff < TimeUnit.DAYS.toMillis(30) -> "${TimeUnit.MILLISECONDS.toDays(diff)} days ago"
                diff < TimeUnit.DAYS.toMillis(365) -> "${diff / TimeUnit.DAYS.toMillis(30)} months ago"
                else -> "${diff / TimeUnit.DAYS.toMillis(365)} years ago"
            }
        }
    }
}