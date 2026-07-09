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

// rc9 (v15) → rc10 (v16): store_entries gained D68 supplier payments — the paid shop
// (sourceId) and the money-out amount. Additive with defaults; her ledger is untouched.
val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE store_entries ADD COLUMN sourceId TEXT")
        db.execSQL("ALTER TABLE store_entries ADD COLUMN moneyOut INTEGER NOT NULL DEFAULT 0")
    }
}

// v16 → v17: the pre-V2 legacy slices were deleted (SPEC F6); their tables held only
// first-build data never shown since the rebuild. The store_* tables — her real ledger —
// are untouched.
val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        for (t in listOf(
            "customers", "ledger_entries", "reminders", "item_types",
            "sales", "sale_lines", "stock_sources", "intake_lines",
        )) db.execSQL("DROP TABLE IF EXISTS `$t`")
    }
}
