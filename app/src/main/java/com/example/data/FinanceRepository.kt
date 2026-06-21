package com.example.data

import kotlinx.coroutines.flow.Flow

class FinanceRepository(private val financeDao: FinanceDao) {
    val allTransactions: Flow<List<TransactionEntity>> = financeDao.getAllTransactions()
    val allBudgets: Flow<List<BudgetEntity>> = financeDao.getAllBudgets()

    suspend fun insertTransaction(transaction: TransactionEntity) {
        financeDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        financeDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        financeDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Int) {
        financeDao.deleteTransactionById(id)
    }

    suspend fun insertBudget(budget: BudgetEntity) {
        financeDao.insertBudget(budget)
    }

    suspend fun deleteBudget(budget: BudgetEntity) {
        financeDao.deleteBudget(budget)
    }
}
