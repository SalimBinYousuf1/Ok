package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        ScheduledMessageEntity::class,
        QuickReplyTemplateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SalimDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun scheduledMessageDao(): ScheduledMessageDao
    abstract fun quickReplyTemplateDao(): QuickReplyTemplateDao

    companion object {
        @Volatile
        private var INSTANCE: SalimDatabase? = null

        fun getInstance(context: Context): SalimDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SalimDatabase::class.java,
                    "salim_messages.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate default quick reply templates
                            CoroutineScope(Dispatchers.IO).launch {
                                val templateDao = getInstance(context).quickReplyTemplateDao()
                                templateDao.insertTemplates(
                                    listOf(
                                        QuickReplyTemplateEntity(title = "On my way", content = "On my way right now!"),
                                        QuickReplyTemplateEntity(title = "In a meeting", content = "I'm in a meeting, will text you soon."),
                                        QuickReplyTemplateEntity(title = "Call me later", content = "Can't talk right now. Please call me later!"),
                                        QuickReplyTemplateEntity(title = "Thanks", content = "Thank you so much!"),
                                        QuickReplyTemplateEntity(title = "Sounds good", content = "Sounds great, looking forward to it.")
                                    )
                                )
                            }
                        }
                    })
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
