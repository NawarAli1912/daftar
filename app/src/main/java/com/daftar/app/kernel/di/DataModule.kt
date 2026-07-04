package com.daftar.app.kernel.di

import android.content.Context
import androidx.room.Room
import com.daftar.app.kernel.db.CustomerDao
import com.daftar.app.kernel.db.DaftarDatabase
import com.daftar.app.kernel.db.LedgerDao
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
        Room.databaseBuilder(context, DaftarDatabase::class.java, "daftar.db").build()

    @Provides
    fun customerDao(db: DaftarDatabase): CustomerDao = db.customerDao()

    @Provides
    fun ledgerDao(db: DaftarDatabase): LedgerDao = db.ledgerDao()
}
