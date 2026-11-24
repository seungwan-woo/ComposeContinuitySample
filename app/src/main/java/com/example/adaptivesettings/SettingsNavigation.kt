package com.example.adaptivesettings

internal const val TWO_PANE_MIN_WIDTH_DP = 600f

internal enum class SettingsPanes {
    ListOnly,
    DetailOnly,
    ListAndDetail,
}

internal data class SettingsNavigation(
    val selectedId: String = "display",
    val showDetailInSinglePane: Boolean = false,
) {
    fun open(sectionId: String): SettingsNavigation =
        SettingsNavigation(sectionId, showDetailInSinglePane = true)

    fun showList(): SettingsNavigation =
        copy(showDetailInSinglePane = false)

    fun panesFor(widthDp: Float, heightDp: Float): SettingsPanes = when {
        widthDp >= TWO_PANE_MIN_WIDTH_DP && widthDp > heightDp -> SettingsPanes.ListAndDetail
        showDetailInSinglePane -> SettingsPanes.DetailOnly
        else -> SettingsPanes.ListOnly
    }
}
