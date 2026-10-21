package id.nkz.nokontzzzmanager.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_tunables")
data class CustomTunableEntity(
    @PrimaryKey
    val path: String,
    val value: String,
    val applyOnBoot: Boolean = false
)
