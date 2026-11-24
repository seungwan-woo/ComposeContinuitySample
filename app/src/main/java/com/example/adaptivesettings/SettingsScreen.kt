package com.example.adaptivesettings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val navigationSaver = listSaver<SettingsNavigation, Any>(
    save = { listOf(it.selectedId, it.showDetailInSinglePane) },
    restore = { (id, showDetail) -> SettingsNavigation(id as String, showDetail as Boolean) },
)

@Composable
internal fun SettingsScreen(modifier: Modifier = Modifier) {
    var navigation by rememberSaveable(stateSaver = navigationSaver) {
        mutableStateOf(SettingsNavigation())
    }
    val listState = rememberLazyListState()
    val detailStateHolder = rememberSaveableStateHolder()
    var adaptiveBrightness by rememberSaveable { mutableStateOf(false) }
    val deviceName = rememberTextFieldState(initialText = "Compose 샘플")

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(modifier),
    ) {
        // Use the actual content bounds before IME padding. The keyboard shouldn't change modes.
        val panes = navigation.panesFor(maxWidth.value, maxHeight.value)
        SettingsPaneRow(
            panes = panes,
            selectedId = navigation.selectedId,
            listState = listState,
            detailStateHolder = detailStateHolder,
            deviceName = deviceName,
            adaptiveBrightness = adaptiveBrightness,
            onBrightnessChange = { adaptiveBrightness = it },
            onOpen = { navigation = navigation.open(it) },
            onBack = { navigation = navigation.showList() },
        )
    }
}

@Composable
private fun SettingsPaneRow(
    panes: SettingsPanes,
    selectedId: String,
    listState: LazyListState,
    detailStateHolder: SaveableStateHolder,
    deviceName: TextFieldState,
    adaptiveBrightness: Boolean,
    onBrightnessChange: (Boolean) -> Unit,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
) {
    val twoPanes = panes == SettingsPanes.ListAndDetail
    if (!LocalInspectionMode.current) {
        BackHandler(enabled = panes == SettingsPanes.DetailOnly, onBack = onBack)
    }

    // Each pane has one composition position across all modes. In particular, a visible
    // detail never moves to a different branch when the list appears or disappears.
    Row(Modifier.fillMaxSize().imePadding()) {
        if (panes != SettingsPanes.DetailOnly) {
            SettingsListPane(
                modifier = Modifier
                    .fillMaxHeight()
                    .then(if (twoPanes) Modifier.listPaneWidth() else Modifier.fillMaxWidth()),
                listState = listState,
                selectedId = selectedId.takeIf { twoPanes },
                onOpen = onOpen,
            )
        }

        if (twoPanes) {
            VerticalDivider(Modifier.fillMaxHeight(), color = MaterialTheme.colorScheme.outlineVariant)
        }

        if (panes != SettingsPanes.ListOnly) {
            val section = settingSections.first { it.id == selectedId }
            detailStateHolder.SaveableStateProvider(section.id) {
                SettingsDetailPane(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    section = section,
                    adaptiveBrightness = adaptiveBrightness,
                    onBrightnessChange = onBrightnessChange,
                    deviceName = deviceName,
                    onBack = onBack.takeUnless { twoPanes },
                )
            }
        }
    }
}

// Read live width in measurement so dragging within 2-pane mode only resizes the panes.
private fun Modifier.listPaneWidth(): Modifier = layout { measurable, constraints ->
    val width = (constraints.maxWidth * 0.36f).roundToInt()
        .coerceIn(240.dp.roundToPx(), 320.dp.roundToPx())
        .coerceAtMost(constraints.maxWidth)
    val pane = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(width, pane.height) { pane.placeRelative(0, 0) }
}

@Composable
private fun SettingsListPane(
    modifier: Modifier,
    listState: LazyListState,
    selectedId: String?,
    onOpen: (String) -> Unit,
) {
    LazyColumn(
        modifier = modifier.testTag("settings-list"),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item(key = "settings-title") {
            Text(
                text = "설정",
                modifier = Modifier.padding(start = 10.dp, top = 4.dp, bottom = 14.dp),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        items(settingSections, key = { it.id }) { section ->
            val selected = section.id == selectedId
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                contentColor = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            ) {
                Column(
                    modifier = Modifier
                        .selectable(selected = selected, onClick = { onOpen(section.id) })
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(section.title, style = MaterialTheme.typography.titleMedium)
                    Text(section.description, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun SettingsDetailPane(
    modifier: Modifier,
    section: SettingSection,
    adaptiveBrightness: Boolean,
    onBrightnessChange: (Boolean) -> Unit,
    deviceName: TextFieldState,
    onBack: (() -> Unit)?,
) {
    val detailScrollState = rememberScrollState()
    // The detail is short. Keeping its fields composed avoids dropping focus when a
    // resize clips the editor or changes the visible scroll range.
    Column(modifier.verticalScroll(detailScrollState)) {
        if (onBack != null) {
            TextButton(onClick = onBack, modifier = Modifier.padding(start = 12.dp, top = 4.dp)) {
                Text("‹  설정 목록")
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(section.description, style = MaterialTheme.typography.bodyMedium)
            }

            when (section.id) {
                "display" -> Surface(shape = RoundedCornerShape(18.dp)) {
                    ListItem(
                        supportingContent = { Text("샘플 설정") },
                        trailingContent = { Switch(checked = adaptiveBrightness, onCheckedChange = null) },
                        modifier = Modifier.toggleable(
                            value = adaptiveBrightness,
                            role = Role.Switch,
                            onValueChange = onBrightnessChange,
                        ),
                    ) { Text("자동 밝기") }
                }
                "device" -> OutlinedTextField(
                    state = deviceName,
                    label = { Text("기기 이름 (샘플)") },
                    modifier = Modifier.fillMaxWidth().testTag("device-name"),
                    lineLimits = TextFieldLineLimits.SingleLine,
                )
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    section.examples.forEach { label ->
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Preview(name = "세로 · 단일 창", widthDp = 390, heightDp = 760)
@Preview(name = "가로 · 2-pane", widthDp = 760, heightDp = 390)
@Composable
private fun SettingsPreview() {
    SettingsTheme { SettingsScreen() }
}
