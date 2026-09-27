package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "salim_user_settings")

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val sendOnEnter: Boolean = false,
    val deliveryReports: Boolean = true,
    val otpDetection: Boolean = true,
    val lockScreenPrivacy: String = "FULL", // FULL, SENDER_ONLY, HIDDEN
    val appLockEnabled: Boolean = false,
    val appLockPin: String = "1234",
    val autoResendFailed: Boolean = false,
    val defaultSimId: Int = -1
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val SEND_ON_ENTER = booleanPreferencesKey("send_on_enter")
        val DELIVERY_REPORTS = booleanPreferencesKey("delivery_reports")
        val OTP_DETECTION = booleanPreferencesKey("otp_detection")
        val LOCK_SCREEN_PRIVACY = stringPreferencesKey("lock_screen_privacy")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val APP_LOCK_PIN = stringPreferencesKey("app_lock_pin")
        val AUTO_RESEND = booleanPreferencesKey("auto_resend")
        val DEFAULT_SIM_ID = stringPreferencesKey("default_sim_id")
    }

    val userSettings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val themeModeStr = prefs[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val themeMode = runCatching { ThemeMode.valueOf(themeModeStr) }.getOrDefault(ThemeMode.SYSTEM)
        val dynamicColor = prefs[PreferencesKeys.DYNAMIC_COLOR] ?: false
        val sendOnEnter = prefs[PreferencesKeys.SEND_ON_ENTER] ?: false
        val deliveryReports = prefs[PreferencesKeys.DELIVERY_REPORTS] ?: true
        val otpDetection = prefs[PreferencesKeys.OTP_DETECTION] ?: true
        val lockScreenPrivacy = prefs[PreferencesKeys.LOCK_SCREEN_PRIVACY] ?: "FULL"
        val appLockEnabled = prefs[PreferencesKeys.APP_LOCK_ENABLED] ?: false
        val appLockPin = prefs[PreferencesKeys.APP_LOCK_PIN] ?: "1234"
        val autoResend = prefs[PreferencesKeys.AUTO_RESEND] ?: false
        val simIdStr = prefs[PreferencesKeys.DEFAULT_SIM_ID] ?: "-1"

        UserSettings(
            themeMode = themeMode,
            dynamicColor = dynamicColor,
            sendOnEnter = sendOnEnter,
            deliveryReports = deliveryReports,
            otpDetection = otpDetection,
            lockScreenPrivacy = lockScreenPrivacy,
            appLockEnabled = appLockEnabled,
            appLockPin = appLockPin,
            autoResendFailed = autoResend,
            defaultSimId = simIdStr.toIntOrNull() ?: -1
        )
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun setSendOnEnter(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SEND_ON_ENTER] = enabled
        }
    }

    suspend fun setDeliveryReports(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DELIVERY_REPORTS] = enabled
        }
    }

    suspend fun setOtpDetection(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.OTP_DETECTION] = enabled
        }
    }

    suspend fun setLockScreenPrivacy(privacy: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.LOCK_SCREEN_PRIVACY] = privacy
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.APP_LOCK_ENABLED] = enabled
        }
    }

    suspend fun setAppLockPin(pin: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.APP_LOCK_PIN] = pin
        }
    }

    suspend fun setAutoResendFailed(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.AUTO_RESEND] = enabled
        }
    }
}
