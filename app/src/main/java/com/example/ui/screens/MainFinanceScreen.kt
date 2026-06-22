package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.BudgetEntity
import com.example.data.TransactionEntity
import com.example.ui.FinanceViewModel
import com.example.ui.components.CategoryPieChart
import com.example.ui.components.ComparisonBarChart
import com.example.ui.components.FinanceLineChart
import com.example.ui.theme.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFinanceScreen(
    viewModel: FinanceViewModel = viewModel()
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf("home") }
    
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val rawTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val budgets by viewModel.allBudgets.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()
    val isPinEnabled by viewModel.isPinEnabled.collectAsStateWithLifecycle()
    val customIconPath by viewModel.customIconPath.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    // If locked, show the Lock screen
    if (!isUnlocked && isPinEnabled) {
        SecureLockScreen(
            viewModel = viewModel,
            onUnlockAttempt = { viewModel.unlockApp() }
        )
        return
    }

    Scaffold(
        containerColor = SolidBlack,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ALGHI 💞 LISA",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when (currentTab) {
                                "home" -> "Ikhtisar"
                                "history" -> "Log Riwayat"
                                "budget" -> "Anggaran"
                                "settings" -> "Pengaturan"
                                else -> "Ikhtisar"
                            },
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.5).sp
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SubCardGrey)
                            .border(1.dp, if (customIconPath != null) NeonCyan else AccentGrey, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (customIconPath != null) {
                            AsyncImage(
                                model = customIconPath,
                                contentDescription = "Custom Loader / App Icon",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CurrencyExchange,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SolidBlack,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkGrey,
                tonalElevation = 8.dp,
                modifier = Modifier.border(1.dp, AccentGrey)
            ) {
                listOf(
                    Triple("home", "Beranda", Icons.Outlined.Dashboard to Icons.Filled.Dashboard),
                    Triple("history", "Riwayat", Icons.Outlined.History to Icons.Filled.History),
                    Triple("budget", "Anggaran", Icons.Outlined.PieChart to Icons.Filled.PieChart),
                    Triple("settings", "Pengaturan", Icons.Outlined.Settings to Icons.Filled.Settings)
                ).forEach { (tabId, label, icons) ->
                    val isSelected = currentTab == tabId
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tabId },
                        label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) icons.second else icons.first,
                                contentDescription = label
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            selectedTextColor = NeonCyan,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = AccentGrey
                        ),
                        modifier = Modifier.testTag("nav_tab_$tabId")
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentTab == "home") {
                FloatingActionButton(
                    onClick = {
                        editingTransaction = null
                        showAddDialog = true
                    },
                    containerColor = NeonCyan,
                    contentColor = SolidBlack,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier
                        .testTag("add_transaction_fab")
                        .size(56.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction", modifier = Modifier.size(28.dp))
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen selection
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "TabTransition"
            ) { targetTab ->
                when (targetTab) {
                    "home" -> DashboardTab(
                        transactions = rawTransactions,
                        viewModel = viewModel,
                        onEditTransaction = { tx ->
                            editingTransaction = tx
                            showAddDialog = true
                        }
                    )
                    "history" -> HistoryTab(
                        transactions = transactions,
                        searchQuery = viewModel.searchQuery.collectAsStateWithLifecycle().value,
                        onQueryChange = { viewModel.updateSearchQuery(it) },
                        onEdit = { tx ->
                            editingTransaction = tx
                            showAddDialog = true
                        },
                        onDelete = { tx -> viewModel.deleteTransaction(tx) }
                    )
                    "budget" -> BudgetTab(
                        transactions = rawTransactions,
                        budgets = budgets,
                        viewModel = viewModel
                    )
                    "settings" -> SettingsTab(
                        viewModel = viewModel
                    )
                }
            }

            // Global Add / Edit Form Modal
            if (showAddDialog) {
                AddEditTransactionDialog(
                    transaction = editingTransaction,
                    viewModel = viewModel,
                    onDismiss = { showAddDialog = false }
                )
            }
        }
    }
}

