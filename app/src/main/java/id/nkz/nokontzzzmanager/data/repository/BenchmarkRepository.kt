package id.nkz.nokontzzzmanager.data.repository

import id.nkz.nokontzzzmanager.data.database.BenchmarkDao
import id.nkz.nokontzzzmanager.data.database.BenchmarkEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BenchmarkRepository @Inject constructor(
    private val benchmarkDao: BenchmarkDao
) {
    fun getAllBenchmarks(): Flow<List<BenchmarkEntity>> = benchmarkDao.getAllBenchmarks()

    fun getBenchmarksByPackageName(packageName: String): Flow<List<BenchmarkEntity>> = benchmarkDao.getBenchmarksByPackageName(packageName)

    fun getBenchmarkById(id: Long): Flow<BenchmarkEntity?> = benchmarkDao.getBenchmarkById(id)

    suspend fun insertBenchmark(benchmark: BenchmarkEntity) {
        benchmarkDao.insertBenchmark(benchmark)
    }

    suspend fun deleteBenchmark(benchmark: BenchmarkEntity) {
        benchmarkDao.deleteBenchmark(benchmark)
    }

    suspend fun clearAll() {
        benchmarkDao.clearAll()
    }
}
