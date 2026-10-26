package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ExpenseEntry
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.RoseError
import com.example.util.PersianDateAndUnitUtils

data class DailyTrendPoint(
    val epochDay: Long,
    val dayLabelFa: String,
    val totalExpense: Double,
    val totalIncome: Double
)

/**
 * Monthly Spending & Income Trends Native Line Chart Component.
 * Visualizes daily expenses versus income using dual glowing Bezier curve lines
 * with gradient fills, grid lines, and interactive touch inspection tooltips.
 */
@Composable
fun MonthlySpendingTrendsLineChart(
    expenses: List<ExpenseEntry>,
    defaultCurrency: String,
    modifier: Modifier = Modifier,
    daysCount: Int = 15
) {
    val todayEpochDay = PersianDateAndUnitUtils.currentLocalEpochDay()

    // Aggregate expenses and incomes per epochDay for the past `daysCount` days
    val trendPoints = remember(expenses, todayEpochDay, daysCount) {
        val startDay = todayEpochDay - (daysCount - 1)
        (startDay..todayEpochDay).map { epochDay ->
            val dayEntries = expenses.filter { it.localEpochDay == epochDay }
            val expSum = dayEntries.filter { it.isExpense }.sumOf { it.amount }
            val incSum = dayEntries.filter { it.isIncome }.sumOf { it.amount }
            val jalali = PersianDateAndUnitUtils.epochDayToJalali(epochDay)
            val label = "${PersianDateAndUnitUtils.toPersianDigits(jalali.day.toString())} ${PersianDateAndUnitUtils.jalaliMonthNamesFa.getOrElse(jalali.month - 1) { "" }}"

            DailyTrendPoint(
                epochDay = epochDay,
                dayLabelFa = label,
                totalExpense = expSum,
                totalIncome = incSum
            )
        }
    }

    val maxAmount = remember(trendPoints) {
        val highest = trendPoints.maxOfOrNull { maxOf(it.totalExpense, it.totalIncome) } ?: 0.0
        if (highest <= 0.0) 100_000.0 else highest * 1.15
    }

    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    var animationTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animationTriggered = true
    }

    val chartProgress by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "spending_trend_line_draw"
    )

    val totalMonthExpense = remember(trendPoints) { trendPoints.sumOf { it.totalExpense } }
    val totalMonthIncome = remember(trendPoints) { trendPoints.sumOf { it.totalIncome } }

    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_spending_trends_line_chart_card"),
        shape = RoundedCornerShape(24.dp),
        accentBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row + Legends
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = EmeraldSuccess.copy(alpha = 0.16f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "روند روزانه خرج و درآمد (ماه جاری)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "مقایسه خطی مخارج در برابر درآمدها",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Legend Pills
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EmeraldSuccess)
                        )
                        Text("درآمد", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(RoseError)
                        )
                        Text("خرج", style = MaterialTheme.typography.labelSmall, color = RoseError, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // High-Level Period Aggregates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("مجموع خرج این دوره:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = PersianDateAndUnitUtils.formatCurrencyAmount(totalMonthExpense, defaultCurrency),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = RoseError
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("مجموع درآمد این دوره:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = PersianDateAndUnitUtils.formatCurrencyAmount(totalMonthIncome, defaultCurrency),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess
                    )
                }
            }

            // Interactive Line Chart Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .pointerInput(trendPoints) {
                        detectTapGestures { offset ->
                            val width = size.width
                            if (width > 0 && trendPoints.isNotEmpty()) {
                                val stepX = width / (trendPoints.size - 1).coerceAtLeast(1)
                                val index = (offset.x / stepX)
                                    .toInt()
                                    .coerceIn(0, trendPoints.size - 1)
                                selectedPointIndex = index
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                    val w = size.width
                    val h = size.height
                    if (w <= 0 || h <= 0 || trendPoints.isEmpty()) return@Canvas

                    val stepX = w / (trendPoints.size - 1).coerceAtLeast(1)

                    // Draw 3 horizontal dashed scale grid lines
                    val gridLines = 3
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    for (i in 0..gridLines) {
                        val y = h * (i.toFloat() / gridLines.toFloat())
                        drawLine(
                            color = Color.White.copy(alpha = 0.08f),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashEffect
                        )
                    }

                    // Compute points coordinates
                    fun getY(amount: Double): Float {
                        val ratio = (amount / maxAmount).coerceIn(0.0, 1.0).toFloat()
                        return h - (ratio * h * 0.85f * chartProgress) - 10f
                    }

                    val incomePoints = trendPoints.mapIndexed { idx, pt ->
                        Offset(idx * stepX, getY(pt.totalIncome))
                    }

                    val expensePoints = trendPoints.mapIndexed { idx, pt ->
                        Offset(idx * stepX, getY(pt.totalExpense))
                    }

                    // Function to build smooth Bezier Path
                    fun buildBezierPath(points: List<Offset>): Path {
                        val path = Path()
                        if (points.isEmpty()) return path
                        path.moveTo(points.first().x, points.first().y)

                        for (i in 0 until points.size - 1) {
                            val p1 = points[i]
                            val p2 = points[i + 1]
                            val controlX1 = p1.x + (p2.x - p1.x) / 2f
                            val controlY1 = p1.y
                            val controlX2 = p1.x + (p2.x - p1.x) / 2f
                            val controlY2 = p2.y
                            path.cubicTo(controlX1, controlY1, controlX2, controlY2, p2.x, p2.y)
                        }
                        return path
                    }

                    // 1. Render Income Line & Fill (Emerald)
                    val incomeLinePath = buildBezierPath(incomePoints)
                    val incomeFillPath = Path().apply {
                        addPath(incomeLinePath)
                        lineTo(incomePoints.last().x, h)
                        lineTo(incomePoints.first().x, h)
                        close()
                    }

                    drawPath(
                        path = incomeFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                EmeraldSuccess.copy(alpha = 0.28f),
                                Color.Transparent
                            )
                        )
                    )

                    drawPath(
                        path = incomeLinePath,
                        color = EmeraldSuccess,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 2. Render Expense Line & Fill (Rose/Coral)
                    val expenseLinePath = buildBezierPath(expensePoints)
                    val expenseFillPath = Path().apply {
                        addPath(expenseLinePath)
                        lineTo(expensePoints.last().x, h)
                        lineTo(expensePoints.first().x, h)
                        close()
                    }

                    drawPath(
                        path = expenseFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                RoseError.copy(alpha = 0.22f),
                                Color.Transparent
                            )
                        )
                    )

                    drawPath(
                        path = expenseLinePath,
                        color = RoseError,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw Selected Point Highlight Line and Circles
                    selectedPointIndex?.let { index ->
                        val selExpOffset = expensePoints.getOrNull(index)
                        val selIncOffset = incomePoints.getOrNull(index)

                        if (selExpOffset != null) {
                            drawLine(
                                color = Color.White.copy(alpha = 0.4f),
                                start = Offset(selExpOffset.x, 0f),
                                end = Offset(selExpOffset.x, h),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = dashEffect
                            )

                            drawCircle(color = RoseError, radius = 6.dp.toPx(), center = selExpOffset)
                            drawCircle(color = Color.White, radius = 3.dp.toPx(), center = selExpOffset)
                        }

                        if (selIncOffset != null) {
                            drawCircle(color = EmeraldSuccess, radius = 6.dp.toPx(), center = selIncOffset)
                            drawCircle(color = Color.White, radius = 3.dp.toPx(), center = selIncOffset)
                        }
                    }
                }
            }

            // Interactive Tooltip Card on Selection
            selectedPointIndex?.let { index ->
                val point = trendPoints.getOrNull(index)
                if (point != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0F1612).copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .testTag("spending_trend_tooltip")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = point.dayLabelFa,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "تراز روز: " + PersianDateAndUnitUtils.formatCurrencyAmount(
                                        point.totalIncome - point.totalExpense,
                                        defaultCurrency
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (point.totalIncome >= point.totalExpense) EmeraldSuccess else RoseError
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "درآمد: ${PersianDateAndUnitUtils.formatCurrencyAmount(point.totalIncome, defaultCurrency)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSuccess
                                )
                                Text(
                                    text = "خرج: ${PersianDateAndUnitUtils.formatCurrencyAmount(point.totalExpense, defaultCurrency)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = RoseError
                                )
                            }
                        }
                    }
                }
            } ?: run {
                Text(
                    text = "💡 برای رزولوشن دقیق تاریخ‌ها، روی هر نقطه از نمودار خطی بالا انگشت بزنید.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
