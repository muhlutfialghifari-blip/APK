package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransactionEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// 1. Line Chart representing Cash Flow for the last 7 days
@Composable
fun FinanceLineChart(
    transactions: List<TransactionEntity>,
    modifier: Modifier = Modifier
) {
    val daySdf = SimpleDateFormat("dd MMM", Locale.getDefault())
    
    // Compute last 7 days
    val cal = Calendar.getInstance()
    val dailyBalances = remember(transactions) {
        val today = Calendar.getInstance()
        val days = (0..6).map { offset ->
            val d = today.clone() as Calendar
            d.add(Calendar.DAY_OF_YEAR, -offset)
            d
        }.reversed()

        // Calculate cumulative net cash flow at each day
        var runningBalance = 0.0
        // Sort transactions ascending to compute running balance
        val sortedTx = transactions.sortedBy { it.timestamp }
        
        days.map { day ->
            // End of this day timestamp
            val endOfDay = day.clone() as Calendar
            endOfDay.set(Calendar.HOUR_OF_DAY, 23)
            endOfDay.set(Calendar.MINUTE, 59)
            endOfDay.set(Calendar.SECOND, 59)
            endOfDay.set(Calendar.MILLISECOND, 999)
            val timeLimit = endOfDay.timeInMillis

            // Sum transactions up to this day
            val txUpToDay = sortedTx.filter { it.timestamp <= timeLimit }
            var balance = 0.0
            txUpToDay.forEach { tx ->
                when (tx.type) {
                    "INCOME" -> balance += tx.amount
                    "EXPENSE" -> balance -= tx.amount
                    // TRANSFER is balance neutral on the aggregate
                }
            }
            
            val label = daySdf.format(day.time)
            Pair(label, balance)
        }
    }

    val maxVal = dailyBalances.maxOfOrNull { Math.abs(it.second) }?.toFloat()?.coerceAtLeast(100f) ?: 100f
    val minVal = dailyBalances.minOfOrNull { it.second }?.toFloat() ?: 0f

    val yMax = maxVal * 1.25f
    val yMin = if (minVal < 0) minVal * 1.25f else 0f

    var animateTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(transactions) {
        animateTrigger = true
    }
    val phase by animateFloatAsState(
        targetValue = if (animateTrigger) 1f else 0f,
        animationSpec = tween(durationMillis = 1000)
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = CardGrey),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, AccentGrey, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
             Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ARUS KAS 7 HARI",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(NeonCyan))
                    Text("MASUK", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(NeonViolet))
                    Text("KELUAR", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val spacing = width / (dailyBalances.size - 1)

                    // Draw reference grid lines
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val y = (height / gridLines) * i
                        drawLine(
                            color = AccentGrey.copy(alpha = 0.3f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Map points to screen coordinates
                    val points = dailyBalances.mapIndexed { index, pair ->
                        val x = index * spacing
                        val ratio = (pair.second.toFloat() - yMin) / (yMax - yMin)
                        val y = height - (ratio * height * phase)
                        Offset(x, y)
                    }

                    // Draw area gradient under the line
                    if (points.isNotEmpty() && phase > 0.05f) {
                        val areaPath = Path().apply {
                            moveTo(points.first().x, height)
                            points.forEach { lineTo(it.x, it.y) }
                            lineTo(points.last().x, height)
                            close()
                        }
                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(NeonCyan.copy(alpha = 0.25f), Color.Transparent),
                                startY = 0f,
                                endY = height
                            )
                        )
                    }

                    // Draw line paths
                    if (points.size > 1) {
                        val linePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }
                        drawPath(
                            path = linePath,
                            color = NeonCyan,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Draw data dots
                    points.forEachIndexed { i, offset ->
                        // outer glow
                        drawCircle(
                            color = NeonCyan.copy(alpha = 0.3f),
                            radius = 7.dp.toPx(),
                            center = offset
                        )
                        // inner core
                        drawCircle(
                            color = SolidBlack,
                            radius = 4.dp.toPx(),
                            center = offset
                        )
                        drawCircle(
                            color = NeonCyan,
                            radius = 2.dp.toPx(),
                            center = offset
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Map Date labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dailyBalances.forEach { pair ->
                    Text(
                        text = pair.first,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// 2. Neon Pie Chart for Category break downs
@Composable
fun CategoryPieChart(
    categorySpending: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    val totalExpense = categorySpending.values.sum()
    val sliceColors = listOf(NeonRed, NeonViolet, NeonCyan, NeonGreen, Color(0xFFFF9F1C), Color(0xFFFF4D6D), Color(0xFF4EA8DE), Color(0xFFD81159))

    Card(
        colors = CardDefaults.cardColors(containerColor = CardGrey),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, AccentGrey, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "PENGELUARAN PER KATEGORI",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (totalExpense == 0.0) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    Text(
                        text = "Belum ada transaksi pengeluaran kategori.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Draw Pie
                    var animationTrigger by remember { mutableStateOf(false) }
                    LaunchedEffect(categorySpending) {
                        animationTrigger = true
                    }
                    val animatedAmount by animateFloatAsState(
                        targetValue = if (animationTrigger) 360f else 0f,
                        animationSpec = tween(1000)
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(130.dp)
                            .padding(8.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            var startAngle = 0f
                            categorySpending.entries.forEachIndexed { i, entry ->
                                val sweep = ((entry.value / totalExpense) * animatedAmount).toFloat()
                                val color = sliceColors[i % sliceColors.size]
                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                                )
                                startAngle += sweep
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Total", color = TextSecondary, fontSize = 10.sp)
                            Text(
                                text = "Rp " + java.text.NumberFormat.getNumberInstance(java.util.Locale("id", "ID")).format(totalExpense),
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Draw Legend
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categorySpending.entries.take(5).forEachIndexed { i, entry ->
                            val color = sliceColors[i % sliceColors.size]
                            val percent = (entry.value / totalExpense) * 100
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = entry.key,
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f%%", percent),
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 3. Double comparing Bar Chart: Income vs Expense comparison
@Composable
fun ComparisonBarChart(
    transactions: List<TransactionEntity>,
    modifier: Modifier = Modifier
) {
    val comparison = remember(transactions) {
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        var incomeSum = 0.0
        var expenseSum = 0.0

        transactions.forEach { tx ->
            val txCal = Calendar.getInstance()
            txCal.timeInMillis = tx.timestamp
            if (txCal.get(Calendar.MONTH) == currentMonth && txCal.get(Calendar.YEAR) == currentYear) {
                if (tx.type == "INCOME") {
                    incomeSum += tx.amount
                } else if (tx.type == "EXPENSE") {
                    expenseSum += tx.amount
                }
            }
        }
        Pair(incomeSum, expenseSum)
    }

    val (income, expense) = comparison
    val maxVal = Math.max(income, expense).toFloat().coerceAtLeast(100f)

    Card(
        colors = CardDefaults.cardColors(containerColor = CardGrey),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, AccentGrey, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "PENDAPATAN VS PENGELUARAN BULANAN",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(modifier = Modifier.height(20.dp))

            var animateTrigger by remember { mutableStateOf(false) }
            LaunchedEffect(transactions) {
                animateTrigger = true
            }
            val animFactor by animateFloatAsState(
                targetValue = if (animateTrigger) 1f else 0f,
                animationSpec = tween(1000)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.Bottom
            ) {
                // Income Bar
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    val incomeRatio = (income.toFloat() / maxVal) * animFactor
                    
                    Text(
                        text = "Rp " + java.text.NumberFormat.getNumberInstance(java.util.Locale("id", "ID")).format(income.toInt()),
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .fillMaxHeight(incomeRatio.coerceIn(0.01f, 1f))
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(NeonCyan, NeonCyan.copy(alpha = 0.4f))
                                )
                            )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "PENDAPATAN",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Expense Bar
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    val expenseRatio = (expense.toFloat() / maxVal) * animFactor
                    Text(
                        text = "Rp " + java.text.NumberFormat.getNumberInstance(java.util.Locale("id", "ID")).format(expense.toInt()),
                        color = NeonRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .fillMaxHeight(expenseRatio.coerceIn(0.01f, 1f))
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(NeonRed, NeonRed.copy(alpha = 0.4f))
                                )
                            )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "PENGELUARAN",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
