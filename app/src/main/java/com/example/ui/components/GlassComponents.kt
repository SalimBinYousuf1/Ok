package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.telephony.PhoneNumberUtils
import com.example.ui.theme.LocalGlassColors
import kotlin.math.abs

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val glassColors = LocalGlassColors.current

    Box(
        modifier = modifier
            .background(color = glassColors.surfaceGlass, shape = shape)
            .border(width = 0.5.dp, color = glassColors.borderGlass, shape = shape)
            .clip(shape),
        content = content
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val glassColors = LocalGlassColors.current
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    } else Modifier

    Box(
        modifier = modifier
            .background(color = glassColors.surfaceGlass, shape = shape)
            .border(width = 0.5.dp, color = glassColors.borderGlass, shape = shape)
            .clip(shape)
            .then(clickableModifier),
        content = content
    )
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color? = null,
    badgeCount: Int = 0
) {
    val glassColors = LocalGlassColors.current
    val bg = containerColor ?: glassColors.surfaceGlass

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(38.dp)
            .clip(CircleShape)
            .background(bg)
            .border(0.5.dp, glassColors.borderGlass, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 2.dp)
                    .size(8.dp)
                    .background(Color(0xFFFF3B30), CircleShape)
            )
        }
    }
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val glassColors = LocalGlassColors.current
    val containerBg = if (isPrimary) {
        glassColors.outgoingBubble
    } else {
        glassColors.surfaceGlass
    }
    val borderCol = if (isPrimary) Color.Transparent else glassColors.borderGlass

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = containerBg,
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .border(0.5.dp, borderCol, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

// Neutral Apple-style avatar palette
private val AvatarPalette = listOf(
    Color(0xFF555555) to Color(0xFF222222),
    Color(0xFF333333) to Color(0xFF111111),
    Color(0xFF666666) to Color(0xFF444444),
    Color(0xFF4A5568) to Color(0xFF2D3748),
    Color(0xFF3B4252) to Color(0xFF2E3440),
    Color(0xFF374151) to Color(0xFF1F2937),
    Color(0xFF475569) to Color(0xFF334155)
)

@Composable
fun AvatarView(
    nameOrNumber: String,
    photoUri: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val initials = PhoneNumberUtils.getInitials(nameOrNumber)
    val colorIndex = abs(nameOrNumber.hashCode()) % AvatarPalette.size
    val (startColor, endColor) = AvatarPalette[colorIndex]

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(startColor, endColor))),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUri.isNullOrBlank()) {
            AsyncImage(
                model = photoUri,
                contentDescription = nameOrNumber,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size)
            )
        } else {
            Text(
                text = initials,
                color = Color.White,
                fontSize = (size.value * 0.4f).sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
