package com.cabel.rutacabel.ui.main

import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.cabel.rutacabel.R
import com.cabel.rutacabel.databinding.ActivityMainBinding
import com.cabel.rutacabel.ui.dashboard.DashboardFragment
import com.cabel.rutacabel.ui.inventory.InventoryFragment
import com.cabel.rutacabel.ui.route.RouteFragment
import com.cabel.rutacabel.ui.client.ClientFragment
import com.cabel.rutacabel.ui.statistics.StatisticsFragment
import com.cabel.rutacabel.utils.PreferenceManager
import com.cabel.rutacabel.utils.UpdateManager
import androidx.activity.OnBackPressedCallback
import com.bumptech.glide.Glide
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var updateManager: UpdateManager
    private var syncDialog: AlertDialog? = null
    private var syncStatusTextView: android.widget.TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(this)

        setupToolbar()
        setupDrawer()
        setupBottomNavigation()

        updateManager = UpdateManager(this)
        updateManager.checkForUpdates()

        setupNavigationHeader()
        setupBackPressed()

        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
        }
        
        scheduleSync()
    }

    private fun setupNavigationHeader() {
        val headerView = binding.navigationView.getHeaderView(0)
        val tvUserName = headerView.findViewById<android.widget.TextView>(R.id.tvUserName)
        val tvUserRole = headerView.findViewById<android.widget.TextView>(R.id.tvUserRole)
        val ivUserAvatar = headerView.findViewById<android.widget.ImageView>(R.id.ivUserAvatar)

        tvUserName.text = preferenceManager.getFullName().ifEmpty { preferenceManager.getUsername() }
        tvUserRole.text = preferenceManager.getRole().ifEmpty { "Usuario" }

        val photoUrl = preferenceManager.getPhotoUrl()
        if (photoUrl.isNotEmpty()) {
            Glide.with(this)
                .load(photoUrl)
                .circleCrop()
                .placeholder(R.mipmap.ic_launcher)
                .into(ivUserAvatar)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)
    }

    private fun setupDrawer() {
        val toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar,
            R.string.app_name, R.string.app_name
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_dashboard -> loadFragment(DashboardFragment())
                R.id.nav_route -> loadFragment(RouteFragment())
                R.id.nav_inventory -> loadFragment(InventoryFragment())
                R.id.nav_clients -> loadFragment(ClientFragment())
                R.id.nav_statistics -> loadFragment(StatisticsFragment())
                R.id.nav_logout -> handleLogout()
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    loadFragment(DashboardFragment())
                    true
                }
                R.id.nav_route -> {
                    loadFragment(RouteFragment())
                    true
                }
                R.id.nav_inventory -> {
                    loadFragment(InventoryFragment())
                    true
                }
                R.id.nav_clients -> {
                    loadFragment(ClientFragment())
                    true
                }
                R.id.nav_statistics -> {
                    loadFragment(StatisticsFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun handleLogout() {
        preferenceManager.clearSession()
        finish()
    }

    private fun setupBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    private val syncReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            if (intent?.action == com.cabel.rutacabel.data.SyncWorker.ACTION_SYNC_STATUS) {
                val syncStarted = intent.getBooleanExtra(com.cabel.rutacabel.data.SyncWorker.EXTRA_SYNC_STARTED, false)
                val syncFinished = intent.getBooleanExtra(com.cabel.rutacabel.data.SyncWorker.EXTRA_SYNC_FINISHED, false)

                if (syncStarted) {
                    showSyncDialog()
                    return
                }

                if (syncFinished) {
                    dismissSyncDialog()
                    return
                }

                // Progress message — update dialog status text
                val message = intent.getStringExtra(com.cabel.rutacabel.data.SyncWorker.EXTRA_MESSAGE) ?: ""
                if (message.isNotEmpty()) {
                    syncStatusTextView?.text = message
                }
            }
        }
    }

    private fun showSyncDialog() {
        if (syncDialog?.isShowing == true) return

        val dialogView = layoutInflater.inflate(R.layout.dialog_sync_loading, null)
        syncStatusTextView = dialogView.findViewById(R.id.tvSyncStatus)

        syncDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        syncDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        syncDialog?.show()
    }

    private fun dismissSyncDialog() {
        syncDialog?.dismiss()
        syncDialog = null
        syncStatusTextView = null
    }

    override fun onResume() {
        super.onResume()
        val filter = android.content.IntentFilter(com.cabel.rutacabel.data.SyncWorker.ACTION_SYNC_STATUS)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(syncReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            registerReceiver(syncReceiver, filter, 0) // Explicitly use no flags or appropriate ones for O+
        } else {
            registerReceiver(syncReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        dismissSyncDialog()
        unregisterReceiver(syncReceiver)
    }

    private fun scheduleSync() {
        val user = preferenceManager.getUser()
        if (user != null) {
            // Only keep periodic sync — initial sync already ran after login
            com.cabel.rutacabel.data.SyncWorker.scheduleSync(this, user.id)
        }
    }
}
