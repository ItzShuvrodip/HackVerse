package com.example.hackverse

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
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
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.setPadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.hackverse.databinding.HomepageBinding
import com.example.hackverse.databinding.NavHeaderHomepageBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.navigation.NavigationView
import com.google.firebase.Firebase
import android.content.Context
import android.content.SharedPreferences
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Homepage : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: HomepageBinding
    private lateinit var hackathonList1: MutableList<Hackathon>
    private lateinit var adapter1: HackathonBoxAdapter
    private lateinit var hackathonList2: MutableList<Hackathon>
    private lateinit var adapter2: HackathonBoxAdapter
    private lateinit var hackathonList3: MutableList<Hackathon>
    private lateinit var adapter3:HackathonShortAdapter
    private var profileImageUrl: String? = null
    private var selectedImageUri: Uri? = null
    private var uploadedUrl: String? = null
    private val IMAGE_PICK_CODE = 104
    private var isDrawerOpen = false
    private val PERMISSION_CODE = 105
    private var nameint=""
    private var emailint=""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val firebaseAuth = FirebaseAuth.getInstance()
        val db = Firebase.firestore
        val user = firebaseAuth.currentUser
        val userID = user?.uid
        db.firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()
        profileImageUrl= intent.getStringExtra("imgu")
        nameint = intent.getStringExtra("name") ?: ""
        emailint = intent.getStringExtra("email") ?: ""
        hackathonList1 = mutableListOf()
        adapter1= HackathonBoxAdapter(hackathonList1)
        hackathonList2 = mutableListOf()
        adapter2= HackathonBoxAdapter(hackathonList2)
        hackathonList3 = mutableListOf()
        adapter3= HackathonShortAdapter(hackathonList3)
        if (user == null) {
            val intent = Intent(this, opening::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }
        binding = HomepageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val imageView=findViewById<ImageView>(R.id.imageView)
        if (profileImageUrl != null||profileImageUrl!=""){
            Glide.with(this).load(profileImageUrl).placeholder(R.drawable.defaultdp).centerCrop().diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(binding.profile)
            Glide.with(this).load(profileImageUrl).placeholder(R.drawable.defaultdp).centerCrop().diskCacheStrategy(DiskCacheStrategy.ALL).into(imageView)
            binding.profile.visibility = View.VISIBLE
        }
        binding.profile.setOnClickListener {
                binding.profile.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(70)
                    .withEndAction {
                        binding.profile.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(70)
                            .withEndAction {
                                if (!isDrawerOpen) {
                                    openDrawer()
                                }else{
                                    closeDrawer()
                                }
                            }
                            .start()
                    }
                    .start()
            }
        binding.navHome.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.navHome.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val navHomeWidth = binding.navHome.width
                val colorSliderLayoutParams = binding.colorSlider.layoutParams
                colorSliderLayoutParams.width = navHomeWidth
                binding.colorSlider.layoutParams = colorSliderLayoutParams
            }
        })
        val nameview=findViewById<TextView>(R.id.nameview)
        val emailview=findViewById<TextView>(R.id.emailview)
        if(nameint!=""&&emailint!=""&&nameint!=null&&emailint!=null&&nameint.isNotEmpty()&&emailint.isNotEmpty()){
            nameview.text = nameint
            emailview.text = emailint
            binding.header.text = "Hi, $nameint"
        }
        if (userID != null) {
            db.collection("Users").document(userID).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("Username")
                    val email = document.getString("Email")
                    val url = document.getString("Profile Picture URL")
                    nameview.text = name
                    emailview.text = email
                    nameint=name.toString()
                    emailint=email.toString()
                    if(profileImageUrl==null||profileImageUrl=="") {
                        profileImageUrl = url
                        Glide.with(this).load(url).placeholder(R.drawable.defaultdp).centerCrop().diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(binding.profile)
                        Glide.with(this).load(url).placeholder(R.drawable.defaultdp).centerCrop().diskCacheStrategy(DiskCacheStrategy.ALL).into(imageView)
                    }
                    binding.profile.visibility = View.VISIBLE
                    binding.header.text = "Hi, $name"
                }else {
                    imageView.setImageResource(R.drawable.defaultdp)
                    nameview.text ="Unknown"
                    binding.header.text = "Hi, Unknown"
                    emailview.text = "abc@xyz.com"
                }
            }
        }
        imageView.setOnClickListener {
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.dppopup)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

            val viewProfilePicture = dialog.findViewById<TextView>(R.id.view_profile_picture)
            val changeProfilePicture = dialog.findViewById<TextView>(R.id.change_profile_picture)
            viewProfilePicture.setOnClickListener {
                dialog.dismiss()
                val viewdp = profileImageUrl?.let { it1 -> viewdp(it1) }
                if (viewdp != null) {
                    viewdp.show(supportFragmentManager, "dp_popup")
                }
            }
            changeProfilePicture.setOnClickListener {
                dialog.dismiss()
                checkPermOpenDialog()
            }

            dialog.show()
        }

        binding.colorSlider.x = binding.navHome.x
        binding.navSearch.setOnClickListener {
            binding.colorSlider.post {
                binding.colorSlider.animate()
                    .x(binding.navSearch.x)
                    .setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        binding.hometext.setTextColor(Color.parseColor("#2E2E2E"))
                        binding.searchtext.setTextColor(Color.parseColor("#036261"))
                        binding.homeicon.imageTintList=ColorStateList.valueOf(Color.parseColor("#2E2E2E"))
                        binding.searchicon.imageTintList= ColorStateList.valueOf(Color.parseColor("#036261"))
                        val intent = Intent(this,Search::class.java)
                        intent.putExtra("imgu",profileImageUrl)
                        intent.putExtra("draw",isDrawerOpen)
                        intent.putExtra("name",nameint)
                        intent.putExtra("email",emailint)
                        startActivity(intent)
                        overridePendingTransition(0,0)
                        finish()
                    }
                    .start()
            }
        }
        binding.navMyhackathons.setOnClickListener {
            binding.colorSlider.post {
                binding.colorSlider.animate()
                    .x(binding.navMyhackathons.x)
                    .setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        binding.hometext.setTextColor(Color.parseColor("#2E2E2E"))
                        binding.myhackathonstext.setTextColor(Color.parseColor("#036261"))
                        binding.homeicon.imageTintList=ColorStateList.valueOf(Color.parseColor("#2E2E2E"))
                        binding.myhackathonsicon.imageTintList= ColorStateList.valueOf(Color.parseColor("#036261"))
                        val intent = Intent(this,MyHackathons::class.java)
                        intent.putExtra("imgu",profileImageUrl)
                        intent.putExtra("draw",isDrawerOpen)
                        intent.putExtra("name",nameint)
                        intent.putExtra("email",emailint)
                        startActivity(intent)
                        overridePendingTransition(0,0)
                        finish()
                    }
                    .start()
            }
        }
        binding.navCalendar.setOnClickListener {
            binding.colorSlider.post {
                binding.colorSlider.animate()
                    .x(binding.navCalendar.x)
                    .setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        binding.hometext.setTextColor(Color.parseColor("#2E2E2E"))
                        binding.calendartext.setTextColor(Color.parseColor("#036261"))
                        binding.homeicon.imageTintList=ColorStateList.valueOf(Color.parseColor("#2E2E2E"))
                        binding.calendaricon.imageTintList= ColorStateList.valueOf(Color.parseColor("#036261"))
                        val intent = Intent(this,Calendar::class.java)
                        intent.putExtra("imgu",profileImageUrl)
                        intent.putExtra("draw",isDrawerOpen)
                        intent.putExtra("name",nameint)
                        intent.putExtra("email",emailint)
                        startActivity(intent)
                        overridePendingTransition(0,0)
                        finish()
                    }
                    .start()
            }
        }
        binding.bg.setOnClickListener {
            closeDrawer()
        }
        val drawState = intent?.getBooleanExtra("draw", false) ?: false
        isDrawerOpen = drawState
        if (isDrawerOpen) {
            binding.bg.visibility = View.VISIBLE
            binding.bg.alpha = 0.7f
            binding.customDrawer.translationX = 0f
        }
        else {
            binding.bg.visibility = View.GONE
            binding.bg.alpha = 0f
        }
        val edit=findViewById<TextView>(R.id.edit)
        val text = "Edit Profile"
        val spannableString = SpannableString(text)
        spannableString.setSpan(UnderlineSpan(), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        edit.text = spannableString
        edit.setOnClickListener {
            val intent = Intent(this,EditProfile::class.java)
            intent.putExtra("backopt",1)
            startActivity(intent)
            finish()
        }
        binding.navAdd.setOnClickListener {
            val scaleDownX = ObjectAnimator.ofFloat(binding.navAdd, "scaleX", 0.9f)
            val scaleDownY = ObjectAnimator.ofFloat(binding.navAdd, "scaleY", 0.9f)
            val scaleUpX = ObjectAnimator.ofFloat(binding.navAdd, "scaleX", 1f)
            val scaleUpY = ObjectAnimator.ofFloat(binding.navAdd, "scaleY", 1f)
            val scaleDown = AnimatorSet()
            scaleDown.playTogether(scaleDownX, scaleDownY)
            scaleDown.duration = 100
            val scaleUp = AnimatorSet()
            scaleUp.playTogether(scaleUpX, scaleUpY)
            scaleUp.duration = 100
            val animatorSet = AnimatorSet()
            animatorSet.playSequentially(scaleDown, scaleUp)
            animatorSet.start()
            binding.navAdd.postDelayed({
                val intent = Intent(this, AddPage::class.java)
                startActivity(intent)
            }, 150)
        }
        binding.options.setOnClickListener {
            showPopupMenu()
        }
        binding.colorSlider.post {
            binding.colorSlider.visibility = View.VISIBLE
            binding.navHome.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(200)
        }
        getData1()
        getData3()
        setupRecyclerView1()
        setupRecyclerView2()
        setupRecyclerView3()
    }
    override fun onBackPressed() {
        if (isDrawerOpen) {
            closeDrawer()
        } else {
            finish()
            super.onBackPressed()
        }
    }
    private fun setupRecyclerView1() {
        adapter1 = HackathonBoxAdapter(hackathonList1)
        binding.featured.layoutManager = GridLayoutManager(this, 2, GridLayoutManager.HORIZONTAL, false)
        binding.featured.adapter = adapter1
    }
    private fun getData1() {
        val db = Firebase.firestore
        hackathonList1.clear()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = db.collection("Hackathons").get().await()
                val openEvents = mutableSetOf<Hackathon>()
                for (document in result) {
                    val likes = try {
                        db.collection("Hackathons").document(document.id).collection("Likes").get()
                            .await().size()
                    } catch (e: Exception) {
                        0
                    }
                    val date = document.getString("Date") ?: "N/A"
                    val eligibility = document.getString("Eligibility") ?: "N/A"
                    val eligibilityDetails = document.getString("Eligibility Details") ?: "N/A"
                    val email = document.getString("Email") ?: "N/A"
                    val fee = document.getString("Fee") ?: "N/A"
                    val judging = document.getString("Judging") ?: "N/A"
                    val lastDate = document.getString("Last Date") ?: "N/A"
                    val name = document.getString("Name") ?: "N/A"
                    val number = document.getString("Number") ?: "N/A"
                    val organisingAuthority = document.getString("Organising Authority") ?: "N/A"
                    val organisingEvent = document.getString("Organising Event") ?: "N/A"
                    val overview = document.getString("Overview") ?: "N/A"
                    val reward = document.getString("Reward") ?: "N/A"
                    val size = document.getString("Size") ?: "N/A"
                    val structure = document.getString("Structure") ?: "N/A"
                    val title = document.getString("Title") ?: "N/A"
                    val uid = document.getString("UID") ?: "N/A"
                    val venue = document.getString("Venue") ?: "N/A"
                    val ImURL = document.getString("Image URL") ?: "N/A"
                    val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
                    val sdf2 = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val lastDateTime = sdf.parse(lastDate)
                    val eventDateTime = sdf2.parse(date)
                    val currentDateTime = Date()
                    if (lastDateTime.after(currentDateTime) && eventDateTime.after(currentDateTime)) {
                        openEvents.add(
                            Hackathon(
                                uid,
                                title,
                                organisingAuthority,
                                organisingEvent,
                                fee,
                                venue,
                                date,
                                lastDate,
                                size,
                                eligibility,
                                overview,
                                eligibilityDetails,
                                structure,
                                judging,
                                reward,
                                name,
                                number,
                                email,
                                ImURL,
                                likes
                            )
                        )
                    }
                withContext(Dispatchers.Main) {
                    hackathonList1.clear()
                    hackathonList1.addAll(openEvents.sortedByDescending { it.likes }.take(10))
                    adapter1.notifyDataSetChanged()
                }
            }
            }catch (_: Exception) {}
        }
    }
    private fun setupRecyclerView2() {
        adapter2 = HackathonBoxAdapter(hackathonList2)
        binding.recent.layoutManager = GridLayoutManager(this, 1, GridLayoutManager.HORIZONTAL, false)
        binding.recent.adapter = adapter2
    }
    private fun setupRecyclerView3() {
        adapter3 = HackathonShortAdapter(hackathonList3)
        binding.updates.layoutManager = LinearLayoutManager(this)
        binding.updates.adapter = adapter3
    }
    private fun getData2() {
        val db = Firebase.firestore
        val firebaseAuth = FirebaseAuth.getInstance()
        val user = firebaseAuth.currentUser
        val recentHackathons = mutableListOf<HackathonTime>()
        val userID = user?.uid
        hackathonList2.clear()

        if (userID != null) {
            db.collection("Users").document(userID).collection("Recents").get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        recentHackathons.add(
                            HackathonTime(
                                document.id,
                                document.getLong("timestamp") ?: 0
                            )
                        )
                    }
                    val rh=recentHackathons.sortedByDescending { it.timestamp }.take(8)
                    val hackathonsToDelete = recentHackathons.filter { it !in rh }
                    for (hackathon in hackathonsToDelete) {
                        db.collection("Users").document(userID).collection("Recents")
                            .document(hackathon.UID)
                            .delete()
                    }


                    val hackathonTasks = mutableListOf<Task<DocumentSnapshot>>()
                    for (hackathon in rh) {
                        val task = db.collection("Hackathons").document(hackathon.UID).get()
                            .addOnSuccessListener { document ->
                                val date = document.getString("Date") ?: "N/A"
                                val eligibility = document.getString("Eligibility") ?: "N/A"
                                val eligibilityDetails = document.getString("Eligibility Details") ?: "N/A"
                                val email = document.getString("Email") ?: "N/A"
                                val fee = document.getString("Fee") ?: "N/A"
                                val judging = document.getString("Judging") ?: "N/A"
                                val lastDate = document.getString("Last Date") ?: "N/A"
                                val name = document.getString("Name") ?: "N/A"
                                val number = document.getString("Number") ?: "N/A"
                                val organisingAuthority = document.getString("Organising Authority") ?: "N/A"
                                val organisingEvent = document.getString("Organising Event") ?: "N/A"
                                val overview = document.getString("Overview") ?: "N/A"
                                val reward = document.getString("Reward") ?: "N/A"
                                val size = document.getString("Size") ?: "N/A"
                                val structure = document.getString("Structure") ?: "N/A"
                                val title = document.getString("Title") ?: "N/A"
                                val uid = document.getString("UID") ?: "N/A"
                                val venue = document.getString("Venue") ?: "N/A"
                                val ImURL = document.getString("Image URL") ?: "N/A"

                                hackathonList2.add(
                                    Hackathon(
                                        uid,
                                        title,
                                        organisingAuthority,
                                        organisingEvent,
                                        fee,
                                        venue,
                                        date,
                                        lastDate,
                                        size,
                                        eligibility,
                                        overview,
                                        eligibilityDetails,
                                        structure,
                                        judging,
                                        reward,
                                        name,
                                        number,
                                        email,
                                        ImURL,
                                        0
                                    )
                                )
                            }
                        hackathonTasks.add(task)
                    }

                    Tasks.whenAllComplete(hackathonTasks).addOnCompleteListener {
                        hackathonList2.sortByDescending { hackathon ->
                            val timestamp =
                                recentHackathons.find { it.UID == hackathon.UID }?.timestamp
                            timestamp ?: 0L
                        }

                        CoroutineScope(Dispatchers.Main).launch {
                            adapter2.notifyDataSetChanged()
                        }
                    }
                }
        }
    }
    private fun getData3() {
        val db = Firebase.firestore
        firebaseAuth = FirebaseAuth.getInstance()
        val user = firebaseAuth.currentUser
        val userID = user?.uid
        hackathonList3.clear()
        getParticipatedHackathonsUIDs { participatedHackathonUIDs ->
            db.collection("Hackathons")
                .get()
                .addOnSuccessListener { result ->
                    val openEvents = mutableListOf<Hackathon>()
                    val registrationClosedEvents = mutableListOf<Hackathon>()
                    val closedEvents = mutableListOf<Hackathon>()
                    for (document in result) {
                        var likes = 0
                        db.collection("Hackathons").document(document.id).collection("Likes").get()
                            .addOnSuccessListener { querySnapshot ->
                                likes = querySnapshot.size()
                            }
                        val date = document.getString("Date") ?: "N/A"
                        val eligibility = document.getString("Eligibility") ?: "N/A"
                        val eligibilityDetails = document.getString("Eligibility Details") ?: "N/A"
                        val email = document.getString("Email") ?: "N/A"
                        val fee = document.getString("Fee") ?: "N/A"
                        val judging = document.getString("Judging") ?: "N/A"
                        val lastDate = document.getString("Last Date") ?: "N/A"
                        val name = document.getString("Name") ?: "N/A"
                        val number = document.getString("Number") ?: "N/A"
                        val organisingAuthority =
                            document.getString("Organising Authority") ?: "N/A"
                        val organisingEvent = document.getString("Organising Event") ?: "N/A"
                        val overview = document.getString("Overview") ?: "N/A"
                        val reward = document.getString("Reward") ?: "N/A"
                        val size = document.getString("Size") ?: "N/A"
                        val structure = document.getString("Structure") ?: "N/A"
                        val title = document.getString("Title") ?: "N/A"
                        val uid = document.getString("UID") ?: "N/A"
                        val venue = document.getString("Venue") ?: "N/A"
                        val ImURL = document.getString("Image URL") ?: "N/A"
                        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
                        val sdf2 = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val lastDateTime = sdf.parse(lastDate)
                        val eventDateTime = sdf2.parse(date)
                        val currentDateTime = Date()
                        if (participatedHackathonUIDs.contains(uid)) {
                            if (lastDateTime.after(currentDateTime) && eventDateTime.after(currentDateTime)) {
                                openEvents.add(
                                    Hackathon(
                                        uid,
                                        title,
                                        organisingAuthority,
                                        organisingEvent,
                                        fee,
                                        venue,
                                        date,
                                        lastDate,
                                        size,
                                        eligibility,
                                        overview,
                                        eligibilityDetails,
                                        structure,
                                        judging,
                                        reward,
                                        name,
                                        number,
                                        email,
                                        ImURL,
                                        likes
                                    )
                                )
                            } else if (lastDateTime.before(currentDateTime) && eventDateTime.after(currentDateTime)) {
                                registrationClosedEvents.add(
                                    Hackathon(
                                        uid,
                                        title,
                                        organisingAuthority,
                                        organisingEvent,
                                        fee,
                                        venue,
                                        date,
                                        lastDate,
                                        size,
                                        eligibility,
                                        overview,
                                        eligibilityDetails,
                                        structure,
                                        judging,
                                        reward,
                                        name,
                                        number,
                                        email,
                                        ImURL,
                                        likes
                                    )
                                )
                            }
                        }
                    }
                    hackathonList3.addAll(openEvents)
                    hackathonList3.addAll(registrationClosedEvents)
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    hackathonList3.sortWith(compareBy {
                        try {
                            sdf.parse(it.date)?.time ?: 0
                        } catch (e: Exception) {
                            0
                        }
                    })
                    adapter3.notifyDataSetChanged()
                }
        }
    }
    override fun onResume() {
        super.onResume()
        getData1()
        getData2()
        adapter2.notifyDataSetChanged()
    }
    private fun openDrawer() {
        val drawerAnimator = ObjectAnimator.ofFloat(binding.customDrawer, "translationX", 0f)
        binding.bg.visibility = View.VISIBLE
        val bgAnimator = ObjectAnimator.ofFloat(binding.bg, "alpha", 0f, 0.7f)
        drawerAnimator.duration = 300
        bgAnimator.duration = 300
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(drawerAnimator, bgAnimator)
        animatorSet.start()

        isDrawerOpen = true
    }
    private fun closeDrawer() {
        val drawerAnimator = ObjectAnimator.ofFloat(binding.customDrawer, "translationX", -binding.customDrawer.width.toFloat())
        val bgAnimator = ObjectAnimator.ofFloat(binding.bg, "alpha", 0.7f, 0f)
        binding.bg.visibility = View.GONE
        drawerAnimator.duration = 300
        bgAnimator.duration = 300
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(drawerAnimator, bgAnimator)
        animatorSet.start()

        isDrawerOpen = false
    }
    private fun showPopupMenu() {
        val popupMenu = PopupMenu(this, binding.options)
        popupMenu.menuInflater.inflate(R.menu.homepage_menu, popupMenu.menu)
        try {
            val field = PopupMenu::class.java.getDeclaredField("mPopup")
            field.isAccessible = true
            val menuHelper = field.get(popupMenu)
            val menu: Menu = popupMenu.menu
            val classPopupHelper = Class.forName(menuHelper.javaClass.name)
            val setForceIcons = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.java)
            setForceIcons.invoke(menuHelper, true)
            val deleteAccountItem = menu.findItem(R.id.delete_account)
            val deleteAccountTitle = SpannableString(deleteAccountItem.title)
            deleteAccountTitle.setSpan(ForegroundColorSpan(Color.parseColor("#D32F2F")), 0, deleteAccountTitle.length, 0)
            deleteAccountItem.title = deleteAccountTitle
            deleteAccountItem.icon?.let {
                DrawableCompat.setTint(it, Color.parseColor("#D32F2F"))
            }
        } catch (e: Exception) {}

        val firebaseAuth = FirebaseAuth.getInstance()
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.sign_out -> {
                    val googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN)
                    googleSignInClient.signOut()
                    firebaseAuth.signOut()
                    Toast.makeText(this, "Signed Out!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, opening::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.delete_account -> {
                    showDeleteConfirmationDialog(firebaseAuth)
                    true
                }
                R.id.help -> {
                    val intent = Intent(this, Help::class.java)
                    startActivity(intent)
                    true
                }
                R.id.aboutus -> {
                    val intent = Intent(this, AboutUs::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }
    private fun getParticipatedHackathonsUIDs(callback: (List<String>) -> Unit) {
        val db = Firebase.firestore
        val user = FirebaseAuth.getInstance().currentUser
        val userID = user?.uid
        if (userID != null) {
            db.collection("Users")
                .document(userID)
                .collection("Participated Hackathons")
                .get()
                .addOnSuccessListener { result ->
                    val participatedHackathonUIDs = result.documents.map { it.id }
                    callback(participatedHackathonUIDs)
                }
        }
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
            val firebaseAuth = FirebaseAuth.getInstance()
            val db = Firebase.firestore
            val user = firebaseAuth.currentUser
            val userID = user?.uid
            selectedImageUri = data?.data
            if (selectedImageUri != null) {
                val contentResolver: ContentResolver = contentResolver
                selectedImageUri?.let { it1 ->
                    uploadImageToCloudinary(it1, contentResolver, userID) { url ->
                        if (url != null) {
                            if (userID != null) {
                                db.collection("Users").document(userID).update("Profile Picture URL", url)
                            }
                            Toast.makeText(this, "Profile Picture Updated !!", Toast.LENGTH_SHORT).show()
                            profileImageUrl=url
                            Glide.with(this).load(url).centerCrop().diskCacheStrategy(DiskCacheStrategy.ALL).into(binding.profile)
                            val imageview=findViewById<ImageView>(R.id.imageView)
                            Glide.with(this).load(url).centerCrop().diskCacheStrategy(DiskCacheStrategy.ALL).into(imageview)
                        }
                    }
                }
            }
        }
    }
    private fun uploadImageToCloudinary(uri: Uri, contentResolver: ContentResolver, UserID: String?=null, onSuccess: (String?) -> Unit) {
        contentResolver.openInputStream(uri)?.readBytes()?.let { byteArray ->
            val publicId = "ProfilePicture/${UserID ?: "default_filename"}"
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

