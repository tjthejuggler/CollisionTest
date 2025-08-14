package com.example.jugglingtracker.di

import android.content.Context
import com.example.jugglingtracker.data.database.JugglingDatabase
import com.example.jugglingtracker.data.dao.UsageEventDao
import com.example.jugglingtracker.data.dao.WeeklyUsageDao
import com.example.jugglingtracker.data.repository.UsageTrackingRepository
import com.example.jugglingtracker.services.UsageTrackingService
import com.example.jugglingtracker.ui.theme.DynamicThemeManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UsageTrackingModule {
    
    @Provides
    @Singleton
    fun provideJugglingDatabase(@ApplicationContext context: Context): JugglingDatabase {
        return JugglingDatabase.getDatabase(context)
    }
    
    @Provides
    @Singleton
    fun provideUsageEventDao(database: JugglingDatabase): UsageEventDao {
        return database.usageEventDao()
    }
    
    @Provides
    @Singleton
    fun provideWeeklyUsageDao(database: JugglingDatabase): WeeklyUsageDao {
        return database.weeklyUsageDao()
    }
    
    @Provides
    @Singleton
    fun provideUsageTrackingRepository(
        usageEventDao: UsageEventDao,
        weeklyUsageDao: WeeklyUsageDao
    ): UsageTrackingRepository {
        return UsageTrackingRepository(usageEventDao, weeklyUsageDao)
    }
    
    @Provides
    @Singleton
    fun provideDynamicThemeManager(
        @ApplicationContext context: Context,
        usageTrackingRepository: UsageTrackingRepository
    ): DynamicThemeManager {
        return DynamicThemeManager(context, usageTrackingRepository)
    }
    
    @Provides
    @Singleton
    fun provideUsageTrackingService(
        @ApplicationContext context: Context,
        usageTrackingRepository: UsageTrackingRepository,
        dynamicThemeManager: DynamicThemeManager
    ): UsageTrackingService {
        return UsageTrackingService(context, usageTrackingRepository, dynamicThemeManager)
    }
}