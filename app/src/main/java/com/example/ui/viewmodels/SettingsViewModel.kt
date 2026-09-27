package com.example.ui.viewmodels

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SalimDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.UserSettings
import com.example.telephony.TelephonyManagerHelper
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class PermissionStatus(
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val permission: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = UserPreferencesRepository(application)
    private val db = SalimDatabase.getInstance(application)

    val settings: StateFlow<UserSettings> = preferencesRepository.userSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    private val _permissionsHealth = MutableStateFlow<List<PermissionStatus>>(emptyList())
    val permissionsHealth: StateFlow<List<PermissionStatus>> = _permissionsHealth.asStateFlow()

    private val _exportResult = MutableStateFlow<String?>(null)
    val exportResult: StateFlow<String?> = _exportResult.asStateFlow()

    init {
        checkPermissions()
    }

    fun checkPermissions() {
        val context = getApplication<Application>()
        val isDefault = TelephonyManagerHelper.isDefaultSmsApp(context)
        val hasSms = ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        val hasContacts = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        _permissionsHealth.value = listOf(
            PermissionStatus(
                title = "Default SMS App",
                description = if (isDefault) "Salim is handling all SMS & MMS" else "Tap to set Salim as default handler",
                isGranted = isDefault
            ),
            PermissionStatus(
                title = "SMS Telephony",
                description = "Send and receive cellular messages directly",
                isGranted = hasSms,
                permission = android.Manifest.permission.SEND_SMS
            ),
            PermissionStatus(
                title = "Contacts Integration",
                description = "Display contact names and avatars privately",
                isGranted = hasContacts,
                permission = android.Manifest.permission.READ_CONTACTS
            ),
            PermissionStatus(
                title = "Notifications",
                description = "Display alerts and allow inline replies",
                isGranted = hasNotif,
                permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) android.Manifest.permission.POST_NOTIFICATIONS else null
            )
        )
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDynamicColor(enabled)
        }
    }

    fun setSendOnEnter(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setSendOnEnter(enabled)
        }
    }

    fun setDeliveryReports(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDeliveryReports(enabled)
        }
    }

    fun setOtpDetection(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setOtpDetection(enabled)
        }
    }

    fun setLockScreenPrivacy(privacy: String) {
        viewModelScope.launch {
            preferencesRepository.setLockScreenPrivacy(privacy)
        }
    }

    fun setAppLock(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAppLockEnabled(enabled)
        }
    }

    fun setAppLockPin(pin: String) {
        viewModelScope.launch {
            preferencesRepository.setAppLockPin(pin)
        }
    }

    fun exportMessagesToJson() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val messages = db.messageDao().getStarredMessages() // or all messages
                    val jsonArray = JSONArray()
                    val file = File(getApplication<Application>().filesDir, "salim_export_${System.currentTimeMillis()}.json")
                    val root = JSONObject().apply {
                        put("app", "Salim")
                        put("version", "1.0.0")
                        put("exportTimestamp", System.currentTimeMillis())
                    }
                    file.writeText(root.toString(2))
                    "Export saved locally: ${file.name}"
                } catch (e: Exception) {
                    "Export failed: ${e.localizedMessage}"
                }
            }
            _exportResult.value = result
        }
    }

    fun clearExportResult() {
        _exportResult.value = null
    }
}
