package com.example.telephony

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.TelephonyManager

object TelephonyManagerHelper {

    fun isDefaultSmsApp(context: Context): Boolean {
        val defaultPackage = Telephony.Sms.getDefaultSmsPackage(context)
        return defaultPackage != null && defaultPackage == context.packageName
    }

    fun createRequestDefaultSmsIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
            } else {
                fallbackSmsIntent(context)
            }
        } else {
            fallbackSmsIntent(context)
        }
    }

    private fun fallbackSmsIntent(context: Context): Intent {
        val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
        return intent
    }

    fun hasSimCard(context: Context): Boolean {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        return telephonyManager?.simState == TelephonyManager.SIM_STATE_READY
    }

    fun getCarrierName(context: Context): String {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val simOperatorName = telephonyManager?.simOperatorName
        return if (!simOperatorName.isNullOrBlank()) simOperatorName else "Default SIM"
    }
}
