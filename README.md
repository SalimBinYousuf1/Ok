# Salim — A Loved, Premium Native Android Messaging App

**Salim** is a production-grade, native Android messaging application designed with Apple-inspired liquid glass aesthetics, real cellular SMS/MMS telephony, default SMS app role handling, local Room persistence, rich notifications with inline reply, private OTP code detection, and scheduled messaging.

---

## Key Features & Highlights

1. **Apple-Inspired Native Liquid Glass Design**
   - Translucent frosted glass surfaces with subtle 1px border highlights, gentle depth, and harmonic color palette.
   - True Black OLED mode and adaptive Light/Dark themes.
   - Accessible WCAG-compliant contrast, 48dp minimum touch targets, and tactile haptic/ripple feedback.

2. **Real SMS/MMS Telephony Integration**
   - Standard Android `SmsManager` multipart SMS sending and receiving.
   - Default SMS App role handling via Android `RoleManager` (Android 10+) with fallback intent.
   - `Telephony.Sms.Intents.SMS_DELIVER` and `SMS_RECEIVED` broadcast receivers.
   - Sent and delivery status callbacks updating message state in real-time.
   - Platform SMS database synchronization for conversation continuity.
   - One-tap local import of existing SMS messages from device telephony provider.

3. **Rich Notifications & Inline Actions**
   - Android Notification Channels (`Messages`, `Scheduled`, `Security & OTP`).
   - `NotificationCompat.MessagingStyle` with person avatars.
   - Inline direct reply action using Android `RemoteInput`.
   - "Mark as Read" action and "Copy Code" action for verification messages.
   - Configurable lock screen privacy (Full content, Sender only, Hidden).

4. **Smart, Private Local Features (No AI Gimmicks)**
   - **Private OTP Detection:** Local regex extraction of 4-8 digit verification codes with 1-tap copy button in message bubble and notification bar.
   - **Scheduled Messaging:** Exact scheduling using Android `AlarmManager` with automatic background broadcast execution.
   - **Quick Reply Templates:** User-customizable canned responses for meetings, transit, and quick replies.
   - **Global Search:** Fast local Room database queries indexing contacts, phone numbers, and message bodies.

5. **Local-First Privacy & Security**
   - All messages, drafts, and settings are stored locally in SQLite via Room and Preferences DataStore.
   - No tracking, no analytics, no external servers, no AI fluff.
   - Local JSON backup and export functionality.

---

## Architecture

- **UI Layer:** 100% Jetpack Compose with Material 3 and custom `GlassSurface` components.
- **State Management:** MVVM with Kotlin Coroutines and `StateFlow`.
- **Database:** Android Room 2.7.0 with Kotlin Symbol Processing (KSP).
- **Preferences:** Jetpack DataStore Preferences.
- **Telephony & Broadcasts:** `SmsReceiver`, `SmsFallbackReceiver`, `MmsReceiver`, `HeadlessSmsSendService`, `SmsStatusReceiver`, `ScheduledMessageReceiver`, `NotificationActionReceiver`.

---

## Build & Testing

```bash
# Build Debug APK
gradle :app:assembleDebug

# Run Unit & Robolectric Tests
gradle :app:testDebugUnitTest
```
