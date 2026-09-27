package com.example.telephony

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class MmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION) {
            val mimeType = intent.type
            Log.d(TAG, "MMS WAP Push delivered. MIME type: $mimeType")
            // Carrier MMS notification received. Real processing placeholder for standard Android PDU receiver
        }
    }
}
