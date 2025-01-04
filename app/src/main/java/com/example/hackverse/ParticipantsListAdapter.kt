package com.example.hackverse


import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.hackverse.databinding.ParticipantsRowBinding
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class ParticipantsListAdapter(private val participants: MutableList<String>, private val flag: Int, private val LeaderUID: String? = null, private val UID: String? = null) : RecyclerView.Adapter<ParticipantsListAdapter.MyViewHolder>() {
    class MyViewHolder(private val binding: ParticipantsRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(uid: String, flag: Int, LeaderUID: String? = null, UID: String? = null,removeParticipantCallback: (String) -> Unit) {

            val db = FirebaseFirestore.getInstance()
            db.collection("Users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val username = document.getString("Username") ?: "Unknown"
                        val useremail = document.getString("Email") ?: "No email"
                        val profileImageUrl = document.getString("Profile Picture URL") ?: ""
                        binding.apply {
                            name.text = username
                            email.text = useremail
                            if(profileImageUrl.isNotEmpty()) {
                                Glide.with(root.context).load(profileImageUrl)
                                    .placeholder(R.drawable.defaultdp).into(image)
                            }
                            image.setOnClickListener {
                                val context = it.context
                                if (context is AppCompatActivity) {
                                    val viewdp = viewdp(profileImageUrl)
                                    viewdp.show(context.supportFragmentManager, "dp_popup")
                                }
                            }
                            emailButton.setOnClickListener {
                                val intent = Intent(Intent.ACTION_SENDTO)
                                intent.data = Uri.parse("mailto:")
                                intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(useremail))
                                root.context.startActivity(intent)
                            }
                            if (flag == 1) {
                                kickButton.visibility = View.VISIBLE
                                kickButton.setOnClickListener {
                                    val dialogView = LayoutInflater.from( root.context).inflate(R.layout.removemem, null)
                                    val builder = AlertDialog.Builder( root.context)
                                        .setView(dialogView)
                                        .setCancelable(true)
                                    val alertDialog = builder.create()
                                    val btnYes = dialogView.findViewById<Button>(R.id.btn_yes)
                                    val btnNo = dialogView.findViewById<Button>(R.id.btn_no)
                                    btnYes.setOnClickListener {
                                        if(UID!=null) {
                                            val participantsRef = LeaderUID?.let {
                                                db.collection("Hackathons").document(UID)
                                                    .collection("Teams Participated")
                                                    .document(LeaderUID)
                                                    .collection("Participants").document(uid)
                                            }
                                            participantsRef?.delete()?.addOnSuccessListener {
                                                val participatedRef =
                                                    db.collection("Users").document(uid)
                                                        .collection("Participated Hackathons")
                                                participatedRef.document(UID).delete()
                                                removeParticipantCallback(uid)
                                            }

                                        }
                                        alertDialog.dismiss()
                                    }

                                    btnNo.setOnClickListener {
                                        alertDialog.dismiss()
                                    }
                                    alertDialog.show()
                                }
                            } else {
                                kickButton.visibility = View.GONE
                            }
                        }
                    }
                }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = ParticipantsRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return participants.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(participants[position], flag, LeaderUID, UID){ uid ->
        removeParticipant(uid)
    }
    }
    fun removeParticipant(uid: String) {
        val index = participants.indexOf(uid)
        if (index != -1) {
            participants.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}