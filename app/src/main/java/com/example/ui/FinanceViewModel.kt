package com.example.ui

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BudgetEntity
import com.example.data.FinanceDatabase
import com.example.data.FinanceRepository
import com.example.data.TransactionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanceRepository
    val allTransactions: StateFlow<List<TransactionEntity>>
    val allBudgets: StateFlow<List<BudgetEntity>>

    // Custom categories persisted in SharedPreferences
    private val prefs = application.getSharedPreferences("FinanceTrackerPrefs", Context.MODE_PRIVATE)

    private val _customExpenseCategories = MutableStateFlow<List<String>>(emptyList())
    val customExpenseCategories = _customExpenseCategories.asStateFlow()

    private val _customIncomeCategories = MutableStateFlow<List<String>>(emptyList())
    val customIncomeCategories = _customIncomeCategories.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Filtered transaction list
    val filteredTransactions: StateFlow<List<TransactionEntity>>

    // PIN lock toggle state
    private val _isPinEnabled = MutableStateFlow(false)
    val isPinEnabled = _isPinEnabled.asStateFlow()

    private val _savedPin = MutableStateFlow("1234")
    val savedPin = _savedPin.asStateFlow()

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked = _isUnlocked.asStateFlow()

    // Custom App Icon path state
    private val _customIconPath = MutableStateFlow<String?>(null)
    val customIconPath = _customIconPath.asStateFlow()

    init {
        val database = FinanceDatabase.getDatabase(application)
        repository = FinanceRepository(database.financeDao())
        allTransactions = repository.allTransactions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        allBudgets = repository.allBudgets.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        _isPinEnabled.value = prefs.getBoolean("pin_enabled", false)
        _savedPin.value = prefs.getString("saved_pin", "1234") ?: "1234"
        _isUnlocked.value = !_isPinEnabled.value // If PIN lock is disabled, it is unlocked automatically
        _customIconPath.value = prefs.getString("custom_icon_path", null)

        // Load or initialize categories
        loadCategories()

        // Combine search and transactions
        filteredTransactions = combine(allTransactions, _searchQuery) { list, query ->
            if (query.isBlank()) list else {
                list.filter {
                    it.category.contains(query, ignoreCase = true) ||
                    it.note.contains(query, ignoreCase = true) ||
                    it.type.contains(query, ignoreCase = true)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    private fun loadCategories() {
        val expenseSaved = prefs.getString("expense_categories_v1", null)
        if (expenseSaved != null) {
            _customExpenseCategories.value = expenseSaved.split("||").filter { it.isNotBlank() }
        } else {
            val defaults = listOf("Makanan", "Belanjaan", "Sewa", "Tagihan", "Transportasi", "Belanja", "Hiburan", "Kesehatan", "Lainnya")
            _customExpenseCategories.value = defaults
            saveExpenseCategories(defaults)
        }

        val incomeSaved = prefs.getString("income_categories_v1", null)
        if (incomeSaved != null) {
            _customIncomeCategories.value = incomeSaved.split("||").filter { it.isNotBlank() }
        } else {
            val defaults = listOf("Gaji", "Pekerjaan Lepas", "Investasi", "Hadiah", "Lainnya")
            _customIncomeCategories.value = defaults
            saveIncomeCategories(defaults)
        }
    }

    private fun saveExpenseCategories(list: List<String>) {
        prefs.edit().putString("expense_categories_v1", list.joinToString("||")).apply()
    }

    private fun saveIncomeCategories(list: List<String>) {
        prefs.edit().putString("income_categories_v1", list.joinToString("||")).apply()
    }

    fun addExpenseCategory(name: String) {
        if (name.isNotBlank() && !_customExpenseCategories.value.contains(name)) {
            val updated = _customExpenseCategories.value + name
            _customExpenseCategories.value = updated
            saveExpenseCategories(updated)
        }
    }

    fun deleteExpenseCategory(name: String) {
        val updated = _customExpenseCategories.value.filter { it != name }
        _customExpenseCategories.value = updated
        saveExpenseCategories(updated)
    }

    fun addIncomeCategory(name: String) {
        if (name.isNotBlank() && !_customIncomeCategories.value.contains(name)) {
            val updated = _customIncomeCategories.value + name
            _customIncomeCategories.value = updated
            saveIncomeCategories(updated)
        }
    }

    fun deleteIncomeCategory(name: String) {
        val updated = _customIncomeCategories.value.filter { it != name }
        _customIncomeCategories.value = updated
        saveIncomeCategories(updated)
    }

    fun setPinEnabled(enabled: Boolean) {
        _isPinEnabled.value = enabled
        prefs.edit().putBoolean("pin_enabled", enabled).apply()
        if (!enabled) {
            _isUnlocked.value = true
        }
    }

    fun changePin(newPin: String) {
        _savedPin.value = newPin
        prefs.edit().putString("saved_pin", newPin).apply()
    }

    fun verifyPin(pin: String): Boolean {
        if (pin == _savedPin.value) {
            _isUnlocked.value = true
            return true
        }
        return false
    }

    fun unlockApp() {
        _isUnlocked.value = true
    }

    fun lockApp() {
        if (_isPinEnabled.value) {
            _isUnlocked.value = false
        }
    }

    fun setCustomIconPath(path: String?) {
        _customIconPath.value = path
        prefs.edit().putString("custom_icon_path", path).apply()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Database Actions
    fun insertTransaction(type: String, sourceAccount: String, destinationAccount: String?, category: String, amount: Double, note: String, timestamp: Long) {
        viewModelScope.launch {
            repository.insertTransaction(
                TransactionEntity(
                    type = type,
                    sourceAccount = sourceAccount,
                    destinationAccount = if (type == "TRANSFER") destinationAccount else null,
                    category = category,
                    amount = amount,
                    note = note,
                    timestamp = timestamp
                )
            )
        }
    }

    fun updateTransaction(id: Int, type: String, sourceAccount: String, destinationAccount: String?, category: String, amount: Double, note: String, timestamp: Long) {
        viewModelScope.launch {
            repository.updateTransaction(
                TransactionEntity(
                    id = id,
                    type = type,
                    sourceAccount = sourceAccount,
                    destinationAccount = if (type == "TRANSFER") destinationAccount else null,
                    category = category,
                    amount = amount,
                    note = note,
                    timestamp = timestamp
                )
            )
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun deleteTransactionById(id: Int) {
        viewModelScope.launch {
            repository.deleteTransactionById(id)
        }
    }

    fun setBudgetLimit(category: String, limit: Double) {
        viewModelScope.launch {
            repository.insertBudget(BudgetEntity(category, limit))
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }

    // CSV Export & Import Utility helper
    fun exportToCSV(context: Context): String? {
        val list = allTransactions.value
        if (list.isEmpty()) {
            Toast.makeText(context, "Tidak ada transaksi untuk diekspor!", Toast.LENGTH_SHORT).show()
            return null
        }
        val builder = java.lang.StringBuilder()
        builder.append("id,type,sourceAccount,destinationAccount,category,amount,note,timestamp\n")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        for (item in list) {
            val dateStr = sdf.format(java.util.Date(item.timestamp))
            val noteEscaped = item.note.replace(",", " ")
            builder.append("${item.id},${item.type},${item.sourceAccount},${item.destinationAccount ?: ""},${item.category},${item.amount},\"$noteEscaped\",${item.timestamp}\n")
        }

        return try {
            val downloadsDir = context.getExternalFilesDir(null) ?: context.filesDir
            val backupFile = File(downloadsDir, "FinanceTracker_Backup.csv")
            backupFile.writeText(builder.toString())
            Toast.makeText(context, "Disimpan di ${backupFile.absolutePath}", Toast.LENGTH_LONG).show()
            builder.toString()
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal ekspor: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    fun importFromCSV(context: Context, csvContent: String) {
        try {
            val lines = csvContent.lineSequence().toList()
            if (lines.size <= 1) {
                Toast.makeText(context, "CSV kosong atau tidak valid!", Toast.LENGTH_SHORT).show()
                return
            }
            var successCount = 0
            viewModelScope.launch {
                for (i in 1 until lines.size) {
                    val line = lines[i].trim()
                    if (line.isEmpty()) continue
                    // Parse line allowing quotes for Notes field
                    val tokens = parseCsvLine(line)
                    if (tokens.size >= 8) {
                        try {
                            val type = tokens[1].trim()
                            val sourceAccount = tokens[2].trim()
                            val destinationAccount = tokens[3].trim().let { if (it.isEmpty()) null else it }
                            val category = tokens[4].trim()
                            val amount = tokens[5].trim().toDoubleOrNull() ?: 0.0
                            val note = tokens[6].trim().removeSurrounding("\"")
                            val timestamp = tokens[7].trim().toLongOrNull() ?: System.currentTimeMillis()

                            repository.insertTransaction(
                                TransactionEntity(
                                    type = type,
                                    sourceAccount = sourceAccount,
                                    destinationAccount = destinationAccount,
                                    category = category,
                                    amount = amount,
                                    note = note,
                                    timestamp = timestamp
                                )
                            )
                            successCount++
                        } catch (e: Exception) {
                            // Skip invalid lines
                        }
                    }
                }
                Toast.makeText(context, "Berhasil mengimpor $successCount transaksi", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal impor: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var curVal = StringBuilder()
        var inQuotes = false
        for (ch in line.toCharArray()) {
            if (inQuotes) {
                if (ch == '\"') {
                    inQuotes = false
                } else {
                    curVal.append(ch)
                }
            } else {
                if (ch == '\"') {
                    inQuotes = true
                } else if (ch == ',') {
                    result.add(curVal.toString())
                    curVal = StringBuilder()
                } else {
                    curVal.append(ch)
                }
            }
        }
        result.add(curVal.toString())
        return result
    }
}
