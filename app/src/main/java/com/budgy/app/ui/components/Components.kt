@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.budgy.app.ui.components

import com.budgy.app.i18n.tr
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Tune
import com.budgy.app.util.symbol
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgy.app.R
import com.budgy.app.data.Account
import com.budgy.app.data.Category
import com.budgy.app.data.TxType
import com.budgy.app.data.Txn
import com.budgy.app.ui.theme.Budgy
import com.budgy.app.ui.theme.Numeric
import com.budgy.app.util.Dates
import com.budgy.app.util.Money

// ---------- Layout building blocks ----------

@Composable
fun BudgyCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Budgy.colors.cardBorder),
    ) {
        Column(Modifier.padding(padding), content = content)
    }
}

@Composable
fun ScreenTitle(
    title: String,
    subtitle: String? = null,
    horizontalPadding: Dp = 20.dp,
    actions: @Composable () -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = horizontalPadding, end = (horizontalPadding - 8.dp).coerceAtLeast(0.dp), top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        actions()
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(
                action,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
fun IconBadge(icon: ImageVector, color: Color, size: Dp = 42.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(size * 0.52f))
    }
}

@Composable
fun LetterAvatar(name: String, color: Color, size: Dp = 42.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().take(1).uppercase().ifEmpty { "?" },
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp,
        )
    }
}

@Composable
fun BudgyLogo(size: Dp = 36.dp) {
    Image(
        painter = painterResource(R.drawable.budgy_logo),
        contentDescription = tr("Budgy"),
        modifier = Modifier.size(size),
    )
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BudgyLogo(64.dp)
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            androidx.compose.material3.FilledTonalButton(onClick = onAction) { Text(action) }
        }
    }
}

// ---------- Money ----------

@Composable
fun MoneyText(
    cents: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = Color.Unspecified,
    showPlus: Boolean = false,
    fontWeight: FontWeight? = FontWeight.SemiBold,
    symbol: String? = null,
) {
    Text(
        Money.format(cents, symbol ?: Budgy.currency, showPlus),
        modifier = modifier,
        style = style.merge(Numeric),
        color = color,
        fontWeight = fontWeight,
        maxLines = 1,
    )
}

/** Thin rounded progress bar. Fraction can exceed 1 (over budget) — it's clamped for drawing. */
@Composable
fun BudgetBar(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 8.dp) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(600), label = "bar")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
fun budgetColor(fraction: Float): Color = when {
    fraction >= 1f -> Budgy.colors.expense
    fraction >= 0.8f -> Budgy.colors.warning
    else -> Budgy.colors.income
}

@Composable
fun DonutChart(
    slices: List<Pair<Color, Long>>,
    modifier: Modifier = Modifier,
    thickness: Dp = 18.dp,
    center: @Composable () -> Unit = {},
) {
    val total = slices.sumOf { it.second }.coerceAtLeast(1)
    val track = MaterialTheme.colorScheme.surfaceVariant
    val progress by animateFloatAsState(1f, tween(700), label = "donut")
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = thickness.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            val gap = if (slices.size > 1) 2.5f else 0f
            var start = -90f
            for ((color, value) in slices) {
                val sweep = 360f * value / total * progress
                if (sweep > gap) {
                    drawArc(
                        color, start + gap / 2, sweep - gap, false,
                        Offset(inset, inset), arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Butt),
                    )
                }
                start += sweep
            }
        }
        center()
    }
}

// ---------- Transactions ----------

