package com.tesis.potatodiseaseai.data.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DetectionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun detectionDao(): DetectionDao
    
    companion object {
        private const val TAG = "AppDatabase"
        
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DatabaseConstants.DATABASE_NAME
                )
                .fallbackToDestructiveMigration() 
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING) 
                .build()
                
                INSTANCE = instance
                Log.d(TAG, "✓ Base de datos inicializada")
                instance
            }
        }
        
        //Método para limpiar la instancia
        fun clearInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}