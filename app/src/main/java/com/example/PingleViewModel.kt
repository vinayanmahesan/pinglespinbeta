package com.example

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PingleDatabase
import com.example.data.ScoreEntity
import com.example.data.ScoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.R

enum class Screen {
    HOME, PLAY, HIGH_SCORES, OPTIONS, MANUAL, PINGUI_SETUP, PINGUI_GAME_EDIT
}

class PingleViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PingleDatabase.getDatabase(application)
    private val repository = ScoreRepository(database.scoreDao())
    private val prefs = application.getSharedPreferences("pingle_prefs", Context.MODE_PRIVATE)

    val currentScreen = MutableStateFlow(Screen.HOME)

    val topScores: StateFlow<List<ScoreEntity>> = repository.topScores
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val pingleSpeed = MutableStateFlow(prefs.getFloat("pingle_speed", 1.0f))
    val pingleFriction = MutableStateFlow(prefs.getFloat("pingle_friction", 15.0f))
    val isManualUnlocked = MutableStateFlow(prefs.getBoolean("manual_unlocked", false))
    val totalSpinDuration = MutableStateFlow(prefs.getLong("total_spin_duration", 0L))
    val pingleTint = MutableStateFlow(prefs.getString("pingle_tint", "none") ?: "none")
    val pingleCustomColor = MutableStateFlow(prefs.getInt("pingle_custom_color", 0xFF00FFCC.toInt()))
    val pingleFoldAngleThreshold = MutableStateFlow(prefs.getFloat("pingle_fold_angle_threshold", 120.0f))
    val pingleCustomImageUri = MutableStateFlow(prefs.getString("pingle_custom_image_uri", null))
    val useCustomImage = MutableStateFlow(prefs.getBoolean("use_custom_image", false))
    val customMediaType = MutableStateFlow(prefs.getString("custom_media_type", "image") ?: "image")

    val loadEverything = MutableStateFlow(prefs.getBoolean("load_everything", false))

    var preloadedPurePringle: ImageBitmap? = null
    var preloadedSinglePringle: ImageBitmap? = null
    var preloadedPringle: ImageBitmap? = null
    var preloadedAppIcon: ImageBitmap? = null

    init {
        if (loadEverything.value) {
            preloadAllAssets(application)
        }
    }

    fun preloadAllAssets(context: Context) {
        viewModelScope.launch {
            try {
                val res = context.resources
                preloadedPurePringle = BitmapFactory.decodeResource(res, R.drawable.img_pure_pringle_1780498841768)?.asImageBitmap()
                preloadedSinglePringle = BitmapFactory.decodeResource(res, R.drawable.img_single_pringle_1780498663196)?.asImageBitmap()
                preloadedPringle = BitmapFactory.decodeResource(res, R.drawable.img_pringle_1780497785206)?.asImageBitmap()
                preloadedAppIcon = BitmapFactory.decodeResource(res, R.drawable.img_app_icon_1780497806731)?.asImageBitmap()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearPreloadedAssets() {
        preloadedPurePringle = null
        preloadedSinglePringle = null
        preloadedPringle = null
        preloadedAppIcon = null
    }

    fun setLoadEverything(enabled: Boolean, context: Context): Boolean {
        if (enabled) {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
            val memoryInfo = android.app.ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memoryInfo)
            val totalRamBytes = memoryInfo.totalMem
            val thresholdBytes = 6L * 1024L * 1024L * 1024L // 6 GB
            
            if (totalRamBytes < thresholdBytes) {
                return false
            }
        }
        loadEverything.value = enabled
        prefs.edit().putBoolean("load_everything", enabled).apply()
        if (enabled) {
            preloadAllAssets(context)
        } else {
            clearPreloadedAssets()
        }
        return true
    }

    val normalLayout = MutableStateFlow(loadLayoutConfig("normal"))
    val psmLayout = MutableStateFlow(loadLayoutConfig("psm"))
    val unfoldedNormalLayout = MutableStateFlow(loadLayoutConfig("unfolded_normal"))
    val foldedNormalLayout = MutableStateFlow(loadLayoutConfig("folded_normal"))
    val unfoldedPsmLayout = MutableStateFlow(loadLayoutConfig("unfolded_psm"))
    val foldedPsmLayout = MutableStateFlow(loadLayoutConfig("folded_psm"))

    private fun loadLayoutConfig(config: String): LayoutConfig {
        val fallbackPingleScale = prefs.getFloat("pingle_scale", 1.0f)
        val fallbackPingleOffsetX = prefs.getFloat("pingle_offset_x", 0f)
        val fallbackPingleOffsetY = prefs.getFloat("pingle_offset_y", 0f)
        val fallbackPingleTilt = prefs.getFloat("pingle_tilt", 0f)
        
        val fallbackTimerScale = prefs.getFloat("timer_scale", 1.0f)
        val fallbackTimerOffsetX = prefs.getFloat("timer_offset_x", 0f)
        val fallbackTimerOffsetY = prefs.getFloat("timer_offset_y", 0f)
        val fallbackTimerTilt = prefs.getFloat("timer_tilt", 0f)
        
        val fallbackSpotifyScale = prefs.getFloat("spotify_scale", 1.0f)
        val fallbackSpotifyOffsetX = prefs.getFloat("spotify_offset_x", 0f)
        val fallbackSpotifyOffsetY = prefs.getFloat("spotify_offset_y", 0f)
        val fallbackSpotifyTilt = prefs.getFloat("spotify_tilt", 0f)

        val fallbackDiscScale = prefs.getFloat("disc_scale", 1.0f)
        val fallbackDiscOffsetX = prefs.getFloat("disc_offset_x", 0f)
        val fallbackDiscOffsetY = prefs.getFloat("disc_offset_y", 0f)
        val fallbackDiscTilt = prefs.getFloat("disc_tilt", 0f)

        return LayoutConfig(
            pingleScale = prefs.getFloat("${config}_pingle_scale", fallbackPingleScale),
            pingleOffsetX = prefs.getFloat("${config}_pingle_offset_x", fallbackPingleOffsetX),
            pingleOffsetY = prefs.getFloat("${config}_pingle_offset_y", fallbackPingleOffsetY),
            pingleTilt = prefs.getFloat("${config}_pingle_tilt", fallbackPingleTilt),
            
            timerScale = prefs.getFloat("${config}_timer_scale", fallbackTimerScale),
            timerOffsetX = prefs.getFloat("${config}_timer_offset_x", fallbackTimerOffsetX),
            timerOffsetY = prefs.getFloat("${config}_timer_offset_y", fallbackTimerOffsetY),
            timerTilt = prefs.getFloat("${config}_timer_tilt", fallbackTimerTilt),
            
            spotifyScale = prefs.getFloat("${config}_spotify_scale", fallbackSpotifyScale),
            spotifyOffsetX = prefs.getFloat("${config}_spotify_offset_x", fallbackSpotifyOffsetX),
            spotifyOffsetY = prefs.getFloat("${config}_spotify_offset_y", fallbackSpotifyOffsetY),
            spotifyTilt = prefs.getFloat("${config}_spotify_tilt", fallbackSpotifyTilt),
            
            discScale = prefs.getFloat("${config}_disc_scale", fallbackDiscScale),
            discOffsetX = prefs.getFloat("${config}_disc_offset_x", fallbackDiscOffsetX),
            discOffsetY = prefs.getFloat("${config}_disc_offset_y", fallbackDiscOffsetY),
            discTilt = prefs.getFloat("${config}_disc_tilt", fallbackDiscTilt)
        )
    }

    fun updateLayoutConfig(config: String, layout: LayoutConfig) {
        when (config) {
            "normal" -> normalLayout.value = layout
            "psm" -> psmLayout.value = layout
            "unfolded_normal" -> unfoldedNormalLayout.value = layout
            "folded_normal" -> foldedNormalLayout.value = layout
            "unfolded_psm" -> unfoldedPsmLayout.value = layout
            "folded_psm" -> foldedPsmLayout.value = layout
        }
        
        prefs.edit()
            .putFloat("${config}_pingle_scale", layout.pingleScale)
            .putFloat("${config}_pingle_offset_x", layout.pingleOffsetX)
            .putFloat("${config}_pingle_offset_y", layout.pingleOffsetY)
            .putFloat("${config}_pingle_tilt", layout.pingleTilt)
            
            .putFloat("${config}_timer_scale", layout.timerScale)
            .putFloat("${config}_timer_offset_x", layout.timerOffsetX)
            .putFloat("${config}_timer_offset_y", layout.timerOffsetY)
            .putFloat("${config}_timer_tilt", layout.timerTilt)
            
            .putFloat("${config}_spotify_scale", layout.spotifyScale)
            .putFloat("${config}_spotify_offset_x", layout.spotifyOffsetX)
            .putFloat("${config}_spotify_offset_y", layout.spotifyOffsetY)
            .putFloat("${config}_spotify_tilt", layout.spotifyTilt)
            
            .putFloat("${config}_disc_scale", layout.discScale)
            .putFloat("${config}_disc_offset_x", layout.discOffsetX)
            .putFloat("${config}_disc_offset_y", layout.discOffsetY)
            .putFloat("${config}_disc_tilt", layout.discTilt)
            .apply()
    }

    fun resetLayoutConfig(config: String) {
        updateLayoutConfig(config, LayoutConfig())
    }

    fun loadActiveLayoutForConfig(config: String) {
        val layout = when (config) {
            "normal" -> normalLayout.value
            "psm" -> psmLayout.value
            "unfolded_normal" -> unfoldedNormalLayout.value
            "folded_normal" -> foldedNormalLayout.value
            "unfolded_psm" -> unfoldedPsmLayout.value
            "folded_psm" -> foldedPsmLayout.value
            else -> LayoutConfig()
        }
        setPingleScale(layout.pingleScale)
        setPingleOffsetX(layout.pingleOffsetX)
        setPingleOffsetY(layout.pingleOffsetY)
        setPingleTilt(layout.pingleTilt)
        
        setTimerScale(layout.timerScale)
        setTimerOffsetX(layout.timerOffsetX)
        setTimerOffsetY(layout.timerOffsetY)
        setTimerTilt(layout.timerTilt)
        
        setSpotifyScale(layout.spotifyScale)
        setSpotifyOffsetX(layout.spotifyOffsetX)
        setSpotifyOffsetY(layout.spotifyOffsetY)
        setSpotifyTilt(layout.spotifyTilt)
        
        setDiscScale(layout.discScale)
        setDiscOffsetX(layout.discOffsetX)
        setDiscOffsetY(layout.discOffsetY)
        setDiscTilt(layout.discTilt)
    }

    fun saveActiveLayoutForConfig(config: String) {
        val layout = LayoutConfig(
            pingleScale = pingleScale.value,
            pingleOffsetX = pingleOffsetX.value,
            pingleOffsetY = pingleOffsetY.value,
            pingleTilt = pingleTilt.value,
            
            timerScale = timerScale.value,
            timerOffsetX = timerOffsetX.value,
            timerOffsetY = timerOffsetY.value,
            timerTilt = timerTilt.value,
            
            spotifyScale = spotifyScale.value,
            spotifyOffsetX = spotifyOffsetX.value,
            spotifyOffsetY = spotifyOffsetY.value,
            spotifyTilt = spotifyTilt.value,
            
            discScale = discScale.value,
            discOffsetX = discOffsetX.value,
            discOffsetY = discOffsetY.value,
            discTilt = discTilt.value
        )
        updateLayoutConfig(config, layout)
    }

    val pingleScale = MutableStateFlow(prefs.getFloat("pingle_scale", 1.0f))
    val pingleOffsetX = MutableStateFlow(prefs.getFloat("pingle_offset_x", 0f))
    val pingleOffsetY = MutableStateFlow(prefs.getFloat("pingle_offset_y", 0f))
    val pingleTilt = MutableStateFlow(prefs.getFloat("pingle_tilt", 0f))

    val timerScale = MutableStateFlow(prefs.getFloat("timer_scale", 1.0f))
    val timerOffsetX = MutableStateFlow(prefs.getFloat("timer_offset_x", 0f))
    val timerOffsetY = MutableStateFlow(prefs.getFloat("timer_offset_y", 0f))
    val timerTilt = MutableStateFlow(prefs.getFloat("timer_tilt", 0f))

    val spotifyScale = MutableStateFlow(prefs.getFloat("spotify_scale", 1.0f))
    val spotifyOffsetX = MutableStateFlow(prefs.getFloat("spotify_offset_x", 0f))
    val spotifyOffsetY = MutableStateFlow(prefs.getFloat("spotify_offset_y", 0f))
    val spotifyTilt = MutableStateFlow(prefs.getFloat("spotify_tilt", 0f))

    val discScale = MutableStateFlow(prefs.getFloat("disc_scale", 1.0f))
    val discOffsetX = MutableStateFlow(prefs.getFloat("disc_offset_x", 0f))
    val discOffsetY = MutableStateFlow(prefs.getFloat("disc_offset_y", 0f))
    val discTilt = MutableStateFlow(prefs.getFloat("disc_tilt", 0f))

    fun setPingleScale(value: Float) {
        pingleScale.value = value
        prefs.edit().putFloat("pingle_scale", value).apply()
    }
    fun setPingleOffsetX(value: Float) {
        pingleOffsetX.value = value
        prefs.edit().putFloat("pingle_offset_x", value).apply()
    }
    fun setPingleOffsetY(value: Float) {
        pingleOffsetY.value = value
        prefs.edit().putFloat("pingle_offset_y", value).apply()
    }
    fun setPingleTilt(value: Float) {
        pingleTilt.value = value
        prefs.edit().putFloat("pingle_tilt", value).apply()
    }

    fun setTimerScale(value: Float) {
        timerScale.value = value
        prefs.edit().putFloat("timer_scale", value).apply()
    }
    fun setTimerOffsetX(value: Float) {
        timerOffsetX.value = value
        prefs.edit().putFloat("timer_offset_x", value).apply()
    }
    fun setTimerOffsetY(value: Float) {
        timerOffsetY.value = value
        prefs.edit().putFloat("timer_offset_y", value).apply()
    }
    fun setTimerTilt(value: Float) {
        timerTilt.value = value
        prefs.edit().putFloat("timer_tilt", value).apply()
    }

    fun setSpotifyScale(value: Float) {
        spotifyScale.value = value
        prefs.edit().putFloat("spotify_scale", value).apply()
    }
    fun setSpotifyOffsetX(value: Float) {
        spotifyOffsetX.value = value
        prefs.edit().putFloat("spotify_offset_x", value).apply()
    }
    fun setSpotifyOffsetY(value: Float) {
        spotifyOffsetY.value = value
        prefs.edit().putFloat("spotify_offset_y", value).apply()
    }
    fun setSpotifyTilt(value: Float) {
        spotifyTilt.value = value
        prefs.edit().putFloat("spotify_tilt", value).apply()
    }

    fun setDiscScale(value: Float) {
        discScale.value = value
        prefs.edit().putFloat("disc_scale", value).apply()
    }
    fun setDiscOffsetX(value: Float) {
        discOffsetX.value = value
        prefs.edit().putFloat("disc_offset_x", value).apply()
    }
    fun setDiscOffsetY(value: Float) {
        discOffsetY.value = value
        prefs.edit().putFloat("disc_offset_y", value).apply()
    }
    fun setDiscTilt(value: Float) {
        discTilt.value = value
        prefs.edit().putFloat("disc_tilt", value).apply()
    }

    fun resetAllPingui() {
        setPingleScale(1.0f)
        setPingleOffsetX(0f)
        setPingleOffsetY(0f)
        setPingleTilt(0f)
        setTimerScale(1.0f)
        setTimerOffsetX(0f)
        setTimerOffsetY(0f)
        setTimerTilt(0f)
        setSpotifyScale(1.0f)
        setSpotifyOffsetX(0f)
        setSpotifyOffsetY(0f)
        setSpotifyTilt(0f)
        setDiscScale(1.0f)
        setDiscOffsetX(0f)
        setDiscOffsetY(0f)
        setDiscTilt(0f)
    }

    val isDebugUnlocked = MutableStateFlow(prefs.getBoolean("debug_unlocked", false))
    val easterRainbowNeon = MutableStateFlow(false)
    val easterMatrixBg = MutableStateFlow(false)
    val easterReverseSpin = MutableStateFlow(false)
    val easterSpaceStars = MutableStateFlow(false)

    val isRainbowNeonUnlocked = MutableStateFlow(false)
    val isMatrixBgUnlocked = MutableStateFlow(false)
    val isReverseSpinUnlocked = MutableStateFlow(false)
    val isSpaceStarsUnlocked = MutableStateFlow(false)

    val invisiblePingleEnabled = MutableStateFlow(false)

    fun setInvisiblePingleEnabled(enabled: Boolean) {}
    fun setEasterRainbowNeon(enabled: Boolean) {}
    fun setEasterMatrixBg(enabled: Boolean) {}
    fun setEasterReverseSpin(enabled: Boolean) {}
    fun setEasterSpaceStars(enabled: Boolean) {}
    fun setRainbowNeonUnlocked(unlocked: Boolean) {}
    fun setMatrixBgUnlocked(unlocked: Boolean) {}
    fun setReverseSpinUnlocked(unlocked: Boolean) {}
    fun setSpaceStarsUnlocked(unlocked: Boolean) {}

    fun setDebugUnlocked(unlocked: Boolean) {
        isDebugUnlocked.value = unlocked
        prefs.edit().putBoolean("debug_unlocked", unlocked).apply()
    }

    fun setCustomImageUri(uriString: String?) {
        pingleCustomImageUri.value = uriString
        prefs.edit().putString("pingle_custom_image_uri", uriString).apply()
    }

    fun setCustomMediaType(type: String) {
        customMediaType.value = type
        prefs.edit().putString("custom_media_type", type).apply()
    }

    fun setUseCustomImage(use: Boolean) {
        useCustomImage.value = use
        prefs.edit().putBoolean("use_custom_image", use).apply()
    }

    fun setScreen(screen: Screen) {
        currentScreen.value = screen
    }

    fun setPingleFoldAngleThreshold(angle: Float) {
        val rounded = Math.round(angle).toFloat().coerceIn(0f, 180f)
        pingleFoldAngleThreshold.value = rounded
        prefs.edit().putFloat("pingle_fold_angle_threshold", rounded).apply()
    }

    fun setPingleSpeed(speed: Float) {
        val rounded = Math.round(speed * 10f) / 10f
        pingleSpeed.value = rounded.coerceIn(0.5f, 10.0f)
        prefs.edit().putFloat("pingle_speed", rounded).apply()
    }

    fun setPingleFriction(friction: Float) {
        val rounded = Math.round(friction).toFloat().coerceIn(0f, 100f)
        pingleFriction.value = rounded
        prefs.edit().putFloat("pingle_friction", rounded).apply()
    }

    fun unlockManual() {
        isManualUnlocked.value = true
        prefs.edit().putBoolean("manual_unlocked", true).apply()
    }

    fun unlockEverything() {
        isManualUnlocked.value = true
        prefs.edit().putBoolean("manual_unlocked", true).apply()
        isDebugUnlocked.value = true
        prefs.edit().putBoolean("debug_unlocked", true).apply()

        val targetDuration = 18000000L // 5 hours in ms
        if (totalSpinDuration.value < targetDuration) {
            totalSpinDuration.value = targetDuration
            prefs.edit().putLong("total_spin_duration", targetDuration).apply()
        }
    }

    fun checkEasterEggUnlocks(durationMs: Long, totalDuration: Long) {
        // disabled
    }

    fun adjustSpeed(increase: Boolean) {
        val current = pingleSpeed.value
        val step = 0.5f
        val newVal = if (increase) {
            (current + step).coerceAtMost(10.0f)
        } else {
            (current - step).coerceAtLeast(0.5f)
        }
        val rounded = Math.round(newVal * 10f) / 10f
        pingleSpeed.value = rounded
        prefs.edit().putFloat("pingle_speed", rounded).apply()
    }

    fun saveScore(durationMs: Long, isManual: Boolean = false) {
        if (durationMs < 100L && !invisiblePingleEnabled.value) return // don't save ultra-tiny accidental spinnigs
        viewModelScope.launch {
            repository.insertScore(ScoreEntity(durationMs = durationMs, isManual = isManual))
            val currentTotal = prefs.getLong("total_spin_duration", 0L)
            val newTotal = currentTotal + durationMs
            prefs.edit().putLong("total_spin_duration", newTotal).apply()
            totalSpinDuration.value = newTotal
            checkEasterEggUnlocks(durationMs, newTotal)
        }
    }

    fun saveDebugScore(durationMs: Long) {
        viewModelScope.launch {
            repository.insertScore(ScoreEntity(durationMs = durationMs, isManual = false, isDebug = true))
        }
    }

    fun setPingleTint(tintId: String) {
        pingleTint.value = tintId
        prefs.edit().putString("pingle_tint", tintId).apply()
    }

    fun setPingleCustomColor(colorInt: Int) {
        pingleCustomColor.value = colorInt
        prefs.edit().putInt("pingle_custom_color", colorInt).apply()
    }

    fun addDebugSpinDuration(ms: Long) {
        val currentTotal = prefs.getLong("total_spin_duration", 0L)
        val newTotal = currentTotal + ms
        prefs.edit().putLong("total_spin_duration", newTotal).apply()
        totalSpinDuration.value = newTotal
    }

    fun clearAllScores() {
        viewModelScope.launch {
            repository.clearScores()
            prefs.edit().putLong("total_spin_duration", 0L).apply()
            totalSpinDuration.value = 0L
        }
    }

    val buttonVibrationMode = MutableStateFlow(prefs.getString("button_vibration_mode", "medium") ?: "medium")
    val spinVibrationMode = MutableStateFlow(prefs.getString("spin_vibration_mode", "light") ?: "light")
    val spinVibrationInterval = MutableStateFlow(prefs.getFloat("spin_vibration_interval", 15.0f))

    fun setButtonVibrationMode(mode: String) {
        buttonVibrationMode.value = mode
        prefs.edit().putString("button_vibration_mode", mode).apply()
    }

    fun setSpinVibrationMode(mode: String) {
        spinVibrationMode.value = mode
        prefs.edit().putString("spin_vibration_mode", mode).apply()
    }

    fun setSpinVibrationInterval(interval: Float) {
        val rounded = Math.round(interval * 10f) / 10f
        val clamped = rounded.coerceIn(5.0f, 90.0f)
        spinVibrationInterval.value = clamped
        prefs.edit().putFloat("spin_vibration_interval", clamped).apply()
    }

    val isChineseCrashed = MutableStateFlow(prefs.getBoolean("is_chinese_crashed", false))

    fun triggerChineseCrash() {
        isChineseCrashed.value = true
        prefs.edit().putBoolean("is_chinese_crashed", true).apply()
    }
}

data class LayoutConfig(
    val pingleScale: Float = 1.0f,
    val pingleOffsetX: Float = 0f,
    val pingleOffsetY: Float = 0f,
    val pingleTilt: Float = 0f,
    val timerScale: Float = 1.0f,
    val timerOffsetX: Float = 0f,
    val timerOffsetY: Float = 0f,
    val timerTilt: Float = 0f,
    val spotifyScale: Float = 1.0f,
    val spotifyOffsetX: Float = 0f,
    val spotifyOffsetY: Float = 0f,
    val spotifyTilt: Float = 0f,
    val discScale: Float = 1.0f,
    val discOffsetX: Float = 0f,
    val discOffsetY: Float = 0f,
    val discTilt: Float = 0f
)

