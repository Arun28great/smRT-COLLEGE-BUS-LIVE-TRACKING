import sys

content = """package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.BusStatus
import com.example.ui.theme.*
import com.example.ui.viewmodels.TrackingViewModel
import com.google.maps.android.compose.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.streetview.StreetView
import com.google.android.gms.maps.StreetViewPanoramaOptions
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: TrackingViewModel,
    modifier: Modifier = Modifier
) {
    val buses by viewModel.buses.collectAsState()
    val selectedBus by viewModel.selectedBus.collectAsState()
    val routes by viewModel.routes.collectAsState()

    var isSatelliteView by remember { mutableStateOf(false) }
    var showTrafficLayer by remember { mutableStateOf(true) }
    var showGeofencing by remember { mutableStateOf(true) }
    var showStreetView by remember { mutableStateOf(false) }

    val campusCenter = LatLng(13.0118, 80.2354)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(campusCenter, 14f)
    }

    var insideGeofenceAlert by remember { mutableStateOf(false) }
    var simulationTick by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            simulationTick = (simulationTick + 1) % 100
            selectedBus?.let { bus ->
                val currentRoute = routes.find { it.id == bus.routeId }
                currentRoute?.let { route ->
                    val pathSize = route.polylinePath.size
                    if (pathSize > 1) {
                        val segmentIndex = (simulationTick / 20) % (pathSize - 1)
                        val subProgress = (simulationTick % 20) / 20.0f
                        val startCoord = route.polylinePath[segmentIndex]
                        val endCoord = route.polylinePath[segmentIndex + 1]

                        val simulatedLat = startCoord.first + (endCoord.first - startCoord.first) * subProgress
                        val simulatedLng = startCoord.second + (endCoord.second - startCoord.second) * subProgress

                        // Do not automatically override user's driver GPS if they are starting broadcast
                        if (bus.status != BusStatus.RUNNING) {
                            viewModel.updateDriverGPS(bus.id, simulatedLat, simulatedLng, (35..55).random())
                        }

                        val distToCampus = kotlin.math.sqrt(
                            (simulatedLat - 13.0118) * (simulatedLat - 13.0118) +
                            (simulatedLng - 80.2354) * (simulatedLng - 80.2354)
                        )
                        insideGeofenceAlert = distToCampus < 0.015
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isSatelliteView) Color(0xFF0D1414) else DarkBg)
    ) {
        if (showStreetView) {
            val streetViewBus = selectedBus ?: buses.firstOrNull()
            val pos = streetViewBus?.let { LatLng(it.currentLat, it.currentLng) } ?: campusCenter
            StreetView(
                modifier = Modifier.fillMaxSize().testTag("interactive_canvas_map"),
                streetViewPanoramaOptionsFactory = {
                    StreetViewPanoramaOptions().position(pos)
                }
            )
        } else {
            val mapProperties = MapProperties(
                mapType = if (isSatelliteView) MapType.SATELLITE else MapType.NORMAL,
                isTrafficEnabled = showTrafficLayer
            )

            GoogleMap(
                modifier = Modifier.fillMaxSize().testTag("interactive_canvas_map"),
                cameraPositionState = cameraPositionState,
                properties = mapProperties
            ) {
                if (showGeofencing) {
                    Circle(
                        center = campusCenter,
                        radius = 500.0,
                        fillColor = CyanPrimary.copy(alpha = 0.15f),
                        strokeColor = CyanPrimary.copy(alpha = 0.8f),
                        strokeWidth = 3f
                    )
                }

                routes.forEach { route ->
                    val points = route.polylinePath.map { LatLng(it.first, it.second) }
                    Polyline(
                        points = points,
                        color = if (route.id == "route-1") BlueAccent.copy(alpha = 0.6f) else Color(0xFF6F7682).copy(alpha = 0.6f),
                        width = 16f
                    )
                    Polyline(
                        points = points,
                        color = if (route.id == "route-1") CyanPrimary else Color(0xFFA1E3FF),
                        width = 4f
                    )

                    route.stops.forEach { stop ->
                        Marker(
                            state = MarkerState(position = LatLng(stop.latitude, stop.longitude)),
                            title = stop.name
                        )
                    }
                }

                Marker(
                    state = MarkerState(position = campusCenter),
                    title = "Campus Hub",
                    snippet = "Main Campus Entrance"
                )

                buses.forEach { bus ->
                    Marker(
                        state = MarkerState(position = LatLng(bus.currentLat, bus.currentLng)),
                        title = bus.number,
                        snippet = "ETA: ${bus.etaMinutes} mins | Speed: ${bus.speedKmh} km/h"
                    )
                }
            }
        }

        // --- MAP CONTROLS FLOATING BAR ---
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .windowInsetsPadding(WindowInsets.statusBars),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = { showStreetView = !showStreetView },
                containerColor = if (showStreetView) SuccessGreen else SurfaceDark,
                contentColor = if (showStreetView) Color.White else BluePrimary,
                modifier = Modifier.size(44.dp).border(1.dp, ThemeBorder, CircleShape)
            ) { Icon(Icons.Default.Streetview, "Street View Toggle") }

            if (!showStreetView) {
                FloatingActionButton(
                    onClick = { isSatelliteView = !isSatelliteView },
                    containerColor = SurfaceDark,
                    contentColor = BluePrimary,
                    modifier = Modifier.size(44.dp).border(1.dp, ThemeBorder, CircleShape)
                ) { Icon(if (isSatelliteView) Icons.Default.Terrain else Icons.Default.Satellite, "Map Style") }

                FloatingActionButton(
                    onClick = { showTrafficLayer = !showTrafficLayer },
                    containerColor = if (showTrafficLayer) SuccessGreen else SurfaceDark,
                    contentColor = if (showTrafficLayer) Color.White else BluePrimary,
                    modifier = Modifier.size(44.dp).border(1.dp, ThemeBorder, CircleShape)
                ) { Icon(Icons.Default.Traffic, "Traffic Toggle") }

                FloatingActionButton(
                    onClick = { showGeofencing = !showGeofencing },
                    containerColor = if (showGeofencing) BluePrimary else SurfaceDark,
                    contentColor = if (showGeofencing) Color.White else BluePrimary,
                    modifier = Modifier.size(44.dp).border(1.dp, ThemeBorder, CircleShape)
                ) { Icon(Icons.Default.WifiTethering, "Geofencing Toggle") }
            }
        }

        // --- GEOFENCING NOTIFICATION BANNER ---
        if (!showStreetView) {
            AnimatedVisibility(
                visible = insideGeofenceAlert && showGeofencing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
                    .padding(horizontal = 24.dp)
            ) {
                Card(colors = CardDefaults.cardColors(containerColor = SuccessGreen), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, "Inside", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Geofence Crossed: Selected bus TN-07 is inside campus radius. ETA: Under 5 mins.", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- MAP LEGEND ---
        if (!showStreetView) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .background(SurfaceDark.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                    .border(1.dp, ThemeBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Map Legend", color = TextPrimaryDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(BluePrimary, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("College Campus Anchor", color = TextSecondaryDark, fontSize = 10.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(CyanPrimary, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bus route stops", color = TextSecondaryDark, fontSize = 10.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(EmergencyRed, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Breakdown / SOS Active", color = TextSecondaryDark, fontSize = 10.sp)
                    }
                }
            }
        }

        // --- SELECTED BUS FLOATING STATUS OVERLAY ---
        selectedBus?.let { bus ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .width(180.dp)
                    .border(1.dp, ThemeBorder, RoundedCornerShape(16.dp))
                    .testTag("map_telemetry_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(bus.number, fontWeight = FontWeight.Bold, color = TextPrimaryDark, fontSize = 12.sp)
                    Text("Next: ${bus.nextStop}", color = TextSecondaryDark, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, "Speed", tint = BluePrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${bus.speedKmh} km/h", color = TextPrimaryDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .background(if (bus.status == BusStatus.RUNNING) SuccessGreen.copy(alpha = 0.2f) else EmergencyRed.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("${bus.etaMinutes}m ETA", color = if (bus.status == BusStatus.RUNNING) SuccessGreen else EmergencyRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
"""

with open("app/src/main/java/com/example/ui/screens/MapScreen.kt", "w") as f:
    f.write(content)
