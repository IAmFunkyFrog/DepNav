package ru.spbu.depnav.ui.map

import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.state.MapState
import ru.spbu.depnav.model.Marker

private const val TAG = "MapViewModel"

const val FLOOR_UNINITIALIZED = Int.MIN_VALUE

// TODO: instead of placing all markers, create a layer with marker graphics adding markers by one dynamically when needed to center and removing afterwards

class MapScreenState : ViewModel() {
    var state by mutableStateOf(MapState(0, 0, 0))
        private set
    var currentFloor by mutableStateOf(FLOOR_UNINITIALIZED)

    fun setParams(width: Int, height: Int, tileSize: Int = 1024) {
        state.shutdown()
        state = MapState(1, width, height, tileSize) {
            scroll(0.5, 0.5)
            scale(0f)
        }
        currentFloor = 0
    }

    fun replaceLayersWith(tileProviders: Iterable<TileStreamProvider>) {
        Log.d(TAG, "Replacing layers...")

        state.removeAllLayers()
        for (tileProvider in tileProviders) state.addLayer(tileProvider)
    }

    fun replaceMarkersWith(markers: Iterable<Marker>) {
        Log.d(TAG, "Replacing markers...")

        state.removeAllMarkers()

        for (marker in markers) state.addMarker(
            id = marker.idStr,
            x = marker.x,
            y = marker.y,
            relativeOffset = Offset(-0.5f, -0.5f),
            clipShape = null
        ) { MarkerView(marker.type, modifier = Modifier.size(20.dp)) }
    }

    fun centerOnMarker(id: String) {
        Log.d(TAG, "Centering on marker $id")

        viewModelScope.launch { state.centerOnMarker(id, 1f) }
    }
}