package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.models.*
import com.example.ui.theme.*
import com.example.ui.viewmodels.TrackingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingDashboardScreen(
    viewModel: TrackingViewModel,
    onNavigateToMap: () -> Unit,
    onNavigateToChat: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val buses by viewModel.buses.collectAsState()
    val alerts by viewModel.emergencyAlerts.collectAsState()
    val records by viewModel.attendanceRecords.collectAsState()
    val scheduledArrivals by viewModel.scheduledArrivals.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()

    val profile = currentUser ?: return
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Smart Transit Hub",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "${profile.role.name} PORTAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = BluePrimary,
                            letterSpacing = 1.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, "Logout", tint = BluePrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToChat) {
                        Icon(Icons.Default.SmartToy, "Chat", tint = BluePrimary)
                    }
                    IconButton(onClick = onNavigateToMap) {
                        Icon(Icons.Default.Map, "Map", tint = BluePrimary)
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = SurfaceDark,
                    titleContentColor = TextPrimaryDark
                )
            )
        },
        containerColor = DarkBg,
        modifier = modifier
            .testTag("dashboard_root")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Welcome Header Card
            item {
                UserProfileCard(profile)
            }

            // Role-specific Panels
            when (profile.role) {
                UserRole.STUDENT -> {
                    studentDashboard(viewModel, buses, records, scheduledArrivals, onNavigateToMap)
                }
                UserRole.PARENT -> {
                    parentDashboard(viewModel, buses, records, scheduledArrivals, onNavigateToMap)
                }
                UserRole.DRIVER -> {
                    driverDashboard(viewModel, buses, allUsers)
                }
                UserRole.ADMIN -> {
                    adminDashboard(viewModel, buses, alerts, allUsers)
                }
            }
        }
    }
}

// --- COMMON COMPONENT: USER PROFILE CARD ---

@Composable
fun UserProfileCard(profile: UserProfile) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ThemeBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = profile.profilePhoto ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100"),
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(54.dp)
                    .background(Color.Gray.copy(alpha = 0.2f), CircleShape)
                    .padding(2.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Welcome, ${profile.name}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Text(
                    text = if (profile.role == UserRole.STUDENT) "ID: ${profile.regNo}" else profile.phoneNumber,
                    fontSize = 12.sp,
                    color = TextSecondaryDark
                )
            }
        }
    }
}

// --- MODULE 1: STUDENT DASHBOARD ---

