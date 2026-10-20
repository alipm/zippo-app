package com.zippo.app.data

import android.content.Context
import androidx.room.*

// 🟢 1. DATABASE ENTITY SCHEMA (Maps to local SQLite rows)
@Entity(tableName = "passenger_cache")
data class LocalPassengerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val passengerType: String,
    val contractType: String,
    val monthlyFare: Int,
    val morningPickupTime: String,
    val eveningPickupTime: String,
    val homeLat: Double,
    val homeLng: Double,
    val destLat: Double,
    val destLng: Double,
    val phoneNumber: String,
    val address: String,
    val profileImageBase64: String,
    val remarks: String? = ""
)

// 🟢 2. DATA ACCESS OBJECT (DAO Interface layer)
@Dao
interface PassengerCacheDao {
    @Query("SELECT * FROM passenger_cache")
    suspend fun getAllCachedPassengers(): List<LocalPassengerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassengerBatch(passengers: List<LocalPassengerEntity>)

    @Query("DELETE FROM passenger_cache")
    suspend fun clearLocalCacheTable()
}

// 🟢 3. CORE ROOM DATABASE CONTAINER HOLDER
@Database(entities = [LocalPassengerEntity::class], version = 1, exportSchema = false)
abstract class ZippoRoomDatabase : RoomDatabase() {
    abstract fun passengerCacheDao(): PassengerCacheDao

    companion object {
        @Volatile
        private var INSTANCE: ZippoRoomDatabase? = null

        fun getDatabase(context: Context): ZippoRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZippoRoomDatabase::class.java,
                    "zippo_local_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
