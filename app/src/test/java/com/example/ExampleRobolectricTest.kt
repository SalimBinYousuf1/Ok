package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.data.local.SalimDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: SalimDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SalimDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Salim", appName)
    }

    @Test
    fun testRoomDatabaseInsertAndQuery() = runBlocking {
        val conv = ConversationEntity(
            recipientAddress = "+15551234567",
            recipientName = "Alice",
            snippet = "Hello world",
            lastMessageTimestamp = System.currentTimeMillis()
        )
        val convId = db.conversationDao().insertConversation(conv)
        assertNotNull(convId)

        val retrievedConv = db.conversationDao().getConversationByAddress("+15551234567")
        assertNotNull(retrievedConv)
        assertEquals("Alice", retrievedConv?.recipientName)

        val msg = MessageEntity(
            conversationId = convId,
            address = "+15551234567",
            body = "Hello world",
            timestamp = System.currentTimeMillis(),
            isOutgoing = true,
            status = "SENT"
        )
        val msgId = db.messageDao().insertMessage(msg)
        assertNotNull(msgId)

        val retrievedMsg = db.messageDao().getMessageById(msgId)
        assertNotNull(retrievedMsg)
        assertEquals("Hello world", retrievedMsg?.body)
    }
}
