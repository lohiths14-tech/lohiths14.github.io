package com.smartfind.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.smartfind.app.data.local.dao.DetectedObjectDao
import com.smartfind.app.data.local.dao.ObjectLocationDao
import com.smartfind.app.data.local.dao.ObjectReminderDao
import com.smartfind.app.data.local.dao.DetectionStatisticsDao
import com.smartfind.app.data.local.entity.DetectedObject
import com.smartfind.app.data.local.entity.ObjectLocation
import com.smartfind.app.data.local.entity.ObjectReminder
import com.smartfind.app.data.local.entity.DetectionStatistics

@Database(
    entities = [DetectedObject::class, ObjectLocation::class, ObjectReminder::class, DetectionStatistics::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class SmartFindDatabase : RoomDatabase() {
    
    abstract fun detectedObjectDao(): DetectedObjectDao
    abstract fun objectLocationDao(): ObjectLocationDao
    abstract fun objectReminderDao(): ObjectReminderDao
    abstract fun detectionStatisticsDao(): DetectionStatisticsDao
    
    companion object {
        @Volatile
        private var INSTANCE: SmartFindDatabase? = null
        
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS object_reminders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        detected_object_id INTEGER NOT NULL,
                        reminder_time INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        message TEXT NOT NULL,
                        is_triggered INTEGER NOT NULL DEFAULT 0,
                        is_cancelled INTEGER NOT NULL DEFAULT 0,
                        created_at INTEGER NOT NULL,
                        FOREIGN KEY(detected_object_id) REFERENCES detected_objects(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                database.execSQL("CREATE INDEX IF NOT EXISTS index_object_reminders_detected_object_id ON object_reminders(detected_object_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_object_reminders_reminder_time ON object_reminders(reminder_time)")
            }
        }
        
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to object_reminders for enhanced features
                database.execSQL("ALTER TABLE object_reminders ADD COLUMN is_recurring INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE object_reminders ADD COLUMN recurrence_type TEXT NOT NULL DEFAULT 'NONE'")
                database.execSQL("ALTER TABLE object_reminders ADD COLUMN recurrence_interval INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE object_reminders ADD COLUMN last_triggered_at INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE object_reminders ADD COLUMN snooze_until INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE object_reminders ADD COLUMN snooze_count INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE object_reminders ADD COLUMN priority TEXT NOT NULL DEFAULT 'MEDIUM'")
                
                // Create detection_statistics table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS detection_statistics (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        total_detections INTEGER NOT NULL DEFAULT 0,
                        unique_objects INTEGER NOT NULL DEFAULT 0,
                        average_confidence REAL NOT NULL DEFAULT 0.0,
                        most_detected_object TEXT NOT NULL DEFAULT '',
                        detection_time_millis INTEGER NOT NULL DEFAULT 0,
                        saved_detections INTEGER NOT NULL DEFAULT 0,
                        manual_captures INTEGER NOT NULL DEFAULT 0,
                        auto_saves INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }
        
        fun getInstance(context: Context): SmartFindDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmartFindDatabase::class.java,
                    "smartfind_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
