package id.nkz.nokontzzzmanager.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Battery history database at schema version 14.
 *
 * IMPORTANT: When adding entities or columns, bump version and add a Migration.
 * Do NOT restore fallbackToDestructiveMigration — it silently wipes user data.
 * Schema JSON exports are enabled; commit them alongside schema changes.
 */
@Database(
    entities = [
        BatteryHistoryEntity::class,
        BatteryGraphEntry::class,
        AppProfileEntity::class,
        CustomTunableEntity::class,
        GameEntity::class,
        BenchmarkEntity::class,
    ],
    version = 14,
    exportSchema = true,
)
abstract class BatteryHistoryDatabase : RoomDatabase() {
    abstract fun batteryHistoryDao(): BatteryHistoryDao
    abstract fun batteryGraphDao(): BatteryGraphDao
    abstract fun appProfileDao(): AppProfileDao
    abstract fun customTunableDao(): CustomTunableDao
    abstract fun gameDao(): GameDao
    abstract fun benchmarkDao(): BenchmarkDao

    companion object {
        /**
         * Stub migration for the next schema version (14 → 15).
         * Replace this with the actual migration when you change the schema.
         *
         * Usage pattern when bumping to version 15:
         *   1. Add your entity/column changes
         *   2. Replace this stub with real SQL (ALTER TABLE, CREATE TABLE, etc.)
         *   3. Add the migration to the database builder in AppModule
         */
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add migration SQL here when bumping to version 15
                // Example: db.execSQL("ALTER TABLE battery_history ADD COLUMN new_field TEXT")
            }
        }
    }
}
