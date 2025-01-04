package com.example.hackverse
import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.dynamiclinks.PendingDynamicLinkData
import com.google.firebase.dynamiclinks.dynamicLinks
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            installSplashScreen()
        }

        setTheme(R.style.Theme_HackVerse)
        setContentView(R.layout.splashscreen)

        checkDynamicLink()
    }

    private fun checkDynamicLink() {
        Firebase.dynamicLinks
            .getDynamicLink(intent)
            .addOnSuccessListener(this) { pendingDynamicLinkData: PendingDynamicLinkData? ->
                val deepLink: Uri? = pendingDynamicLinkData?.link
                val uid = deepLink?.getQueryParameter("uid")
                val invite=deepLink?.getQueryParameter("userID")

                if (uid != null) {
                    if(invite!=null){
                        lifecycleScope.launch {
                            delay(3000)
                            val homepageIntent = Intent(this@SplashScreen, Homepage::class.java)
                            homepageIntent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(homepageIntent)
                            val inviteIntent =
                                Intent(this@SplashScreen, DetailsRegistration::class.java)
                            inviteIntent.putExtra("UID", uid)
                            inviteIntent.putExtra("Invite", invite)
                            inviteIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(inviteIntent)
                            finish()
                        }

                    }else {
                        lifecycleScope.launch {
                            delay(3000)
                            val homepageIntent = Intent(this@SplashScreen, Homepage::class.java)
                            homepageIntent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(homepageIntent)
                            val hackathonIntent =
                                Intent(this@SplashScreen, HackathonPage::class.java)
                            hackathonIntent.putExtra("UID", uid)
                            hackathonIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(hackathonIntent)
                            finish()
                        }
                    }
                } else {
                    proceedToDefault()
                }
            }
            .addOnFailureListener {
                proceedToDefault()
            }
    }

    private fun proceedToDefault() {
        lifecycleScope.launch {
            delay(3000)
            val nextActivity = if (Firebase.auth.currentUser != null) {
                Homepage::class.java
            } else {
                opening::class.java
            }
            val intent = Intent(this@SplashScreen, nextActivity)
            val options = ActivityOptions.makeCustomAnimation(
                this@SplashScreen,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent, options.toBundle())
            finish()
        }
    }
}