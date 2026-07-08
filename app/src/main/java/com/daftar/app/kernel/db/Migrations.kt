package com.daftar.app.kernel.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Real migrations so a real user's ledger survives an app update. Destructive fallback stays
// on for older/unknown version jumps, but the paths people actually take must be lossless.

// rc7 (v14) → rc8 (v15): store_sources gained `debt` (supplier credit for market shops).
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE store_sources ADD COLUMN debt INTEGER NOT NULL DEFAULT 0")
    }
}
