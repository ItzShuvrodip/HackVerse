package com.example.hackverse

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hackverse.databinding.SignuppageBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.firestore
import java.util.Calendar


class SignUpPage : AppCompatActivity() {
    private lateinit var binding: SignuppageBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = SignuppageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        val db = Firebase.firestore

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.logintext.setOnClickListener {
            val intent = Intent(this, LoginPage::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
        val dobEditText = binding.DOB
        dobEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val todayInMillis = calendar.timeInMillis
            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                dobEditText.setText(formattedDate)
                },
                year,
                month,
                day
            )
            datePickerDialog.datePicker.maxDate = (todayInMillis-(13*365.25*24*60*60*1000).toLong())
            datePickerDialog.show()
        }
        binding.buttonSignup.setOnClickListener {
            val email = binding.Email1.text.toString()
            val pass = binding.Password1.text.toString()
            val confirmPass = binding.Password2.text.toString()
            val username = binding.Username.text.toString()
            val dob = binding.DOB.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty()) {
                    if (pass == confirmPass) {

                        firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val user = firebaseAuth.currentUser
                                val userID = user?.uid
                                val userMap = hashMapOf(
                                    "UserID" to userID,
                                    "Username" to username,
                                    "Email" to email,
                                    "Date_of_Birth" to dob
                                )
                                if (userID != null)
                                {
                                    db.collection("Users").document(userID).set(userMap)
                                }
                                binding.verify.visibility = android.view.View.VISIBLE
                                user?.sendEmailVerification()
                                    ?.addOnCompleteListener { verifyTask ->
                                        if (verifyTask.isSuccessful) {
                                            Toast.makeText(
                                                this,
                                                "Verification email sent. Please verify your email.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            val verificationCheckThread = Thread {
                                                while (true) {
                                                    user.reload()
                                                    if (user.isEmailVerified) {
                                                        runOnUiThread {
                                                            Toast.makeText(
                                                                this,
                                                                "Email verified successfully!",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            val intent =
                                                                Intent(this, Homepage::class.java)
                                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                                            startActivity(intent)
                                                            finish()
                                                        }
                                                        break
                                                    }
                                                    Thread.sleep(3000)
                                                }
                                            }
                                            verificationCheckThread.start()
                                        }
                                    }
                            } else {
                                try {
                                    throw task.exception ?: Exception("Weak Password!")
                                } catch (e: FirebaseAuthWeakPasswordException) {
                                    Toast.makeText(this, "Weak Password!", Toast.LENGTH_SHORT).show()
                                } catch (e: FirebaseAuthInvalidCredentialsException) {
                                    Toast.makeText(this, "Invalid email format!", Toast.LENGTH_SHORT).show()
                                } catch (e: FirebaseAuthUserCollisionException) {
                                    Toast.makeText(this, "Email already in use!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(this, "Weak Password!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        Toast.makeText(this, "Password is not matching", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Empty Fields Are not Allowed !!", Toast.LENGTH_SHORT).show()

            }
        }
        binding.google.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    firebaseAuthWithGoogle(account.idToken!!)
                }
            } catch (_: ApiException) {}
        }
    }
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = firebaseAuth.currentUser
                val userID = user?.uid
                if (userID != null) {
                    val usersRef = Firebase.firestore.collection("Users").document(userID)
                    usersRef.get().addOnSuccessListener { document ->
                        if (!document.exists()) {
                            val userMap = hashMapOf(
                                "UserID" to userID,
                                "Username" to user.displayName,
                                "Email" to user.email,
                                "Profile Picture URL" to user.photoUrl.toString()
                            )
                            usersRef.set(userMap).addOnSuccessListener {
                                val intent =
                                    Intent(this, Homepage::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                startActivity(intent)
                                finish()
                            }
                        } else {
                            val intent =
                                Intent(this, Homepage::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(intent)
                            finish()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Google Sign-In failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

}