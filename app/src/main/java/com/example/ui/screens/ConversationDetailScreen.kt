package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.InternalStorageImageManager
import com.example.data.local.MessageEntity
import com.example.telephony.PhoneNumberUtils
import com.example.ui.components.AvatarView
import com.example.ui.components.ComposerDock
import com.example.ui.components.DateSeparator
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassIconButton
import com.example.ui.components.MessageBubble
import com.example.ui.theme.LocalGlassColors
import com.example.ui.theme.SalimRed
import com.example.ui.viewmodels.ConversationViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationDetailScreen(
    viewModel: ConversationViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val glassColors = LocalGlassColors.current
    val coroutineScope = rememberCoroutineScope()
    val conversation by viewModel.conversation.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val draftText by viewModel.draftText.collectAsStateWithLifecycle()
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    var hasInitiallyScrolled by remember { mutableStateOf(false) }
    var previousMessageCount by remember { mutableIntStateOf(0) }

    var selectedMessageForAction by remember { mutableStateOf<MessageEntity?>(null) }
    var showActionSheet by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }

    // Android Photo Picker for zero-permission image attachment
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                // Persist image directly into app's private storage so it never disappears
                val persistentPath = InternalStorageImageManager.saveImageToInternalStorage(context, uri)
                if (persistentPath != null) {
                    viewModel.sendMessage(attachmentUri = persistentPath, attachmentMimeType = "image/jpeg")
                } else {
                    Toast.makeText(context, "Could not attach image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    BackHandler {
        onNavigateBack()
    }

    // Immediately land on the latest message on launch without starting at the top and auto-scrolling visibly down
    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            if (!hasInitiallyScrolled) {
                // Instant initial scroll without animation
                listState.scrollToItem(messages.size - 1)
                hasInitiallyScrolled = true
                previousMessageCount = messages.size
            } else if (messages.size > previousMessageCount) {
                // Smooth scroll down only when a new message arrives
                listState.animateScrollToItem(messages.size - 1)
                previousMessageCount = messages.size
            }
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Scroll-to-bottom button is visible ONLY when user isn't on the latest message; fades out on latest message
    val isScrolledAwayFromLatest by remember {
        derivedStateOf {
            if (messages.isEmpty()) false
            else {
                val layoutInfo = listState.layoutInfo
                val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisibleIndex < messages.size - 1
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = glassColors.canvasBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // Apple-inspired Frosted Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(LocalGlassColors.current.surfaceGlass)
                    .border(0.5.dp, LocalGlassColors.current.borderGlass)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("back_button")
                )

                Spacer(modifier = Modifier.width(6.dp))

                val name = conversation?.recipientName ?: ""
                val address = conversation?.recipientAddress ?: ""
                val displayName = name.ifBlank { PhoneNumberUtils.formatForDisplay(address) }

                // Tapping contact redirects directly to default Android Contacts app details screen
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                openContactDetails(context, address)
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AvatarView(
                        nameOrNumber = displayName,
                        size = 38.dp
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = PhoneNumberUtils.formatForDisplay(address),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp,
                            maxLines = 1
                        )
                    }
                }

                GlassIconButton(
                    icon = Icons.Default.Call,
                    contentDescription = "Call Contact",
                    onClick = {
                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:$address")
                        }
                        context.startActivity(dialIntent)
                    },
                    modifier = Modifier.testTag("call_button")
                )
            }
        },
        bottomBar = {
            ComposerDock(
                text = draftText,
                onTextChanged = { viewModel.onDraftTextChanged(it) },
                onSendClick = { viewModel.sendMessage() },
                onAttachClick = { showAttachmentSheet = true },
                onScheduleClick = { showScheduleDialog = true }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)
            ) {
                var lastDateMillis = 0L

                items(
                    items = messages,
                    key = { it.id }
                ) { message ->
                    if (isDifferentDay(lastDateMillis, message.timestamp)) {
                        DateSeparator(timestamp = message.timestamp)
                        lastDateMillis = message.timestamp
                    }

                    MessageBubble(
                        message = message,
                        onLongClick = {
                            selectedMessageForAction = message
                            showActionSheet = true
                        },
                        onRetryClick = {
                            viewModel.retryMessage(message.id)
                        }
                    )
                }
            }

            // Scroll-to-bottom floating button: visible ONLY when away from latest message, fades out on latest message
            AnimatedVisibility(
                visible = isScrolledAwayFromLatest,
                enter = fadeIn(animationSpec = tween(180)),
                exit = fadeOut(animationSpec = tween(180)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(LocalGlassColors.current.surfaceGlass)
                        .border(0.5.dp, LocalGlassColors.current.borderGlass, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            coroutineScope.launch {
                                if (messages.isNotEmpty()) {
                                    listState.animateScrollToItem(messages.size - 1)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Scroll to bottom",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }

    // Message Actions Bottom Sheet (Apple iOS HIG)
    if (showActionSheet && selectedMessageForAction != null) {
        val msg = selectedMessageForAction!!
        ModalBottomSheet(
            onDismissRequest = {
                showActionSheet = false
                selectedMessageForAction = null
            },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Message Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                ActionRowItem(
                    icon = Icons.Default.ContentCopy,
                    title = "Copy Text",
                    onClick = {
                        val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clip?.setPrimaryClip(ClipData.newPlainText("Salim Message", msg.body))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        showActionSheet = false
                    }
                )

                ActionRowItem(
                    icon = Icons.Default.Share,
                    title = "Share",
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, msg.body)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Message"))
                        showActionSheet = false
                    }
                )

                ActionRowItem(
                    icon = if (msg.isStarred) Icons.Default.StarOutline else Icons.Default.Star,
                    title = if (msg.isStarred) "Unstar" else "Star Message",
                    onClick = {
                        viewModel.toggleStarred(msg)
                        showActionSheet = false
                    }
                )

                ActionRowItem(
                    icon = Icons.Default.Delete,
                    title = "Delete Message",
                    tint = SalimRed,
                    onClick = {
                        viewModel.deleteMessage(msg.id)
                        showActionSheet = false
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Attachment & Quick Reply Sheet
    if (showAttachmentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Attach & Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Pick Photo from Gallery (Saved to internal storage so it never vanishes)
                ActionRowItem(
                    icon = Icons.Default.Image,
                    title = "Photo Library",
                    onClick = {
                        showAttachmentSheet = false
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )

                // Schedule Message
                ActionRowItem(
                    icon = Icons.Default.Schedule,
                    title = "Schedule Message",
                    onClick = {
                        showAttachmentSheet = false
                        showScheduleDialog = true
                    }
                )

                if (templates.isNotEmpty()) {
                    Text(
                        text = "Quick Replies",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )

                    templates.forEach { template ->
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            shape = RoundedCornerShape(12.dp),
                            onClick = {
                                viewModel.applyTemplate(template)
                                showAttachmentSheet = false
                            }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = template.title,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = template.content,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Schedule Send Dialog
    if (showScheduleDialog) {
        val now = Calendar.getInstance()
        val in30Mins = Calendar.getInstance().apply { add(Calendar.MINUTE, 30) }
        val thisEvening = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 19)
            set(Calendar.MINUTE, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        val tomorrowMorning = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
        }

        AlertDialog(
            onDismissRequest = { showScheduleDialog = false },
            title = {
                Text("Schedule Send", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Choose when to send automatically:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    ScheduleOptionRow(
                        title = "In 30 minutes",
                        time = formatTimeOnly(in30Mins.timeInMillis),
                        onClick = {
                            viewModel.scheduleMessage(in30Mins.timeInMillis)
                            showScheduleDialog = false
                            Toast.makeText(context, "Scheduled for ${formatTimeOnly(in30Mins.timeInMillis)}", Toast.LENGTH_SHORT).show()
                        }
                    )

                    ScheduleOptionRow(
                        title = "This evening",
                        time = "7:00 PM",
                        onClick = {
                            viewModel.scheduleMessage(thisEvening.timeInMillis)
                            showScheduleDialog = false
                            Toast.makeText(context, "Scheduled for 7:00 PM", Toast.LENGTH_SHORT).show()
                        }
                    )

                    ScheduleOptionRow(
                        title = "Tomorrow morning",
                        time = "9:00 AM",
                        onClick = {
                            viewModel.scheduleMessage(tomorrowMorning.timeInMillis)
                            showScheduleDialog = false
                            Toast.makeText(context, "Scheduled for 9:00 AM", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showScheduleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Redirects to the system's default Contacts app to display the contact's card.
 */
private fun openContactDetails(context: Context, number: String) {
    try {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )
        val projection = arrayOf(
            ContactsContract.PhoneLookup._ID,
            ContactsContract.PhoneLookup.LOOKUP_KEY
        )
        val cursor = context.contentResolver.query(uri, projection, null, null, null)
        var contactUri: Uri? = null
        cursor?.use {
            if (it.moveToFirst()) {
                val idIdx = it.getColumnIndex(ContactsContract.PhoneLookup._ID)
                val keyIdx = it.getColumnIndex(ContactsContract.PhoneLookup.LOOKUP_KEY)
                if (idIdx != -1 && keyIdx != -1) {
                    val id = it.getLong(idIdx)
                    val lookupKey = it.getString(keyIdx)
                    contactUri = ContactsContract.Contacts.getLookupUri(id, lookupKey)
                }
            }
        }

        if (contactUri != null) {
            val intent = Intent(Intent.ACTION_VIEW, contactUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } else {
            // Not saved yet: Open Contact insert or edit screen
            val insertIntent = Intent(Intent.ACTION_INSERT_OR_EDIT).apply {
                type = ContactsContract.Contacts.CONTENT_ITEM_TYPE
                putExtra(ContactsContract.Intents.Insert.PHONE, number)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(insertIntent)
        }
    } catch (e: Exception) {
        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(dialIntent)
    }
}

@Composable
private fun ActionRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = tint,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ScheduleOptionRow(
    title: String,
    time: String,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(text = time, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun isDifferentDay(t1: Long, t2: Long): Boolean {
    if (t1 == 0L) return true
    val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
    val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
    return c1.get(Calendar.YEAR) != c2.get(Calendar.YEAR) ||
            c1.get(Calendar.DAY_OF_YEAR) != c2.get(Calendar.DAY_OF_YEAR)
}

private fun formatTimeOnly(timestamp: Long): String {
    return java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
}
