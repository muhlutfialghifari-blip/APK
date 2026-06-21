package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "INCOME", "EXPENSE", "TRANSFER"
    val sourceAccount: String, // "Cash", "Saldo"
    val destinationAccount: String? = null, // Only used when type is "TRANSFER"
    val category: String,
    val amount: Double,
    val note: String,
    val timestamp: Long
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val categoryName: String,
    val monthlyLimitAmount: Double
)
