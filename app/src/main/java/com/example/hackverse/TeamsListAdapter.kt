package com.example.hackverse


import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.hackverse.databinding.TeamRowBinding
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

class TeamsListAdapter(private val teams: MutableList<String>, private val UID: String? = null) : RecyclerView.Adapter<TeamsListAdapter.MyViewHolder>() {
    class MyViewHolder(private val binding: TeamRowBinding) : RecyclerView.ViewHolder(binding.root) {
        private lateinit var memberList: MutableList<String>
        private lateinit var adapter: ParticipantsListAdapter
        fun bind(uid: String, UID: String? = null, position: Int) {

            val db = FirebaseFirestore.getInstance()
            var isDropdownOpen = false
            memberList = mutableListOf()
            adapter = ParticipantsListAdapter(memberList, -1, uid, UID)
            db.collection("Hackathons").document(UID ?: "").get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val size = document.getString("Size") ?: "0"
                        if (size.toInt()<2) {
                            binding.section4.visibility = View.GONE
                            binding.none.visibility = View.GONE
                            binding.members.visibility = View.GONE
                        }
                        else {
                            binding.section4.visibility = View.VISIBLE
                            binding.none.visibility = View.GONE
                            binding.members.visibility = View.VISIBLE
                        }
                        }
                    }
            db.collection("Hackathons").document(UID ?: "").collection("Teams Participated").document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val teamName = document.getString("Team Name") ?: "Unknown Team"
                        binding.teamname.text = teamName
                    }
                    }
            db.collection("Users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val username = document.getString("Username") ?: "Unknown"
                        val useremail = document.getString("Email") ?: "No email"
                        val profileImageUrl = document.getString("Profile Picture URL") ?: ""
                        binding.apply {
                            leadername.text = username
                            leaderemail.text = useremail
                            if(profileImageUrl.isNotEmpty()) {
                                Glide.with(root.context).load(profileImageUrl)
                                    .placeholder(R.drawable.defaultdp).into(leaderimage)
                            }
                            teamnumber.text = "${position + 1}."
                            leaderimage.setOnClickListener {
                                val context = it.context
                                if (context is AppCompatActivity) {
                                    val viewdp = viewdp(profileImageUrl)
                                    viewdp.show(context.supportFragmentManager, "dp_popup")
                                }
                            }
                            emaillead.setOnClickListener {
                                val intent = Intent(Intent.ACTION_SENDTO)
                                intent.data = Uri.parse("mailto:")
                                intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(useremail))
                                root.context.startActivity(intent)
                            }
                            dropdown.setOnClickListener {
                                if (isDropdownOpen) {
                                    member.animate()
                                        .translationY(-member.height.toFloat())
                                        .setDuration(300)
                                        .withEndAction {
                                            member.visibility = View.GONE
                                        }
                                        .start()
                                    dropdown.setImageResource(R.drawable.dropdown)
                                    adapter.notifyDataSetChanged()
                                } else {
                                    member.visibility = View.VISIBLE
                                    member.animate()
                                        .translationY(0f)
                                        .setDuration(400)
                                        .start()
                                    dropdown.setImageResource(R.drawable.dropup)
                                    getData(uid, UID)
                                }
                                isDropdownOpen = !isDropdownOpen
                            }
                        }
                    }
                }
            getData(uid, UID)
            setupRecyclerView(binding.root.context, uid, UID)
        }
        private fun getData(uid: String? = null, UID: String? = null) {
            val db = Firebase.firestore
            if (uid != null && UID != null) {
                memberList.clear()
                db.collection("Hackathons").document(UID)
                    .collection("Teams Participated").document(uid)
                    .collection("Participants")
                    .get()
                    .addOnSuccessListener { result ->
                        for (document in result) {
                            memberList.add(document.id)
                        }
                        if (memberList.size == 0) {
                            binding.none.visibility = View.VISIBLE
                            binding.members.visibility = View.GONE
                        }else{
                            binding.none.visibility = View.GONE
                            binding.members.visibility = View.VISIBLE
                        }
                        adapter.notifyDataSetChanged()
                    }
            }
        }


        private fun setupRecyclerView(root: Context, uid: String? = null, UID: String? = null) {
            adapter = ParticipantsListAdapter(memberList,-1,uid,UID)
            binding.members.layoutManager = LinearLayoutManager(root)
            binding.members.adapter = adapter
        }

    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = TeamRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return teams.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(teams[position], UID, position)
    }

}