fun LazyListScope.studentDashboard(
    viewModel: TrackingViewModel,
    buses: List<Bus>,
    records: List<AttendanceRecord>,
    scheduledArrivals: List<ScheduledArrival>,
    onNavigateToMap: () -> Unit
) {
    val myBus = buses.find { it.id == "bus-1" }

    item {
        Text("Your Assigned Commute", color = TextPrimaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }

    myBus?.let { bus ->
        item {
            AnimatedVisibility(visible = bus.etaMinutes < 5 && bus.status == BusStatus.RUNNING) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = SuccessGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.NotificationsActive, "Alert", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Proximity Alert: Your bus is arriving shortly. ETA is under 5 minutes.",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ThemeBorder, RoundedCornerShape(16.dp))
                    .clickable { onNavigateToMap() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(bus.routeName, color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Box(
                            modifier = Modifier
                                .background(SuccessGreen.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(bus.status.name, color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Assigned Vehicle", color = TextSecondaryDark, fontSize = 10.sp)
                            Text(bus.number, color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Speed", color = TextSecondaryDark, fontSize = 10.sp)
                            Text("${bus.speedKmh} km/h", color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timelapse, "ETA", tint = BluePrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ETA: ${bus.etaMinutes} mins to Campus", color = BluePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Battery indicator for Electric Bus theme
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BatteryChargingFull, "Battery", tint = SuccessGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${bus.batteryPercent}% Power", color = TextPrimaryDark, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            RouteScheduleCard(
                routeId = bus.routeId,
                routeName = bus.routeName,
                scheduledArrivals = scheduledArrivals
            )
        }
    }

    item {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ThemeBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Contactless Attendance", color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Scan boarding QR code displayed inside the bus.", color = TextSecondaryDark, fontSize = 11.sp)

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.scanAttendance("student-1", "bus-1", AttendanceStatus.BOARDED, "Gajendra Circle")
                    },
                    modifier = Modifier.fillMaxWidth().testTag("scan_boarding_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Icon(Icons.Default.QrCodeScanner, "QR Scan")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Board Bus (Simulate QR Scan)")
                }
            }
        }
    }

    item {
        Text("Your Trip Logs Today", color = TextPrimaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }

    items(records) { record ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ThemeBorder, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(SuccessGreen.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, "Checked", tint = SuccessGreen)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(record.studentName, color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("${record.stopName} Stop", color = TextSecondaryDark, fontSize = 10.sp)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(record.status.name, color = BluePrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(record.time, color = TextSecondaryDark, fontSize = 9.sp)
                }
            }
        }
    }
}

// --- MODULE 2: PARENT DASHBOARD ---

fun LazyListScope.parentDashboard(
    viewModel: TrackingViewModel,
    buses: List<Bus>,
    records: List<AttendanceRecord>,
    scheduledArrivals: List<ScheduledArrival>,
    onNavigateToMap: () -> Unit
) {
    val myBus = buses.find { it.id == "bus-1" }

    item {
        Text("Your Child Commute Tracker", color = TextPrimaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }

    myBus?.let { bus ->
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ThemeBorder, RoundedCornerShape(16.dp))
                    .clickable { onNavigateToMap() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Child: Arun Kumar", color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Box(
                            modifier = Modifier
                                .background(SuccessGreen.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Assigned", color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Active Bus Location: ${bus.nextStop}", color = TextSecondaryDark, fontSize = 11.sp)
                    Text("Commute ETA: ${bus.etaMinutes} mins with speed ${bus.speedKmh} km/h", color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Medium)

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onNavigateToMap,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                    ) {
                        Icon(Icons.Default.MyLocation, "Track")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Live Tracker Map")
                    }
                }
            }
        }

        item {
            RouteScheduleCard(
                routeId = bus.routeId,
                routeName = bus.routeName,
                scheduledArrivals = scheduledArrivals
            )
        }
    }

    item {
        Text("Boarding Logs Today", color = TextPrimaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }

    items(records) { record ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ThemeBorder, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    Icon(Icons.Default.NotificationsActive, "Boarded", tint = BluePrimary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Arun Kumar Boarded", color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("At stop ${record.stopName}", color = TextSecondaryDark, fontSize = 10.sp)
                    }
                }
                Text(record.time, color = BluePrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- MODULE 3: DRIVER DASHBOARD ---

fun LazyListScope.driverDashboard(
    viewModel: TrackingViewModel,
    buses: List<Bus>,
    allUsers: List<UserProfile>
) {
    val myBus = buses.find { it.id == "bus-1" } ?: return

    item {
        Text("Your Trip Controls", color = TextPrimaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }

    item {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ThemeBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Active Route: Adyar-Campus Shuttle", color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Assigned Bus ID: ${myBus.number}", color = TextSecondaryDark, fontSize = 11.sp)

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.updateBusStatus(myBus.id, BusStatus.RUNNING)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                    ) {
                        Text("Start Trip")
                    }

                    Button(
                        onClick = {
                            viewModel.updateBusStatus(myBus.id, BusStatus.IDLE)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("End Trip")
                    }
                }
            }
        }
    }

    item {
        val assignedStudents = allUsers.filter { it.role == UserRole.STUDENT && it.assignedBusId == myBus.id }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ThemeBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Passenger Boarding Controls", color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${assignedStudents.size} students assigned to your stop routes.", color = TextSecondaryDark, fontSize = 11.sp)
                    }
                    Icon(Icons.Default.VerifiedUser, "Verified", tint = CyanPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (assignedStudents.isEmpty()) {
                    Text("No students assigned to this bus route.", color = TextSecondaryDark, fontSize = 12.sp)
                } else {
                    assignedStudents.forEach { student ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(1.dp, ThemeBorder.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkBg)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(student.name, color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("Reg No: ${student.regNo ?: "N/A"} | Stop: ${student.savedCampusStop ?: "Hostel Zone"}", color = TextSecondaryDark, fontSize = 11.sp)
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .background(BlueSecondary.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Verify Stop",
                                            color = CyanPrimary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.scanAttendance(student.uid, myBus.id, AttendanceStatus.BOARDED, student.savedCampusStop ?: "Hostel Zone")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                        modifier = Modifier.weight(1f).height(32.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Boarded", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    
                                    Button(
                                        onClick = {
                                            viewModel.scanAttendance(student.uid, myBus.id, AttendanceStatus.DROPPED, student.savedCampusStop ?: "Hostel Zone")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                                        modifier = Modifier.weight(1f).height(32.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Deboarded", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    
                                    Button(
                                        onClick = {
                                            viewModel.scanAttendance(student.uid, myBus.id, AttendanceStatus.ABSENT, student.savedCampusStop ?: "Hostel Zone")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AlertOrange),
                                        modifier = Modifier.weight(1f).height(32.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Leave / Absent", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    item {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ThemeBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Emergency SOS Controls", color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Report high priority distress directly to campus administrators.", color = TextSecondaryDark, fontSize = 11.sp)

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.triggerSOS(myBus.id, "Flat tyre reported by driver. Replacement vehicle requested.")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                    modifier = Modifier.fillMaxWidth().testTag("sos_emergency_btn")
                ) {
                    Icon(Icons.Default.Warning, "SOS")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TRIGGER DISTRESS ALARM")
                }
            }
        }
    }

    item {
        Text("Live Bus GPS Simulator", color = TextPrimaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }

    item {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ThemeBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Simulate Driving Speeds", color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        viewModel.updateDriverGPS(myBus.id, 13.0118, 80.2354, 45)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BlueAccent)
                ) {
                    Text("Accelerate Commute GPS (45km/h)")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.updateBusStatus(myBus.id, BusStatus.RUNNING)
                        // This starts the broadcast conceptually
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Icon(Icons.Default.CellTower, "Broadcast")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Live Location Broadcast")
                }
            }
        }
    }
}

// --- MODULE 4: ADMIN DASHBOARD ---

fun LazyListScope.adminDashboard(
    viewModel: TrackingViewModel,
    buses: List<Bus>,
    alerts: List<EmergencyAlert>,
    allUsers: List<UserProfile>
) {
    item {
        Text("Admin Fleet Analytics Overview", color = TextPrimaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }

    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, ThemeBorder, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Total Buses", color = TextSecondaryDark, fontSize = 10.sp)
                    Text("${buses.size} Active", color = TextPrimaryDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, ThemeBorder, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("SOS Distress", color = TextSecondaryDark, fontSize = 10.sp)
                    Text("${alerts.filter { !it.isResolved }.size} Critical", color = if (alerts.isNotEmpty()) EmergencyRed else TextPrimaryDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    item {
        Text("Active Crisis SOS Room", color = TextPrimaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }

    items(alerts) { alert ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ThemeBorder, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vehicle ${alert.busNumber}", color = EmergencyRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    if (!alert.isResolved) {
                        Button(
                            onClick = { viewModel.resolveSOS(alert.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            modifier = Modifier.height(28.dp).padding(0.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Text("Resolve", fontSize = 10.sp)
                        }
                    } else {
                        Text("Resolved", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(alert.message, color = TextPrimaryDark, fontSize = 11.sp)
            }
        }
    }

    // --- STUDENT DIRECTORY & ASSIGNMENT PANEL ---
    item {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ThemeBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Manage Students & Bus Assignment", color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Select a student to assign/change their active college bus.", color = TextSecondaryDark, fontSize = 11.sp)
                    }
                    Icon(Icons.Default.People, "Students", tint = BluePrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))

                val students = allUsers.filter { it.role == UserRole.STUDENT }
                if (students.isEmpty()) {
                    Text("No students currently registered in database.", color = TextSecondaryDark, fontSize = 12.sp)
                } else {
                    students.forEach { student ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(1.dp, ThemeBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkBg)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(student.name, color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("Reg: ${student.regNo ?: "N/A"} | ${student.phoneNumber}", color = TextSecondaryDark, fontSize = 11.sp)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(BlueSecondary.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        val assignedBus = buses.find { it.id == student.assignedBusId }
                                        Text(
                                            text = assignedBus?.number ?: "Unassigned",
                                            color = CyanPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Assign Student to Bus Fleet:", color = TextSecondaryDark, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    buses.forEach { bus ->
                                        val isCurrent = student.assignedBusId == bus.id
                                        FilterChip(
                                            selected = isCurrent,
                                            onClick = { viewModel.assignStudentToBus(student.uid, bus.id) },
                                            label = { Text(bus.number, fontSize = 10.sp, color = if (isCurrent) Color.White else TextSecondaryDark) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = BluePrimary,
                                                containerColor = SurfaceDarkCard
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- PARENT & DRIVER DIRECTORY ---
    item {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ThemeBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            var showDriversSubTab by remember { mutableStateOf(true) }
            
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showDriversSubTab) "Campus Driver Directory" else "Campus Parent Directory",
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = showDriversSubTab,
                            onClick = { showDriversSubTab = true },
                            label = { Text("Drivers", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = !showDriversSubTab,
                            onClick = { showDriversSubTab = false },
                            label = { Text("Parents", fontSize = 10.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                if (showDriversSubTab) {
                    val drivers = allUsers.filter { it.role == UserRole.DRIVER }
                    if (drivers.isEmpty()) {
                        Text("No drivers registered.", color = TextSecondaryDark, fontSize = 11.sp)
                    } else {
                        drivers.forEach { driver ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, "Driver", tint = CyanPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(driver.name, color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(driver.email, color = TextSecondaryDark, fontSize = 10.sp)
                                    }
                                }
                                Text(driver.phoneNumber, color = BluePrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    val parents = allUsers.filter { it.role == UserRole.PARENT }
                    if (parents.isEmpty()) {
                        Text("No parents registered.", color = TextSecondaryDark, fontSize = 11.sp)
                    } else {
                        parents.forEach { parent ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.People, "Parent", tint = BluePrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(parent.name, color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("Parent of: ${parent.studentName ?: "N/A"}", color = TextSecondaryDark, fontSize = 10.sp)
                                    }
                                }
                                Text(parent.phoneNumber, color = BluePrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- ADD REGISTERED USERS ---
    item {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ThemeBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            var regRole by remember { mutableStateOf(UserRole.STUDENT) }
            var regName by remember { mutableStateOf("") }
            var regEmail by remember { mutableStateOf("") }
            var regPhone by remember { mutableStateOf("") }
            var regRegNo by remember { mutableStateOf("") }
            var regChildName by remember { mutableStateOf("") }
            var selectedBusForStudent by remember { mutableStateOf("") }

            Column(modifier = Modifier.padding(16.dp)) {
                Text("Register Campus Profiles", color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Add new students, parents, or drivers directly to the central transit database.", color = TextSecondaryDark, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    UserRole.values().filter { it != UserRole.ADMIN }.forEach { role ->
                        val isSelected = regRole == role
                        FilterChip(
                            selected = isSelected,
                            onClick = { regRole = role },
                            label = { Text(role.name, fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = regName,
                    onValueChange = { regName = it },
                    label = { Text("Full Name", fontSize = 11.sp, color = TextSecondaryDark) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark, focusedBorderColor = BluePrimary, unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f))
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = regEmail,
                    onValueChange = { regEmail = it },
                    label = { Text("Email Address", fontSize = 11.sp, color = TextSecondaryDark) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark, focusedBorderColor = BluePrimary, unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f))
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = regPhone,
                    onValueChange = { regPhone = it },
                    label = { Text("Phone Number", fontSize = 11.sp, color = TextSecondaryDark) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark, focusedBorderColor = BluePrimary, unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f))
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (regRole == UserRole.STUDENT) {
                    OutlinedTextField(
                        value = regRegNo,
                        onValueChange = { regRegNo = it },
                        label = { Text("Register Number (e.g. CS2026402)", fontSize = 11.sp, color = TextSecondaryDark) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark, focusedBorderColor = BluePrimary, unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f))
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Assign Initial Bus:", color = TextSecondaryDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        buses.forEach { bus ->
                            val isSel = selectedBusForStudent == bus.id
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedBusForStudent = bus.id },
                                label = { Text(bus.number, fontSize = 10.sp) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (regRole == UserRole.PARENT) {
                    OutlinedTextField(
                        value = regChildName,
                        onValueChange = { regChildName = it },
                        label = { Text("Student/Child Full Name", fontSize = 11.sp, color = TextSecondaryDark) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark, focusedBorderColor = BluePrimary, unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        if (regName.isNotBlank() && regEmail.isNotBlank()) {
                            when (regRole) {
                                UserRole.STUDENT -> {
                                    viewModel.addStudentProfile(regName, regEmail, regPhone, regRegNo, selectedBusForStudent.ifBlank { "bus-1" })
                                }
                                UserRole.PARENT -> {
                                    viewModel.addParentProfile(regName, regEmail, regPhone, regChildName)
                                }
                                UserRole.DRIVER -> {
                                    viewModel.addDriverProfile(regName, regEmail, regPhone)
                                }
                                else -> {}
                            }
                            regName = ""
                            regEmail = ""
                            regPhone = ""
                            regRegNo = ""
                            regChildName = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("Add Profile", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // --- REGISTER NEW VEHICLE FLEET ---
    item {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ThemeBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            var number by remember { mutableStateOf("") }
            var driver by remember { mutableStateOf("") }
            var phone by remember { mutableStateOf("") }

            Column(modifier = Modifier.padding(16.dp)) {
                Text("Add New Campus Bus Fleet", color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("Registration Number (e.g. TN-07-2026)", fontSize = 11.sp, color = TextSecondaryDark) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark, focusedBorderColor = BluePrimary, unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f))
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = driver,
                    onValueChange = { driver = it },
                    label = { Text("Driver Name", fontSize = 11.sp, color = TextSecondaryDark) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark, focusedBorderColor = BluePrimary, unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f))
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Driver Phone Number", fontSize = 11.sp, color = TextSecondaryDark) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark, focusedBorderColor = BluePrimary, unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f))
                )
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (number.isNotBlank()) {
                            viewModel.addBus(number, driver, phone, "route-1")
                            number = ""
                            driver = ""
                            phone = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Text("Register Vehicle")
                }
            }
        }
    }
}

@Composable
fun RouteScheduleCard(
    routeId: String,
    routeName: String,
    scheduledArrivals: List<ScheduledArrival>
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ThemeBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, "Schedule", tint = CyanPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Route Scheduled Arrival Times", color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(routeName, color = TextSecondaryDark, fontSize = 11.sp)
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    tint = BluePrimary
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = ThemeBorder, modifier = Modifier.padding(bottom = 12.dp))
                    
                    val arrivals = scheduledArrivals.filter { it.routeId == routeId }
                    if (arrivals.isEmpty()) {
                        Text("No scheduled arrivals available for this route.", color = TextSecondaryDark, fontSize = 12.sp)
                    } else {
                        arrivals.forEach { arrival ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Place, "Stop", tint = BluePrimary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(arrival.stopName, color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(BlueSecondary.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = arrival.scheduledTime,
                                        color = CyanPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
