package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.example.MainActivity
import com.example.R
import com.example.data.local.UserPreferencesRepository
import com.example.telephony.PhoneNumberUtils
import kotlinx.coroutines.flow.firstOrNull

object NotificationCoordinator {

    const val CHANNEL_MESSAGES = "salim_channel_messages"
    const val CHANNEL_SCHEDULED = "salim_channel_scheduled"
    const val CHANNEL_OTP = "salim_channel_otp"

    private const val NOTIFICATION_ID_BASE = 1000

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return

            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming text messages and conversation updates"
                enableVibration(true)
                setShowBadge(true)
            }

            val scheduledChannel = NotificationChannel(
                CHANNEL_SCHEDULED,
                "Scheduled Messages",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts when scheduled messages are sent"
            }

            val otpChannel = NotificationChannel(
                CHANNEL_OTP,
                "Security & Verification Codes",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "One-time passwords and verification codes with quick copy"
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(listOf(messagesChannel, scheduledChannel, otpChannel))
        }
    }

    suspend fun showIncomingMessageNotification(
        context: Context,
        conversationId: Long,
        senderAddress: String,
        senderName: String,
        body: String,
        otpCode: String? = null
    ) {
        createNotificationChannels(context)

        val prefsRepo = UserPreferencesRepository(context)
        val settings = prefsRepo.userSettings.firstOrNull()
        val lockPrivacy = settings?.lockScreenPrivacy ?: "FULL"

        val displayName = if (senderName.isNotBlank()) senderName else PhoneNumberUtils.formatForDisplay(senderAddress)
        val notificationId = (NOTIFICATION_ID_BASE + (conversationId % 10000)).toInt()

        // Content intent: open conversation in MainActivity
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("extra_conversation_id", conversationId)
            putExtra("extra_address", senderAddress)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // RemoteInput for Inline Reply
        val remoteInput = RemoteInput.Builder(NotificationActionReceiver.KEY_TEXT_REPLY)
            .setLabel("Reply to $displayName")
            .build()

        val replyIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_INLINE_REPLY
            putExtra(NotificationActionReceiver.EXTRA_CONVERSATION_ID, conversationId)
            putExtra(NotificationActionReceiver.EXTRA_ADDRESS, senderAddress)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            "Reply",
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        // Mark as Read Action
        val markReadIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_READ
            putExtra(NotificationActionReceiver.EXTRA_CONVERSATION_ID, conversationId)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val markReadPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val markReadAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_agenda,
            "Mark as read",
            markReadPendingIntent
        ).build()

        val channel = if (otpCode != null) CHANNEL_OTP else CHANNEL_MESSAGES

        val userPerson = Person.Builder()
            .setName(displayName)
            .setKey(senderAddress)
            .build()

        val displayContent = when (lockPrivacy) {
            "HIDDEN" -> "New message"
            "SENDER_ONLY" -> "Message from $displayName"
            else -> body
        }

        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(displayName)
            .setContentText(displayContent)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .addAction(replyAction)
            .addAction(markReadAction)

        // If OTP detected, add Copy Code Action
        if (otpCode != null) {
            val copyIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_COPY_OTP
                putExtra(NotificationActionReceiver.EXTRA_OTP_CODE, otpCode)
                putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            }
            val copyPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 3,
                copyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val copyAction = NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_save,
                "Copy code ($otpCode)",
                copyPendingIntent
            ).build()
            builder.addAction(copyAction)
        }

        if (lockPrivacy == "FULL") {
            val messagingStyle = NotificationCompat.MessagingStyle(userPerson)
                .addMessage(body, System.currentTimeMillis(), userPerson)
            builder.setStyle(messagingStyle)
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Notifications permission not granted
        }
    }

    fun dismissNotification(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
}
