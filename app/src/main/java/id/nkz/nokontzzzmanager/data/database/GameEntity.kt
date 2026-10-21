package id.nkz.nokontzzzmanager.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isBenchmarkEnabled: Boolean = true,
    val targetFps: Int = 60,
    val preferredLayerPattern: String? = null
)
