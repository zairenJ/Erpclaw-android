package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities =
    [
      AccountEntity::class,
      JournalEntryEntity::class,
      JournalLineEntity::class,
      InventoryItemEntity::class,
      StockMovementEntity::class,
      CustomerEntity::class,
      InvoiceEntity::class,
      InvoiceLineEntity::class,
      VendorEntity::class,
      PurchaseBillEntity::class,
      PurchaseBillLineEntity::class,
      EmployeeEntity::class,
      ExpenseClaimEntity::class,
      PayrollRunEntity::class,
      SyncOutboxEntity::class,
    ],
  version = 1,
  exportSchema = false,
)
abstract class ErpDatabase : RoomDatabase() {
  abstract fun erpDao(): ErpDao

  companion object {
    @Volatile private var INSTANCE: ErpDatabase? = null

    fun getInstance(context: Context): ErpDatabase {
      return INSTANCE
        ?: synchronized(this) {
          INSTANCE
            ?: Room.databaseBuilder(
                context.applicationContext,
                ErpDatabase::class.java,
                "erpclaw_local.db",
              )
              .build()
              .also { INSTANCE = it }
        }
    }
  }
}
