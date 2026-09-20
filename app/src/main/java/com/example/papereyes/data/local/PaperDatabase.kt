package com.example.papereyes.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.papereyes.data.model.Paper

/**
 * Current pre-release schema. Room schema versions are kept monotonic even
 * before the first public app release, so development installs never require a
 * database downgrade. No legacy migration is registered because no released
 * PaperEyes database exists yet; clear/uninstall incompatible pre-release data.
 */
@Database(
    entities = [
        Paper::class,
        ProjectEntity::class,
        PaperProjectCrossRef::class
    ],
    version = 4,
    exportSchema = true
)
abstract class PaperDatabase : RoomDatabase() {

    abstract fun paperDao(): PaperDao
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: PaperDatabase? = null

        fun getDatabase(context: Context): PaperDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PaperDatabase::class.java,
                    "papereyes_database"
                ).build().also { INSTANCE = it }
            }
    }
}
