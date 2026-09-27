package com.example.telephony

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

data class ContactInfo(
    val name: String,
    val number: String,
    val photoUri: String? = null
)

object ContactResolver {

    fun resolveContact(context: Context, number: String): ContactInfo {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return ContactInfo(name = PhoneNumberUtils.formatForDisplay(number), number = number)
        }

        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number)
            )
            val projection = arrayOf(
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI
            )
            val cursor: Cursor? = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    val photoIndex = it.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI)
                    val name = if (nameIndex != -1) it.getString(nameIndex) else null
                    val photo = if (photoIndex != -1) it.getString(photoIndex) else null
                    if (!name.isNullOrBlank()) {
                        return ContactInfo(name = name, number = number, photoUri = photo)
                    }
                }
            }
        } catch (e: Exception) {
            // Silently fall back to formatted number
        }

        return ContactInfo(name = PhoneNumberUtils.formatForDisplay(number), number = number)
    }

    fun searchContacts(context: Context, query: String): List<ContactInfo> {
        val results = mutableListOf<ContactInfo>()
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return results
        }

        try {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
            )
            val selection = if (query.isNotBlank()) {
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? OR ${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
            } else null
            val selectionArgs = if (query.isNotBlank()) {
                val q = "%$query%"
                arrayOf(q, q)
            } else null
            val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC LIMIT 30"

            val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
            cursor?.use {
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
                val seenNumbers = mutableSetOf<String>()

                while (it.moveToNext()) {
                    val name = if (nameIdx != -1) it.getString(nameIdx) ?: "" else ""
                    val num = if (numIdx != -1) it.getString(numIdx) ?: "" else ""
                    val photo = if (photoIdx != -1) it.getString(photoIdx) else null
                    val normalized = PhoneNumberUtils.normalizeNumber(num)
                    if (normalized.isNotEmpty() && !seenNumbers.contains(normalized)) {
                        seenNumbers.add(normalized)
                        results.add(ContactInfo(name = name, number = num, photoUri = photo))
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback gracefully
        }
        return results
    }
}
