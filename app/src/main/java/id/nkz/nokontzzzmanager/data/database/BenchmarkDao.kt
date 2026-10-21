package id.nkz.nokontzzzmanager.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BenchmarkDao {
    @Query("SELECT * FROM benchmarks ORDER BY timestamp DESC")
    fun getAllBenchmarks(): Flow<List<BenchmarkEntity>>

    @Query("SELECT * FROM benchmarks WHERE packageName = :packageName ORDER BY timestamp DESC")
    fun getBenchmarksByPackageName(packageName: String): Flow<List<BenchmarkEntity>>

    @Query("SELECT * FROM benchmarks WHERE id = :id")
    fun getBenchmarkById(id: Long): Flow<BenchmarkEntity?>

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertBenchmark(benchmark: BenchmarkEntity)

    @Delete
    suspend fun deleteBenchmark(benchmark: BenchmarkEntity)
    
    @Query("DELETE FROM benchmarks")
    suspend fun clearAll()
}
