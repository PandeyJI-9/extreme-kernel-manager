package id.nkz.nokontzzzmanager.data.repository

import id.nkz.nokontzzzmanager.data.database.BatteryGraphDao
import id.nkz.nokontzzzmanager.data.database.BatteryGraphEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BatteryGraphRepository @Inject constructor(
    private val batteryGraphDao: BatteryGraphDao
) {
    suspend fun insertEntry(entry: BatteryGraphEntry): Long {
        return batteryGraphDao.insertEntry(entry)
    }

    fun getAllEntries(): Flow<List<BatteryGraphEntry>> {
        return batteryGraphDao.getAllEntries()
    }

    fun getEntriesSince(startTime: Long): Flow<List<BatteryGraphEntry>> {
        return batteryGraphDao.getEntriesSince(startTime)
    }

    suspend fun deleteOldEntries(cutoffTime: Long): Int {
        return batteryGraphDao.deleteOldEntries(cutoffTime)
    }

    suspend fun deleteAllEntries(): Int {
        return batteryGraphDao.deleteAllEntries()
    }
}
