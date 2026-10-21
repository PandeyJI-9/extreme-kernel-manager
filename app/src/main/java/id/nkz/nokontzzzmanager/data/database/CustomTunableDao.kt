package id.nkz.nokontzzzmanager.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomTunableDao {
    @Query("SELECT * FROM custom_tunables ORDER BY path ASC")
    fun getAllTunables(): Flow<List<CustomTunableEntity>>

    @Query("SELECT * FROM custom_tunables WHERE applyOnBoot = 1")
    suspend fun getBootTunables(): List<CustomTunableEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTunable(tunable: CustomTunableEntity)

    @Delete
    suspend fun deleteTunable(tunable: CustomTunableEntity)
    
    @Query("DELETE FROM custom_tunables WHERE path = :path")
    suspend fun deleteTunableByPath(path: String)
}
