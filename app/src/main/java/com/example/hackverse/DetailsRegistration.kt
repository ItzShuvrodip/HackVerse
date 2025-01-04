package com.example.hackverse

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.launch
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isNotEmpty
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.hackverse.databinding.DetailsregistrationBinding
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.ShortDynamicLink
import com.google.firebase.dynamiclinks.dynamicLinks
import com.google.firebase.firestore.FieldValue
import com.example.hackverse.BuildConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetailsRegistration : AppCompatActivity() {
    private lateinit var binding: DetailsregistrationBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private var isLeader=-1
    private var isMemberOwn=-1
    private var isMemberHack=-1
    val scope = CoroutineScope(Dispatchers.Main)
    var nameh: String? = null
    private var number: Int = 0
    private var maxNumber:Int=0
    private var dplink: String? = null
    private var invite : String? = null
    var iurl:String?=null
    var flag: Int = 0
    private var LeaderUID : String? = null
    private var teamnameh: String? = null
    private lateinit var memberList: MutableList<String>
    private lateinit var adapter: ParticipantsListAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.my_primary)
        binding = DetailsregistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseAuth = FirebaseAuth.getInstance()
        memberList = mutableListOf()
        val user = firebaseAuth.currentUser
        val userID = user?.uid
        val db = Firebase.firestore
        val intent = intent
        val UID = intent.getStringExtra("UID")
        adapter = ParticipantsListAdapter(memberList, flag, LeaderUID, UID)
        invite = intent.getStringExtra("Invite")
        if (userID == null) {
            val intent = Intent(this, opening::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }
        else{
            scope.launch{
                    UpdateUI(UID,userID)
            }
        }

        if (UID != null) {
            val userRef2 =
                FirebaseFirestore.getInstance().collection("Hackathons").document(UID)
            userRef2.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    nameh = document.getString("Title")
                    iurl = document.getString("Image URL")
                    if(iurl==null||iurl==""||iurl.isNullOrEmpty())
                    {
                        iurl = "android.resource://${BuildConfig.APPLICATION_ID}/${R.drawable.logo}"
                    }
                    binding.hackathon.text = document.getString("Title")
                    maxNumber = document.getString("Size")?.toInt() ?: 0
                }
            }
        }
        if (invite != null) {
            val participantsRef = UID?.let {
                db.collection("Hackathons").document(it)
                    .collection("Teams Participated").document(invite!!)
                    .collection("Participants")
            }
            participantsRef?.get()?.addOnSuccessListener { querySnapshot ->
                number = querySnapshot.size()
            }
        }
        binding.leaderimage.setOnClickListener {
                val viewdp = dplink?.let { it1 -> viewdp(it1) }
            if (viewdp != null) {
                viewdp.show(supportFragmentManager, "dp_popup")
            }
        }
        binding.ResetLink.setOnClickListener {
            if (UID != null&&userID!=null) {
                createLink(UID, userID, teamnameh?: "",
                    onSuccess = { link ->
                        val teamsRef = db.collection("Hackathons").document(UID)
                            .collection("Teams Participated")
                        val teamData = mapOf(
                            "Invite Link" to link
                        )
                        teamsRef.document(userID).update(teamData)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "Invite Link Updated",
                                    Toast.LENGTH_SHORT
                                ).show()
                                binding.link.text = link
                            }
                    })
            }
        }
        binding.copy.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Invite Link", binding.link.text.toString())
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Invite Link copied to clipboard", Toast.LENGTH_SHORT).show()
        }
        binding.share.setOnClickListener {
            val sendIntent = Intent()
            sendIntent.action = Intent.ACTION_SEND
            sendIntent.putExtra(Intent.EXTRA_TEXT, binding.link.text.toString())
            sendIntent.type ="text/plain"
            startActivity(Intent.createChooser(sendIntent, "Share via"))
        }
        binding.register.setOnClickListener {
            if (invite != null) {
                if(isMemberOwn==-1&&isLeader==-1&&isMemberHack==-1){
                if (number < maxNumber - 1) {
                    if (userID != null && UID != null) {
                        val userRef = db.collection("Users").document(userID)
                        val participatedHackathonsRef =
                            userRef.collection("Participated Hackathons")
                        participatedHackathonsRef.document(UID).set(hashMapOf<String, Any>())
                            .addOnSuccessListener {
                                val inviteCollectionRef = db.collection("Hackathons").document(UID)
                                    .collection("Teams Participated")
                                val participantRef =
                                    inviteCollectionRef.document(invite!!)
                                        .collection("Participants")
                                participantRef.document(userID).set(hashMapOf<String, Any>())
                                    .addOnSuccessListener {
                                        Toast.makeText(
                                            this,
                                            "Invitation Accepted", Toast.LENGTH_SHORT
                                        ).show()
                                        invite=null
                                        scope.launch {
                                            UpdateUI(UID, userID)
                                        }
                                    }
                            }
                    }
                } else {
                    Toast.makeText(this, "Team is full", Toast.LENGTH_SHORT).show()
                }
            }else if(isMemberOwn == 1 && isLeader == 1){
                    binding.register.setTextColor(Color.parseColor("#808080"))
                    binding.register.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                    Toast.makeText(this, "You are already a Team Leader, you need to disband your current Team", Toast.LENGTH_SHORT).show()
                }else if(isMemberOwn == 1 && isLeader == -1){
                    binding.register.setTextColor(Color.parseColor("#808080"))
                    binding.register.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                    Toast.makeText(this, "You are already a Member of another Team, you need to leave your current Team", Toast.LENGTH_SHORT).show()
                }else{
                    scope.launch {
                        UpdateUI(UID, userID)
                    }
                }
            }
                else {
                if (binding.register.text == "                 Save Changes                 ") {
                    if (binding.teamnameedit.text?.isNotEmpty() == true) {
                        binding.teamnameedit.error = null
                        val teamName = binding.teamnameedit.text.toString()
                        if (userID != null && UID != null) {
                                    binding.ResetLink.visibility = View.GONE
                                    flag = 0
                                    val teamsRef = db.collection("Hackathons").document(UID)
                                        .collection("Teams Participated")
                                    val teamData =mapOf(
                                        "Team Name" to teamName
                                    )
                                    teamsRef.document(userID).update(teamData)
                                        .addOnSuccessListener {
                                            Toast.makeText(
                                                this,
                                                "Saved Changes",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            scope.launch {
                                                UpdateUI(UID, userID)
                                            }
                                        }
                        }

                    } else {
                        binding.teamnameedit.error = "Required Field"
                    }
                } else {
                    if (binding.teamnameedit.text?.isNotEmpty() == true) {
                        binding.teamnameedit.error = null
                        val teamName = binding.teamnameedit.text.toString()
                        if (userID != null && UID != null) {

                            val userRef = db.collection("Users").document(userID)
                            val participatedHackathonsRef =
                                userRef.collection("Participated Hackathons")
                            participatedHackathonsRef.document(UID).set(hashMapOf<String, Any>())
                                .addOnSuccessListener {
                                    createLink(UID, userID, teamName,
                                        onSuccess = { link ->
                                            val teamsRef = db.collection("Hackathons").document(UID)
                                                .collection("Teams Participated")
                                            val teamData = hashMapOf(
                                                "Team Name" to teamName,
                                                "Invite Link" to link
                                            )
                                            teamsRef.document(userID).set(teamData)
                                                .addOnSuccessListener {
                                                    Toast.makeText(
                                                        this,
                                                        "Registration successful",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    scope.launch {
                                                        UpdateUI(UID, userID)
                                                    }
                                                }
                                        })
                                }
                        }

                    } else {
                        binding.teamnameedit.error = "Required Field"
                    }
                }
            }
            }
        binding.delete.setOnClickListener {
            if (UID != null && userID != null) {
                if (isLeader == 1) {
                    val dialogView = LayoutInflater.from(this).inflate(R.layout.deleteteam, null)
                    val builder = AlertDialog.Builder(this)
                        .setView(dialogView)
                        .setCancelable(true)
                    val alertDialog = builder.create()
                    val btnYes = dialogView.findViewById<Button>(R.id.btn_yes)
                    val btnNo = dialogView.findViewById<Button>(R.id.btn_no)
                    btnYes.setOnClickListener {

                        lifecycleScope.launch{
                            deleteTeam(UID, userID){message->
                                invite=null
                                LeaderUID=null
                                Toast.makeText(this@DetailsRegistration, "$message", Toast.LENGTH_SHORT).show()
                                scope.launch{
                                    UpdateUI(UID,userID)
                                }
                            }

                        }
                        alertDialog.dismiss()
                    }

                    btnNo.setOnClickListener {
                        alertDialog.dismiss()
                    }
                    alertDialog.show()
                } else {
                    val dialogView = LayoutInflater.from(this).inflate(R.layout.leaveteam, null)
                    val builder = AlertDialog.Builder(this)
                        .setView(dialogView)
                        .setCancelable(true)
                    val alertDialog = builder.create()
                    val btnYes = dialogView.findViewById<Button>(R.id.btn_yes)
                    val btnNo = dialogView.findViewById<Button>(R.id.btn_no)
                    btnYes.setOnClickListener {
                        val participantsRef = LeaderUID?.let {
                            db.collection("Hackathons").document(UID)
                                .collection("Teams Participated").document(LeaderUID!!)
                                .collection("Participants").document(userID)
                        }
                        participantsRef?.delete()?.addOnSuccessListener {
                            val participatedRef = db.collection("Users").document(userID)
                                .collection("Participated Hackathons")
                            participatedRef.document(UID).delete()
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Team left Successfully !!", Toast.LENGTH_SHORT).show()
                                    invite=null
                                    scope.launch{
                                        UpdateUI(UID,userID)
                                    }
                                }
                        }
                        alertDialog.dismiss()
                    }

                    btnNo.setOnClickListener {
                        alertDialog.dismiss()
                    }
                    alertDialog.show()

                    }
                }
        }
        binding.edit.setOnClickListener {
            flag=1
            getData(LeaderUID)
            setupRecyclerView()
            binding.register.text = "                 Save Changes                 "
            binding.register.setTextColor(Color.parseColor("#FFFFFF"))
            binding.register.backgroundTintList =
                ColorStateList.valueOf(Color.parseColor("#4CAF50"))
            binding.register.isEnabled = true
            binding.register.visibility = View.VISIBLE
            binding.teamnameedit.visibility = View.VISIBLE
            binding.teamnameedit.setText(teamnameh)
            binding.teamnameediterror.visibility = View.VISIBLE
            binding.teamname.visibility = View.GONE
            binding.ResetLink.visibility = View.VISIBLE
        }
        binding.emaillead.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:")
            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(binding.leaderemail.text.toString()))
            startActivity(intent)
        }
        binding.back.setOnClickListener {
            val intent = Intent(this, HackathonPage::class.java)
            intent.putExtra("UID", UID)
            startActivity(intent)
            finish()
        }
    }
    override fun onBackPressed() {
        super.onBackPressed()
        val UID = intent.getStringExtra("UID")
        val intent = Intent(this, HackathonPage::class.java)
        intent.putExtra("UID", UID)
        startActivity(intent)
        finish()
    }
    private suspend fun CheckStatus(UID: String?, userID: String?, onComplete: () -> Unit) {
        val db = Firebase.firestore
        if (UID != null && userID != null) {
            isLeader = -1
            isMemberOwn = -1
            isMemberHack = -1

            try {
                val participatedDoc = db.collection("Users")
                    .document(userID)
                    .collection("Participated Hackathons")
                    .document(UID).get().await()

                if (participatedDoc.exists()) {
                    isMemberOwn = 1
                }

                val leaderDoc = db.collection("Hackathons").document(UID)
                    .collection("Teams Participated")
                    .document(userID).get().await()

                if (leaderDoc.exists()) {
                    LeaderUID = userID
                    isLeader = 1
                }


                val teamSnapshot = db.collection("Hackathons").document(UID)
                    .collection("Teams Participated").get().await()

                for (teamDoc in teamSnapshot.documents) {
                    val participantsRef = db.collection("Hackathons").document(UID)
                        .collection("Teams Participated").document(teamDoc.id)
                        .collection("Participants").document(userID)

                    val teamData = participantsRef.get().await()
                    if (teamData.exists()) {
                        isMemberHack = 1
                        LeaderUID = teamDoc.id
                        break
                    }
                }

                onComplete()

            } catch (e: Exception) {}
        }
    }
    private suspend fun UpdateUI(UID:String?,userID:String?) {
        val db = Firebase.firestore
        if (UID != null && userID != null) {
            CheckStatus(UID, userID) {
                if (invite == null) {
                    if ((isMemberHack == 1 && isMemberOwn == -1) || (isMemberOwn == 1 && isMemberHack == 1 && isLeader == 1)) {
                        val participantsRef = LeaderUID?.let {
                            db.collection("Hackathons").document(UID)
                                .collection("Teams Participated").document(it)
                                .collection("Participants").document(userID)
                        }
                        participantsRef?.delete()?.addOnSuccessListener {
                            scope.launch {
                                UpdateUI(UID, userID)
                            }
                        }
                    }
                    if (isMemberHack == -1 && isMemberOwn == 1 && isLeader == -1) {
                        val participatedRef = db.collection("Users").document(userID)
                            .collection("Participated Hackathons")
                        participatedRef.document(UID).delete()
                            .addOnSuccessListener {
                                isMemberOwn = -1
                                scope.launch {
                                    UpdateUI(UID, userID)
                                }
                            }
                    }
                    if (isLeader == 1 && isMemberOwn == -1 && isMemberHack == -1) {

                        val userRef = db.collection("Users").document(userID)
                        userRef.collection("Participated Hackathons").document(UID)
                            .set(hashMapOf<String, Any>())
                            .addOnSuccessListener {
                                isMemberOwn == 1
                                scope.launch {
                                    UpdateUI(UID, userID)
                                }
                            }
                    }
                    if (isLeader == -1 && isMemberOwn == 1 && isMemberHack == 1) {
                        binding.delete.visibility = View.VISIBLE
                        binding.delete.setImageDrawable(getDrawable(R.drawable.leave))
                        binding.edit.visibility = View.GONE
                        binding.section2.visibility = View.GONE
                        binding.section2B.visibility = View.GONE
                        binding.section2A.visibility = View.GONE
                        binding.teamnameedit.visibility = View.GONE
                        binding.teamnameedit.setText("")
                        binding.teamnameediterror.visibility = View.GONE
                        binding.teamname.visibility = View.VISIBLE
                        binding.bar1.visibility = View.GONE
                        binding.section4.visibility = View.VISIBLE
                        binding.members.visibility = View.VISIBLE
                        binding.register.visibility = View.GONE
                        val userRef =
                            LeaderUID?.let {
                                FirebaseFirestore.getInstance().collection("Users").document(it)
                            }
                        if (userRef != null) {
                            userRef.get().addOnSuccessListener { document ->
                                if (document.exists()) {
                                    binding.leadername.text = document.getString("Username")
                                    binding.leaderemail.text = document.getString("Email")
                                    dplink=document.getString("Profile Picture URL")
                                    if (!document.getString("Profile Picture URL")
                                            .isNullOrEmpty()
                                    ) {
                                        Glide.with(this)
                                            .load(document.getString("Profile Picture URL"))
                                            .placeholder(R.drawable.defaultdp).centerCrop()
                                            .into(binding.leaderimage)
                                    }
                                }
                            }
                        }
                        val teamsRef = db.collection("Hackathons").document(UID)
                            .collection("Teams Participated")
                        LeaderUID?.let {
                            teamsRef.document(it).get()
                                .addOnSuccessListener { document ->
                                    if (document.exists()) {
                                        binding.teamname.text = document.getString("Team Name")
                                        binding.link.text = document.getString("Invite Link")
                                    }
                                }
                            getData(it)
                            setupRecyclerView()
                        }
                    }
                    if (isLeader == -1 && isMemberOwn == -1 && isMemberHack == -1) {
                        binding.delete.visibility = View.GONE
                        binding.edit.visibility = View.GONE
                        binding.section2.visibility = View.VISIBLE
                        binding.section2A.visibility = View.VISIBLE
                        binding.section2B.visibility = View.GONE
                        binding.teamnameedit.visibility = View.VISIBLE
                        binding.teamnameedit.setText("")
                        binding.teamnameediterror.visibility = View.VISIBLE
                        binding.teamname.visibility = View.GONE
                        binding.bar1.visibility = View.VISIBLE
                        binding.section4.visibility = View.GONE
                        binding.members.visibility = View.GONE
                        binding.register.visibility = View.VISIBLE
                        binding.register.text = "                 Register Team                 "
                        binding.register.setTextColor(Color.parseColor("#036261"))
                        binding.register.backgroundTintList =
                            ColorStateList.valueOf(Color.parseColor("#BAF0EF"))
                        binding.link.text = "Click Register to get Link"
                        val userRef =
                            FirebaseFirestore.getInstance().collection("Users").document(userID)
                        userRef.get().addOnSuccessListener { document ->
                            if (document.exists()) {
                                dplink=document.getString("Profile Picture URL")
                                binding.leadername.text = document.getString("Username")
                                binding.leaderemail.text = document.getString("Email")
                                if (!document.getString("Profile Picture URL").isNullOrEmpty()) {
                                    Glide.with(this).load(document.getString("Profile Picture URL"))
                                        .placeholder(R.drawable.defaultdp).centerCrop()
                                        .into(binding.leaderimage)
                                }
                            }
                        }

                    }
                    if (isMemberOwn == 1 && isLeader == 1) {
                        if (isMemberHack == -1) {
                            binding.delete.visibility = View.VISIBLE
                            binding.edit.visibility = View.VISIBLE
                            binding.delete.setImageDrawable(getDrawable(R.drawable.ic_menu_delete))
                            binding.section2.visibility = View.VISIBLE
                            binding.section2A.visibility = View.VISIBLE
                            binding.section2B.visibility = View.GONE
                            binding.teamnameedit.visibility = View.GONE
                            binding.teamnameedit.setText("")
                            binding.teamnameediterror.visibility = View.GONE
                            binding.teamname.visibility = View.VISIBLE
                            binding.bar1.visibility = View.VISIBLE
                            binding.section4.visibility = View.VISIBLE
                            binding.members.visibility = View.VISIBLE
                            binding.register.visibility = View.GONE
                            val userRef =
                                FirebaseFirestore.getInstance().collection("Users").document(userID)
                            userRef.get().addOnSuccessListener { document ->
                                if (document.exists()) {
                                    dplink=document.getString("Profile Picture URL")
                                    binding.leadername.text = document.getString("Username")
                                    binding.leaderemail.text = document.getString("Email")
                                    if (!document.getString("Profile Picture URL")
                                            .isNullOrEmpty()
                                    ) {
                                        Glide.with(this)
                                            .load(document.getString("Profile Picture URL"))
                                            .placeholder(R.drawable.defaultdp).centerCrop()
                                            .into(binding.leaderimage)
                                    }

                                }
                            }
                            val teamsRef =
                                db.collection("Hackathons").document(UID)
                                    .collection("Teams Participated")
                            teamsRef.document(userID).get()
                                .addOnSuccessListener { document ->
                                    if (document.exists()) {
                                        binding.teamname.text = document.getString("Team Name")
                                        teamnameh = document.getString("Team Name")
                                        binding.link.text = document.getString("Invite Link")
                                    }
                                }
                            getData(userID)
                            setupRecyclerView()

                        }
                    }
                } else {
                    binding.delete.visibility = View.GONE
                    binding.edit.visibility = View.GONE
                    binding.section2A.visibility = View.GONE
                    binding.section2.visibility = View.VISIBLE
                    binding.section2B.visibility = View.VISIBLE
                    binding.teamnameedit.visibility = View.GONE
                    binding.teamnameedit.setText("")
                    binding.teamnameediterror.visibility = View.GONE
                    binding.teamname.visibility = View.VISIBLE
                    binding.bar1.visibility = View.VISIBLE
                    binding.section4.visibility = View.VISIBLE
                    binding.members.visibility = View.VISIBLE
                    binding.register.visibility = View.VISIBLE
                    binding.register.text = "                 Accept Invite                 "
                    binding.register.setTextColor(Color.parseColor("#FFFFFF"))
                    binding.register.backgroundTintList =
                        ColorStateList.valueOf(Color.parseColor("#4CAF50"))
                    val userRef =
                        invite?.let {
                            FirebaseFirestore.getInstance().collection("Users").document(it)
                        }
                    if (userRef != null) {
                        userRef.get().addOnSuccessListener { document ->
                            if (document.exists()) {
                                dplink=document.getString("Profile Picture URL")
                                binding.leaderinvite.text = document.getString("Username")
                                binding.leadername.text = document.getString("Username")
                                binding.leaderemail.text = document.getString("Email")
                                if (!document.getString("Profile Picture URL")
                                        .isNullOrEmpty()
                                ) {
                                    Glide.with(this)
                                        .load(document.getString("Profile Picture URL"))
                                        .placeholder(R.drawable.defaultdp).centerCrop()
                                        .into(binding.leaderimage)
                                }
                            }
                        }
                    }
                    val teamsRef =
                        db.collection("Hackathons").document(UID)
                            .collection("Teams Participated")
                    invite?.let {
                        teamsRef.document(it).get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    binding.teamname.text = document.getString("Team Name")
                                }
                            }
                        getData(it)
                        setupRecyclerView()
                    }
                }
            }
        }
    }
    private fun createLink(UID: String, userID: String, teamname: String, onSuccess: (String) -> Unit) {
        val link = Uri.parse("https://hackverse.page.link/hackathon?uid=$UID&userID=$userID")
        val imageUrl = if (!iurl.isNullOrEmpty()) {
            Uri.parse(iurl)
        } else {
            Uri.parse("android.resource://${BuildConfig.APPLICATION_ID}/${R.drawable.logo}")
        }
        Firebase.dynamicLinks
            .createDynamicLink()
            .setLink(link)
            .setDomainUriPrefix("https://hackverse.page.link")
            .setAndroidParameters(
                DynamicLink.AndroidParameters.Builder("com.example.hackverse")
                    .build()
            )
            .setSocialMetaTagParameters(
                DynamicLink.SocialMetaTagParameters.Builder()
                    .setTitle("Join $nameh Team - $teamname")
                    .setDescription("Join Now!")
                    .setImageUrl(imageUrl)
                    .build()
            )
            .buildShortDynamicLink()
            .addOnCompleteListener { task: Task<ShortDynamicLink> ->
                if (task.isSuccessful) {
                    val shortLink = task.result?.shortLink.toString()
                    onSuccess(shortLink)
                }
            }
    }
    private fun setupRecyclerView() {
        val UID = intent.getStringExtra("UID")
        adapter = ParticipantsListAdapter(memberList,flag,LeaderUID,UID)
        binding.members.layoutManager = LinearLayoutManager(this)
        binding.members.adapter = adapter
    }
    private fun getData(leader: String?) {
        val db = Firebase.firestore
        val UID = intent.getStringExtra("UID")
        if (leader != null && UID != null) {
            memberList.clear()
            db.collection("Hackathons").document(UID)
                .collection("Teams Participated").document(leader)
                .collection("Participants")
                .get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        memberList.add(document.id)
                    }
                    adapter.notifyDataSetChanged()
                }
        }
    }
    private suspend fun deleteTeam(UID: String, LeaderUID: String, onSuccess: (String) -> Unit) {
        val db = Firebase.firestore
        try {
            val participantsRef = db.collection("Hackathons").document(UID)
                .collection("Teams Participated").document(LeaderUID)
                .collection("Participants")
            val participantsSnapshot = participantsRef.get().await()

            participantsSnapshot.forEach { document ->
                val userID = document.id
                val userHackathonRef = db.collection("Users").document(userID)
                    .collection("Participated Hackathons").document(UID)
                userHackathonRef.delete().await()
            }

            val participantsSnapshot2 = participantsRef.get().await()
            for (document in participantsSnapshot2.documents) {
                document.reference.delete().await()
            }

            val leaderRef = db.collection("Hackathons").document(UID)
                .collection("Teams Participated").document(LeaderUID)
            leaderRef.delete().await()

            val participatedRef = db.collection("Users").document(LeaderUID)
                .collection("Participated Hackathons").document(UID)
            participatedRef.delete().await()

            onSuccess("Team deleted successfully")

        } catch (e: Exception) {}
    }
    }
