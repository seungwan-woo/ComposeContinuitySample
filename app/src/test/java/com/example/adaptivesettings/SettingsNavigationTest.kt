package com.example.adaptivesettings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsNavigationTest {
    @Test
    fun wideLandscapeShowsBothPanesAndNarrowLandscapeShowsList() {
        val navigation = SettingsNavigation()

        assertEquals(SettingsPanes.ListAndDetail, navigation.panesFor(600f, 390f))
        assertEquals(SettingsPanes.ListOnly, navigation.panesFor(599f, 390f))
        assertEquals(SettingsPanes.ListOnly, navigation.panesFor(720f, 900f))
    }

    @Test
    fun selectedDetailFollowsUserAcrossRotationAndResize() {
        val navigation = SettingsNavigation().open("device")

        assertEquals(SettingsPanes.DetailOnly, navigation.panesFor(390f, 760f))
        assertEquals(SettingsPanes.ListAndDetail, navigation.panesFor(760f, 390f))
        assertEquals(SettingsPanes.DetailOnly, navigation.panesFor(520f, 390f))
        assertEquals("device", navigation.selectedId)
    }

    @Test
    fun compactBackShowsListAndRemembersLastWideSelection() {
        val navigation = SettingsNavigation().open("device").showList()

        assertEquals(SettingsPanes.ListOnly, navigation.panesFor(390f, 760f))
        assertEquals(SettingsPanes.ListAndDetail, navigation.panesFor(760f, 390f))
        assertEquals("device", navigation.selectedId)
    }
}
