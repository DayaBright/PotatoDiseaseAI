package com.tesis.potatodiseaseai.data.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [EnfermedadEntity::class, AnalisisEntity::class],
    version = DatabaseConstants.DATABASE_VERSION,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun enfermedadDao(): EnfermedadDao
    abstract fun analisisDao(): AnalisisDao

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
                    // Migración explícita v1→v2 (sin perder datos en futuras versiones)
                    .addMigrations(DatabaseMigrations.MIGRATION_1_2)
                    // Insertar datos semilla en instalaciones totalmente nuevas
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            DatabaseMigrations.insertSeedData(db)
                            Log.d(TAG, "✓ Datos semilla de enfermedades insertados")
                        }
                    })
                    .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                    .build()

                INSTANCE = instance
                Log.d(TAG, "✓ Base de datos v${DatabaseConstants.DATABASE_VERSION} inicializada")
                instance
            }
        }

        fun clearInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}