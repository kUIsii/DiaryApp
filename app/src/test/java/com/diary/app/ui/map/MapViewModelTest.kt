package com.diary.app.ui.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapViewModelTest {

    // --- extractCityFromLocation ---

    @Test
    fun `extract city returns second to last part from comma-separated location`() {
        assertEquals("上海", extractCityFromLocation("徐汇区, 上海, 中国"))
    }

    @Test
    fun `extract city returns single part when no comma`() {
        assertEquals("北京", extractCityFromLocation("北京"))
    }

    @Test
    fun `extract city returns empty for blank location`() {
        assertEquals("", extractCityFromLocation(""))
    }

    @Test
    fun `extract city handles two-part location`() {
        assertEquals("Tokyo", extractCityFromLocation("Shibuya, Tokyo"))
    }

    @Test
    fun `extract city trims whitespace from parts`() {
        assertEquals("Paris", extractCityFromLocation("  Eiffel Tower , Paris , France "))
    }

    // --- MapMarker construction ---

    @Test
    fun `map marker stores all fields correctly`() {
        val marker = MapMarker(
            id = 1L,
            title = "test",
            latitude = 31.23,
            longitude = 121.47,
            location = "Shanghai",
            createdAt = 1000L,
            moodLevel = 4
        )

        assertEquals(1L, marker.id)
        assertEquals("test", marker.title)
        assertEquals(31.23, marker.latitude, 0.001)
        assertEquals(121.47, marker.longitude, 0.001)
        assertEquals("Shanghai", marker.location)
        assertEquals(1000L, marker.createdAt)
        assertEquals(4, marker.moodLevel)
    }

    @Test
    fun `map marker allows null mood level`() {
        val marker = MapMarker(
            id = 2L, title = "no mood", latitude = 0.0, longitude = 0.0,
            location = "", createdAt = 0L, moodLevel = null
        )

        assertNull(marker.moodLevel)
    }

    // --- MapStats defaults ---

    @Test
    fun `map stats defaults to zero and null`() {
        val stats = MapStats()

        assertEquals(0, stats.totalEntries)
        assertEquals(0, stats.uniqueLocations)
        assertEquals(0, stats.citiesVisited)
        assertNull(stats.firstEntryDate)
        assertNull(stats.lastEntryDate)
    }

    // --- MapUiState defaults ---

    @Test
    fun `map ui state defaults to loading with empty markers`() {
        val state = MapUiState()

        assertEquals(true, state.isLoading)
        assertEquals(emptyList<MapMarker>(), state.markers)
        assertNull(state.selectedMarker)
        assertEquals(MapStats(), state.stats)
        assertNull(state.error)
    }

    // --- computeMapStats ---

    @Test
    fun `compute stats counts total entries`() {
        val markers = listOf(
            marker(1L, "A", "Shanghai", 100L),
            marker(2L, "B", "Beijing", 200L)
        )

        val stats = computeMapStats(markers)

        assertEquals(2, stats.totalEntries)
    }

    @Test
    fun `compute stats counts unique locations excluding blanks`() {
        val markers = listOf(
            marker(1L, "A", "Shanghai", 100L),
            marker(2L, "B", "Shanghai", 200L),
            marker(3L, "C", "", 300L),
            marker(4L, "D", "Beijing", 400L)
        )

        val stats = computeMapStats(markers)

        assertEquals(2, stats.uniqueLocations)
    }

    @Test
    fun `compute stats extracts unique cities from location strings`() {
        val markers = listOf(
            marker(1L, "A", "Pudong, Shanghai, China", 100L),
            marker(2L, "B", "Xuhui, Shanghai, China", 200L),
            marker(3L, "C", "Haidian, Beijing, China", 300L)
        )

        val stats = computeMapStats(markers)

        assertEquals(2, stats.citiesVisited)
    }

    @Test
    fun `compute stats finds first and last entry dates`() {
        val markers = listOf(
            marker(1L, "A", "loc", 300L),
            marker(2L, "B", "loc", 100L),
            marker(3L, "C", "loc", 200L)
        )

        val stats = computeMapStats(markers)

        assertEquals(100L, stats.firstEntryDate)
        assertEquals(300L, stats.lastEntryDate)
    }

    @Test
    fun `compute stats returns null dates for empty markers`() {
        val stats = computeMapStats(emptyList())

        assertEquals(0, stats.totalEntries)
        assertNull(stats.firstEntryDate)
        assertNull(stats.lastEntryDate)
    }

    @Test
    fun `compute stats handles single marker`() {
        val markers = listOf(marker(1L, "Only", "Tokyo, Japan", 500L))

        val stats = computeMapStats(markers)

        assertEquals(1, stats.totalEntries)
        assertEquals(1, stats.uniqueLocations)
        assertEquals(1, stats.citiesVisited)
        assertEquals(500L, stats.firstEntryDate)
        assertEquals(500L, stats.lastEntryDate)
    }

    @Test
    fun `compute stats ignores blank locations for city count`() {
        val markers = listOf(
            marker(1L, "A", "", 100L),
            marker(2L, "B", "  ", 200L)
        )

        val stats = computeMapStats(markers)

        assertEquals(0, stats.uniqueLocations)
        assertEquals(0, stats.citiesVisited)
    }

    // --- selectMarker via MapUiState copy ---

    @Test
    fun `selecting marker updates ui state`() {
        val marker = marker(1L, "Selected", "loc", 100L)
        val state = MapUiState()
        val updated = state.copy(selectedMarker = marker)

        assertEquals(marker, updated.selectedMarker)
    }

    @Test
    fun `deselecting marker sets null`() {
        val marker = marker(1L, "Selected", "loc", 100L)
        val state = MapUiState(selectedMarker = marker)
        val updated = state.copy(selectedMarker = null)

        assertNull(updated.selectedMarker)
    }

    // helper
    private fun marker(id: Long, title: String, location: String, createdAt: Long) =
        MapMarker(id = id, title = title, latitude = 0.0, longitude = 0.0,
            location = location, createdAt = createdAt, moodLevel = null)
}
