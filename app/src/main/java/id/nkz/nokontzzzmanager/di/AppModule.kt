package id.nkz.nokontzzzmanager.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.nkz.nokontzzzmanager.data.database.AppProfileDao
import id.nkz.nokontzzzmanager.data.database.BatteryGraphDao
import id.nkz.nokontzzzmanager.data.database.BatteryHistoryDatabase
import id.nkz.nokontzzzmanager.data.database.BenchmarkDao
import id.nkz.nokontzzzmanager.data.database.CustomTunableDao
import id.nkz.nokontzzzmanager.data.database.GameDao
import id.nkz.nokontzzzmanager.data.repository.BatteryMonitorProvider
import id.nkz.nokontzzzmanager.data.repository.CpuMonitorProvider
import id.nkz.nokontzzzmanager.data.repository.KernelFeatureRepository
import id.nkz.nokontzzzmanager.data.repository.KernelInfoProvider
import id.nkz.nokontzzzmanager.data.repository.MemoryMonitorProvider
import id.nkz.nokontzzzmanager.data.repository.NativeTelemetryReader
import id.nkz.nokontzzzmanager.data.repository.RootRepository
import id.nkz.nokontzzzmanager.data.repository.SysfsHelper
import id.nkz.nokontzzzmanager.data.repository.SystemRepository
import id.nkz.nokontzzzmanager.data.repository.ThermalRepository
import id.nkz.nokontzzzmanager.data.repository.TuningRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideTuningRepository(@ApplicationContext context: Context): TuningRepository = TuningRepository(context)

    @Provides
    @Singleton
    fun provideThermalRepository(
        @ApplicationContext context: Context,
        rootRepository: RootRepository,
        @ThermalSettings thermalDataStore: DataStore<Preferences>
    ): ThermalRepository = ThermalRepository(context, rootRepository, thermalDataStore)

    @Provides
    @Singleton
    fun provideSystemRepository(
        @ApplicationContext context: Context,
        tuningRepository: TuningRepository,
        rootRepository: RootRepository,
        sysfsHelper: SysfsHelper,
        kernelFeatures: KernelFeatureRepository,
        cpuMonitor: CpuMonitorProvider,
        batteryMonitor: BatteryMonitorProvider,
        memoryMonitor: MemoryMonitorProvider,
        kernelInfoProvider: KernelInfoProvider,
        nativeTelemetryReader: NativeTelemetryReader,
    ): SystemRepository =
        SystemRepository(context, tuningRepository, rootRepository, sysfsHelper, kernelFeatures, cpuMonitor, batteryMonitor, memoryMonitor, kernelInfoProvider, nativeTelemetryReader)

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(produceFile = { context.preferencesDataStoreFile("settings") })
    }

    @Provides
    @Singleton
    @ThermalSettings
    fun provideThermalDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(produceFile = { context.preferencesDataStoreFile("thermal_settings") })
    }

    @Provides
    @Singleton
    fun provideBatteryHistoryDatabase(@ApplicationContext context: Context): BatteryHistoryDatabase =
        Room.databaseBuilder(context, BatteryHistoryDatabase::class.java, "battery_history_db")
            .addMigrations(BatteryHistoryDatabase.MIGRATION_14_15)
            .build()

    @Provides @Singleton
    fun provideBatteryGraphDao(db: BatteryHistoryDatabase): BatteryGraphDao = db.batteryGraphDao()

    @Provides @Singleton
    fun provideAppProfileDao(db: BatteryHistoryDatabase): AppProfileDao = db.appProfileDao()

    @Provides @Singleton
    fun provideCustomTunableDao(db: BatteryHistoryDatabase): CustomTunableDao = db.customTunableDao()

    @Provides @Singleton
    fun provideGameDao(db: BatteryHistoryDatabase): GameDao = db.gameDao()

    @Provides @Singleton
    fun provideBenchmarkDao(db: BatteryHistoryDatabase): BenchmarkDao = db.benchmarkDao()
}
