package com.example.hackverse

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.hackverse.databinding.AddpageBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddPage : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: AddpageBinding
    private var selectedImageUri: Uri? = null
    private var uploadedUrl: String? = null
    private val IMAGE_PICK_CODE = 104
    private val PERMISSION_CODE = 105
    var flag= -1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(this, R.color.my_primary)

        binding = AddpageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseAuth = FirebaseAuth.getInstance()
        val db = Firebase.firestore
        val user = firebaseAuth.currentUser
        val userID = user?.uid
        var userName: String? = null
        if (userID == null) {
            val intent = Intent(this, opening::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }else {
            val userRef = FirebaseFirestore.getInstance().collection("Users").document(userID)
            userRef.get().addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    userName = documentSnapshot.getString("Username")
                }
            }
        }

        binding.back.setOnClickListener {
            finish()
        }
        val dateEditText = binding.date
        dateEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val todayInMillis = calendar.timeInMillis
            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                var formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                if(formattedDate==null)
                {
                    formattedDate = "01/01/2001"
                }
                val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
                val sdf2 = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val lastDateTime = if(!binding.lastdate.text.isNullOrEmpty()){
                    sdf.parse(binding.lastdate.text.toString())
                }else{
                    null
                }
                val eventDateTime = sdf2.parse(formattedDate)
                if(lastDateTime != null && lastDateTime.after(eventDateTime))
                {
                    binding.lastdate.text?.clear()
                    dateEditText.setText(formattedDate)
                    binding.lastdateerror.error = "Invalid Date"
                    Toast.makeText(this, "Registration Date should be before Event Date !!", Toast.LENGTH_LONG).show()
                }else {
                    dateEditText.setText(formattedDate)
                    binding.lastdateerror.error = null
                }
            },
                year,
                month,
                day
            )
            datePickerDialog.datePicker.minDate = todayInMillis
            datePickerDialog.show()
        }
        val lastdateEditText = binding.lastdate
        lastdateEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val todayInMillis = calendar.timeInMillis
            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                var formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                if(formattedDate==null)
                {
                    formattedDate = "01/01/2001"
                }
                lastdateEditText.setText(formattedDate)
                val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                    var formattedTime = if (selectedHour >= 12) {
                        String.format("%02d:%02d PM", if (selectedHour > 12) selectedHour - 12 else 12, selectedMinute)
                    } else {
                        String.format("%02d:%02d AM", if (selectedHour == 0) 12 else selectedHour, selectedMinute)
                    }
                    if(formattedTime==null)
                    {
                        formattedTime = "00:00 AM"
                    }
                    val formattedDateTime = "$formattedDate $formattedTime"
                    val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
                    val sdf2 = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val lastDateTime = sdf.parse(formattedDateTime)
                    val eventDateTime = if(!binding.date.text.isNullOrEmpty()) {
                        sdf2.parse(binding.date.text.toString())
                    }else{
                        null
                    }
                    if(eventDateTime != null && lastDateTime.after(eventDateTime))
                    {
                        lastdateEditText.setText("")
                        binding.lastdateerror.error = "Invalid Date"
                        Toast.makeText(this, "Registration Date should be before Event Date !!", Toast.LENGTH_LONG).show()
                    }else {
                        lastdateEditText.setText(formattedDateTime)
                        binding.lastdateerror.error = null
                    }
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)
                timePickerDialog.show()
            }, year, month, day)
            datePickerDialog.datePicker.minDate =todayInMillis
            datePickerDialog.show()
            }
        var fee: String? = null
        CloudinaryConfig()
        binding.free.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                fee = "Free"
                binding.paid.clearFocus()
                binding.paid.text?.clear()
            } else {
                fee = null
            }
        }
        binding.paid.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.free.isChecked = false
                fee = binding.paid.text.toString()
            }
        }
        binding.edit.setOnClickListener {
            checkPermOpenDialog()
        }
        binding.upload.setOnClickListener{
                        val title = binding.title.text.toString()
                        val organisingevent = binding.organisingevent.text.toString()
                        val organisingauthority = binding.organisingauthority.text.toString()
                        val venue = binding.venue.text.toString()
                        val date = binding.date.text.toString()
                        val lastdate = binding.lastdate.text.toString()
                        var size="0"
                        if(binding.size.text.isNullOrEmpty()) {
                            size="0"
                        }else{
                            size = binding.size.text.toString()
                        }
                        val eligibility = binding.eligibility.text.toString()
                        val overview = binding.overview.text.toString()
                        val eligibilityd = binding.eligibilityd.text.toString()
                        val structure = binding.structure.text.toString()
                        val judging = binding.judging.text.toString()
                        val reward = binding.reward.text.toString()
                        val name = binding.name.text.toString()
                        val number = binding.number.text.toString()
                        val email = binding.email.text.toString()

                        if (!binding.free.isChecked) {
                            fee = binding.paid.text.toString().takeIf { it.isNotEmpty() }
                        }
                        if (fee == null) {
                            binding.paiderror.error = "Required Field!"
                            binding.scroll.scrollTo(0, 0)
                            Toast.makeText(this, "All fields are Required !!", Toast.LENGTH_LONG).show()
                        } else {
                            binding.paiderror.error = null
                        }

                        if (title.isEmpty()) {
                            binding.titleerror.error = "Required Field!"
                            binding.scroll.scrollTo(0, 0)
                            Toast.makeText(this, "All fields are Required !!", Toast.LENGTH_LONG).show()
                        } else {
                            binding.titleerror.error = null
                        }
                        if (organisingevent.isEmpty()) {
                            binding.organisingeventerror.error = "Required Field!"
                            binding.scroll.scrollTo(0, 0)
                            Toast.makeText(this, "All fields are Required !!", Toast.LENGTH_LONG).show()
                        } else {
                            binding.organisingeventerror.error = null
                        }
                        if (organisingauthority.isEmpty()) {
                            binding.organisingauthorityerror.error = "Required Field!"
                            binding.scroll.scrollTo(0, 0)
                            Toast.makeText(this, "All fields are Required !!", Toast.LENGTH_LONG).show()
                        } else {
                            binding.organisingauthorityerror.error = null
                        }
                        if (venue.isEmpty()) {
                            binding.venueerror.error = "Required Field!"
                            binding.scroll.scrollTo(0, 0)
                            Toast.makeText(this, "All fields are Required !!", Toast.LENGTH_LONG).show()
                        } else {
                            binding.venueerror.error = null
                        }
                        if (size.isEmpty()) {
                            size="0"
                            binding.sizeerror.error = "Required Field!"
                            binding.scroll.scrollTo(0, 0)
                            Toast.makeText(this, "All fields are Required !!", Toast.LENGTH_LONG).show()
                        } else if(size.toInt()<1) {
                            size="0"
                            binding.sizeerror.error = "Minimum size is 1"
                            binding.scroll.scrollTo(0, 0)
                            Toast.makeText(this, "Size cannot be less than 1 !!", Toast.LENGTH_LONG).show()
                        }
                        else if(size==null){
                            size="0"
                            binding.sizeerror.error = "Required Field!"
                            binding.scroll.scrollTo(0, 0)
                            Toast.makeText(this, "All fields are Required !!", Toast.LENGTH_LONG).show()
                        }else{
                            size=binding.size.text.toString()
                            binding.sizeerror.error = null
                        }
                        if (eligibility.isEmpty()) {
                            binding.eligibilityerror.error = "Required Field!"
                            binding.scroll.scrollTo(0, 0)
                            Toast.makeText(this, "All fields are Required !!", Toast.LENGTH_LONG).show()
                        } else {
                            binding.eligibilityerror.error = null
                        }
                        if(size.toInt() <= 1){
                            size= "0"
                        }
                        if (date.isEmpty()) {
                            binding.dateerror.error = "Required Field!"
                            binding.scroll.scrollTo(0, 0)
                            Toast.makeText(this, "All fields are Required !!", Toast.LENGTH_LONG).show()
                        } else {
                            binding.dateerror.error = null
                        }
                        if (lastdate.isEmpty()||lastdate.length<15) {
                            binding.lastdateerror.error = "Required Field!"
                            binding.scroll.scrollTo(0, 0)
                            Toast.makeText(this, "All fields are Required !!", Toast.LENGTH_LONG).show()
                        } else {
                            binding.lastdateerror.error = null
                        }
                        if (overview.isEmpty()) {
                            binding.overviewerror.error = "Required Field!"
                            binding.scroll.scrollTo(0, 0)
                            Toast.makeText(this, "All fields are Required !!", Toast.LENGTH_LONG).show()
                        } else {
                            binding.overviewerror.error = null
                        }
                        if (eligibilityd.isEmpty()) {
                            binding.eligibilityderror.error = "Required Field!"
                            binding.scroll.scrollTo(0, 0)
                            Toast.makeText(this, "All fields are Required !!", Toast.LENGTH_LONG).show()
                        } else {
                            binding.eligibilityderror.error = null
                        }
                        if (structure.isEmpty()) {
                            binding.structureerror.error = "Required Field!"
                            binding.scroll.scrollTo(0, 0)
                            Toast.makeText(this, "All fields are Required !!", Toast.LENGTH_LONG).show()
                        } else {
                            binding.structureerror.error = null
                        }
                        if (judging.isEmpty()) {
                            binding.judgingerror.error = "Required Field!"
                            binding.scroll.scrollTo(0, 0)
                            Toast.makeText(this, "All fields are Required !!", Toast.LENGTH_LONG).show()
                        } else {
                            binding.judgingerror.error = null
                        }
                        if (reward.isEmpty()) {
                            binding.rewarderror.error = "Required Field!"
                            binding.scroll.scrollTo(0, 0)
                            Toast.makeText(this, "All fields are Required !!", Toast.LENGTH_LONG).show()
                        } else {
                            binding.rewarderror.error = null
                        }
                        if (name.isEmpty()) {
                            binding.nameerror.error = "Required Field!"
                            binding.scroll.scrollTo(0, 0)
                            Toast.makeText(this, "All fields are Required !!", Toast.LENGTH_LONG).show()
                        } else {
                            binding.nameerror.error = null
                        }
                        if (number.isEmpty()) {
                            binding.numbererror.error = "Required Field!"
                            binding.scroll.scrollTo(0, 0)
                            Toast.makeText(this, "All fields are Required !!", Toast.LENGTH_LONG).show()
                        } else {
                            binding.numbererror.error = null
                        }
                        if (email.isEmpty()) {
                            binding.emailerror.error = "Required Field!"
                            binding.scroll.scrollTo(0, 0)
                            Toast.makeText(this, "All fields are Required !!", Toast.LENGTH_LONG).show()
                        } else {
                            binding.emailerror.error = null
                        }
                        if ( size.toInt()>=1 && size!="" && size!=null && lastdate.length>15 && title.isNotEmpty() && fee != null && organisingevent.isNotEmpty() && organisingauthority.isNotEmpty() && venue.isNotEmpty() && date.isNotEmpty() && lastdate.isNotEmpty() && size.isNotEmpty() && eligibility.isNotEmpty() && overview.isNotEmpty() && eligibilityd.isNotEmpty() && structure.isNotEmpty() && judging.isNotEmpty() && reward.isNotEmpty() && name.isNotEmpty() && number.isNotEmpty() && email.isNotEmpty()) {
                            val hackathon = hashMapOf(
                                "UserID" to userID,
                                "Username" to userName,
                                "Title" to title,
                                "Organising Event" to organisingevent,
                                "Organising Authority" to organisingauthority,
                                "Venue" to venue,
                                "Date" to date,
                                "Last Date" to lastdate,
                                "Size" to size,
                                "Fee" to fee,
                                "Eligibility" to eligibility,
                                "Overview" to overview,
                                "Eligibility Details" to eligibilityd,
                                "Structure" to structure,
                                "Judging" to judging,
                                "Reward" to reward,
                                "Name" to name,
                                "Number" to number,
                                "Email" to email
                            )
                            db.collection("Hackathons").add(hackathon)
                                .addOnSuccessListener { documentReference ->
                                    val generatedId = documentReference.id
                                    db.collection("Hackathons").document(generatedId)
                                        .update("UID", generatedId)
                                    binding.upload.text="Uploading..."
                                    binding.upload.setTextColor(Color.parseColor("#808080"))
                                    binding.upload.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                                    binding.upload.isEnabled = false
                                    binding.upload.isClickable = false
                                    binding.upload.icon = null
                                    if(flag==-1||selectedImageUri==null)
                                    {
                                        Toast.makeText(this, "Hackathon added Successfully !!", Toast.LENGTH_LONG).show()
                                        val intent = Intent(this, Homepage::class.java)
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                        startActivity(intent)
                                        overridePendingTransition(0, 0)
                                    }
                                    else{
                                    val contentResolver: ContentResolver = contentResolver
                                    selectedImageUri?.let { it1 ->
                                        uploadImageToCloudinary(it1, contentResolver, generatedId) { url ->
                                            if (url != null) {
                                                db.collection("Hackathons").document(generatedId).update("Image URL", url)
                                                Toast.makeText(this, "Hackathon added Successfully !!", Toast.LENGTH_LONG).show()
                                                val intent = Intent(this, Homepage::class.java)
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                                startActivity(intent)
                                                overridePendingTransition(0, 0)
                                            }
                                        }
                                    }
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        this,
                                        "Hackathon Addition Failed !!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }

        }
    }
    private fun checkPermOpenDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES), PERMISSION_CODE)
            } else {
                pickImageFromChooser()
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_CODE)
            } else {
                pickImageFromChooser()
            }
        }
    }
    private fun pickImageFromChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png"))
        startActivityForResult(Intent.createChooser(intent, "Select Image"), IMAGE_PICK_CODE)
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickImageFromChooser()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE) {
            selectedImageUri = data?.data
            if (selectedImageUri != null) {
                Toast.makeText(this, "Hackathon Banner Updated !!", Toast.LENGTH_SHORT).show()
                val imageView = findViewById<ImageView>(R.id.display)
                flag = 1
                Glide.with(this).load(selectedImageUri).centerCrop().into(imageView)
            }
        }
    }
    private fun uploadImageToCloudinary(uri: Uri, contentResolver: ContentResolver,UID: String?=null, onSuccess: (String?) -> Unit) {
           contentResolver.openInputStream(uri)?.readBytes()?.let { byteArray ->
            val publicId = "HackathonBanner/${UID ?: "default_filename"}"
            MediaManager.get().upload(byteArray)
                .option("resource_type", "image")
                .option("public_id", publicId)
                .option("overwrite", true)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String?) {}
                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                    override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                        val imageUrl = resultData?.get("url")?.toString()
                        val secureImageUrl = if (imageUrl != null && imageUrl.startsWith("http://")) {
                            imageUrl.replace("http://", "https://")
                        } else {
                            imageUrl
                        }
                        onSuccess(secureImageUrl)
                    }
                    override fun onError(requestId: String?, error: ErrorInfo?) {}
                    override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
                }).dispatch()
        }
    }


}
