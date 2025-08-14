package com.example.jugglingtracker.ui.theme

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.jugglingtracker.data.entities.UsageLevel
import com.example.jugglingtracker.data.repository.UsageTrackingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicThemeManager @Inject constructor(
    private val context: Context,
    private val usageTrackingRepository: UsageTrackingRepository
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    
    private val _currentAccentColor = MutableLiveData<String>()
    val currentAccentColor: LiveData<String> = _currentAccentColor
    
    private val _currentUsageLevel = MutableLiveData<UsageLevel>()
    val currentUsageLevel: LiveData<UsageLevel> = _currentUsageLevel
    
    private val _isDarkTheme = MutableLiveData<Boolean>()
    val isDarkTheme: LiveData<Boolean> = _isDarkTheme
    
    companion object {
        private const val PREF_IS_DARK_THEME = "is_dark_theme"
        private const val PREF_LAST_ACCENT_COLOR = "last_accent_color"
        private const val PREF_LAST_USAGE_LEVEL = "last_usage_level"
        private const val PREF_DYNAMIC_COLORS_ENABLED = "dynamic_colors_enabled"
        
        // Default colors when usage tracking is not available
        private const val DEFAULT_LIGHT_ACCENT = "#FF3182CE"
        private const val DEFAULT_DARK_ACCENT = "#FF42A5F5"
    }
    
    init {
        // Load saved preferences
        _isDarkTheme.value = prefs.getBoolean(PREF_IS_DARK_THEME, false)
        _currentAccentColor.value = prefs.getString(PREF_LAST_ACCENT_COLOR, DEFAULT_LIGHT_ACCENT)
        
        // Initialize with saved or default values
        updateThemeFromUsage()
    }
    
    fun toggleDarkTheme() {
        val newDarkTheme = !(_isDarkTheme.value ?: false)
        _isDarkTheme.value = newDarkTheme
        prefs.edit().putBoolean(PREF_IS_DARK_THEME, newDarkTheme).apply()
        
        // Update accent color for new theme
        updateThemeFromUsage()
    }
    
    fun setDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
        prefs.edit().putBoolean(PREF_IS_DARK_THEME, isDark).apply()
        updateThemeFromUsage()
    }
    
    fun isDynamicColorsEnabled(): Boolean {
        return prefs.getBoolean(PREF_DYNAMIC_COLORS_ENABLED, true)
    }
    
    fun setDynamicColorsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_DYNAMIC_COLORS_ENABLED, enabled).apply()
        if (enabled) {
            updateThemeFromUsage()
        } else {
            // Reset to default colors
            val defaultColor = if (_isDarkTheme.value == true) DEFAULT_DARK_ACCENT else DEFAULT_LIGHT_ACCENT
            _currentAccentColor.value = defaultColor
            prefs.edit().putString(PREF_LAST_ACCENT_COLOR, defaultColor).apply()
        }
    }
    
    fun updateThemeFromUsage() {
        if (!isDynamicColorsEnabled()) return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val usageLevel = usageTrackingRepository.getCurrentUsageLevel()
                val accentColor = usageTrackingRepository.getCurrentAccentColor(_isDarkTheme.value ?: false)
                
                CoroutineScope(Dispatchers.Main).launch {
                    _currentUsageLevel.value = usageLevel
                    _currentAccentColor.value = accentColor
                    
                    // Save to preferences
                    prefs.edit()
                        .putString(PREF_LAST_ACCENT_COLOR, accentColor)
                        .putInt(PREF_LAST_USAGE_LEVEL, usageLevel.level)
                        .apply()
                }
            } catch (e: Exception) {
                // Fallback to default colors if usage tracking fails
                CoroutineScope(Dispatchers.Main).launch {
                    val defaultColor = if (_isDarkTheme.value == true) DEFAULT_DARK_ACCENT else DEFAULT_LIGHT_ACCENT
                    _currentAccentColor.value = defaultColor
                }
            }
        }
    }
    
    fun getCurrentAccentColorInt(): Int {
        return try {
            Color.parseColor(_currentAccentColor.value ?: DEFAULT_LIGHT_ACCENT)
        } catch (e: Exception) {
            Color.parseColor(DEFAULT_LIGHT_ACCENT)
        }
    }
    
    fun getAccentColorForUsageLevel(level: UsageLevel, isDark: Boolean = _isDarkTheme.value ?: false): String {
        return if (isDark) level.darkColor else level.color
    }
    
    // Get complementary colors based on current accent
    fun getPrimaryVariantColor(): String {
        val currentColor = _currentAccentColor.value ?: DEFAULT_LIGHT_ACCENT
        return adjustColorBrightness(currentColor, -0.2f)
    }
    
    fun getSecondaryColor(): String {
        val currentColor = _currentAccentColor.value ?: DEFAULT_LIGHT_ACCENT
        return adjustColorHue(currentColor, 30f) // Shift hue by 30 degrees
    }
    
    private fun adjustColorBrightness(colorString: String, factor: Float): String {
        return try {
            val color = Color.parseColor(colorString)
            val hsv = FloatArray(3)
            Color.colorToHSV(color, hsv)
            hsv[2] = (hsv[2] * (1 + factor)).coerceIn(0f, 1f)
            String.format("#%08X", Color.HSVToColor(hsv))
        } catch (e: Exception) {
            colorString
        }
    }
    
    private fun adjustColorHue(colorString: String, hueDelta: Float): String {
        return try {
            val color = Color.parseColor(colorString)
            val hsv = FloatArray(3)
            Color.colorToHSV(color, hsv)
            hsv[0] = (hsv[0] + hueDelta) % 360f
            String.format("#%08X", Color.HSVToColor(hsv))
        } catch (e: Exception) {
            colorString
        }
    }
    
    // Theme data class for easy access
    data class ThemeColors(
        val primary: String,
        val primaryVariant: String,
        val secondary: String,
        val isDark: Boolean,
        val usageLevel: UsageLevel
    )
    
    fun getCurrentThemeColors(): ThemeColors {
        return ThemeColors(
            primary = _currentAccentColor.value ?: DEFAULT_LIGHT_ACCENT,
            primaryVariant = getPrimaryVariantColor(),
            secondary = getSecondaryColor(),
            isDark = _isDarkTheme.value ?: false,
            usageLevel = _currentUsageLevel.value ?: UsageLevel.LEVELS.first()
        )
    }
    
    // Method to be called when app starts or resumes
    fun refreshTheme() {
        updateThemeFromUsage()
    }
    
    // Method to be called when usage events are tracked
    fun onUsageEventTracked() {
        if (isDynamicColorsEnabled()) {
            updateThemeFromUsage()
        }
    }
}