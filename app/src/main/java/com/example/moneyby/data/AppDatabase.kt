package com.example.moneyby.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.RoomDatabase.Callback
import androidx.room.migration.Migration
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(entities = [Transaction::class, Account::class, Budget::class, SavingGoal::class, Category::class, RecurringTransaction::class, BillReminder::class, PendingTransaction::class], version = 12, exportSchema = true)

abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun pendingTransactionDao(): PendingTransactionDao
    abstract fun accountDao(): AccountDao

    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun savingGoalDao(): SavingGoalDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun billReminderDao(): BillReminderDao

    companion object {
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Schema is the same, nothing changed
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN type TEXT NOT NULL DEFAULT 'Expense'")
                // Default income categories
                db.execSQL("INSERT INTO categories (name, iconName, color, type) VALUES ('Salary', 'payments', ${0xFF4CAF50.toInt()}, 'Income')")
                db.execSQL("INSERT INTO categories (name, iconName, color, type) VALUES ('Gift', 'redeem', ${0xFFFFEB3B.toInt()}, 'Income')")
                db.execSQL("INSERT INTO categories (name, iconName, color, type) VALUES ('Investment', 'trending_up', ${0xFF2196F3.toInt()}, 'Income')")
                db.execSQL("INSERT INTO categories (name, iconName, color, type) VALUES ('Others', 'category', ${0xFF9E9E9E.toInt()}, 'Income')")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {

            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN accountNumberSuffix TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN transactionHash TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN isAutoDetected INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `pending_transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `amount` REAL NOT NULL, `category` TEXT NOT NULL, `date` INTEGER NOT NULL, `type` TEXT NOT NULL, `accountSuffix` TEXT, `merchant` TEXT, `rawText` TEXT NOT NULL, `transactionHash` TEXT NOT NULL)")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_transactions_transactionHash` ON `transactions` (`transactionHash`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_pending_transactions_transactionHash` ON `pending_transactions` (`transactionHash`)")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add "Others" as an expense category for better auto-detection fallback
                db.execSQL("INSERT INTO categories (name, iconName, color, type) VALUES ('Others', 'category', ${0xFF9E9E9E.toInt()}, 'Expense')")
            }
        }


        @Volatile
        private var Instance: AppDatabase? = null

        fun getDatabase(context: Context, passphrase: ByteArray): AppDatabase {
            return Instance ?: synchronized(this) {
                val factory = SupportOpenHelperFactory(passphrase)
                Room.databaseBuilder(context, AppDatabase::class.java, "moneyby_secure_database")
                    .openHelperFactory(factory)
                    .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)


                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Use raw SQL to insert default accounts because Instance might not be initialized yet
                            db.execSQL("INSERT INTO accounts (name, type, initialBalance, accountNumberSuffix) VALUES ('Cash', 'Cash', 0.0, NULL)")
                            db.execSQL("INSERT INTO accounts (name, type, initialBalance, accountNumberSuffix) VALUES ('Bank', 'Bank', 0.0, NULL)")

                            
                            // Default categories
                            db.execSQL("INSERT INTO categories (name, iconName, color, type) VALUES ('Food', 'restaurant', ${0xFFFF9800.toInt()}, 'Expense')")
                            db.execSQL("INSERT INTO categories (name, iconName, color, type) VALUES ('Transport', 'directions_car', ${0xFF2196F3.toInt()}, 'Expense')")
                            db.execSQL("INSERT INTO categories (name, iconName, color, type) VALUES ('Shopping', 'shopping_cart', ${0xFFE91E63.toInt()}, 'Expense')")
                            db.execSQL("INSERT INTO categories (name, iconName, color, type) VALUES ('Health', 'medical_services', ${0xFF4CAF50.toInt()}, 'Expense')")
                            db.execSQL("INSERT INTO categories (name, iconName, color, type) VALUES ('Entertainment', 'movie', ${0xFF9C27B0.toInt()}, 'Expense')")
                            db.execSQL("INSERT INTO categories (name, iconName, color, type) VALUES ('Utilities', 'bolt', ${0xFF795548.toInt()}, 'Expense')")
                            db.execSQL("INSERT INTO categories (name, iconName, color, type) VALUES ('Others', 'category', ${0xFF9E9E9E.toInt()}, 'Expense')")
                            
                            db.execSQL("INSERT INTO categories (name, iconName, color, type) VALUES ('Salary', 'payments', ${0xFF4CAF50.toInt()}, 'Income')")
                            db.execSQL("INSERT INTO categories (name, iconName, color, type) VALUES ('Gift', 'redeem', ${0xFFFFEB3B.toInt()}, 'Income')")
                            db.execSQL("INSERT INTO categories (name, iconName, color, type) VALUES ('Investment', 'trending_up', ${0xFF2196F3.toInt()}, 'Income')")
                            db.execSQL("INSERT INTO categories (name, iconName, color, type) VALUES ('Others', 'category', ${0xFF9E9E9E.toInt()}, 'Income')")
                        }
                    })

                    .build()
                    .also { Instance = it }
            }
        }
    }
}
