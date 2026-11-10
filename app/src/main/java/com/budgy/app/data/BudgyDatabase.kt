package com.budgy.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Account::class, Category::class, Txn::class, Budget::class, Subscription::class,
        Recurring::class, Goal::class, GoalEntry::class, Iou::class, PriceChange::class,
        Debt::class, EnvelopeMove::class,
        Holding::class, InvestTxn::class, PriceSnap::class,
    ],
    version = 6,
    exportSchema = false,
)
abstract class BudgyDatabase : RoomDatabase() {
    abstract fun dao(): BudgyDao

    companion object {
        fun build(context: Context): BudgyDatabase =
            Room.databaseBuilder(context, BudgyDatabase::class.java, "budgy.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build()

        /** V5 -> V6: funds and ETFs priced in another currency (e.g. S&P 500 ETFs in US dollars). */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `holdings` ADD COLUMN `priceCurrency` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `holdings` ADD COLUMN `fxRateMicros` INTEGER NOT NULL DEFAULT 1000000")
                db.execSQL("ALTER TABLE `holdings` ADD COLUMN `quotePrice` REAL NOT NULL DEFAULT 0")
            }
        }

        /** V4 -> V5: investments (crypto and Pag-IBIG MP2). Nothing existing changes. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `holdings` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `symbol` TEXT NOT NULL, `kind` TEXT NOT NULL, `coinId` TEXT NOT NULL, " +
                        "`price` REAL NOT NULL, `priceUpdated` INTEGER NOT NULL, `color` INTEGER NOT NULL, " +
                        "`rateBps` INTEGER NOT NULL, `maturityDate` INTEGER, `note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `invest_txns` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`holdingId` INTEGER NOT NULL, `type` TEXT NOT NULL, `units` REAL NOT NULL, " +
                        "`amount` INTEGER NOT NULL, `date` INTEGER NOT NULL, `accountId` INTEGER, " +
                        "`txnId` INTEGER, `note` TEXT NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_invest_txns_holdingId` ON `invest_txns` (`holdingId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `price_snaps` (" +
                        "`holdingId` INTEGER NOT NULL, `date` INTEGER NOT NULL, `price` REAL NOT NULL, " +
                        "PRIMARY KEY(`holdingId`, `date`))"
                )
            }
        }

        /** V3 -> V4: tags, cross-currency transfers, archived accounts and account currencies. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `tags` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `toAmount` INTEGER")
                db.execSQL("ALTER TABLE `accounts` ADD COLUMN `archived` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `accounts` ADD COLUMN `currency` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `accounts` ADD COLUMN `fxRateMicros` INTEGER NOT NULL DEFAULT 1000000")
            }
        }

        /** V2 -> V3: debts, envelope moves, and a start month for each budget. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `budgets` ADD COLUMN `startDate` INTEGER NOT NULL DEFAULT 0")
                // Existing budgets start rolling over from the month you upgrade.
                val monthStart = java.time.YearMonth.now().atDay(1).toEpochDay()
                db.execSQL("UPDATE `budgets` SET `startDate` = $monthStart")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `debts` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `balance` INTEGER NOT NULL, `aprBps` INTEGER NOT NULL, " +
                        "`minPayment` INTEGER NOT NULL, `color` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `envelope_moves` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`fromCategoryId` INTEGER NOT NULL, `toCategoryId` INTEGER NOT NULL, " +
                        "`amount` INTEGER NOT NULL, `date` INTEGER NOT NULL)"
                )
            }
        }

        /** V1 -> V2: keeps every V1 row, adds new columns and tables. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `recurringId` INTEGER")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `iouId` INTEGER")
                db.execSQL("ALTER TABLE `subscriptions` ADD COLUMN `sharedWith` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `subscriptions` ADD COLUMN `lastUsed` INTEGER")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `recurring` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`type` TEXT NOT NULL, `amount` INTEGER NOT NULL, `accountId` INTEGER NOT NULL, " +
                        "`toAccountId` INTEGER, `categoryId` INTEGER, `note` TEXT NOT NULL, " +
                        "`cycle` TEXT NOT NULL, `customDays` INTEGER NOT NULL, `nextDate` INTEGER NOT NULL, " +
                        "`autoLog` INTEGER NOT NULL, `active` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `goals` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `target` INTEGER NOT NULL, `deadline` INTEGER, " +
                        "`icon` TEXT NOT NULL, `color` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
                        "`archived` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `goal_entries` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`goalId` INTEGER NOT NULL, `amount` INTEGER NOT NULL, `date` INTEGER NOT NULL, " +
                        "`note` TEXT NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_entries_goalId` ON `goal_entries` (`goalId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ious` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`person` TEXT NOT NULL, `amount` INTEGER NOT NULL, `direction` TEXT NOT NULL, " +
                        "`note` TEXT NOT NULL, `date` INTEGER NOT NULL, `settled` INTEGER NOT NULL, " +
                        "`settledDate` INTEGER)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `price_changes` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`subscriptionId` INTEGER NOT NULL, `oldAmount` INTEGER NOT NULL, " +
                        "`newAmount` INTEGER NOT NULL, `date` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_price_changes_subscriptionId` ON `price_changes` (`subscriptionId`)")
            }
        }
    }
}
