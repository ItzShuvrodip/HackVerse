package com.example.hackverse

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.hackverse.databinding.InformationBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import java.io.File
import android.graphics.pdf.PdfDocument
import androidx.recyclerview.widget.LinearLayoutManager
import java.io.FileOutputStream

class Information : AppCompatActivity() {
    private lateinit var binding: InformationBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var teamList: MutableList<String>
    private lateinit var adapter: TeamsListAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.my_primary)
        binding = InformationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseAuth = FirebaseAuth.getInstance()
        val user = firebaseAuth.currentUser
        val userID = user?.uid
        val db = Firebase.firestore
        val intent = intent
        teamList = mutableListOf()
        val UID = intent.getStringExtra("UID")
        adapter = TeamsListAdapter(teamList, UID)
        if (userID == null) {
            val intent = Intent(this, opening::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }
        getData()
        setupRecyclerView()

        binding.back.setOnClickListener {
            finish()
        }
        binding.download.setOnClickListener {
            teamDatatxt()
        }

    }
    private fun getData(){
        val db = Firebase.firestore
        val UID = intent.getStringExtra("UID")
        teamList.clear()
        if(UID!=null) {
            db.collection("Hackathons").document(UID)
                .collection("Teams Participated")
                .get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        teamList.add(document.id)
                    }
                    adapter.notifyDataSetChanged()
                }
        }
    }
    private fun setupRecyclerView() {
        val UID = intent.getStringExtra("UID")
        adapter = TeamsListAdapter(teamList,UID)
        binding.teams.layoutManager = LinearLayoutManager(this)
        binding.teams.adapter = adapter
    }
    private fun teamDatatxt() {
        val stringBuilder = StringBuilder()
        val db = FirebaseFirestore.getInstance()
        val intent = intent
        val UID = intent.getStringExtra("UID")
        var Title: String? = null

        if (UID != null) {
            db.collection("Hackathons").document(UID).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val title = document.getString("Title") ?: "Unknown Title"
                        stringBuilder.append("Hackathon Name: $title\n\n")
                        Title = title

                        val userRef = db.collection("Hackathons").document(UID)
                            .collection("Teams Participated")
                        userRef.get()
                            .addOnSuccessListener { querySnapshot ->
                                val totalTeams = querySnapshot.size()
                                var processedTeams = 0

                                querySnapshot.forEach { teamDoc ->
                                    val teamName = teamDoc.getString("Team Name") ?: "Unknown Team"
                                    val teamStringBuilder = StringBuilder(" Team: $teamName\n")
                                    db.collection("Users").document(teamDoc.id).get()
                                        .addOnSuccessListener { leaderDoc ->
                                            if (leaderDoc.exists()) {
                                                val leaderName = leaderDoc.getString("Username") ?: "Unknown Leader"
                                                val leaderEmail = leaderDoc.getString("Email") ?: "Unknown Email"
                                                teamStringBuilder.append(" ● Leader:\n")
                                                teamStringBuilder.append("   - $leaderName ($leaderEmail)\n")
                                            }
                                            val participantsRef = userRef.document(teamDoc.id).collection("Participants")
                                            participantsRef.get()
                                                .addOnSuccessListener { participantSnapshot ->
                                                    if (participantSnapshot.isEmpty) {
                                                        teamStringBuilder.append(" ● Members:\n   - None\n")
                                                    } else {
                                                        teamStringBuilder.append(" ● Members:\n")
                                                        val totalParticipants = participantSnapshot.size()
                                                        var processedParticipants = 0

                                                        participantSnapshot.forEach { participantDoc ->
                                                            db.collection("Users").document(participantDoc.id).get()
                                                                .addOnSuccessListener { participantData ->
                                                                    if (participantData.exists()) {
                                                                        val participantName = participantData.getString("Username") ?: "Unknown Participant"
                                                                        val participantEmail = participantData.getString("Email") ?: "Unknown Email"
                                                                        teamStringBuilder.append("   - $participantName ($participantEmail)\n")
                                                                    }
                                                                    processedParticipants++
                                                                    if (processedParticipants == totalParticipants) {
                                                                        synchronized(stringBuilder) {
                                                                            stringBuilder.append(teamStringBuilder.toString())
                                                                            stringBuilder.append("\n")
                                                                        }
                                                                        synchronized(processedTeams) {
                                                                            processedTeams++
                                                                            if (processedTeams == totalTeams) {
                                                                                val fileName = "$Title.pdf"
                                                                                saveToFile(stringBuilder.toString(), fileName)
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                        }
                                                    }
                                                    if (participantSnapshot.isEmpty) {
                                                        synchronized(stringBuilder) {
                                                            stringBuilder.append(teamStringBuilder.toString())
                                                            stringBuilder.append("\n")
                                                        }
                                                        synchronized(processedTeams) {
                                                            processedTeams++
                                                            if (processedTeams == totalTeams) {
                                                                val fileName = "$Title.pdf"
                                                                saveToFile(stringBuilder.toString(), fileName)
                                                            }
                                                        }
                                                    }
                                                }
                                        }
                                }
                                if (totalTeams == 0) {
                                    val fileName = "$Title.pdf"
                                    saveToFile(stringBuilder.toString(), fileName)
                                }
                            }
                    }
                }
        }
    }
    private fun saveToFile(content: String, fileName: String) {
        val pdfDocument = PdfDocument()
        var pageCount = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageCount).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        val paint = android.graphics.Paint()
        val textX = 20f
        var textY = 40f
        val lineHeight = paint.textSize + 6f

        val lines = content.lines()
        for (line in lines) {
            if (textY + lineHeight > pageInfo.pageHeight) {
                pdfDocument.finishPage(page)
                pageCount++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageCount).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                textY = 40f
            }
            canvas.drawText(line, textX, textY, paint)
            textY += lineHeight
        }

        pdfDocument.finishPage(page)

        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val pdfFile = File(downloadsDir, fileName)
            pdfDocument.writeTo(FileOutputStream(pdfFile))
            Toast.makeText(this, "Hackathon Details PDF saved to Downloads", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to save PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }
    }

}