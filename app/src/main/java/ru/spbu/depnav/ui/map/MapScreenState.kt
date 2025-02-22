package ru.spbu.depnav.ui.map

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.centerOnMarker
import ovh.plrapps.mapcompose.api.onTap
import ovh.plrapps.mapcompose.api.removeAllLayers
import ovh.plrapps.mapcompose.api.removeAllMarkers
import ovh.plrapps.mapcompose.api.removeMarker
import ovh.plrapps.mapcompose.api.setScrollOffsetRatio
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.state.MapState
import ru.spbu.depnav.model.Marker
import ru.spbu.depnav.model.MarkerText

private const val TAG = "MapViewModel"

/**
 * State of the [MapScreen].
 */
class MapScreenState : ViewModel() {
    /**
     * State of the map currently displayed.
     */
    var state by mutableStateOf(MapState(0, 0, 0))
        private set

    /**
     * Whether the map currently displayed is in dark theme.
     */
    var usesDarkThemeTiles: Boolean? = null
        private set

    /**
     * The floor currently displayed. Equals [Int.MIN_VALUE] by default.
     */
    var currentFloor by mutableStateOf(Int.MIN_VALUE)

    /**
     * Whether any UI is displayed on top of the map.
     */
    var showUI by mutableStateOf(true)
        private set

    /**
     * Whether a marker is highlighted.
     *
     * It is separate from [highlightedMarker] because text needs to stay visible while hiding
     * animation is playing.
     */
    var highlightMarker by mutableStateOf(false)

    /**
     * The marker currently highlighted.
     */
    var highlightedMarker by mutableStateOf<Pair<Marker, MarkerText>?>(null)
        private set

    /**
     * Sets the parameters of the displayed map.
     */
    fun setParams(width: Int, height: Int, tileSize: Int = 1024) {
        state.shutdown()
        state = MapState(1, width, height, tileSize) { scale(0f) }.apply {
            setScrollOffsetRatio(0.5f, 0.5f)
            onTap { _, _ ->
                if (!highlightMarker) showUI = !showUI
                highlightMarker = false
                highlightedMarker?.let { (marker, _) -> state.removeMarker(marker.idStr) }
            }
        }
    }

    private fun placeMarker(marker: Marker, markerText: MarkerText, isHighlighted: Boolean) {
        state.addMarker(
            id = marker.idStr,
            x = marker.x,
            y = marker.y,
            zIndex = if (isHighlighted) 1f else 0f,
            relativeOffset = Offset(-0.5f, -0.5f),
            clickable = false,
            clipShape = null
        ) {
            MarkerView(
                title = markerText.title ?: "",
                type = marker.type,
                isClosed = marker.isClosed,
                isHighlighted = isHighlighted,
                modifier =
                if (!markerText.title.isNullOrBlank() || !markerText.description.isNullOrBlank()) {
                    Modifier.clickable { highlightMarker(marker, markerText) }
                } else {
                    Modifier
                }
            )
        }
    }

    private fun highlightMarker(marker: Marker, markerText: MarkerText) {
        val newMarker = marker.copy(id = Int.MIN_VALUE) // Real IDs start from 1
        val newMarkerText = markerText.copy(markerId = newMarker.id)

        highlightedMarker?.let { (oldMarker, _) -> state.removeMarker(oldMarker.idStr) }

        showUI = true
        highlightedMarker = newMarker to newMarkerText
        highlightMarker = true

        placeMarker(newMarker, newMarkerText, isHighlighted = true)
    }

    /**
     * Replaces the currently displayed layers.
     */
    fun replaceLayersWith(tileProviders: Iterable<TileStreamProvider>, isDark: Boolean) {
        Log.d(TAG, "Replacing layers...")

        state.removeAllLayers()
        for (tileProvider in tileProviders) state.addLayer(tileProvider)
        usesDarkThemeTiles = isDark
    }

    /**
     * Replaces the currently displayed markers.
     */
    fun replaceMarkersWith(markersWithText: Map<Marker, MarkerText>) {
        Log.d(TAG, "Replacing markers...")

        state.removeAllMarkers()

        for ((marker, markerText) in markersWithText) {
            placeMarker(marker, markerText, isHighlighted = false)
        }
    }

    /**
     * Centers on the specified marker and highlights it.
     */
    fun focusOnMarker(marker: Marker, markerText: MarkerText) {
        Log.d(TAG, "Centering on marker $marker")

        highlightMarker(marker, markerText)
        viewModelScope.launch { state.centerOnMarker(marker.idStr, 1f) }
    }
}
