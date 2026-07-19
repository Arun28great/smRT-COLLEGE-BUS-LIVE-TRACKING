package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.models.AttendanceRecord
import com.example.models.AttendanceStatus
import com.example.models.Bus
import com.example.models.BusStatus
import com.example.models.ChatMessage
import com.example.models.EmergencyAlert
import com.example.models.NotificationMessage
import com.example.models.ScheduledArrival
import com.example.models.UserProfile
import com.example.models.UserRole
import com.example.repositories.TrackingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrackingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TrackingRepository(application)

    // Current logged-in user
    val currentUser: StateFlow<UserProfile?> = repository.currentUser

    // Master operational lists
    val buses: StateFlow<List<Bus>> = repository.buses
    val routes = repository.routes
    val emergencyAlerts: StateFlow<List<EmergencyAlert>> = repository.emergencyAlerts
    val notifications: StateFlow<List<NotificationMessage>> = repository.notifications
    val attendanceRecords: StateFlow<List<AttendanceRecord>> = repository.attendanceRecords
    val chatMessages: StateFlow<List<ChatMessage>> = repository.chatMessages
    val scheduledArrivals: StateFlow<List<ScheduledArrival>> = repository.scheduledArrivals

    private val _allUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    val allUsers: StateFlow<List<UserProfile>> = _allUsers.asStateFlow()

    // UI Loading & Errors
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    // Temporary values
    private val _selectedBus = MutableStateFlow<Bus?>(null)
    val selectedBus = _selectedBus.asStateFlow()

    init {
        // Default to select first bus
        viewModelScope.launch {
            buses.collect { list ->
                if (_selectedBus.value == null && list.isNotEmpty()) {
                    _selectedBus.value = list.first()
                }
            }
        }
        fetchAllUsers()
    }

    fun selectBus(bus: Bus) {
        _selectedBus.value = bus
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // --- Authentication ---

    fun loginUser(email: String, password: String, onSuccess: (UserProfile) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            repository.login(email, password).fold(
                onSuccess = { profile ->
                    _isLoading.value = false
                    onSuccess(profile)
                },
                onFailure = { error ->
                    _isLoading.value = false
                    _errorMessage.value = error.message ?: "Authentication failed."
                }
            )
        }
    }

    fun registerUser(name: String, email: String, role: UserRole, phone: String, additional: String, password: String, onSuccess: (UserProfile) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            repository.register(name, email, role, phone, additional, password).fold(
                onSuccess = { profile ->
                    _isLoading.value = false
                    onSuccess(profile)
                },
                onFailure = { error ->
                    _isLoading.value = false
                    _errorMessage.value = error.message ?: "Registration failed."
                }
            )
        }
    }

    fun logoutUser(onSuccess: () -> Unit) {
        repository.logout()
        onSuccess()
    }

    // --- Driver Controls ---

    fun updateBusStatus(busId: String, status: BusStatus) {
        viewModelScope.launch {
            repository.updateBusStatus(busId, status)
        }
    }

    fun updateDriverGPS(busId: String, lat: Double, lng: Double, speed: Int) {
        viewModelScope.launch {
            repository.updateDriverGPS(busId, lat, lng, speed)
        }
    }

    fun triggerSOS(busId: String, message: String) {
        viewModelScope.launch {
            repository.triggerSOS(busId, message)
        }
    }

    fun resolveSOS(sosId: String) {
        viewModelScope.launch {
            repository.resolveSOS(sosId)
        }
    }

    // --- QR Attendance Scans ---

    fun scanAttendance(studentId: String, busId: String, status: AttendanceStatus, stopName: String) {
        viewModelScope.launch {
            repository.scanQRAttendance(studentId, busId, status, stopName)
        }
    }

    // --- AI Chat Assistant ---

    fun sendMessageToAi(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.sendMessageToAi(text)
        }
    }

    // --- Admin Fleet Management ---

    fun addBus(number: String, driver: String, phone: String, routeId: String) {
        viewModelScope.launch {
            repository.addBus(number, driver, phone, routeId)
        }
    }

    fun broadcastAnnouncement(title: String, body: String) {
        viewModelScope.launch {
            repository.sendBroadcastNotification(title, body)
        }
    }

    // --- Saved Campus Stop Preference ---

    fun updateSavedCampusStop(stop: String) {
        viewModelScope.launch {
            repository.updateSavedCampusStop(stop)
        }
    }

    // --- Scheduled Arrival Times (MongoDB Backend Route) ---

    fun saveScheduledArrival(arrival: ScheduledArrival) {
        viewModelScope.launch {
            repository.saveScheduledArrival(arrival)
        }
    }

    fun getScheduledArrivalsForRoute(routeId: String, onResult: (List<ScheduledArrival>) -> Unit) {
        viewModelScope.launch {
            val result = repository.getScheduledArrivalsForRoute(routeId)
            onResult(result)
        }
    }

    // --- User Profile Operations ---

    fun fetchAllUsers() {
        viewModelScope.launch {
            _allUsers.value = repository.getAllUserProfiles()
        }
    }

    fun assignStudentToBus(studentId: String, busId: String) {
        viewModelScope.launch {
            repository.assignStudentToBus(studentId, busId)
            fetchAllUsers()
        }
    }

    fun addDriverProfile(name: String, email: String, phone: String) {
        viewModelScope.launch {
            repository.addDriverProfile(name, email, phone)
            fetchAllUsers()
        }
    }

    fun addParentProfile(name: String, email: String, phone: String, childName: String) {
        viewModelScope.launch {
            repository.addParentProfile(name, email, phone, childName)
            fetchAllUsers()
        }
    }

    fun addStudentProfile(name: String, email: String, phone: String, regNo: String, busId: String) {
        viewModelScope.launch {
            repository.addStudentProfile(name, email, phone, regNo, busId)
            fetchAllUsers()
        }
    }
}
