package com.example.models

enum class UserRole {
    STUDENT,
    PARENT,
    DRIVER,
    ADMIN
}

data class UserProfile(
    val uid: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val phoneNumber: String,
    val assignedBusId: String? = null,
    val studentName: String? = null, // Used for parent roles
    val regNo: String? = null, // Used for student roles
    val profilePhoto: String? = null,
    val savedCampusStop: String? = "Hostel Zone"
)

enum class BusStatus {
    IDLE,
    RUNNING,
    DELAYED,
    BREAKDOWN
}

data class Bus(
    val id: String,
    val number: String,
    val driverId: String,
    val driverName: String,
    val driverPhone: String,
    val routeId: String,
    val routeName: String,
    val status: BusStatus,
    val speedKmh: Int,
    val currentLat: Double,
    val currentLng: Double,
    val etaMinutes: Int,
    val nextStop: String,
    val totalCapacity: Int,
    val activeBoarded: Int,
    val batteryPercent: Int = 85, // Supportive electric bus indicators
    val maintenanceDue: String = "Aug 20, 2026",
    val occupancy: String = "64%"
)

data class ScheduledArrival(
    val id: String,
    val routeId: String,
    val busId: String,
    val stopName: String,
    val scheduledTime: String,
    val estimatedTime: String? = null
)

data class RouteStop(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val scheduledTime: String
)

data class BusRoute(
    val id: String,
    val name: String,
    val stops: List<RouteStop>,
    val polylinePath: List<Pair<Double, Double>> // Simulated map lines
)

enum class NotificationCategory {
    STARTED,
    ARRIVED,
    DELAYED,
    NEAR_STOP,
    EMERGENCY,
    ATTENDANCE,
    ANNOUNCEMENT
}

data class NotificationMessage(
    val id: String,
    val title: String,
    val body: String,
    val timestamp: String,
    val category: NotificationCategory,
    val isRead: Boolean = false
)

enum class AttendanceStatus {
    BOARDED,
    DROPPED,
    ABSENT
}

data class AttendanceRecord(
    val id: String,
    val studentId: String,
    val studentName: String,
    val busId: String,
    val date: String,
    val time: String,
    val status: AttendanceStatus,
    val stopName: String
)

data class EmergencyAlert(
    val id: String,
    val busId: String,
    val busNumber: String,
    val driverName: String,
    val latitude: Double,
    val longitude: Double,
    val message: String,
    val timestamp: String,
    val isResolved: Boolean = false
)

data class ChatMessage(
    val id: String,
    val sender: String, // "user" or "ai"
    val content: String,
    val timestamp: String
)

data class FuelLog(
    val date: String,
    val amountLitres: Double,
    val cost: Double,
    val odometer: Double
)
