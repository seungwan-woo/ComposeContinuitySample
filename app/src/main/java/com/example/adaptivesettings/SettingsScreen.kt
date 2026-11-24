package com.example.adaptivesettings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
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

    val transition = updateTransition(panes, label = "Settings panes")
    val split = transition.animateFloat(
        transitionSpec = {
            if (initialState == SettingsPanes.ListAndDetail || targetState == SettingsPanes.ListAndDetail) {
                tween(durationMillis = 220)
            } else {
                snap()
            }
        },
        label = "List width",
    ) { mode -> mode.splitMarker }
    val geometry = remember { PaneGeometryMotion() }
    // Keep an outgoing pane composed until its viewport closes. Only measurement reads
    // the animation every frame, so the editor stays in its original composition slot.
    val listHasWidth by remember(split) { derivedStateOf { split.value > 0f } }
    val detailHasWidth by remember(split) { derivedStateOf { split.value < 2f } }
    val backHasHeight by remember(split) { derivedStateOf { split.value < 1f } }

    Layout(
        modifier = Modifier.fillMaxSize().imePadding().clipToBounds(),
        content = {
            if (panes != SettingsPanes.DetailOnly || listHasWidth) {
                SettingsListPane(
                    modifier = Modifier.layoutId(PaneSlot.List).paneViewport { split.value < 1f },
                    listState = listState,
                    selectedId = selectedId.takeIf { twoPanes },
                    onOpen = onOpen,
                )
            }
            if (listHasWidth && detailHasWidth) {
                VerticalDivider(
                    modifier = Modifier.layoutId(PaneSlot.Divider),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
            if (panes != SettingsPanes.ListOnly || detailHasWidth) {
                val section = settingSections.first { it.id == selectedId }
                detailStateHolder.SaveableStateProvider(section.id) {
                    SettingsDetailPane(
                        modifier = Modifier.layoutId(PaneSlot.Detail).paneViewport { split.value > 1f },
                        section = section,
                        adaptiveBrightness = adaptiveBrightness,
                        onBrightnessChange = onBrightnessChange,
                        deviceName = deviceName,
                        split = split,
                        onBack = onBack.takeIf { panes == SettingsPanes.DetailOnly || backHasHeight },
                        backEnabled = panes == SettingsPanes.DetailOnly,
                    )
                }
            }
        },
    ) { children, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val rail = (width * 0.36f).roundToInt().coerceIn(240.dp.roundToPx(), 320.dp.roundToPx())
            .coerceAtMost(width)
        val targetListWidth = when (panes) {
            SettingsPanes.DetailOnly -> 0
            SettingsPanes.ListAndDetail -> rail
            SettingsPanes.ListOnly -> width
        }
        val dividerSize = if (targetListWidth in 1 until width) {
            1.dp.roundToPx().coerceAtMost(width - targetListWidth)
        } else {
            0
        }
        val targetDetailStart = targetListWidth + dividerSize
        val target = PaneGeometry(targetListWidth, targetDetailStart, width - targetDetailStart)
        val (listWidth, detailStart, detailWidth) = geometry.measure(panes, split.value, target)
        val dividerWidth = detailStart - listWidth
        val list = children.firstOrNull { it.layoutId == PaneSlot.List }
            ?.measure(Constraints.fixed(listWidth, height))
        val divider = children.firstOrNull { it.layoutId == PaneSlot.Divider }
            ?.measure(Constraints.fixed(dividerWidth, height))
        val detail = children.firstOrNull { it.layoutId == PaneSlot.Detail }
            ?.measure(Constraints.fixed(detailWidth, height))
        layout(width, height) {
            if (listWidth > 0) list?.placeRelative(0, 0)
            if (dividerWidth > 0) divider?.placeRelative(listWidth, 0)
            if (detailWidth > 0) detail?.placeRelative(detailStart, 0)
        }
    }
}

private enum class PaneSlot { List, Divider, Detail }

// The markers carry progress between modes, independently of the current window bounds.
private val SettingsPanes.splitMarker: Float
    get() = when (this) {
        SettingsPanes.DetailOnly -> 0f
        SettingsPanes.ListAndDetail -> 1f
        SettingsPanes.ListOnly -> 2f
    }

private data class PaneGeometry(val listWidth: Int, val detailStart: Int, val detailWidth: Int)

private class PaneGeometryMotion {
    private var targetMode: SettingsPanes? = null
    private var lastMeasured: PaneGeometry? = null
    private var start: PaneGeometry? = null
    private var startMarker = 0f

    // Capture the previous frame when the mode changes. New window dimensions must not
    // stretch that frame before the animation starts. Retarget from the frame on screen.
    fun measure(mode: SettingsPanes, marker: Float, target: PaneGeometry): PaneGeometry {
        if (targetMode != mode) {
            targetMode = mode
            start = lastMeasured
            startMarker = marker
        }
        val endMarker = mode.splitMarker
        val origin = start
        val result = if (origin == null || startMarker == endMarker || marker == endMarker) {
            target
        } else {
            val fraction = ((marker - startMarker) / (endMarker - startMarker)).coerceIn(0f, 1f)
            PaneGeometry(
                listWidth = lerp(origin.listWidth, target.listWidth, fraction),
                detailStart = lerp(origin.detailStart, target.detailStart, fraction),
                detailWidth = lerp(origin.detailWidth, target.detailWidth, fraction),
            )
        }
        lastMeasured = result
        return result
    }
}

// Reveal a nearly closed pane through a crop rather than reflowing its contents into a sliver.
private fun Modifier.paneViewport(isPartiallyRevealed: () -> Boolean): Modifier =
    clipToBounds().layout { measurable, constraints ->
        val width = constraints.maxWidth
        val childWidth = if (isPartiallyRevealed()) width.coerceAtLeast(240.dp.roundToPx()) else width
        val pane = measurable.measure(Constraints.fixed(childWidth, constraints.maxHeight))
        layout(width, pane.height) { pane.placeRelative(0, 0) }
    }

private fun Modifier.backButtonViewport(split: State<Float>): Modifier =
    clipToBounds().layout { measurable, constraints ->
        val button = measurable.measure(constraints)
        val fraction = (1f - split.value).coerceIn(0f, 1f)
        val height = (button.height * fraction).roundToInt()
        layout(button.width, height) {
            // Slide the label out before the slot is fully closed.
            button.placeRelative(0, 2 * (height - button.height))
        }
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
    split: State<Float>,
    onBack: (() -> Unit)?,
    backEnabled: Boolean,
) {
    val detailScrollState = rememberScrollState()
    // The detail is short. Keeping its fields composed avoids dropping focus when a
    // resize clips the editor or changes the visible scroll range.
    Column(modifier.verticalScroll(detailScrollState)) {
        if (onBack != null) {
            TextButton(
                onClick = onBack,
                enabled = backEnabled,
                colors = ButtonDefaults.textButtonColors(disabledContentColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.backButtonViewport(split).padding(start = 12.dp, top = 4.dp),
            ) {
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
