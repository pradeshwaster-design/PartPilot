package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        LocationEntity::class,
        PartEntity::class,
        PartEventEntity::class,
        ProjectEntity::class,
        ProjectPartEntity::class,
        CartItemEntity::class,
        AiActionLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PartPilotDatabase : RoomDatabase() {
    abstract fun dao(): PartPilotDao

    companion object {
        @Volatile
        private var INSTANCE: PartPilotDatabase? = null

        fun getDatabase(context: Context): PartPilotDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PartPilotDatabase::class.java,
                    "partpilot.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed initial data on database creation
                            CoroutineScope(Dispatchers.IO).launch {
                                val dao = getDatabase(context).dao()
                                seedDatabase(dao)
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedDatabase(dao: PartPilotDao) {
            SeedData.locations.forEach { dao.insertLocation(it) }
            SeedData.parts.forEach { dao.insertPart(it) }
            SeedData.projects.forEach { dao.insertProject(it) }
            SeedData.projectParts.forEach { dao.insertProjectPart(it) }
            SeedData.partEvents.forEach { dao.insertPartEvent(it) }
            SeedData.cartItems.forEach { dao.insertCartItem(it) }
        }
    }
}
