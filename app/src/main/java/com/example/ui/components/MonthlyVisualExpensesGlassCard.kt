package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.PieChart
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ExpenseEntry
import com.example.ui.theme.GlassmorphismDefaults
import com.example.ui.theme.LiquidGlassCard
import com.example.util.PersianDateAndUnitUtils

/**
 * Visual Expenses Summary Component.
 * Displays monthly spending distribution by category using native animated Compose Canvas charts
 * within a dedicated GlassCard for the Personal Life OS dashboard.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonthlyVisualExpensesGlassCard(
    expenses: List<ExpenseEntry>,
    defaultCurrency: String,
    onNavigateToExpenses: () -> Unit,
    modifier: Modifier = Modifier
) {
    val todayEpochDay = PersianDateAndUnitUtils.currentLocalEpochDay()
    val jalaliToday = PersianDateAndUnitUtils.epochDayToJalali(todayEpochDay)

    // Filter expenses belonging to the current Jalali month and year
    val currentMonthExpenses = expenses.filter { exp ->
        if (exp.isIncome) return@filter false
        val expJalali = PersianDateAndUnitUtils.epochDayToJalali(exp.localEpochDay)
        expJalali.year == jalaliToday.year && expJalali.month == jalaliToday.month
    }

    val totalSpent = currentMonthExpenses.sumOf { it.amount }

    // Category mapping with vibrant neon color indicators
    data class CategorySlice(
        val categoryCode: String,
        val titleFa: String,
        val totalAmount: Double,
        val percentage: Float,
        val color: Color
    )

    val categoryColors = mapOf(
        "FOOD" to Color(0xFFFFB300),      // Amber
        "COFFEE" to Color(0xFFFF8A65),    // Peach / Coral
        "SHOPPING" to Color(0xFF10B981),  // Emerald
        "TRANSPORT" to Color(0xFF00E5FF), // Cyan
        "BILLS" to Color(0xFFA78BFA),     // Violet
        "HEALTH" to Color(0xFFFF5252),    // Rose
        "OTHER" to Color(0xFF38BDF8)      // Sky
    )

    val categoryTitles = mapOf(
        "FOOD" to "خوراک و رستوران",
        "COFFEE" to "کافه و نوشیدنی",
        "SHOPPING" to "خرید و پوشاک",
        "TRANSPORT" to "حمل‌ونقل و سوخت",
        "BILLS" to "قبوض و شارژ",
        "HEALTH" to "سلامت و دارو",
        "OTHER" to "سایر هزینه‌ها"
    )

    val groupedCategories = currentMonthExpenses
        .groupBy { it.category }
        .map { (cat, list) ->
            val sum = list.sumOf { it.amount }
            val ratio = if (totalSpent > 0.0) (sum / totalSpent).toFloat() else 0f
            CategorySlice(
                categoryCode = cat,
                titleFa = categoryTitles[cat] ?: cat,
                totalAmount = sum,
                percentage = ratio,
                color = categoryColors[cat] ?: Color(0xFF818CF8)
            )
        }
        .sortedByDescending { it.totalAmount }

    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animationTriggered = true
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 900),
        label = "expense_chart_sweep"
    )

    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("visual_expenses_summary_card"),
        shape = RoundedCornerShape(24.dp),
        accentBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
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
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "تحلیل بصری هزینه‌های این ماه",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val monthNameFa = PersianDateAndUnitUtils.jalaliMonthNamesFa.getOrElse(jalaliToday.month - 1) { "ماه جاری" }
                        Text(
                            text = "$monthNameFa ${PersianDateAndUnitUtils.toPersianDigits(jalaliToday.year.toString())}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.clickable { onNavigateToExpenses() }
                ) {
                    Text(
                        text = "لاگ مخارج ←",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Radial Donut Chart & Monthly Total
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Interactive Donut Chart
                Box(
                    modifier = Modifier.size(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(130.dp)) {
                        val strokeWidth = 14.dp.toPx()
                        val arcSize = size.minDimension - strokeWidth
                        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                        // Base track ring
                        drawArc(
                            color = Color(0x22FFFFFF),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(arcSize, arcSize),
                            style = Stroke(width = strokeWidth)
                        )

                        if (totalSpent > 0.0 && groupedCategories.isNotEmpty()) {
                            var currentStartAngle = -90f
                            groupedCategories.forEach { slice ->
                                val sweep = (slice.percentage * 360f) * animatedProgress
                                if (sweep > 0.5f) {
                                    drawArc(
                                        color = slice.color,
                                        startAngle = currentStartAngle,
                                        sweepAngle = (sweep - 2f).coerceAtLeast(0.5f),
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = Size(arcSize, arcSize),
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                }
                                currentStartAngle += slice.percentage * 360f
                            }
                        } else {
                            // Subtle empty glow arc
                            drawArc(
                                color = Color(0xFF10B981).copy(alpha = 0.3f),
                                startAngle = -90f,
                                sweepAngle = 360f * animatedProgress,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(arcSize, arcSize),
                                style = Stroke(width = strokeWidth)
                            )
                        }
                    }

                    // Center Content
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "کل مخارج",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = PersianDateAndUnitUtils.formatCurrencyAmount(totalSpent, defaultCurrency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Top 3 Category Summary Pill List
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    if (groupedCategories.isEmpty()) {
                        Text(
                            text = "هنوز هزینه‌ای در این ماه ثبت نشده است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        groupedCategories.take(3).forEach { slice ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(slice.color)
                                )
                                Column {
                                    Text(
                                        text = slice.titleFa,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${PersianDateAndUnitUtils.toPersianDigits((slice.percentage * 100).toInt().toString())}٪ • ${PersianDateAndUnitUtils.formatCurrencyAmount(slice.totalAmount, defaultCurrency)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Category Distribution Pills Grid
            if (groupedCategories.size > 3) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    groupedCategories.drop(3).forEach { slice ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, slice.color.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(slice.color)
                                )
                                Text(
                                    text = "${slice.titleFa}: ${PersianDateAndUnitUtils.toPersianDigits((slice.percentage * 100).toInt().toString())}٪",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
