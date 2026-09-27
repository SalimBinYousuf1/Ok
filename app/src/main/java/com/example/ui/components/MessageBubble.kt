package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.MessageEntity
import com.example.ui.theme.LocalGlassColors
import com.example.ui.theme.SalimRed
import com.example.ui.theme.TiemposTextFontFamily

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: MessageEntity,
    onLongClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val glassColors = LocalGlassColors.current
    val clipboard = LocalClipboardManager.current
    val isOutgoing = message.isOutgoing

    // Apple iMessage signature asymmetrical bubble shape
    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomStart = 18.dp,
            bottomEnd = 4.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomStart = 4.dp,
            bottomEnd = 18.dp
        )
    }

    val backgroundColor = if (isOutgoing) {
        glassColors.outgoingBubble
    } else {
        glassColors.incomingBubble
    }

    val textColor = if (isOutgoing) {
        glassColors.outgoingText
    } else {
        glassColors.incomingText
    }

    val metaColor = if (isOutgoing) {
        textColor.copy(alpha = 0.65f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = 14.dp,
                vertical = 2.5.dp
            ),
        contentAlignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            // Persistent Attachment Preview if present
            if (!message.attachmentUri.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(glassColors.surfaceGlass)
                        .border(1.dp, glassColors.borderGlass, RoundedCornerShape(16.dp))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(message.attachmentUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Photo Attachment",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 230.dp, height = 170.dp)
                    )
                }
            }

            // Smart OTP Quick Copy Bar if detected (Apple style)
            if (!message.otpCode.isNullOrBlank() && !isOutgoing) {
                GlassCard(
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .testTag("otp_copy_card"),
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        clipboard.setText(AnnotatedString(message.otpCode))
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Code",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Copy code: ${message.otpCode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Compact Apple iMessage bubble
            if (message.body.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(bubbleShape)
                        .background(backgroundColor)
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                            onLongClick = onLongClick
                        )
                        .padding(horizontal = 13.dp, vertical = 8.5.dp)
                        .testTag("message_bubble_${message.id}")
                ) {
                    Column {
                        // Tiempos Text editorial font for all text messages
                        Text(
                            text = message.body,
                            color = textColor,
                            fontFamily = TiemposTextFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 15.5.sp,
                            lineHeight = 21.sp,
                            letterSpacing = (-0.15).sp
                        )

                        Spacer(modifier = Modifier.height(2.5.dp))

                        Row(
                            modifier = Modifier.align(Alignment.End),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (message.isStarred) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Starred",
                                    tint = if (isOutgoing) metaColor else Color(0xFFFFB300),
                                    modifier = Modifier.size(11.dp)
                                )
                            }

                            Text(
                                text = formatMessageTime(message.timestamp),
                                color = metaColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal
                            )

                            if (isOutgoing) {
                                DeliveryStatusIcon(
                                    status = message.status,
                                    metaColor = metaColor,
                                    onRetryClick = onRetryClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeliveryStatusIcon(
    status: String,
    metaColor: Color,
    onRetryClick: () -> Unit
) {
    when (status) {
        "SENDING", "PENDING" -> {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Sending",
                tint = metaColor,
                modifier = Modifier.size(11.dp)
            )
        }
        "SENT" -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Sent",
                tint = metaColor,
                modifier = Modifier.size(11.dp)
            )
        }
        "DELIVERED" -> {
            Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = metaColor,
                    modifier = Modifier.size(11.dp)
                )
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Delivered",
                    tint = metaColor,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
        "FAILED" -> {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .border(1.dp, SalimRed, CircleShape)
                    .clickable(onClick = onRetryClick)
                    .padding(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Failed. Tap to retry",
                    tint = SalimRed,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