@Composable
fun TxnRow(
    txn: Txn,
    category: Category?,
    accounts: Map<Long, Account>,
    onClick: () -> Unit,
    showDate: Boolean = false,
) {
    val colors = Budgy.colors
    val (icon, tint, title) = when (txn.type) {
        TxType.TRANSFER -> Triple(Icons.Rounded.SwapHoriz, MaterialTheme.colorScheme.tertiary, tr("Transfer"))
        TxType.LEND -> Triple(Icons.Rounded.Group, MaterialTheme.colorScheme.tertiary, tr("Money out · IOU"))
        TxType.COLLECT -> Triple(Icons.Rounded.Group, MaterialTheme.colorScheme.tertiary, tr("Money in · IOU"))
        TxType.ADJUST -> Triple(Icons.Rounded.Tune, MaterialTheme.colorScheme.tertiary, tr("Balance adjustment"))
        TxType.INVEST -> Triple(Icons.AutoMirrored.Rounded.ShowChart, MaterialTheme.colorScheme.tertiary, tr("Investment buy"))
        TxType.DIVEST -> Triple(Icons.AutoMirrored.Rounded.ShowChart, MaterialTheme.colorScheme.tertiary, tr("Investment sell"))
        else -> Triple(
            CategoryIcons.get(category?.icon),
            category?.let { Color(it.color) } ?: MaterialTheme.colorScheme.onSurfaceVariant,
            category?.name ?: tr("Uncategorized"),
        )
    }
    val fromName = accounts[txn.accountId]?.name ?: tr("Deleted account")
    val subtitle = buildString {
        if (txn.type == TxType.TRANSFER) {
            append(fromName).append(" → ").append(accounts[txn.toAccountId]?.name ?: tr("Deleted account"))
        } else {
            if (txn.note.isNotBlank()) append(txn.note).append(" · ")
            append(fromName)
        }
        if (showDate) append(" · ").append(Dates.short(txn.date))
        tagList(txn.tags).forEach { append(" · #").append(it) }
    }
    val sym = accounts[txn.accountId]?.symbol(Budgy.currency)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, tint)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        when (txn.type) {
            TxType.EXPENSE -> MoneyText(-txn.amount, color = colors.expense, symbol = sym)
            TxType.INCOME -> MoneyText(txn.amount, color = colors.income, showPlus = true, symbol = sym)
            TxType.TRANSFER -> MoneyText(txn.amount, color = MaterialTheme.colorScheme.onSurfaceVariant, symbol = sym)
            TxType.LEND -> MoneyText(-txn.amount, color = MaterialTheme.colorScheme.onSurfaceVariant, symbol = sym)
            TxType.COLLECT -> MoneyText(txn.amount, color = MaterialTheme.colorScheme.onSurfaceVariant, showPlus = true, symbol = sym)
            TxType.ADJUST -> MoneyText(txn.amount, color = MaterialTheme.colorScheme.onSurfaceVariant, showPlus = true, symbol = sym)
            TxType.INVEST -> MoneyText(-txn.amount, color = MaterialTheme.colorScheme.onSurfaceVariant, symbol = sym)
            TxType.DIVEST -> MoneyText(txn.amount, color = MaterialTheme.colorScheme.onSurfaceVariant, showPlus = true, symbol = sym)
        }
    }
}

// ---------- Inputs ----------

/** A tappable field that opens a dropdown. */
@Composable
fun <T> SelectField(
    label: String,
    value: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        FieldShell(label = label, value = value, leading = leading, onClick = { open = true }, trailing = Icons.Rounded.KeyboardArrowDown)
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { o ->
                DropdownMenuItem(
                    text = { Text(optionLabel(o)) },
                    onClick = { open = false; onSelect(o) },
                )
            }
        }
    }
}

@Composable
fun FieldShell(
    label: String,
    value: String,
    onClick: () -> Unit,
    leading: ImageVector? = null,
    trailing: ImageVector? = null,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Icon(leading, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (trailing != null) {
            Icon(trailing, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DateField(label: String, epochDay: Long, onChange: (Long) -> Unit) {
    var open by remember { mutableStateOf(false) }
    FieldShell(label = label, value = Dates.dayLabel(epochDay), leading = Icons.Rounded.CalendarMonth, onClick = { open = true })
    if (open) {
        DatePickerDialogBudgy(epochDay, onDismiss = { open = false }) { onChange(it); open = false }
    }
}

@Composable
fun DatePickerDialogBudgy(initial: Long, onDismiss: () -> Unit, onPick: (Long) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = Dates.toPickerMillis(initial))
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Dates.fromPickerMillis(it)) } ?: onDismiss()
            }) { Text(tr("OK")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel")) } },
    ) {
        DatePicker(state = state)
    }
}

@Composable
fun ColorPicker(colors: List<Long>, selected: Long, onSelect: (Long) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        colors.forEach { c ->
            val isSel = c == selected
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(c))
                    .then(
                        if (isSel) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), CircleShape)
                        else Modifier
                    )
                    .clickable { onSelect(c) },
                contentAlignment = Alignment.Center,
            ) {
                if (isSel) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String = tr("Delete"),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirm, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel")) } },
    )
}

/** Big number pad for fast amount entry. */
@Composable
fun AmountKeypad(onKey: (String) -> Unit, modifier: Modifier = Modifier) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "⌫"),
    )
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { onKey(key) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (key == "⌫") {
                            Icon(Icons.AutoMirrored.Rounded.Backspace, tr("Delete"))
                        } else {
                            Text(key, style = MaterialTheme.typography.titleLarge.merge(Numeric))
                        }
                    }
                }
            }
        }
    }
}

/** Applies a keypad press to the amount text: max 2 decimals, max 10 whole digits. */
fun applyKey(current: String, key: String): String = when (key) {
    "⌫" -> current.dropLast(1)
    "." -> if (current.contains(".")) current else (current.ifEmpty { "0" } + ".")
    else -> {
        val parts = current.split(".")
        when {
            parts.size > 1 && parts[1].length >= 2 -> current
            parts.size == 1 && parts[0].length >= 10 -> current
            current == "0" -> key
            else -> current + key
        }
    }
}

/** "Baguio trip, Food trip" → ["Baguio trip", "Food trip"] */
fun tagList(tags: String): List<String> = tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }

fun joinTags(list: List<String>): String = list.map { it.trim().replace(",", " ") }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }.joinToString(",")
