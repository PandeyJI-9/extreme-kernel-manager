package id.nkz.nokontzzzmanager.data.repository

import id.nkz.nokontzzzmanager.data.database.GameDao
import id.nkz.nokontzzzmanager.data.database.GameEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GameRepository @Inject constructor(
    private val gameDao: GameDao
) {
    fun getAllGames(): Flow<List<GameEntity>> = gameDao.getAllGames()

    fun getGameByPackageName(packageName: String): Flow<GameEntity?> = gameDao.getGameByPackageName(packageName)

    suspend fun insertGame(game: GameEntity) {
        gameDao.insertGame(game)
    }

    suspend fun deleteGame(game: GameEntity) {
        gameDao.deleteGame(game)
    }
}
