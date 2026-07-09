package com.daftar.app.kernel.di

import android.content.Context
import androidx.room.Room
import com.daftar.app.kernel.db.DaftarDatabase
import com.daftar.app.kernel.db.MIGRATION_14_15
import com.daftar.app.kernel.db.MIGRATION_15_16
import com.daftar.app.kernel.db.MIGRATION_16_17
import com.daftar.app.kernel.db.StoreDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): DaftarDatabase =
        Room.databaseBuilder(context, DaftarDatabase::class.java, "daftar.db")
            .addMigrations(MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17) // preserve her ledger across updates
            .fallbackToDestructiveMigration() // only for older/unknown jumps
            .build()

    @Provides
    fun storeDao(db: DaftarDatabase): StoreDao = db.storeDao()
}
