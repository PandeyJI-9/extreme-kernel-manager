package id.nkz.nokontzzzmanager.data.repository

import id.nkz.nokontzzzmanager.data.database.AppProfileDao
import id.nkz.nokontzzzmanager.data.database.AppProfileEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppProfileRepository @Inject constructor(
    private val appProfileDao: AppProfileDao
) {
    fun getAllProfiles(): Flow<List<AppProfileEntity>> = appProfileDao.getAllProfiles()

    suspend fun getProfile(packageName: String): AppProfileEntity? = appProfileDao.getProfile(packageName)

    suspend fun insertProfile(profile: AppProfileEntity) = appProfileDao.insertProfile(profile)

    suspend fun deleteProfile(profile: AppProfileEntity) = appProfileDao.deleteProfile(profile)
    
    suspend fun deleteProfileByPackage(packageName: String) = appProfileDao.deleteProfileByPackage(packageName)
}
