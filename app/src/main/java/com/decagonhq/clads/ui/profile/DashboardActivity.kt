package com.decagonhq.clads.ui.profile

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.decagonhq.clads.R
import com.decagonhq.clads.data.domain.MessagesNotificationModel
import com.decagonhq.clads.data.local.CladsDatabase
import com.decagonhq.clads.databinding.DashboardActivityBinding
import com.decagonhq.clads.ui.messages.MessagesFragment
import com.decagonhq.clads.util.Constants
import com.decagonhq.clads.util.CustomProgressDialog
import com.decagonhq.clads.util.EncodeEmail.encodeUserEmail
import com.decagonhq.clads.util.SessionManager
import com.decagonhq.clads.util.loadImage
import com.decagonhq.clads.util.logOut
import com.decagonhq.clads.viewmodels.ClientViewModel
import com.decagonhq.clads.viewmodels.ClientsRegisterViewModel
import com.decagonhq.clads.viewmodels.ImageUploadViewModel
import com.decagonhq.clads.viewmodels.UserProfileViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class DashboardActivity : AppCompatActivity(), updateToolbarTitleListener {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: DashboardActivityBinding
    private lateinit var navController: NavController
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var listener: NavController.OnDestinationChangedListener
    private lateinit var editProfile: TextView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbarNotificationIcon: ImageView
    private lateinit var toolbarProfilePicture: ImageView
    private lateinit var toolbarUserName: TextView
    private lateinit var toolbarFragmentName: TextView
    private lateinit var drawerCloseIcon: ImageView
    private lateinit var profileImage: ImageView
    private lateinit var progressDialog: CustomProgressDialog
    private lateinit var navigationView: NavigationView
    private lateinit var profileName: TextView

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var database: CladsDatabase

    @Inject
    @Named(Constants.IMAGE_RETROFIT)
    lateinit var imageRetrofit: Retrofit

    private val userProfileViewModel: UserProfileViewModel by viewModels()
    private val imageUploadViewModel: ImageUploadViewModel by viewModels()
    private val clientViewModel: ClientViewModel by viewModels()
    private val clientsRegisterViewModel: ClientsRegisterViewModel by viewModels()

    override fun onStart() {
        super.onStart()

        clientViewModel.getClients()
        userProfileViewModel.getUserProfile()
        imageUploadViewModel.getRemoteGalleryImages()

        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                userProfileViewModel.getLocalDatabaseUserProfile()
                clientViewModel.getLocalDatabaseClients()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DashboardActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarDashboard.dashboardActivityToolbar)

        /*Set Status bar Color*/
        window?.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window?.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window?.statusBarColor = resources?.getColor(R.color.white)!!
        window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        drawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navViewHeader = navView.getHeaderView(0)
        editProfile = navViewHeader.findViewById(R.id.nav_drawer_edit_profile_text_view)
        drawerCloseIcon = navViewHeader.findViewById(R.id.nav_drawer_close_icon_image_view)
        profileImage = navViewHeader.findViewById(R.id.nav_drawer_profile_avatar_image_view)

        progressDialog = CustomProgressDialog(this)

        /*Initialize Toolbar Views*/
        toolbarNotificationIcon =
            binding.appBarDashboard.dashboardActivityToolbarNotificationImageView
        toolbarProfilePicture = binding.appBarDashboard.dashboardActivityToolbarProfileImageView
        toolbarUserName = binding.appBarDashboard.dashboardActivityToolbarHiIjeomaTextView
        toolbarFragmentName = binding.appBarDashboard.dashboardActivityToolbarFragmentNameTextView

        bottomNavigationView =
            binding.appBarDashboard.contentDashboard.dashboardActivityBottomNavigationView
        navigationView = binding.navView

        profileName = navViewHeader.findViewById(R.id.nav_drawer_user_full_name_text_view)
        navController = findNavController(R.id.nav_host_fragment_content_dashboard)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            navController.graph, drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        bottomNavigationView.setupWithNavController(navController)
        hideCustomBarBarConstraintLayout()

        /*Set Up Navigation Change Listener*/
        onDestinationChangedListener()

        /*Edit User Detail onClick*/
        editProfile.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            navController.navigate(R.id.editProfileFragment)
        }

        /*Close Drawer Icon*/
        drawerCloseIcon.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        /*Open Messages onClick*/
        toolbarNotificationIcon.setOnClickListener {
            navController.navigate(R.id.nav_messages)
            toolbarProfilePicture.visibility = View.INVISIBLE
            toolbarUserName.visibility = View.INVISIBLE
            toolbarNotificationIcon.visibility = View.GONE
        }

        navigationView.setNavigationItemSelectedListener { it ->
            when (it.itemId) {
                R.id.clientFragment -> {
                    findNavController(R.id.nav_host_fragment_content_dashboard).navigate(R.id.clientFragment)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    return@setNavigationItemSelectedListener true
                }
                R.id.resourceGeneralFragment -> {
                    findNavController(R.id.nav_host_fragment_content_dashboard).navigate(R.id.resourceGeneralFragment)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    return@setNavigationItemSelectedListener true
                }
                R.id.subscriptionFragment -> {
                    findNavController(R.id.nav_host_fragment_content_dashboard).navigate(R.id.subscriptionFragment)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    return@setNavigationItemSelectedListener true
                }
                R.id.transactionHistory -> {
                    findNavController(R.id.nav_host_fragment_content_dashboard).navigate(R.id.transaction_history)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    return@setNavigationItemSelectedListener true
                }
                R.id.logout -> {

                    // Using a dialog to ask the user for confirmation before logging out
                    val confirmationDialog = AlertDialog.Builder(this)
                    confirmationDialog.setMessage(R.string.logout_confirmation_dialog_message)
                    confirmationDialog.setPositiveButton(R.string.yes) { _: DialogInterface, _: Int ->
                        logOut(sessionManager, database)
                        userProfileViewModel.userProfile.removeObservers(this)
                    }
                    confirmationDialog.setNegativeButton(
                        R.string.no
                    ) { _: DialogInterface, _: Int ->
                        drawerLayout.closeDrawer(GravityCompat.START)
                    }
                    confirmationDialog.create().show()

                    return@setNavigationItemSelectedListener true
                }
                else -> return@setNavigationItemSelectedListener true
            }
        }
    }

    /*CLose Nav Drawer if open, on back press*/
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun getUserName() {
        /*Observing the user profile to display the user name*/
        userProfileViewModel.userProfile.observe(
            this,
            Observer {
                it.data.let { userProfile ->

                    toolbarUserName.text =
                        getString(
                            R.string.hi,
                            userProfile?.firstName?.capitalize(Locale.ROOT) ?: " "
                        )

                    val fullName = "${userProfile?.firstName ?: "---"} ${
                    userProfile?.lastName ?: "---"
                    }"
                    profileName.text = fullName.capitalize(Locale.ROOT)

                    /*Load Profile Picture with Glide*/
                    toolbarProfilePicture.loadImage(userProfile?.thumbnail)

                    /*load profile image from shared pref*/
                    profileImage.loadImage(userProfile?.thumbnail)

                    /*Instantiate firebase user model*/
                    val userId = userProfile?.id
                    val userEmail = encodeUserEmail(userProfile?.email)
                    val firstName = userProfile?.firstName
                    val lastName = userProfile?.lastName
                    val userImage = userProfile?.thumbnail

                    /*Add signedIn user to firebase*/
                    val fireBaseRef =
                        FirebaseDatabase.getInstance().getReference("users/$userEmail")
                    val user = MessagesNotificationModel(
                        "",
                        firstName,
                        lastName,
                        userEmail,
                        userId,
                        userImage
                    )
                    fireBaseRef.setValue(user).addOnSuccessListener {
                    }
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        navController.addOnDestinationChangedListener(listener)
        getUserName()
    }

    private fun createNotification() {
        // Create the NotificationChannel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("ID", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            // create an explicit intent to open the second activity
            val notifyIntent = Intent(this, MessagesFragment::class.java).apply {
                putExtra("firstName", "Ola")
                putExtra("lastName", "Michavelli")
                putExtra("body", getString(R.string.lorem_ipsum))
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            // get the pending intent
            val notifyPendingIntent =
                PendingIntent.getActivity(
                    this,
                    0,
                    notifyIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            val builder = NotificationCompat.Builder(this, "ID")
                .setContentTitle("My notification")
                .setSmallIcon(R.drawable.clads_logo_blue)
                .setContentText("You have a notification")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(notifyPendingIntent)
                .setAutoCancel(true)

            // set on click listener to button1 to open this assignment
//            notify_me.setOnClickListener {
//                notificationManager.notify(100, builder.build())
//            }
        }
    }

    override fun onPause() {
        super.onPause()
        navController.removeOnDestinationChangedListener(listener)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_dashboard)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    /*Set Up Navigation Change Listener*/
    private fun onDestinationChangedListener() {
        listener =
            NavController.OnDestinationChangedListener { controller, destination, arguments ->
                toolbarFragmentName.text = destination.label ?: " "
//                getString(R.string.app_name)
                when (destination.id) {
                    R.id.nav_home -> {
                        bottomNavigationView.visibility = View.VISIBLE
                        toolbarProfilePicture.visibility = View.VISIBLE
                        toolbarUserName.visibility = View.VISIBLE
                        toolbarNotificationIcon.visibility = View.VISIBLE
                        toolbarFragmentName.visibility = View.GONE
                    }
                    R.id.editProfileFragment -> {
                        bottomNavigationView.visibility = View.GONE
                        toolbarProfilePicture.visibility = View.GONE
                        toolbarUserName.visibility = View.GONE
                        toolbarNotificationIcon.visibility = View.GONE
                        toolbarFragmentName.visibility = View.VISIBLE
                    }
                    R.id.clientFragment -> {
                        bottomNavigationView.visibility = View.GONE
                        toolbarProfilePicture.visibility = View.GONE
                        toolbarUserName.visibility = View.GONE
                        toolbarNotificationIcon.visibility = View.GONE
                        toolbarFragmentName.visibility = View.VISIBLE
                    }
                    R.id.addClientFragment -> {
                        bottomNavigationView.visibility = View.GONE
                        toolbarProfilePicture.visibility = View.GONE
                        toolbarUserName.visibility = View.GONE
                        toolbarNotificationIcon.visibility = View.GONE
                        toolbarFragmentName.visibility = View.VISIBLE
                    }
                    R.id.nav_messages -> {
                        bottomNavigationView.visibility = View.VISIBLE
                        toolbarProfilePicture.visibility = View.GONE
                        toolbarUserName.visibility = View.GONE
                        toolbarNotificationIcon.visibility = View.GONE
                        toolbarFragmentName.visibility = View.VISIBLE
                    }
                    R.id.nav_media -> {
                        bottomNavigationView.visibility = View.VISIBLE
                        toolbarProfilePicture.visibility = View.GONE
                        toolbarUserName.visibility = View.GONE
                        toolbarNotificationIcon.visibility = View.GONE
                        toolbarFragmentName.visibility = View.VISIBLE
                    }
                    R.id.photoGalleryEditImageFragment -> {
                        bottomNavigationView.visibility = View.GONE
                        toolbarProfilePicture.visibility = View.GONE
                        toolbarUserName.visibility = View.GONE
                        toolbarNotificationIcon.visibility = View.GONE
                        toolbarFragmentName.visibility = View.INVISIBLE
                    }
                    R.id.mediaFragmentPhotoName -> {
                        bottomNavigationView.visibility = View.GONE
                        toolbarProfilePicture.visibility = View.GONE
                        toolbarUserName.visibility = View.GONE
                        toolbarNotificationIcon.visibility = View.GONE
                        toolbarFragmentName.visibility = View.INVISIBLE
                    }
                    R.id.resourceArticlesFragment -> {
                        bottomNavigationView.visibility = View.GONE
                        toolbarProfilePicture.visibility = View.GONE
                        toolbarUserName.visibility = View.GONE
                        toolbarNotificationIcon.visibility = View.GONE
                        toolbarFragmentName.visibility = View.VISIBLE
                    }
                    R.id.resourceGeneralFragment -> {
                        bottomNavigationView.visibility = View.GONE
                        toolbarProfilePicture.visibility = View.GONE
                        toolbarUserName.visibility = View.GONE
                        toolbarNotificationIcon.visibility = View.GONE
                        toolbarFragmentName.visibility = View.VISIBLE
                    }
                    R.id.resourceVideosFragment -> {
                        bottomNavigationView.visibility = View.GONE
                        toolbarProfilePicture.visibility = View.GONE
                        toolbarUserName.visibility = View.GONE
                        toolbarNotificationIcon.visibility = View.GONE
                        toolbarFragmentName.visibility = View.VISIBLE
                    }
                    R.id.resourceViewIndividualArticleFragment -> {
                        bottomNavigationView.visibility = View.GONE
                        toolbarProfilePicture.visibility = View.GONE
                        toolbarUserName.visibility = View.GONE
                        toolbarNotificationIcon.visibility = View.GONE
                        toolbarFragmentName.visibility = View.VISIBLE
                    }
                    R.id.individualVideoScreenFragment -> {
                        bottomNavigationView.visibility = View.GONE
                        toolbarProfilePicture.visibility = View.GONE
                        toolbarUserName.visibility = View.GONE
                        toolbarNotificationIcon.visibility = View.GONE
                        toolbarFragmentName.visibility = View.VISIBLE
                    }
                    R.id.clientChatFragment -> {
                        bottomNavigationView.visibility = View.GONE
                        toolbarProfilePicture.visibility = View.VISIBLE
                        toolbarUserName.visibility = View.VISIBLE
                        toolbarNotificationIcon.visibility = View.GONE
                        toolbarFragmentName.visibility = View.GONE
                    }
                    else -> {
                        bottomNavigationView.visibility = View.GONE
                        toolbarProfilePicture.visibility = View.GONE
                        toolbarUserName.visibility = View.GONE
                        toolbarNotificationIcon.visibility = View.GONE
                        toolbarFragmentName.visibility = View.VISIBLE
                    }
                }
            }
    }

    fun setCustomActionBarTitle(message: String) {
        binding.appBarDashboard.dashboardActivityToolbar.title = message
    }

    private fun hideCustomBarBarConstraintLayout() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.photoGalleryEditImageFragment) {
                binding.appBarDashboard.dashboardActivityToolbarCL.visibility = View.GONE
            } else {
                binding.appBarDashboard.dashboardActivityToolbarCL.visibility = View.VISIBLE
            }
        }
    }

    override fun updateTitle(title: String) {
        toolbarFragmentName.text = title
    }

    override fun updateUserName(userName: String) {
        toolbarUserName.text = userName
    }

    override fun profileImage(image: String) {
        toolbarProfilePicture.loadImage(image)
    }
}

interface updateToolbarTitleListener {
    fun updateTitle(title: String)
    fun updateUserName(userName: String)
    fun profileImage(image: String)
}
