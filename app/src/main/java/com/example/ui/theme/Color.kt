package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Refined Apple monochrome & neutral accents (no bright blue for bubbles)
val SalimAccent = Color(0xFF007AFF)
val SalimBlue = Color(0xFF007AFF) // Compatibility alias for accent blue
val SalimGreen = Color(0xFF34C759)
val SalimRed = Color(0xFFFF3B30)
val SalimOrange = Color(0xFFFF9500)
val SalimEmerald = Color(0xFF10B981)

// ==========================================
// 1. LIGHT THEME (Apple Frosted Liquid Glass)
// ==========================================
val LightCanvas = Color(0xFFF2F2F7)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceGlass = Color(0xD9FFFFFF) // 85% opacity frosted white
val LightSurfaceSubtle = Color(0xFFE5E5EA)
val LightBorderGlass = Color(0x18000000) // 10% black hairline border
val LightBorderSubtle = Color(0x0D000000)
val LightTextPrimary = Color(0xFF000000)
val LightTextSecondary = Color(0xFF8E8E93)
val LightTextTertiary = Color(0xFFAEAEB2)
val LightBubbleIncoming = Color(0xFFE9E9EB) // iOS standard light grey
val LightBubbleOutgoing = Color(0xFF1C1C1E) // Apple Obsidian neutral black
val LightTextIncoming = Color(0xFF000000)
val LightTextOutgoing = Color(0xFFFFFFFF)

// ==========================================
// 2. DARK THEME (Apple Graphite Glass)
// ==========================================
val DarkCanvas = Color(0xFF000000) // True deep dark - avoids any white bleed
val DarkSurface = Color(0xFF121214)
val DarkSurfaceGlass = Color(0xD91C1C1E) // 85% opacity frosted graphite
val DarkSurfaceSubtle = Color(0xFF242426)
val DarkBorderGlass = Color(0x26FFFFFF) // 15% white hairline border
val DarkBorderSubtle = Color(0x14FFFFFF)
val DarkTextPrimary = Color(0xFFFFFFFF)
val DarkTextSecondary = Color(0xFF8E8E93)
val DarkTextTertiary = Color(0xFF636366)
val DarkBubbleIncoming = Color(0xFF2C2C2E) // iOS Dark neutral grey
val DarkBubbleOutgoing = Color(0xFFE5E5EA) // Crisp contrast Apple white/off-white
val DarkTextIncoming = Color(0xFFFFFFFF)
val DarkTextOutgoing = Color(0xFF000000)

// ==========================================
// 3. "SALIM" ASGL THEME (Deep Charcoal & Emerald Glass)
// ==========================================
val AsglCanvas = Color(0xFF0D0F12) // Deep Charcoal background
val AsglSurface = Color(0xFF13171D)
val AsglSurfaceGlass = Color(0xD91A232A) // Subtle emerald glass accent
val AsglSurfaceSubtle = Color(0xFF1E2832)
val AsglBorderGlass = Color(0x3334D399) // Subtle emerald-tinted frosted highlight
val AsglBorderSubtle = Color(0x1A10B981)
val AsglTextPrimary = Color(0xFFF1F5F9)
val AsglTextSecondary = Color(0xFF94A3B8)
val AsglTextTertiary = Color(0xFF64748B)
val AsglBubbleIncoming = Color(0xFF1A232A) // Charcoal emerald glass
val AsglBubbleOutgoing = Color(0xFFE2EBE8) // Frosted pale silver
val AsglTextIncoming = Color(0xFFF1F5F9)
val AsglTextOutgoing = Color(0xFF0D0F12)