// SECURE PIN KEYPAD LOCK SCREEN
@Composable
fun SecureLockScreen(
    viewModel: FinanceViewModel,
    onUnlockAttempt: () -> Unit
) {
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val customIconPath by viewModel.customIconPath.collectAsStateWithLifecycle()

    var lockScreenVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        lockScreenVisible = true
    }

    LaunchedEffect(pinInput) {
        if (pinInput.length == 4) {
            val success = viewModel.verifyPin(pinInput)
            if (success) {
                onUnlockAttempt()
            } else {
                isError = true
                Toast.makeText(context, "PIN Salah! Silakan coba lagi", Toast.LENGTH_SHORT).show()
                // Wait a moment and then reset
                kotlinx.coroutines.delay(800)
                pinInput = ""
                isError = false
            }
        }
    }

    AnimatedVisibility(
        visible = lockScreenVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = 850, easing = LinearOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SolidBlack),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 400.dp)
            ) {
                if (customIconPath != null) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CardGrey)
                            .border(1.5.dp, if (isError) NeonRed else NeonCyan, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(java.io.File(customIconPath!!))
                                .crossfade(true)
                                .build(),
                            contentDescription = "Custom App Identity Logo",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = if (isError) NeonRed else NeonCyan,
                        modifier = Modifier.size(64.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "BRANKAS KEUANGAN TERKUNCI",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isError) NeonRed else NeonCyan
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Masukkan PIN Keamanan untuk membuka",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                // PIN Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val isActive = i < pinInput.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isError -> NeonRed
                                        isActive -> NeonCyan
                                        else -> AccentGrey
                                    }
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "(PIN Bawaan: 1234)",
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(30.dp))

                // Numeric Keypad Grid
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val rows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("C", "0", "⌫")
                    )

                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            row.forEach { char ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1.8f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CardGrey)
                                        .clickable {
                                            if (isError) return@clickable
                                            when (char) {
                                                "C" -> pinInput = ""
                                                "⌫" -> {
                                                    if (pinInput.isNotEmpty()) {
                                                        pinInput = pinInput.dropLast(1)
                                                    }
                                                }
                                                else -> {
                                                    if (pinInput.length < 4) {
                                                        pinInput += char
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = char,
                                        fontSize = 20.sp,
                                        color = if (char == "C" || char == "⌫") NeonCyan else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun textButtonColor(): Color = NeonCyan.copy(alpha = 0.7f)

// ---------------- HOME DASHBOARD TAB ----------------
@Composable
fun DashboardTab(
    transactions: List<TransactionEntity>,
    viewModel: FinanceViewModel,
    onEditTransaction: (TransactionEntity) -> Unit
) {
    val (netWorth, cash, saldo) = remember(transactions) {
        var cashSum = 0.0
        var saldoSum = 0.0
        
        transactions.forEach { tx ->
            when (tx.type) {
                "INCOME" -> {
                    if (tx.sourceAccount == "Tunai") cashSum += tx.amount
                    else saldoSum += tx.amount
                }
                "EXPENSE" -> {
                    if (tx.sourceAccount == "Tunai") cashSum -= tx.amount
                    else saldoSum -= tx.amount
                }
                "TRANSFER" -> {
                    // deduct from source, add to destination
                    if (tx.sourceAccount == "Tunai") cashSum -= tx.amount else saldoSum -= tx.amount
                    if (tx.destinationAccount == "Tunai") cashSum += tx.amount else saldoSum += tx.amount
                }
            }
        }
        val net = cashSum + saldoSum
        Triple(net, cashSum, saldoSum)
    }

    val df = DecimalFormat("Rp #,##0", java.text.DecimalFormatSymbols(java.util.Locale("id", "ID")))

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Combined Sleek Net Worth & Balance Sub-accounts
            Card(
                colors = CardDefaults.cardColors(containerColor = CardGrey),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AccentGrey, RoundedCornerShape(24.dp)),
                 shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "TOTAL KEKAYAAN BERSIH",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = df.format(netWorth),
                            color = NeonCyan,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Light,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            letterSpacing = (-1.5).sp
                        )
                        // Sleek trend growth badge
                        Surface(
                            color = NeonCyan.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "+2.4%",
                                color = NeonCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Cash card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SubCardGrey),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, AccentGrey.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Payments, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "TUNAI",
                                        color = TextMuted,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = df.format(cash),
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Saldo card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SubCardGrey),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, AccentGrey.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = null, tint = NeonViolet, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "SALDO",
                                        color = TextMuted,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = df.format(saldo),
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 7-day Cash Flow Line Chart
        item {
            FinanceLineChart(
                transactions = transactions,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Recent Transactions Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TRANSAKSI TERBARU",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        if (transactions.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardGrey),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada riwayat transaksi. Ketuk + di bawah untuk menambahkan.",
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            itemsIndexed(transactions.take(5)) { index, tx ->
                TransactionRowItem(
                    transaction = tx,
                    onEdit = onEditTransaction,
                    staggerIndex = index
                )
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

// ---------------- TRANSACTION LIST COMPONENT ----------------
@Composable
fun TransactionRowItem(
    transaction: TransactionEntity,
    onEdit: (TransactionEntity) -> Unit,
    staggerIndex: Int = 0
) {
    val df = DecimalFormat("Rp #,##0", java.text.DecimalFormatSymbols(java.util.Locale("id", "ID")))
    val dateSdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = transaction.id) {
        kotlinx.coroutines.delay(staggerIndex * 50L)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = 450, easing = LinearOutSlowInEasing)) +
                slideInVertically(
                    initialOffsetY = { 40 },
                    animationSpec = tween(durationMillis = 450, easing = LinearOutSlowInEasing)
                ) +
                scaleIn(
                    initialScale = 0.95f,
                    animationSpec = tween(durationMillis = 450, easing = LinearOutSlowInEasing)
                ),
        exit = fadeOut(animationSpec = tween(durationMillis = 200))
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SubCardGrey.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AccentGrey.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .clickable { onEdit(transaction) }
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type Icon
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when (transaction.type) {
                                    "INCOME" -> NeonCyan.copy(alpha = 0.15f)
                                    "EXPENSE" -> NeonRed.copy(alpha = 0.15f)
                                    else -> NeonViolet.copy(alpha = 0.15f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (transaction.type) {
                                "INCOME" -> Icons.Default.ArrowDownward
                                "EXPENSE" -> Icons.Default.ArrowUpward
                                else -> Icons.Default.SwapHoriz
                            },
                            contentDescription = null,
                            tint = when (transaction.type) {
                                "INCOME" -> NeonCyan
                                "EXPENSE" -> NeonRed
                                else -> NeonViolet
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    val displaySource = if (transaction.sourceAccount == "Cash") "Tunai" else transaction.sourceAccount
                    val displayDest = if (transaction.destinationAccount == "Cash") "Tunai" else transaction.destinationAccount

                    Column {
                        Text(
                            text = transaction.category,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${displaySource}${if (transaction.type == "TRANSFER") " ➔ " + displayDest else ""} • ${transaction.note.ifBlank { "Belum ada catatan" }}",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${if (transaction.type == "INCOME") "+" else if (transaction.type == "EXPENSE") "-" else ""}${df.format(transaction.amount)}",
                        color = when (transaction.type) {
                            "INCOME" -> NeonCyan
                            "EXPENSE" -> NeonRed
                            else -> NeonViolet
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dateSdf.format(Date(transaction.timestamp)),
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// ---------------- HISTORY TAB ----------------
@Composable
fun HistoryTab(
    transactions: List<TransactionEntity>,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onEdit: (TransactionEntity) -> Unit,
    onDelete: (TransactionEntity) -> Unit
) {
    val groupedTransactions = remember(transactions) {
        val groupSdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        transactions.groupBy { groupSdf.format(Date(it.timestamp)) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            placeholder = { Text("Cari kategori, tipe, atau catatan...", color = TextSecondary, fontSize = 13.sp) },
            prefix = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan, modifier = Modifier.padding(end = 6.dp)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_history_bar"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = AccentGrey,
                focusedContainerColor = CardGrey,
                unfocusedContainerColor = CardGrey,
                focusedLabelColor = NeonCyan,
                unfocusedLabelColor = TextSecondary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tidak ada riwayat transaksi yang cocok.",
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groupedTransactions.entries.forEach { entry ->
                    item {
                        Text(
                            text = entry.key,
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    itemsIndexed(entry.value) { index, tx ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                TransactionRowItem(
                                    transaction = tx,
                                    onEdit = onEdit,
                                    staggerIndex = index
                                )
                            }
                            IconButton(onClick = { onDelete(tx) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NeonRed)
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

// ---------------- BUDGETING TAB ----------------
@Composable
fun BudgetTab(
    transactions: List<TransactionEntity>,
    budgets: List<BudgetEntity>,
    viewModel: FinanceViewModel
) {
    val context = LocalContext.current
    var inputCategory by remember { mutableStateOf("") }
    var inputLimitStr by remember { mutableStateOf("") }
    var showAddBudgetDialog by remember { mutableStateOf(false) }

    // Aggregate monthly spending per category for CURRENT calendar month
    val currentSpentPerCategory = remember(transactions) {
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        
        val map = mutableMapOf<String, Double>()
        transactions.forEach { tx ->
            if (tx.type == "EXPENSE") {
                val txCal = Calendar.getInstance()
                txCal.timeInMillis = tx.timestamp
                if (txCal.get(Calendar.MONTH) == currentMonth && txCal.get(Calendar.YEAR) == currentYear) {
                    map[tx.category] = (map[tx.category] ?: 0.0) + tx.amount
                }
            }
        }
        map
    }

    // Spend analysis mapping for ALL categories
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Pie Breakdown
        CategoryPieChart(categorySpending = currentSpentPerCategory)

        // Comparison Bar Chart
        ComparisonBarChart(transactions = transactions)

        // Budgets header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ANGGARAN PENGELUARAN BULANAN",
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
            Button(
                onClick = { showAddBudgetDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = NeonViolet, contentColor = TextPrimary),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.AddTask, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("BATAS LIMIT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (budgets.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardGrey),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada anggaran yang diatur. Ketuk BATAS LIMIT untuk menetapkan target bulanan.",
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            budgets.forEach { budget ->
                val spent = currentSpentPerCategory[budget.categoryName] ?: 0.0
                val percent = if (budget.monthlyLimitAmount > 0) spent / budget.monthlyLimitAmount else 0.0
                val isCritical = percent >= 0.85

                Card(
                    colors = CardDefaults.cardColors(containerColor = CardGrey),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (isCritical) NeonRed.copy(alpha = 0.5f) else AccentGrey, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = budget.categoryName,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Terpakai Rp " + java.text.NumberFormat.getNumberInstance(java.util.Locale("id", "ID")).format(spent.toInt()) + " dari Rp " + java.text.NumberFormat.getNumberInstance(java.util.Locale("id", "ID")).format(budget.monthlyLimitAmount.toInt()),
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            // Delete budget
                            IconButton(onClick = { viewModel.deleteBudget(budget) }) {
                                Icon(Icons.Default.Clear, contentDescription = "Delete", tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Custom Progress Bar turning Warning Red when spending exceeds 85%
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(AccentGrey)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(percent.toFloat().coerceAtMost(1f))
                                    .clip(CircleShape)
                                    .background(if (isCritical) NeonRed else NeonCyan)
                            )
                        }

                        // Danger label
                        if (isCritical) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "PERINGATAN KRITIS: MELEBIHI 85% DARI TARGET ANGGARAN",
                                color = NeonRed,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }

    // Set Budget Dialog
    if (showAddBudgetDialog) {
        val categoriesExpense by viewModel.customExpenseCategories.collectAsStateWithLifecycle()
        var selectedCategory by remember { mutableStateOf(categoriesExpense.firstOrNull() ?: "") }
        var limitInput by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddBudgetDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkGrey),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeonViolet, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "ATUR TARGET PENGELUARAN BULANAN",
                        color = NeonViolet,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // Drops Selection
                    Column {
                        Text("Kategori target", color = TextSecondary, fontSize = 11.sp)
                        var expanded by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CardGrey)
                                .border(1.dp, AccentGrey, RoundedCornerShape(8.dp))
                                .clickable { expanded = true }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(selectedCategory, color = TextPrimary)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = NeonCyan)
                            }
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(DarkGrey)
                        ) {
                            categoriesExpense.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat, color = TextPrimary) },
                                    onClick = {
                                        selectedCategory = cat
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Input Limit
                    OutlinedTextField(
                        value = limitInput,
                        onValueChange = { if (it.toDoubleOrNull() != null || it.isEmpty()) limitInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("Batas pengeluaran bulanan (Rp)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonViolet,
                            unfocusedBorderColor = AccentGrey,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAddBudgetDialog = false }) {
                            Text("Batal", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val limit = limitInput.toDoubleOrNull() ?: 0.0
                                if (selectedCategory.isNotBlank() && limit > 0) {
                                    viewModel.setBudgetLimit(selectedCategory, limit)
                                    showAddBudgetDialog = false
                                } else {
                                    Toast.makeText(context, "Masukkan jumlah batas yang valid", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonViolet, contentColor = TextPrimary)
                        ) {
                            Text("SIMPAN TARGET", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Helper to copy selected file URI into local sandboxed system
private fun saveUriToInternalStorage(context: Context, uri: android.net.Uri): java.io.File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = java.io.File(context.filesDir, "custom_app_icon_${System.currentTimeMillis()}.png")
        context.filesDir.listFiles()?.forEach { f ->
            if (f.name.startsWith("custom_app_icon") && f.exists()) {
                f.delete()
            }
        }
        val outputStream = java.io.FileOutputStream(file)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// ---------------- SETTINGS TAB ----------------
@Composable
fun SettingsTab(
    viewModel: FinanceViewModel
) {
    val context = LocalContext.current
    val categoriesExpense by viewModel.customExpenseCategories.collectAsStateWithLifecycle()
    val categoriesIncome by viewModel.customIncomeCategories.collectAsStateWithLifecycle()
    val isPinEnabled by viewModel.isPinEnabled.collectAsStateWithLifecycle()
    val customIconPath by viewModel.customIconPath.collectAsStateWithLifecycle()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val file = saveUriToInternalStorage(context, uri)
            if (file != null) {
                viewModel.setCustomIconPath(file.absolutePath)
                Toast.makeText(context, "Ikon kustom berhasil dipasang!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Gagal memproses gambar!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var expenseCategoryInput by remember { mutableStateOf("") }
    var incomeCategoryInput by remember { mutableStateOf("") }
    var showChangePinDialog by remember { mutableStateOf(false) }

    if (showChangePinDialog) {
        var currentInput by remember { mutableStateOf("") }
        var newInput by remember { mutableStateOf("") }
        var confirmInput by remember { mutableStateOf("") }
        val savedPinState by viewModel.savedPin.collectAsStateWithLifecycle()

        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            containerColor = SolidBlack,
            modifier = Modifier
                .border(1.dp, NeonCyan, RoundedCornerShape(16.dp))
                .padding(4.dp),
            title = {
                Text(
                    text = "UBAH PIN KEAMANAN",
                    color = NeonCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text("PIN Sekarang", color = TextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = currentInput,
                            onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) currentInput = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("Ketik PIN saat ini", color = TextMuted, fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = AccentGrey,
                                focusedLabelColor = NeonCyan,
                                unfocusedLabelColor = TextSecondary,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    Column {
                        Text("PIN Baru (4 Digit)", color = TextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = newInput,
                            onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) newInput = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("Ketik PIN baru", color = TextMuted, fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = AccentGrey,
                                focusedLabelColor = NeonCyan,
                                unfocusedLabelColor = TextSecondary,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    Column {
                        Text("Konfirmasi PIN Baru", color = TextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = confirmInput,
                            onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) confirmInput = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("Ketik ulang PIN baru", color = TextMuted, fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = AccentGrey,
                                focusedLabelColor = NeonCyan,
                                unfocusedLabelColor = TextSecondary,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (currentInput != savedPinState) {
                            Toast.makeText(context, "PIN Sekarang salah!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newInput.length != 4) {
                            Toast.makeText(context, "PIN Baru harus tepat 4 digit!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newInput != confirmInput) {
                            Toast.makeText(context, "Konfirmasi PIN tidak cocok!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        viewModel.changePin(newInput)
                        Toast.makeText(context, "PIN Keamanan berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                        showChangePinDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = SolidBlack)
                ) {
                    Text("SIMPAN", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text("BATAL", color = TextSecondary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Security Panel
        Card(
            colors = CardDefaults.cardColors(containerColor = CardGrey),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ANTARMUKA KEAMANAN",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Pengaman Kunci PIN", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Minta verifikasi PIN pengaman saat aplikasi dibuka", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = isPinEnabled,
                        onCheckedChange = { viewModel.setPinEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCyan,
                            checkedTrackColor = NeonCyan.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = AccentGrey
                        )
                    )
                }

                if (isPinEnabled) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { showChangePinDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGrey, contentColor = TextPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("UBAH PIN KEAMANAN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // CSV Local Backups
        Card(
            colors = CardDefaults.cardColors(containerColor = CardGrey),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "CADANGAN DATA LOKAL",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                    Button(
                        onClick = {
                            val exported = viewModel.exportToCSV(context)
                            if (exported != null) {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(exported))
                                Toast.makeText(context, "Cadangan data disalin ke papan klip!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = SolidBlack),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("EKSPOR CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // For import, we'll offer a quick import trigger that loads from a pre-made file or reads clipboard data
                    Button(
                        onClick = {
                            val clipBoard = clipboardManager.getText()
                            if (clipBoard != null && clipBoard.text.contains("id,type,sourceAccount")) {
                                viewModel.importFromCSV(context, clipBoard.text)
                            } else {
                                Toast.makeText(context, "Format cadangan tidak ditemukan di papan klip! Salin CSV lalu ketuk impor.", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGrey, contentColor = TextPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("IMPOR PAPAN KLIP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Expense Category Manager
        Card(
            colors = CardDefaults.cardColors(containerColor = CardGrey),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "MANAJEMEN KATEGORI PENGELUARAN",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = expenseCategoryInput,
                        onValueChange = { expenseCategoryInput = it },
                        placeholder = { Text("Tambah kategori baru...", fontSize = 12.sp, color = TextSecondary) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = AccentGrey,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (expenseCategoryInput.isNotBlank()) {
                                viewModel.addExpenseCategory(expenseCategoryInput)
                                expenseCategoryInput = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Add", tint = NeonCyan, modifier = Modifier.size(32.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // chip lists
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    modifierSpacing = 6.dp
                ) {
                    categoriesExpense.forEach { cat ->
                        AssistChip(
                            onClick = { viewModel.deleteExpenseCategory(cat) },
                            label = { Text(cat, fontSize = 11.sp) },
                            trailingIcon = { Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = TextPrimary,
                                leadingIconContentColor = NeonCyan,
                                trailingIconContentColor = NeonRed
                            ),
                            chipBorder = AssistChipDefaults.assistChipBorder(borderColor = AccentGrey)
                        )
                    }
                }
            }
        }

        // Income Category Manager
        Card(
            colors = CardDefaults.cardColors(containerColor = CardGrey),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "MANAJEMEN KATEGORI PENDAPATAN",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = incomeCategoryInput,
                        onValueChange = { incomeCategoryInput = it },
                        placeholder = { Text("Tambah kategori baru...", fontSize = 12.sp, color = TextSecondary) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = AccentGrey,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (incomeCategoryInput.isNotBlank()) {
                                viewModel.addIncomeCategory(incomeCategoryInput)
                                incomeCategoryInput = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Add", tint = NeonCyan, modifier = Modifier.size(32.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    modifierSpacing = 6.dp
                ) {
                    categoriesIncome.forEach { cat ->
                        AssistChip(
                            onClick = { viewModel.deleteIncomeCategory(cat) },
                            label = { Text(cat, fontSize = 11.sp) },
                            trailingIcon = { Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = TextPrimary,
                                leadingIconContentColor = NeonCyan,
                                trailingIconContentColor = NeonRed
                            ),
                            chipBorder = AssistChipDefaults.assistChipBorder(borderColor = AccentGrey)
                        )
                    }
                }
            }
        }

        // App Icon Customizer Manager Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardGrey),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "KUSTOMISASI IKON APLIKASI",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Ubah ikon aplikasi di pengaturan dan bilah navigasi dengan foto Anda sendiri.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Simulated Beautiful Android Launcher / Homescreen widget
                Text(
                    text = "PRATINJAU LAYAR BERANDA SIMULASI",
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SolidBlack)
                        .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Mock Status Bar / Info time
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("12:00", color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = TextMuted, modifier = Modifier.size(10.dp))
                                Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(10.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Launcher 4-items Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mock App 1: Telepon
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(SubCardGrey)
                                        .border(1.dp, AccentGrey, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF2ECC71), modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Telepon", color = TextPrimary, fontSize = 9.sp)
                            }

                            // Dynamic App 2: FinanceFlow (Our custom logo!)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(SubCardGrey)
                                        .border(2.dp, NeonCyan, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (customIconPath != null) {
                                        AsyncImage(
                                            model = customIconPath,
                                            contentDescription = "Dynamic Custom Icon Preview",
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("FinanceFlow", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            // Mock App 3: Kamera
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(SubCardGrey)
                                        .border(1.dp, AccentGrey, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color(0xFF95A5A6), modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Kamera", color = TextPrimary, fontSize = 9.sp)
                            }

                            // Mock App 4: Pesan
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(SubCardGrey)
                                        .border(1.dp, AccentGrey, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Message, contentDescription = null, tint = Color(0xFF3498DB), modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Pesan", color = TextPrimary, fontSize = 9.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = SolidBlack),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PILIH FOTO BARU", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (customIconPath != null) {
                        Button(
                            onClick = {
                                viewModel.setCustomIconPath(null)
                                Toast.makeText(context, "Ikon kustom disetel kembali ke bawaan", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGrey, contentColor = NeonRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("GUNAKAN BAWAAN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// Custom flow row layout helper to safely prevent standard compose flow layouts versions mismatch bugs
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    modifierSpacing: androidx.compose.ui.unit.Dp = 6.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        var rowWidth = 0
        var rowHeight = 0
        val placeables = measurables.map { it.measure(constraints) }
        
        layout(constraints.maxWidth, constraints.maxHeight.coerceAtMost(600)) {
            var xVal = 0
            var yVal = 0
            val spacingPx = modifierSpacing.roundToPx()
            
            placeables.forEach { pl ->
                if (xVal + pl.width > constraints.maxWidth) {
                    xVal = 0
                    yVal += pl.height + spacingPx
                }
                pl.placeRelative(xVal, yVal)
                xVal += pl.width + spacingPx
            }
        }
    }
}

// Icon trailing helper constructor
@Composable
fun AssistChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    trailingIcon: @Composable () -> Unit,
    colors: ChipColors = AssistChipDefaults.assistChipColors(),
    chipBorder: ChipBorder = AssistChipDefaults.assistChipBorder()
) {
    Surface(
        onClick = onClick,
        color = CardGrey,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.border(1.dp, AccentGrey, RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            label()
            Spacer(modifier = Modifier.width(6.dp))
            trailingIcon()
        }
    }
}

// ---------------- ADD / EDIT TRANSACTION DIALOG ----------------
@Composable
fun AddEditTransactionDialog(
    transaction: TransactionEntity?,
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isEditMode = transaction != null

    var txType by remember { mutableStateOf(transaction?.type ?: "EXPENSE") } // "INCOME", "EXPENSE", "TRANSFER"
    var sourceAccount by remember { mutableStateOf(if (transaction?.sourceAccount == "Cash") "Tunai" else (transaction?.sourceAccount ?: "Tunai")) } // "Tunai", "Saldo"
    var destinationAccount by remember { mutableStateOf(if (transaction?.destinationAccount == "Cash") "Tunai" else (transaction?.destinationAccount ?: "Saldo")) } // used when type is "TRANSFER"
    var inputAmount by remember { mutableStateOf(transaction?.amount?.let { if (it % 1 == 0.0) it.toInt().toString() else it.toString() } ?: "") }
    var inputNote by remember { mutableStateOf(transaction?.note ?: "") }
    var selectedTimestamp by remember { mutableStateOf(transaction?.timestamp ?: System.currentTimeMillis()) }

    // Categories
    val categoriesExpense by viewModel.customExpenseCategories.collectAsStateWithLifecycle()
    val categoriesIncome by viewModel.customIncomeCategories.collectAsStateWithLifecycle()
    val dropCategories = if (txType == "INCOME") categoriesIncome else if (txType == "EXPENSE") categoriesExpense else listOf("Transfer")

    var selectedCategory by remember(txType) {
        mutableStateOf(
            if (isEditMode && transaction!!.type == txType) transaction.category
            else dropCategories.firstOrNull() ?: "Lainnya"
        )
    }

    val dateFormater = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkGrey),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, NeonCyan, RoundedCornerShape(20.dp))
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (isEditMode) "DEKRIPSI & EDIT DETAIL TRANSAKSI" else "ENKRIPSI DETAIL TRANSAKSI BARU",
                    color = NeonCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Segmented Type Chooser (Income / Expense / Transfer)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardGrey)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    listOf("EXPENSE" to "PENGELUARAN", "INCOME" to "PENDAPATAN", "TRANSFER" to "TRANSFER").forEach { (type, label) ->
                        val active = txType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (active) (if (type == "EXPENSE") NeonRed else if (type == "INCOME") NeonCyan else NeonViolet) else Color.Transparent)
                                .clickable {
                                    txType = type
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (active) SolidBlack else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                // Amount Field
                Column {
                    Text("JUMLAH NOMINAL (RP)", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                             .fillMaxWidth()
                             .height(54.dp)
                             .clip(RoundedCornerShape(10.dp))
                             .background(CardGrey)
                             .border(1.dp, NeonCyan, RoundedCornerShape(10.dp))
                             .padding(horizontal = 14.dp)
                    ) {
                        Text("Rp", color = NeonCyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = inputAmount.ifEmpty { "0" },
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Account Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (txType == "TRANSFER") "DEBIT DARI" else "AKUN", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CardGrey)
                                .border(1.dp, AccentGrey, RoundedCornerShape(8.dp)),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            listOf("Tunai", "Saldo").forEach { acc ->
                                val selected = sourceAccount == acc
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selected) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
                                        .clickable {
                                            sourceAccount = acc
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(acc, color = if (selected) NeonCyan else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (txType == "TRANSFER") {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("KREDIT KE", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CardGrey)
                                    .border(1.dp, AccentGrey, RoundedCornerShape(8.dp)),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                listOf("Tunai", "Saldo").forEach { acc ->
                                    val selected = destinationAccount == acc
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (selected) NeonViolet.copy(alpha = 0.2f) else Color.Transparent)
                                            .clickable {
                                                destinationAccount = acc
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(acc, color = if (selected) NeonViolet else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Category dropdown Selection
                if (txType != "TRANSFER") {
                    Column {
                        Text("KATEGORI", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        var expanded by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CardGrey)
                                .border(1.dp, AccentGrey, RoundedCornerShape(8.dp))
                                .clickable { expanded = true }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(selectedCategory, color = TextPrimary, fontSize = 13.sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = NeonCyan)
                            }
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(DarkGrey)
                        ) {
                            dropCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat, color = TextPrimary) },
                                    onClick = {
                                        selectedCategory = cat
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Notes Field
                Column {
                    Text("CATATAN / MEMO", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = inputNote,
                        onValueChange = { inputNote = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Uraikan catatan transaksi...", fontSize = 12.sp, color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = CardGrey,
                            unfocusedContainerColor = CardGrey,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = AccentGrey
                        ),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                }

                // Backdating Native Date Picker Trigger
                Column {
                    Text("TANGGAL STAMP", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardGrey)
                            .border(1.dp, AccentGrey, RoundedCornerShape(8.dp))
                            .clickable {
                                val cal = Calendar.getInstance()
                                cal.timeInMillis = selectedTimestamp
                                android.app.DatePickerDialog(
                                    context,
                                    android.R.style.Theme_DeviceDefault_Dialog_Alert,
                                    { _, y, m, d ->
                                        val selectedCal = Calendar.getInstance()
                                        selectedCal.set(y, m, d)
                                        selectedTimestamp = selectedCal.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = dateFormater.format(Date(selectedTimestamp)), color = TextPrimary, fontSize = 13.sp)
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Tactile internal Calculator style Keyboard pad
                Text("KEYPAD AMAN TAKTIL", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                CalculatorKeyboard(
                    onDigitPress = { digit ->
                        if (digit == ".") {
                            if (!inputAmount.contains(".")) {
                                inputAmount += if (inputAmount.isEmpty()) "0." else "."
                            }
                        } else {
                            // limit to 8digits
                            if (inputAmount.replace(".", "").length < 8) {
                                inputAmount += digit
                            }
                        }
                    },
                    onBackspace = {
                        if (inputAmount.isNotEmpty()) {
                            inputAmount = inputAmount.dropLast(1)
                        }
                    },
                    onClear = {
                        inputAmount = ""
                    },
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                )

                // Dialog Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("BATAL", color = TextSecondary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amountVal = inputAmount.toDoubleOrNull() ?: 0.0
                            if (amountVal <= 0.0) {
                                Toast.makeText(context, "Silakan masukkan jumlah nominal > 0", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (txType == "TRANSFER" && sourceAccount == destinationAccount) {
                                Toast.makeText(context, "Akun debit dan kredit harus berbeda", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if (isEditMode) {
                                viewModel.updateTransaction(
                                    id = transaction!!.id,
                                    type = txType,
                                    sourceAccount = sourceAccount,
                                    destinationAccount = if (txType == "TRANSFER") destinationAccount else null,
                                    category = if (txType == "TRANSFER") "Transfer" else selectedCategory,
                                    amount = amountVal,
                                    note = inputNote,
                                    timestamp = selectedTimestamp
                                )
                                Toast.makeText(context, "Transaksi berhasil diperbarui", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.insertTransaction(
                                    type = txType,
                                    sourceAccount = sourceAccount,
                                    destinationAccount = if (txType == "TRANSFER") destinationAccount else null,
                                    category = if (txType == "TRANSFER") "Transfer" else selectedCategory,
                                    amount = amountVal,
                                    note = inputNote,
                                    timestamp = selectedTimestamp
                                )
                                Toast.makeText(context, "Transaksi baru berhasil dienkripsi dan disimpan", Toast.LENGTH_SHORT).show()
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = SolidBlack),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isEditMode) "DEKRIPSI" else "ENKRIPSI ENTRI",
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorKeyboard(
    onDigitPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = listOf(
        listOf("7", "8", "9"),
        listOf("4", "5", "6"),
        listOf("1", "2", "3"),
        listOf("C", "0", "⌫")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkGrey)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when (key) {
                                    "⌫" -> NeonRed.copy(alpha = 0.12f)
                                    "C" -> AccentGrey
                                    else -> CardGrey
                                }
                            )
                            .clickable {
                                when (key) {
                                    "⌫" -> onBackspace()
                                    "C" -> onClear()
                                    else -> onDigitPress(key)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = key,
                            color = when (key) {
                                "⌫" -> NeonRed
                                "C" -> TextSecondary
                                else -> NeonCyan
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

private fun copyUriToLocal(context: Context, uri: Uri): java.io.File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val outputFile = java.io.File(context.filesDir, "custom_app_icon.png")
        if (outputFile.exists()) {
            outputFile.delete()
        }
        outputFile.outputStream().use { outputStream ->
            inputStream.use { it.copyTo(outputStream) }
        }
        outputFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
