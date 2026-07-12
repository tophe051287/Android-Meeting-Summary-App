package com.ramonapps.meetingscribe.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MeetingDao {

    @Query("SELECT * FROM meetings ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<Meeting>>

    @Query("SELECT * FROM meetings WHERE id = :id")
    fun observeById(id: Long): Flow<Meeting?>

    @Query("SELECT * FROM meetings WHERE id = :id")
    suspend fun getById(id: Long): Meeting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meeting: Meeting): Long

    @Update
    suspend fun update(meeting: Meeting)

    @Delete
    suspend fun delete(meeting: Meeting)
}
