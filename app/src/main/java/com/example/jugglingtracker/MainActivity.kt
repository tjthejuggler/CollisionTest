package com.example.jugglingtracker

import android.os.Bundle
import android.view.Menu
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.jugglingtracker.databinding.ActivityMainBinding
import com.example.jugglingtracker.services.UsageTrackingService
import com.example.jugglingtracker.ui.theme.DynamicThemeManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    
    @Inject
    lateinit var usageTrackingService: UsageTrackingService
    
    @Inject
    lateinit var dynamicThemeManager: DynamicThemeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)
        
        // Initialize usage tracking
        usageTrackingService.startSession()
        
        // Initialize dynamic theming
        setupDynamicTheming()

        binding.appBarMain.fab.setOnClickListener { view ->
            // Navigate to add pattern screen when FAB is clicked
            val navController = findNavController(R.id.nav_host_fragment_content_main)
            navController.navigate(R.id.nav_add_edit_pattern)
        }
        
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_patterns, R.id.nav_stats, R.id.nav_test_history, R.id.nav_progress_chart, R.id.nav_settings
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        
        // Track navigation events
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.nav_patterns -> usageTrackingService.trackPatternViewed(0) // General patterns view
                R.id.nav_stats -> { /* Stats viewing is tracked within the fragment */ }
                R.id.nav_test_history -> usageTrackingService.trackHistoryViewed()
                R.id.nav_progress_chart -> usageTrackingService.trackProgressViewed()
                R.id.nav_settings -> usageTrackingService.trackSettingsAccessed()
            }
        }
    }
    
    private fun setupDynamicTheming() {
        // Observe theme changes and apply them
        dynamicThemeManager.currentAccentColor.observe(this) { color ->
            // Update dynamic colors in the theme
            updateDynamicColors(color)
        }
        
        // Refresh theme on startup
        dynamicThemeManager.refreshTheme()
    }
    
    private fun updateDynamicColors(accentColor: String) {
        // This would typically involve updating the theme colors programmatically
        // For now, we'll let the theme manager handle the color updates
        lifecycleScope.launch {
            // The theme colors will be updated through the resource system
            // when the theme manager updates the dynamic color resources
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh theme when app resumes
        dynamicThemeManager.refreshTheme()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // End usage tracking session
        usageTrackingService.endSession()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}