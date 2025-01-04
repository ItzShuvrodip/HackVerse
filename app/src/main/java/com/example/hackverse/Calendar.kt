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
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.hackverse.databinding.CalendarBinding
import com.github.sundeepk.compactcalendarview.CompactCalendarView
import com.github.sundeepk.compactcalendarview.domain.Event
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.text.clear

class Calendar : AppCompatActivity() {
    private lateinit var binding: CalendarBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private var profileImageUrl: String? = null
    private var selectedImageUri: Uri? = null
    private val IMAGE_PICK_CODE = 104
    private lateinit var hackathonList: MutableList<Hackathon>
    private lateinit var adapter: HackathonListAdapter
    private var isDrawerOpen = false
    private val PERMISSION_CODE = 105
    private var nameint=""
    private var emailint=""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.my_primary)
        binding = CalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.navHome.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.navHome.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val navHomeWidth = binding.navHome.width
                val colorSliderLayoutParams = binding.colorSlider.layoutParams
                colorSliderLayoutParams.width = navHomeWidth
                binding.colorSlider.layoutParams = colorSliderLayoutParams
                binding.colorSlider.x = binding.navCalendar.x
            }
        })
        firebaseAuth = FirebaseAuth.getInstance()
        val user = firebaseAuth.currentUser
        val userID = user?.uid
        hackathonList = mutableListOf()
        val db = Firebase.firestore
        nameint = intent.getStringExtra("name") ?: ""
        emailint = intent.getStringExtra("email") ?: ""
        db.firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()
        profileImageUrl= intent.getStringExtra("imgu")
        val imageView=findViewById<ImageView>(R.id.imageView)
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
        if (profileImageUrl != null||profileImageUrl!=""){
            Glide.with(this).load(profileImageUrl).placeholder(R.drawable.defaultdp).centerCrop().diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(binding.profile)
            Glide.with(this).load(profileImageUrl).placeholder(R.drawable.defaultdp).centerCrop().diskCacheStrategy(DiskCacheStrategy.ALL).into(imageView)
            binding.profile.visibility = View.VISIBLE
        }
        if (userID == null) {
            val intent = Intent(this, opening::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }
        val nameview=findViewById<TextView>(R.id.nameview)
        val emailview=findViewById<TextView>(R.id.emailview)
        if(nameint!=""&&emailint!=""&&nameint!=null&&emailint!=null){
            nameview.text = nameint
            emailview.text = emailint
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
                }else {
                    imageView.setImageResource(R.drawable.defaultdp)
                }
            }
        }
        val sdf3 = SimpleDateFormat("EEEE, d MMMM, yyyy", Locale.getDefault())
        val formattedDate = sdf3.format(Date())
        val today: Date = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        binding.events.text = "Events : $formattedDate"
        getEvents()
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
        binding.navHome.setOnClickListener {
            binding.colorSlider.post {
                binding.colorSlider.animate()
                    .x(binding.navHome.x)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .setDuration(300)
                    .withEndAction {
                        binding.calendartext.setTextColor(Color.parseColor("#2E2E2E"))
                        binding.hometext.setTextColor(Color.parseColor("#036261"))
                        binding.calendaricon.imageTintList= ColorStateList.valueOf(Color.parseColor("#2E2E2E"))
                        binding.homeicon.imageTintList= ColorStateList.valueOf(Color.parseColor("#036261"))
                        val intent = Intent(this,Homepage::class.java)
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
        binding.navSearch.setOnClickListener {
            binding.colorSlider.post {
                binding.colorSlider.animate()
                    .x(binding.navSearch.x)
                    .setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        binding.calendartext.setTextColor(Color.parseColor("#2E2E2E"))
                        binding.searchtext.setTextColor(Color.parseColor("#036261"))
                        binding.calendaricon.imageTintList= ColorStateList.valueOf(Color.parseColor("#2E2E2E"))
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
                        binding.calendartext.setTextColor(Color.parseColor("#2E2E2E"))
                        binding.myhackathonstext.setTextColor(Color.parseColor("#036261"))
                        binding.calendaricon.imageTintList= ColorStateList.valueOf(Color.parseColor("#2E2E2E"))
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
            val intent = Intent(this, EditProfile::class.java)
            intent.putExtra("backopt", 4)
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
            binding.navCalendar.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(200)
        }
        val monthTextView = findViewById<TextView>(R.id.monthTextView)
        val compactCalendarView = findViewById<CompactCalendarView>(R.id.CalendarView)
        compactCalendarView.setFirstDayOfWeek(Calendar.SUNDAY)
        fun getMonthName(date: Date): String {
            val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            return sdf.format(date)
        }
        val initialDate = compactCalendarView.firstDayOfCurrentMonth
        compactCalendarView.setCurrentSelectedDayIndicatorStyle(1)
        compactCalendarView.setCurrentSelectedDayBackgroundColor(Color.parseColor("#349D9B"))
        compactCalendarView.setCurrentSelectedDayTextColor(Color.parseColor("#FFFFFF"))
        getData(today)
        setupRecyclerView()
        monthTextView.text = getMonthName(initialDate)
        compactCalendarView.shouldDrawIndicatorsBelowSelectedDays(true)
        compactCalendarView.setListener(object : CompactCalendarView.CompactCalendarViewListener {
            override fun onDayClick(dateClicked: Date) {
                if (dateClicked.equals(today)) {
                    compactCalendarView.setCurrentSelectedDayIndicatorStyle(1)
                    compactCalendarView.setCurrentSelectedDayBackgroundColor(Color.parseColor("#267B74"))
                    compactCalendarView.setCurrentSelectedDayTextColor(Color.parseColor("#FFFFFF"))
                }else{
                    compactCalendarView.setCurrentSelectedDayIndicatorStyle(2)
                    compactCalendarView.setCurrentSelectedDayBackgroundColor(Color.parseColor("#349D9B"))
                    compactCalendarView.setCurrentSelectedDayTextColor(Color.parseColor("#1B2B34"))
                }
                val sdf = SimpleDateFormat("EEEE, d MMMM, yyyy", Locale.getDefault())
                val formattedDate2 = sdf.format(dateClicked)
                binding.events.text = "Events : $formattedDate2"
                getData(dateClicked)
                setupRecyclerView()
            }
            override fun onMonthScroll(firstDayOfNewMonth: Date) {
                monthTextView.text = getMonthName(firstDayOfNewMonth)
            }
        })
        binding.next.setOnClickListener {
            compactCalendarView.scrollRight()
        }
        binding.previous.setOnClickListener {
            compactCalendarView.scrollLeft()
        }
    }
    private fun getEvents() {
        val compactCalendarView = findViewById<CompactCalendarView>(R.id.CalendarView)
        val db = Firebase.firestore
        firebaseAuth = FirebaseAuth.getInstance()
        val user = firebaseAuth.currentUser
        val userID = user?.uid
        if (userID != null) {
            compactCalendarView.removeAllEvents()
            db.collection("Users").document(userID).collection("Participated Hackathons").get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        db.collection("Hackathons").document(document.id).get()
                            .addOnSuccessListener { documentSnapshot ->
                                val sdf =
                                    SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
                                val sdf2 = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                try {
                                    val lastDateTime =
                                        sdf.parse(documentSnapshot.getString("Last Date"))
                                    val eventDateTime =
                                        sdf2.parse(documentSnapshot.getString("Date"))
                                    val currentDateTime = Date()
                                    val currentDate = Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }.time
                                    val eventdate = eventDateTime?.time
                                    val event = eventdate?.let {
                                        if (lastDateTime != null && lastDateTime.after(currentDateTime) && eventDateTime != null && eventDateTime.after(currentDateTime)){
                                            Event(Color.parseColor("#2DBE60"), it, documentSnapshot.getString("Title"))
                                        }
                                        else if(lastDateTime != null && lastDateTime.before(currentDateTime) && eventDateTime != null && (eventDateTime.after(currentDateTime)||eventDateTime.equals(currentDate))){
                                            Event(Color.parseColor("#FFA500"), it, documentSnapshot.getString("Title"))
                                        }
                                        else if(lastDateTime != null && lastDateTime.before(currentDateTime) && eventDateTime != null && eventDateTime.before(currentDateTime) && !eventDateTime.equals(currentDate)){
                                            Event(Color.parseColor("#C8102E"), it, documentSnapshot.getString("Title"))
                                        }
                                        else{
                                            null
                                        }
                                    }
                                    if (event != null) {
                                        compactCalendarView.addEvent(event)
                                    }
                                }catch (e: Exception){

                                }
                            }
                    }
                }
        }
    }
    private fun setupRecyclerView() {
        adapter = HackathonListAdapter(hackathonList)
        binding.itemspanel.layoutManager = LinearLayoutManager(this)
        binding.itemspanel.adapter = adapter
    }
    private fun getData(dateClicked: Date) {
        val db = Firebase.firestore
        firebaseAuth = FirebaseAuth.getInstance()
        val user = firebaseAuth.currentUser
        val userID = user?.uid
        hackathonList.clear()
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
                            if (lastDateTime.after(currentDateTime) && eventDateTime.after(currentDateTime)&&eventDateTime.equals(dateClicked)) {
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
                            } else if (lastDateTime.before(currentDateTime) && eventDateTime.after(currentDateTime)&&eventDateTime.equals(dateClicked)) {
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
                            } else if (lastDateTime.before(
                                    currentDateTime
                                ) && eventDateTime.before(currentDateTime)&&eventDateTime.equals(dateClicked)
                            ) {
                                closedEvents.add(
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
                    hackathonList.addAll(openEvents)
                    hackathonList.addAll(registrationClosedEvents)
                    hackathonList.addAll(closedEvents)
                    adapter.notifyDataSetChanged()
                }
        }
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
    override fun onBackPressed() {
        if (isDrawerOpen) {
            closeDrawer()
        } else {
            finish()
            super.onBackPressed()
        }
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
                            Glide.with(this).load(url).centerCrop().diskCacheStrategy(
                                DiskCacheStrategy.ALL).into(binding.profile)
                            val imageview=findViewById<ImageView>(R.id.imageView)
                            Glide.with(this).load(url).centerCrop().diskCacheStrategy(
                                DiskCacheStrategy.ALL).into(imageview)
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