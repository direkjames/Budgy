@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.budgy.app.ui.screens

import com.budgy.app.i18n.tr
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgy.app.data.Category
import com.budgy.app.data.CategoryKind
import com.budgy.app.data.DefaultData
import com.budgy.app.ui.BudgyNav
import com.budgy.app.ui.BudgyViewModel
import com.budgy.app.ui.components.BudgyCard
import com.budgy.app.ui.components.CategoryIcons
import com.budgy.app.ui.components.ColorPicker
import com.budgy.app.ui.components.ConfirmDialog
import com.budgy.app.ui.components.IconBadge

@Composable
fun CategoriesScreen(vm: BudgyViewModel, go: BudgyNav) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(0) }
    val kind = if (tab == 0) CategoryKind.EXPENSE else CategoryKind.INCOME
    var editing by remember { mutableStateOf<Category?>(null) }
    var creating by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(tr("Categories")) },
                navigationIcon = { IconButton(onClick = go::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Back")) } },
                actions = { IconButton(onClick = { creating = true }) { Icon(Icons.Rounded.Add, tr("Add category")) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            SecondaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(tr("Expenses")) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(tr("Income")) })
            }
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(categories.filter { it.kind == kind }, key = { it.id }) { c ->
                    BudgyCard(onClick = { editing = c }, padding = 12.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(CategoryIcons.get(c.icon), Color(c.color), 38.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(c.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    if (creating || editing != null) {
        CategoryEditor(
            initial = editing,
            kind = editing?.kind ?: kind,
            nextOrder = categories.count { it.kind == kind },
            onSave = { vm.saveCategory(it) },
            onDelete = { vm.deleteCategory(it) },
            onDismiss = { creating = false; editing = null },
        )
    }
}

@Composable
private fun CategoryEditor(
    initial: Category?,
    kind: CategoryKind,
    nextOrder: Int,
    onSave: (Category) -> Unit,
    onDelete: (Category) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var icon by remember { mutableStateOf(initial?.icon ?: "other") }
    var color by remember { mutableStateOf(initial?.color ?: DefaultData.swatches[9]) }
    var error by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) tr("New {0} category", kind.label.lowercase()) else tr("Edit category")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(CategoryIcons.get(icon), Color(color), 48.dp)
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(24); error = false },
                        label = { Text(tr("Name")) },
                        isError = error,
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(tr("Icon"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CategoryIcons.all.forEach { (key, vec) ->
                        val sel = key == icon
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sel) Color(color).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { icon = key },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                vec, null,
                                tint = if (sel) Color(color) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
                Text(tr("Color"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ColorPicker(DefaultData.swatches, color) { color = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) {
                    error = true
                } else {
                    val base = initial ?: Category(name = "", kind = kind, icon = icon, color = color, sortOrder = nextOrder)
                    onSave(base.copy(name = name.trim(), icon = icon, color = color))
                    onDismiss()
                }
            }) { Text(tr("Save")) }
        },
        dismissButton = {
            Row {
                if (initial != null) {
                    TextButton(onClick = { confirmDelete = true }) { Text(tr("Delete"), color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDismiss) { Text(tr("Cancel")) }
            }
        },
    )

    if (confirmDelete && initial != null) {
        ConfirmDialog(
            title = tr("Delete {0}?", initial.name),
            text = tr("Transactions in this category will become Uncategorized, and its budget will be removed."),
            onConfirm = { onDelete(initial); onDismiss() },
            onDismiss = { confirmDelete = false },
        )
    }
}
