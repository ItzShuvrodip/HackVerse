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
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.ListPopupWindow
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.hackverse.databinding.SearchBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.text.equals

class Search : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: SearchBinding
    private lateinit var hackathonList: MutableList<Hackathon>
    private lateinit var filteredData: MutableList<Hackathon>
    private lateinit var adapter: HackathonListAdapter
    private var profileImageUrl: String? = null
    private var selectedImageUri: Uri? = null
    private lateinit var filterMenuAdapter: FilterMenuAdapter
    private lateinit var listPopupWindow: ListPopupWindow
    private var searchJob: Job? = null
    private var sort: String? = null
    private var sortorder=true
    private var search: String? = null
    private val IMAGE_PICK_CODE = 104
    private var isDrawerOpen = false
    private val PERMISSION_CODE = 105
    private var nameint=""
    private var emailint=""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.my_primary)
        firebaseAuth = FirebaseAuth.getInstance()
        val user = firebaseAuth.currentUser
        val db = Firebase.firestore
        val userID = user?.uid
        db.firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()
        profileImageUrl= intent.getStringExtra("imgu")
        filteredData = mutableListOf()
        binding = SearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        nameint = intent.getStringExtra("name") ?: ""
        emailint = intent.getStringExtra("email") ?: ""
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
                binding.colorSlider.x = binding.navSearch.x
            }
        })

        val filterOptions = listOf("Free", "Paid", "Registration Open", "Registration Closed", "Event Closed")
        filterMenuAdapter = FilterMenuAdapter(this, filterOptions)
        listPopupWindow = ListPopupWindow(this)
        listPopupWindow.setAdapter(filterMenuAdapter)
        listPopupWindow.anchorView = binding.filterMenu
        listPopupWindow.isModal = true
        listPopupWindow.width = 500
        listPopupWindow.setOnItemClickListener { _, _, position, _ ->
            val item = filterOptions[position]
            filterMenuAdapter.toggleSelection(item)
            applyFilters()
        }

        binding.filterMenu.setOnClickListener {
            listPopupWindow.show()
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

        binding.refresh.setColorSchemeResources(R.color.my_primary)
        binding.refresh.setOnRefreshListener {
            Handler(mainLooper).postDelayed({
                getData(search)
                setupRecyclerView()
                binding.refresh.isRefreshing = false
            }, 2000)
        }
        hackathonList = mutableListOf()
        binding.searchbar.doOnTextChanged { text, _, _, _ ->
            searchJob?.cancel()
            searchJob = lifecycleScope.launch {
                binding.refresh.isRefreshing = true
                delay(300)
                search = text.toString()
                getData(search)
                setupRecyclerView()
            }
        }

        binding.navHome.setOnClickListener {
            binding.colorSlider.post {
                binding.colorSlider.animate()
                    .x(binding.navHome.x)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .setDuration(300)
                    .withEndAction {
                        binding.searchtext.setTextColor(Color.parseColor("#2E2E2E"))
                        binding.hometext.setTextColor(Color.parseColor("#036261"))
                        binding.searchicon.imageTintList= ColorStateList.valueOf(Color.parseColor("#2E2E2E"))
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
        binding.navCalendar.setOnClickListener {
            binding.colorSlider.post {
                binding.colorSlider.animate()
                    .x(binding.navCalendar.x)
                    .setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        binding.searchtext.setTextColor(Color.parseColor("#2E2E2E"))
                        binding.calendartext.setTextColor(Color.parseColor("#036261"))
                        binding.searchicon.imageTintList=ColorStateList.valueOf(Color.parseColor("#2E2E2E"))
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
        binding.navMyhackathons.setOnClickListener {
            binding.colorSlider.post {
                binding.colorSlider.animate()
                    .x(binding.navMyhackathons.x)
                    .setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        binding.searchtext.setTextColor(Color.parseColor("#2E2E2E"))
                        binding.myhackathonstext.setTextColor(Color.parseColor("#036261"))
                        binding.searchicon.imageTintList=ColorStateList.valueOf(Color.parseColor("#2E2E2E"))
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
        } else {
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
            intent.putExtra("backopt",2)
            startActivity(intent)
            finish()
        }
        binding.refresh.isRefreshing = true
        getData(search)
        setupRecyclerView()
        binding.sortMenu.setOnClickListener {
            showSortMenu()
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
            binding.navSearch.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(200)
        }

    }
    private fun setupRecyclerView() {
        adapter = HackathonListAdapter(filteredData)
        binding.itemspanel.layoutManager = LinearLayoutManager(this)
        binding.itemspanel.adapter = adapter
    }

    private fun getData(SearchQuery: String? = null) {
        val db = Firebase.firestore
        hackathonList.clear()
        CoroutineScope(Dispatchers.IO).launch {
            val result = db.collection("Hackathons").get().await()
            val openEvents = mutableListOf<Hackathon>()
            val registrationClosedEvents = mutableListOf<Hackathon>()
            val closedEvents = mutableListOf<Hackathon>()
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
                if ((SearchQuery != null && ((title.contains(
                        SearchQuery,
                        ignoreCase = true
                    )) || (organisingAuthority.contains(
                        SearchQuery,
                        ignoreCase = true
                    )) || (organisingEvent.contains(
                        SearchQuery,
                        ignoreCase = true
                    ))) || SearchQuery == null) && lastDateTime.after(currentDateTime) && eventDateTime.after(
                        currentDateTime
                    )
                ) {
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
                } else if ((SearchQuery != null && ((title.contains(
                        SearchQuery,
                        ignoreCase = true
                    )) || (organisingAuthority.contains(
                        SearchQuery,
                        ignoreCase = true
                    )) || (organisingEvent.contains(
                        SearchQuery,
                        ignoreCase = true
                    ))) || SearchQuery == null) && lastDateTime.before(currentDateTime) && eventDateTime.after(
                        currentDateTime
                    )
                ) {
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
                } else if ((SearchQuery != null && ((title.contains(
                        SearchQuery,
                        ignoreCase = true
                    )) || (organisingAuthority.contains(
                        SearchQuery,
                        ignoreCase = true
                    )) || (organisingEvent.contains(
                        SearchQuery,
                        ignoreCase = true
                    ))) || SearchQuery == null) && lastDateTime.before(currentDateTime) && eventDateTime.before(
                        currentDateTime
                    )
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
            hackathonList.addAll(openEvents)
            hackathonList.addAll(registrationClosedEvents)
            hackathonList.addAll(closedEvents)
            if (binding.sortMenu.text == "Alphabetically") {
                hackathonList.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            } else if (binding.sortMenu.text == "Popularity") {
                hackathonList.sortByDescending { it.likes }
            } else if (binding.sortMenu.text == "Registration Fee") {
                hackathonList.sortWith(compareBy {
                    if (it.fee.equals("Free", ignoreCase = true)) {
                        Double.MIN_VALUE
                    } else {
                        try {
                            it.fee.toDouble()
                        } catch (e: NumberFormatException) {
                            Double.MAX_VALUE
                        }
                    }
                })
            } else if (binding.sortMenu.text == "Registration Date") {
                val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
                hackathonList.sortWith(compareBy {
                    try {
                        sdf.parse(it.lastdate)?.time ?: 0
                    } catch (e: Exception) {
                        0
                    }
                })
                hackathonList.reverse()
            } else if (binding.sortMenu.text == "Event Date") {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                hackathonList.sortWith(compareBy {
                    try {
                        sdf.parse(it.date)?.time ?: 0
                    } catch (e: Exception) {
                        0
                    }
                })
                hackathonList.reverse()
            }
            if (!sortorder) {
                hackathonList.reverse()
            }
            runOnUiThread {
                applyFilters()
                binding.refresh.isRefreshing = false
                adapter.notifyDataSetChanged()
            }
        }
    }
    private fun showSortMenu() {
        val popupMenu = PopupMenu(this, binding.sortMenu)
        popupMenu.menuInflater.inflate(R.menu.sortby, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item ->
            binding.refresh.isRefreshing = true
            when (item.itemId) {
                R.id.menu_option_1 -> {
                    if(sort=="Alphabetically")
                    {
                        sortorder=!sortorder
                    }else{
                        sortorder=true
                    }
                    if(sortorder) {
                        binding.sortMenu.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            AppCompatResources.getDrawable(
                                binding.sortMenu.context,
                                R.drawable.sort
                            ), null,
                            AppCompatResources.getDrawable(
                                binding.sortMenu.context,
                                R.drawable.dropdown
                            ), null
                        )
                    }else{
                        binding.sortMenu.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            AppCompatResources.getDrawable(
                                binding.sortMenu.context,
                                R.drawable.sort
                            ), null,
                            AppCompatResources.getDrawable(
                                binding.sortMenu.context,
                                R.drawable.dropup
                            ), null
                        )
                    }
                    binding.sortMenu.text = "Alphabetically"
                    sort="Alphabetically"
                    getData(search)
                    setupRecyclerView()
                    true
                }
                R.id.menu_option_2 -> {
                    if(sort=="Popularity")
                    {
                        sortorder=!sortorder
                    }else{
                        sortorder=true
                    }
                    if(sortorder) {
                        binding.sortMenu.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            AppCompatResources.getDrawable(
                                binding.sortMenu.context,
                                R.drawable.sort
                            ), null,
                            AppCompatResources.getDrawable(
                                binding.sortMenu.context,
                                R.drawable.dropdown
                            ), null
                        )
                    }else{
                        binding.sortMenu.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            AppCompatResources.getDrawable(
                                binding.sortMenu.context,
                                R.drawable.sort
                            ), null,
                            AppCompatResources.getDrawable(
                                binding.sortMenu.context,
                                R.drawable.dropup
                            ), null
                        )
                    }
                    sort="Popularity"
                    binding.sortMenu.text = "Popularity"
                    getData(search)
                    setupRecyclerView()
                    true
                }
                R.id.menu_option_3 -> {
                    if(sort=="Registration Fee")
                    {
                        sortorder=!sortorder
                    }else{
                        sortorder=true
                    }
                    if(sortorder) {
                        binding.sortMenu.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            AppCompatResources.getDrawable(
                                binding.sortMenu.context,
                                R.drawable.sort
                            ), null,
                            AppCompatResources.getDrawable(
                                binding.sortMenu.context,
                                R.drawable.dropdown
                            ), null
                        )
                    }else{
                        binding.sortMenu.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            AppCompatResources.getDrawable(
                                binding.sortMenu.context,
                                R.drawable.sort
                            ), null,
                            AppCompatResources.getDrawable(
                                binding.sortMenu.context,
                                R.drawable.dropup
                            ), null
                        )
                    }
                    sort="Registration Fee"
                    binding.sortMenu.text = "Registration Fee"
                    getData(search)
                    setupRecyclerView()
                    true
                }
                R.id.menu_option_4 -> {
                    if(sort=="Registration Date")
                    {
                        sortorder=!sortorder
                    }else{
                        sortorder=true
                    }
                    if(sortorder) {
                        binding.sortMenu.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            AppCompatResources.getDrawable(
                                binding.sortMenu.context,
                                R.drawable.sort
                            ), null,
                            AppCompatResources.getDrawable(
                                binding.sortMenu.context,
                                R.drawable.dropdown
                            ), null
                        )
                    }else{
                        binding.sortMenu.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            AppCompatResources.getDrawable(
                                binding.sortMenu.context,
                                R.drawable.sort
                            ), null,
                            AppCompatResources.getDrawable(
                                binding.sortMenu.context,
                                R.drawable.dropup
                            ), null
                        )
                    }
                    sort="Registration Date"
                    binding.sortMenu.text = "Registration Date"
                    getData(search)
                    setupRecyclerView()
                    true
                }
                R.id.menu_option_5 -> {
                    if(sort=="Event Date")
                    {
                        sortorder=!sortorder
                    }else{
                        sortorder=true
                    }
                    if(sortorder) {
                        binding.sortMenu.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            AppCompatResources.getDrawable(
                                binding.sortMenu.context,
                                R.drawable.sort
                            ), null,
                            AppCompatResources.getDrawable(
                                binding.sortMenu.context,
                                R.drawable.dropdown
                            ), null
                        )
                    }else{
                        binding.sortMenu.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            AppCompatResources.getDrawable(
                                binding.sortMenu.context,
                                R.drawable.sort
                            ), null,
                            AppCompatResources.getDrawable(
                                binding.sortMenu.context,
                                R.drawable.dropup
                            ), null
                        )
                    }
                    sort="Event Date"
                    binding.sortMenu.text = "Event Date"
                    getData(search)
                    setupRecyclerView()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }
    private fun applyFilters() {
        val selectedFilters = filterMenuAdapter.getSelectedItems()
        filteredData.clear()

        if (selectedFilters.isEmpty()) {
            filteredData.addAll(hackathonList)
        } else {
            filteredData.addAll(hackathonList.filter { data ->
                val group1Result = when {
                    "Free" in selectedFilters && "Paid" in selectedFilters ->
                        data.fee.equals("Free", ignoreCase = true) || !data.fee.equals("Free", ignoreCase = true)
                    "Free" in selectedFilters -> data.fee.equals("Free", ignoreCase = true)
                    "Paid" in selectedFilters -> !data.fee.equals("Free", ignoreCase = true)
                    else -> false
                }

                val group2Result = when {
                    "Registration Open" in selectedFilters && "Registration Closed" in selectedFilters && "Event Closed" in selectedFilters ->
                        data.lastdate.isRegistrationOpen() ||
                                (!data.lastdate.isRegistrationOpen() && data.date.isEventOpen()) ||
                                !data.date.isEventOpen()
                    "Registration Open" in selectedFilters && "Registration Closed" in selectedFilters ->
                        data.lastdate.isRegistrationOpen() ||
                                (!data.lastdate.isRegistrationOpen() && data.date.isEventOpen())
                    "Registration Open" in selectedFilters && "Event Closed" in selectedFilters ->
                        data.lastdate.isRegistrationOpen() || !data.date.isEventOpen()
                    "Registration Closed" in selectedFilters && "Event Closed" in selectedFilters ->
                        (!data.lastdate.isRegistrationOpen() && data.date.isEventOpen())|| !data.date.isEventOpen()
                    "Registration Open" in selectedFilters -> data.lastdate.isRegistrationOpen()
                    "Registration Closed" in selectedFilters -> !data.lastdate.isRegistrationOpen() && data.date.isEventOpen()
                    "Event Closed" in selectedFilters -> !data.date.isEventOpen()
                    else -> false
                }

                if (("Registration Open" in selectedFilters ||
                            "Registration Closed" in selectedFilters ||
                            "Event Closed" in selectedFilters) &&
                    ("Free" in selectedFilters ||
                            "Paid" in selectedFilters)) {
                    group1Result && group2Result
                } else {
                    group1Result || group2Result
                }
            })

        }
        adapter.notifyDataSetChanged()
    }
    fun String.isRegistrationOpen(): Boolean {
        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        return try {
            val lastDate = sdf.parse(this)
            lastDate?.after(Date()) ?: false
        } catch (e: Exception) {
            false
        }
    }
    fun String.isEventOpen(): Boolean {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return try {
            val date = sdf.parse(this)
            date?.after(Date()) ?: false
        } catch (e: Exception) {
            false
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
        val popupMenu = android.widget.PopupMenu(this, binding.options)
        popupMenu.menuInflater.inflate(R.menu.homepage_menu, popupMenu.menu)
        try {
            val field = android.widget.PopupMenu::class.java.getDeclaredField("mPopup")
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
