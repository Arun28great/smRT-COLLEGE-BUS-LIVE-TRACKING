package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.models.NotificationCategory
import com.example.models.NotificationMessage
import com.example.ui.theme.*
import com.example.ui.viewmodels.TrackingViewModel

// --- NOTIFICATIONS CENTER SCREEN ---

@Composable
fun NotificationsScreen(
    viewModel: TrackingViewModel,
    modifier: Modifier = Modifier
) {
    val notifications by viewModel.notifications.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .windowInsetsPadding(WindowInsets.statusBars),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Notifications, "Alerts", tint = BluePrimary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Notification Center", color = TextPrimaryDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Inbox, "Empty", tint = TextSecondaryDark, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No active logs today.", color = TextSecondaryDark, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notifications) { msg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ThemeBorder, RoundedCornerShape(12.dp))
                            .testTag("notification_item_${msg.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (msg.category == NotificationCategory.EMERGENCY) EmergencyRed.copy(alpha = 0.15f) else SurfaceDark
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = when (msg.category) {
                                            NotificationCategory.EMERGENCY -> EmergencyRed.copy(alpha = 0.2f)
                                            NotificationCategory.DELAYED -> AlertOrange.copy(alpha = 0.2f)
                                            else -> BluePrimary.copy(alpha = 0.2f)
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (msg.category) {
                                        NotificationCategory.EMERGENCY -> Icons.Default.Warning
                                        NotificationCategory.DELAYED -> Icons.Default.Timelapse
                                        NotificationCategory.ARRIVED -> Icons.Default.CheckCircle
                                        else -> Icons.Default.DirectionsBus
                                    },
                                    contentDescription = "Category",
                                    tint = when (msg.category) {
                                        NotificationCategory.EMERGENCY -> EmergencyRed
                                        NotificationCategory.DELAYED -> AlertOrange
                                        else -> BluePrimary
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(msg.title, color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(msg.body, color = TextSecondaryDark, fontSize = 11.sp, lineHeight = 16.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(msg.timestamp, color = TextSecondaryDark, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

// --- PROFILE, REPORTS & AUXILIARY HELP SCREEN ---

@Composable
fun SettingsAboutScreen(
    viewModel: TrackingViewModel,
    modifier: Modifier = Modifier
) {
    var darkModeEnabled by remember { mutableStateOf(true) }
    var speedAlertLimit by remember { mutableStateOf(50f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .windowInsetsPadding(WindowInsets.statusBars),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Settings, "Settings", tint = BluePrimary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Settings & Information", color = TextPrimaryDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App settings controls
            item {
                Text("App Controls", color = TextPrimaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ThemeBorder, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Always Dark Theme", color = TextPrimaryDark, fontSize = 13.sp)
                            Switch(checked = darkModeEnabled, onCheckedChange = { darkModeEnabled = it })
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Overspeed Alert Threshold: ${speedAlertLimit.toInt()} km/h", color = TextPrimaryDark, fontSize = 12.sp)
                        Slider(
                            value = speedAlertLimit,
                            onValueChange = { speedAlertLimit = it },
                            valueRange = 40f..80f,
                            colors = SliderDefaults.colors(thumbColor = BluePrimary, activeTrackColor = BluePrimary)
                        )
                    }
                }
            }

            // Saved Campus Stop Preference Selector
            item {
                val currentUser by viewModel.currentUser.collectAsState()
                currentUser?.let { user ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Saved Campus Stop Preference", color = TextPrimaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, ThemeBorder, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Current Saved Stop:", color = TextSecondaryDark, fontSize = 12.sp)
                                    Text(
                                        text = user.savedCampusStop ?: "Hostel Zone",
                                        color = CyanPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Select a campus stop below. You will receive a Proximity Alert notification once your assigned bus is within 5 minutes of this stop.",
                                    color = TextSecondaryDark,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                val stopsList = listOf("Main Gate", "Gajendra Circle", "Hostel Zone", "Adyar Depot Stop", "Tidel Park Circle", "Tambaram Station", "Chromepet Bus Bay", "Pallavaram Jn", "Guindy Kathipara", "Campus Main Block")
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    stopsList.forEach { stop ->
                                        val isSelected = (user.savedCampusStop ?: "Hostel Zone") == stop
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { viewModel.updateSavedCampusStop(stop) },
                                            label = { Text(stop, fontSize = 11.sp, color = if (isSelected) Color.White else TextSecondaryDark) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = BluePrimary,
                                                containerColor = DarkBg
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Emergency Contacts Directory
            item {
                Text("Emergency Contact Directory", color = TextPrimaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ThemeBorder, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Campus Transit Control", color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("+91 9003214567", color = TextSecondaryDark, fontSize = 10.sp)
                            }
                            IconButton(onClick = {}) {
                                Icon(Icons.Default.Phone, "Call", tint = SuccessGreen)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Medical Health Emergency", color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("+91 911 2026 42", color = TextSecondaryDark, fontSize = 10.sp)
                            }
                            IconButton(onClick = {}) {
                                Icon(Icons.Default.LocalHospital, "Call", tint = EmergencyRed)
                            }
                        }
                    }
                }
            }

            // About system
            item {
                Text("About College Bus tracking Systems", color = TextPrimaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ThemeBorder, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Smart Transit Fleet Platform v2.6.4", color = BluePrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "This system leverages Google Maps GPS and Gemini AI models to predict traffic, estimate precise arrivals, analyze driver velocities, and report emergencies instantly to ensure safe and fully connected college commutes.",
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
