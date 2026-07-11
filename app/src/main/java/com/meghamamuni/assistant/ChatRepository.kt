package com.meghamamuni.assistant

import android.content.Context
import android.content.SharedPreferences
import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ==================== Room Database Entities ====================

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "text" // "text", "voice", "command"
)

// ==================== DAO ====================

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY timestamp ASC LIMIT 200")
    suspend fun getAllMessages(): List<MessageEntity>

    @Insert
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("DELETE FROM messages")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM messages")
    suspend fun getMessageCount(): Int

    @Query("SELECT * FROM messages ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(limit: Int): List<MessageEntity>
}

// ==================== Database ====================

@Database(entities = [MessageEntity::class], version = 1, exportSchema = false)
abstract class MeghaDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: MeghaDatabase? = null

        fun getInstance(context: Context): MeghaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MeghaDatabase::class.java,
                    "megha_mamuni_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ==================== User Settings ====================

class UserSettings(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("megha_settings", Context.MODE_PRIVATE)

    var userName: String
        get() = prefs.getString("user_name", "বন্ধু") ?: "বন্ধু"
        set(value) = prefs.edit().putString("user_name", value).apply()

    var language: String
        get() = prefs.getString("language", "bn") ?: "bn"
        set(value) = prefs.edit().putString("language", value).apply()

    var isDarkMode: Boolean
        get() = prefs.getBoolean("dark_mode", false)
        set(value) = prefs.edit().putBoolean("dark_mode", value).apply()

    var isPinEnabled: Boolean
        get() = prefs.getBoolean("pin_enabled", false)
        set(value) = prefs.edit().putBoolean("pin_enabled", value).apply()

    var pinCode: String
        get() = prefs.getString("pin_code", "") ?: ""
        set(value) = prefs.edit().putString("pin_code", value).apply()

    var isWakeWordEnabled: Boolean
        get() = prefs.getBoolean("wake_word_enabled", false)
        set(value) = prefs.edit().putBoolean("wake_word_enabled", value).apply()

    var voicePitch: Float
        get() = prefs.getFloat("voice_pitch", 1.2f)
        set(value) = prefs.edit().putFloat("voice_pitch", value).apply()

    var voiceSpeed: Float
        get() = prefs.getFloat("voice_speed", 0.9f)
        set(value) = prefs.edit().putFloat("voice_speed", value).apply()

    var selectedAvatar: Int
        get() = prefs.getInt("avatar", 0)
        set(value) = prefs.edit().putInt("avatar", value).apply()

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean("first_launch", true)
        set(value) = prefs.edit().putBoolean("first_launch", value).apply()
}

// ==================== Repository ====================

class ChatRepository(context: Context) {
    private val db = MeghaDatabase.getInstance(context)
    private val dao = db.messageDao()
    val settings = UserSettings(context)

    suspend fun saveMessage(text: String, isUser: Boolean, type: String = "text"): Long =
        withContext(Dispatchers.IO) {
            dao.insertMessage(MessageEntity(text = text, isUser = isUser, type = type))
        }

    suspend fun getAllMessages(): List<MessageEntity> =
        withContext(Dispatchers.IO) { dao.getAllMessages() }

    suspend fun getRecentMessages(limit: Int = 50): List<MessageEntity> =
        withContext(Dispatchers.IO) { dao.getRecentMessages(limit) }

    suspend fun clearHistory() =
        withContext(Dispatchers.IO) { dao.clearAll() }

    suspend fun getMessageCount(): Int =
        withContext(Dispatchers.IO) { dao.getMessageCount() }
}
