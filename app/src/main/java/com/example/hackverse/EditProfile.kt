package com.example.hackverse

import android.app.Activity
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.hackverse.databinding.EditprofileBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import java.util.Calendar

class EditProfile : AppCompatActivity() {
    private lateinit var binding: EditprofileBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private var selectedImageUri: Uri? = null
    private val IMAGE_PICK_CODE = 104
    private val PERMISSION_CODE = 105
    private var flag=0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.my_primary)
        binding = EditprofileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseAuth = FirebaseAuth.getInstance()
        val user = firebaseAuth.currentUser
        val userID = user?.uid
        var name=""
        var dob=""
        var profileImageUrl = ""
        val db = FirebaseFirestore.getInstance()
        val backopt=intent.getIntExtra("backopt",1)
        binding.save.visibility = View.GONE
        if (userID == null) {
            val intent = Intent(this, opening::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }else{
            db.collection("Users").document(userID).get().addOnSuccessListener {
                if (it!= null) {
                    binding.name.setText(it.getString("Username"))
                    binding.email.setText(it.getString("Email"))
                    binding.dob.setText(it.getString("Date_of_Birth"))
                    name = it.getString("Username").toString()
                    dob = it.getString("Date_of_Birth").toString()
                    binding.name.setSelection(binding.name.length())
                    binding.dob.setSelection(binding.dob.length())
                    if (it.getString("Profile Picture URL") != null) {
                        profileImageUrl = it.getString("Profile Picture URL").toString()
                        Glide.with(this).load(it.getString("Profile Picture URL"))
                            .placeholder(R.drawable.placeholder2).into(binding.profilePicture)
                    }
                    binding.save.visibility = View.GONE
                }
            }
        }
        binding.dob.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val todayInMillis = calendar.timeInMillis
            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                binding.dob.setText(formattedDate)
            },
                year,
                month,
                day
            )
            datePickerDialog.datePicker.maxDate = (todayInMillis-(13*365.25*24*60*60*1000).toLong())
            datePickerDialog.show()
        }
        binding.name.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.save.visibility = if (binding.name.text.toString() != name || binding.dob.text.toString() != dob) View.VISIBLE else View.GONE
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.dob.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.save.visibility = if (binding.name.text.toString() != name || binding.dob.text.toString() != dob) View.VISIBLE else View.GONE
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                binding.save.visibility = View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        binding.back.setOnClickListener {
            var intent: Intent
            if(backopt==1) {
                intent = Intent(this, Homepage::class.java)
            }else if(backopt==2){
                intent = Intent(this, Search::class.java)
            }else if(backopt==3){
                intent = Intent(this, MyHackathons::class.java)
            }else if(backopt==4){
                intent = Intent(this, com.example.hackverse.Calendar::class.java)
            }else{
                intent = Intent(this, Homepage::class.java)
            }
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
        binding.save.setOnClickListener {
            if (userID != null) {
                if(binding.name.text.toString().isEmpty()){
                    binding.nameInput.error="Name cannot be empty"
                    Toast.makeText(this,"Name cannot be empty", Toast.LENGTH_SHORT).show()
                }
                if(binding.dob.text.toString().isEmpty()){
                    binding.dobInput.error="Date of Birth cannot be empty"
                    Toast.makeText(this,"Date of Birth cannot be empty", Toast.LENGTH_SHORT).show()
                }
                if(binding.name.text.toString().isNotEmpty()&&binding.dob.text.toString().isNotEmpty()){
                    binding.dob.isEnabled=false
                    binding.name.isEnabled=false
                    binding.nameInput.error=null
                    binding.dobInput.error=null
                    binding.save.text="Saving..."
                    binding.save.setTextColor(Color.parseColor("#808080"))
                    binding.save.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                    binding.save.isEnabled = false
                    binding.save.isClickable = false
                    binding.save.icon = null
                    db.collection("Users").document(userID).update("Username",binding.name.text.toString(),"Date_of_Birth",binding.dob.text.toString())
                        .addOnSuccessListener {
                            if(flag==0){
                            binding.save.visibility=View.GONE
                            Toast.makeText(this,"Profile Updated", Toast.LENGTH_SHORT).show()
                                binding.save.text="Save Details"
                                binding.save.setTextColor(Color.parseColor("#FFFFFF"))
                                binding.save.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
                                binding.save.isEnabled = true
                                binding.save.isClickable = true
                                binding.save.icon = getDrawable(R.drawable.save)
                                binding.name.isFocusable = true
                                binding.name.isEnabled = true
                                binding.dob.isEnabled = true
                                binding.name.setSelection(binding.name.length())
                                db.collection("Users").document(userID).get().addOnSuccessListener {
                                    if (it!= null) {
                                        binding.name.setText(it.getString("Username"))
                                        binding.email.setText(it.getString("Email"))
                                        binding.dob.setText(it.getString("Date_of_Birth"))
                                        name = it.getString("Username").toString()
                                        dob = it.getString("Date_of_Birth").toString()
                                        binding.name.setSelection(binding.name.length())
                                        binding.dob.setSelection(binding.dob.length())
                                        if (it.getString("Profile Picture URL") != null) {
                                            profileImageUrl = it.getString("Profile Picture URL").toString()
                                            Glide.with(this).load(it.getString("Profile Picture URL"))
                                                .placeholder(R.drawable.placeholder2).into(binding.profilePicture)
                                        }
                                        binding.save.visibility = View.GONE
                                    }
                                }
                                }else{
                                val contentResolver: ContentResolver = contentResolver
                                selectedImageUri?.let { it1 ->
                                    uploadImageToCloudinary(
                                        it1,
                                        contentResolver,
                                        userID
                                    ) { url ->
                                        if (url != null) {
                                            db.collection("Users").document(userID)
                                                .update("Profile Picture URL", url)
                                            Toast.makeText(
                                                this,
                                                "Profile Updated",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            binding.save.text="Save Details"
                                            binding.save.setTextColor(Color.parseColor("#FFFFFF"))
                                            binding.save.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
                                            binding.save.isEnabled = true
                                            binding.save.isClickable = true
                                            binding.name.isFocusable = true
                                            binding.name.isEnabled = true
                                            binding.dob.isEnabled = true
                                            binding.name.setSelection(binding.name.length())
                                            binding.dob.setSelection(binding.dob.length())
                                            binding.save.icon = getDrawable(R.drawable.save)
                                            binding.save.visibility=View.GONE
                                            db.collection("Users").document(userID).get().addOnSuccessListener {
                                                if (it!= null) {
                                                    binding.name.setText(it.getString("Username"))
                                                    binding.email.setText(it.getString("Email"))
                                                    binding.dob.setText(it.getString("Date_of_Birth"))
                                                    name = it.getString("Username").toString()
                                                    dob = it.getString("Date_of_Birth").toString()
                                                    binding.name.setSelection(binding.name.length())
                                                    if (it.getString("Profile Picture URL") != null) {
                                                        profileImageUrl = it.getString("Profile Picture URL").toString()
                                                        Glide.with(this).load(it.getString("Profile Picture URL"))
                                                            .placeholder(R.drawable.placeholder2).into(binding.profilePicture)
                                                    }
                                                    binding.save.visibility = View.GONE
                                                }
                                            }
                                            Toast.makeText(this,"Profile Updated", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }

                }
            }

        }
        binding.profilePicture.setOnClickListener {

            val dialog = Dialog(this)
            dialog.setContentView(R.layout.dppopup)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

            val viewProfilePicture = dialog.findViewById<TextView>(R.id.view_profile_picture)
            val changeProfilePicture = dialog.findViewById<TextView>(R.id.change_profile_picture)
            viewProfilePicture.setOnClickListener {
                dialog.dismiss()
                if(profileImageUrl!="") {
                    viewdp(profileImageUrl).show(supportFragmentManager, "dp_popup")
                }
            }
            changeProfilePicture.setOnClickListener {
                dialog.dismiss()
                checkPermOpenDialog()
            }

            dialog.show()
        }
        binding.deleteProfile.setOnClickListener {
            showDeleteConfirmationDialog(firebaseAuth)
        }
    }
    override fun onBackPressed() {
        val backopt = intent.getIntExtra("backopt", 1)
        Log.d("EditProfile", "backopt value: $backopt")
        var intent: Intent
        if (backopt == 1) {
            intent = Intent(this, Homepage::class.java)
        } else if (backopt == 2) {
            intent = Intent(this, Search::class.java)
        } else if (backopt == 3) {
            intent = Intent(this, MyHackathons::class.java)
        } else if (backopt == 4) {
            intent = Intent(this, com.example.hackverse.Calendar::class.java)
        } else {
            intent = Intent(this, Homepage::class.java)
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        super.onBackPressed()
        finish()
    }
    private fun showDeleteConfirmationDialog(firebaseAuth: FirebaseAuth) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.deleteddialogue, null)
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
        val alertDialog = builder.create()
        val btnYes = dialogView.findViewById<Button>(R.id.btn_yes)
        val btnNo = dialogView.findViewById<Button>(R.id.btn_no)
        btnYes.setOnClickListener {
            deleteUserAccount(firebaseAuth)
            alertDialog.dismiss()
        }

        btnNo.setOnClickListener {
            alertDialog.dismiss()
        }
        alertDialog.show()
    }
    private fun deleteUserAccount(firebaseAuth: FirebaseAuth) {
        val user: FirebaseUser? = firebaseAuth.currentUser
        val db = Firebase.firestore

        if (user != null) {
            val userID = user.uid
            user.delete().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    db.collection("Users").document(userID).delete()
                    Toast.makeText(this, "Account deleted successfully!", Toast.LENGTH_SHORT).show()
                    firebaseAuth.signOut()
                    val intent = Intent(this, opening::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Failed to delete account: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "No user is currently signed in.", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "Profile Picture Updated !!i", Toast.LENGTH_SHORT).show()
                flag++
                binding.save.visibility = View.VISIBLE
                Glide.with(this).load(selectedImageUri).centerCrop().into(binding.profilePicture)
            }
        }
    }
    private fun uploadImageToCloudinary(uri: Uri, contentResolver: ContentResolver, UID: String?=null, onSuccess: (String?) -> Unit) {
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