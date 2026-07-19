package com.example.repositories

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.database.AppDatabase
import com.example.database.AttendanceEntity
import com.example.database.ChatEntity
import com.example.database.NotificationEntity
import com.example.models.AttendanceRecord
import com.example.models.AttendanceStatus
import com.example.models.Bus
import com.example.models.BusRoute
import com.example.models.BusStatus
import com.example.models.ChatMessage
import com.example.models.EmergencyAlert
import com.example.models.NotificationCategory
import com.example.models.NotificationMessage
import com.example.models.RouteStop
import com.example.models.ScheduledArrival
import com.example.models.UserProfile
import com.example.models.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import org.bson.Document
import kotlinx.coroutines.CoroutineScope

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// --- Gemini API Retrofit Setup ---

data class Part(val text: String? = null)

data class Content(val parts: List<Part>)

data class GenerateContentRequest(
    val contents: List<Content>
)

data class Candidate(val content: Content)

data class GenerateContentResponse(
    val candidates: List<Candidate>
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

class TrackingRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    // Firebase state fallbacks (if services are active or empty)
    private var firebaseAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null
    private var mongoDatabase: MongoDatabase? = null


    init {
        try {
            firebaseAuth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w("TrackingRepository", "Firebase not initialized, defaulting to offline-first simulation: ${e.message}")
        }
    }

    // Dynamic states
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _buses = MutableStateFlow<List<Bus>>(emptyList())
    val buses: StateFlow<List<Bus>> = _buses.asStateFlow()

    private val _routes = MutableStateFlow<List<BusRoute>>(emptyList())
    val routes: StateFlow<List<BusRoute>> = _routes.asStateFlow()

    private val _emergencyAlerts = MutableStateFlow<List<EmergencyAlert>>(emptyList())
    val emergencyAlerts: StateFlow<List<EmergencyAlert>> = _emergencyAlerts.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationMessage>>(emptyList())
    val notifications: StateFlow<List<NotificationMessage>> = _notifications.asStateFlow()

    private val _attendanceRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendanceRecords: StateFlow<List<AttendanceRecord>> = _attendanceRecords.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _scheduledArrivals = MutableStateFlow<List<ScheduledArrival>>(emptyList())
    val scheduledArrivals: StateFlow<List<ScheduledArrival>> = _scheduledArrivals.asStateFlow()

    // Mock Users database for immediate login in browser streaming emulator
    private val mockProfiles = listOf(
        UserProfile(
            uid = "student-1",
            name = "Arun Kumar",
            email = "student@college.edu",
            role = UserRole.STUDENT,
            phoneNumber = "+91 9443210987",
            assignedBusId = "bus-1",
            regNo = "CS2026402",
            profilePhoto = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&h=100&fit=crop"
        ),
        UserProfile(
            uid = "parent-1",
            name = "Ramanathan K.",
            email = "parent@college.edu",
            role = UserRole.PARENT,
            phoneNumber = "+91 9840123456",
            assignedBusId = "bus-1",
            studentName = "Arun Kumar",
            profilePhoto = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100&h=100&fit=crop"
        ),
        UserProfile(
            uid = "driver-1",
            name = "Driver Selvam",
            email = "driver@college.edu",
            role = UserRole.DRIVER,
            phoneNumber = "+91 9772134567",
            assignedBusId = "bus-1",
            profilePhoto = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=100&h=100&fit=crop"
        ),
        UserProfile(
            uid = "admin-1",
            name = "Transport Admin Sridhar",
            email = "admin@college.edu",
            role = UserRole.ADMIN,
            phoneNumber = "+91 9003214567",
            profilePhoto = "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=100&h=100&fit=crop"
        )
    )

    init {
        // Initialize base routes and buses
        setupInitialData()
        loadLocalData()
        initMongoDb()
    }

    private fun setupInitialData() {
        val campusLat = 13.0118
        val campusLng = 80.2354 // IIT Madras campus as a beautiful geographic reference point

        val stopsRoute1 = listOf(
            RouteStop("Main Gate", 13.0064, 80.2443, "08:15 AM"),
            RouteStop("Gajendra Circle", 13.0105, 80.2402, "08:22 AM"),
            RouteStop("Hostel Zone", 13.0142, 80.2309, "08:28 AM"),
            RouteStop("Adyar Depot Stop", 13.0035, 80.2520, "08:40 AM"),
            RouteStop("Tidel Park Circle", 12.9890, 80.2464, "08:50 AM")
        )

        val stopsRoute2 = listOf(
            RouteStop("Tambaram Station", 12.9250, 80.1200, "07:45 AM"),
            RouteStop("Chromepet Bus Bay", 12.9510, 80.1410, "07:55 AM"),
            RouteStop("Pallavaram Jn", 12.9680, 80.1650, "08:05 AM"),
            RouteStop("Guindy Kathipara", 13.0080, 80.2050, "08:25 AM"),
            RouteStop("Campus Main Block", campusLat, campusLng, "08:45 AM")
        )

        val path1 = listOf(
            Pair(13.0064, 80.2443),
            Pair(13.0080, 80.2425),
            Pair(13.0105, 80.2402),
            Pair(13.0120, 80.2350),
            Pair(13.0142, 80.2309),
            Pair(13.0035, 80.2520),
            Pair(12.9890, 80.2464)
        )

        val path2 = listOf(
            Pair(12.9250, 80.1200),
            Pair(12.9510, 80.1410),
            Pair(12.9680, 80.1650),
            Pair(13.0080, 80.2050),
            Pair(13.0118, 80.2354)
        )

        _routes.value = listOf(
            BusRoute("route-1", "Adyar-Campus Shuttle", stopsRoute1, path1),
            BusRoute("route-2", "Tambaram Express Line", stopsRoute2, path2)
        )

        _scheduledArrivals.value = listOf(
            ScheduledArrival("sa-1", "route-1", "bus-1", "Main Gate", "08:15 AM"),
            ScheduledArrival("sa-2", "route-1", "bus-1", "Gajendra Circle", "08:22 AM"),
            ScheduledArrival("sa-3", "route-1", "bus-1", "Hostel Zone", "08:28 AM"),
            ScheduledArrival("sa-4", "route-1", "bus-1", "Adyar Depot Stop", "08:40 AM"),
            ScheduledArrival("sa-5", "route-1", "bus-1", "Tidel Park Circle", "08:50 AM"),
            ScheduledArrival("sa-6", "route-2", "bus-2", "Tambaram Station", "07:45 AM"),
            ScheduledArrival("sa-7", "route-2", "bus-2", "Chromepet Bus Bay", "07:55 AM"),
            ScheduledArrival("sa-8", "route-2", "bus-2", "Pallavaram Jn", "08:05 AM"),
            ScheduledArrival("sa-9", "route-2", "bus-2", "Guindy Kathipara", "08:25 AM"),
            ScheduledArrival("sa-10", "route-2", "bus-2", "Campus Main Block", "08:45 AM")
        )

        _buses.value = listOf(
            Bus(
                id = "bus-1",
                number = "TN-07-CS-4201",
                driverId = "driver-1",
                driverName = "Driver Selvam",
                driverPhone = "+91 9772134567",
                routeId = "route-1",
                routeName = "Adyar-Campus Shuttle",
                status = BusStatus.RUNNING,
                speedKmh = 45,
                currentLat = 13.0105,
                currentLng = 80.2402,
                etaMinutes = 12,
                nextStop = "Hostel Zone",
                totalCapacity = 50,
                activeBoarded = 32,
                batteryPercent = 92
            ),
            Bus(
                id = "bus-2",
                number = "TN-11-AA-9876",
                driverId = "driver-2",
                driverName = "Driver Mani",
                driverPhone = "+91 9845566778",
                routeId = "route-2",
                routeName = "Tambaram Express Line",
                status = BusStatus.DELAYED,
                speedKmh = 15,
                currentLat = 12.9680,
                currentLng = 80.1650,
                etaMinutes = 28,
                nextStop = "Guindy Kathipara",
                totalCapacity = 60,
                activeBoarded = 54,
                batteryPercent = 68
            )
        )

        _emergencyAlerts.value = listOf(
            EmergencyAlert(
                id = "sos-1",
                busId = "bus-2",
                busNumber = "TN-11-AA-9876",
                driverName = "Driver Mani",
                latitude = 12.9680,
                longitude = 80.1650,
                message = "Heavy traffic congestion & flat tire alert reported near Pallavaram Jn.",
                timestamp = "08:10 AM",
                isResolved = false
            )
        )
    }

    private fun loadLocalData() {
        scope.launch {
            // Notifications Flow to StateFlow
            db.notificationDao().getAllNotifications().collect { list ->
                if (list.isEmpty()) {
                    // Populate initial notification templates
                    val initialList = listOf(
                        NotificationEntity("n-1", "Bus Trip Started", "Bus TN-07-CS-4201 has left the first stop Main Gate on route Adyar-Campus Shuttle.", getCurrentTimeFormatted(), "STARTED"),
                        NotificationEntity("n-2", "ETA Update", "Your bus has changed speed. Current ETA to Campus: 12 mins.", getCurrentTimeFormatted(), "NEAR_STOP"),
                        NotificationEntity("n-3", "Route Delay", "Bus TN-11-AA-9876 is experiencing a minor 10 mins delay due to high traffic.", getCurrentTimeFormatted(), "DELAYED")
                    )
                    for (entity in initialList) {
                        db.notificationDao().insertNotification(entity)
                    }
                } else {
                    _notifications.value = list.map {
                        NotificationMessage(
                            id = it.id,
                            title = it.title,
                            body = it.body,
                            timestamp = it.timestamp,
                            category = NotificationCategory.valueOf(it.category),
                            isRead = it.isRead == 1
                        )
                    }
                }
            }
        }

        scope.launch {
            // Attendance Flow to StateFlow
            db.attendanceDao().getAllRecords().collect { list ->
                if (list.isEmpty()) {
                    val initialList = listOf(
                        AttendanceEntity("att-1", "student-1", "Arun Kumar", "bus-1", getCurrentDateFormatted(), "08:24 AM", "BOARDED", "Gajendra Circle")
                    )
                    for (entity in initialList) {
                        db.attendanceDao().insertRecord(entity)
                    }
                } else {
                    _attendanceRecords.value = list.map {
                        AttendanceRecord(
                            id = it.id,
                            studentId = it.studentId,
                            studentName = it.studentName,
                            busId = it.busId,
                            date = it.date,
                            time = it.time,
                            status = AttendanceStatus.valueOf(it.status),
                            stopName = it.stopName
                        )
                    }
                }
            }
        }

        scope.launch {
            // Chat history Flow to StateFlow
            db.chatDao().getChatHistory().collect { list ->
                if (list.isEmpty()) {
                    val initialList = listOf(
                        ChatEntity(sender = "ai", content = "Hello! I am your Smart College Bus Assistant. Ask me anything like: 'Where is Bus 1?', 'How is driver Selvam performing?', or 'Will Tambaram line be late?'", timestamp = "08:00 AM")
                    )
                    for (entity in initialList) {
                        db.chatDao().insertMessage(entity)
                    }
                } else {
                    _chatMessages.value = list.map {
                        ChatMessage(
                            id = it.id.toString(),
                            sender = it.sender,
                            content = it.content,
                            timestamp = it.timestamp
                        )
                    }
                }
            }
        }
    }

    private val dynamicProfiles = mutableListOf<UserProfile>().apply {
        addAll(mockProfiles)
    }

    // --- Authentication ---

    suspend fun login(email: String, password: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim()
        
        // Basic validations
        if (cleanEmail.isEmpty()) {
            return@withContext Result.failure(Exception("Email address cannot be empty."))
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return@withContext Result.failure(Exception("Please enter a valid email address."))
        }
        if (password.isEmpty()) {
            return@withContext Result.failure(Exception("Password cannot be empty."))
        }

        // Try MongoDB Database first for dynamic user validation
        val db = mongoDatabase
        if (db != null) {
            try {
                Log.i("TrackingRepository", "Checking credentials in MongoDB for: $cleanEmail")
                val usersCol = db.getCollection("users")
                
                // Case-insensitive query for email
                val filter = Document("email", Document("\$regex", "^" + java.util.regex.Pattern.quote(cleanEmail) + "$").append("\$options", "i"))
                val doc = usersCol.find(filter).first()
                
                if (doc != null) {
                    val dbPassword = doc.getString("password") ?: ""
                    if (dbPassword == password) {
                        val profile = UserProfile(
                            uid = doc.getString("_id") ?: doc.getString("uid") ?: UUID.randomUUID().toString(),
                            name = doc.getString("name") ?: "",
                            email = doc.getString("email") ?: cleanEmail,
                            role = UserRole.valueOf(doc.getString("role") ?: "STUDENT"),
                            phoneNumber = doc.getString("phoneNumber") ?: "",
                            assignedBusId = doc.getString("assignedBusId") ?: "bus-1",
                            studentName = doc.getString("studentName"),
                            regNo = doc.getString("regNo"),
                            profilePhoto = doc.getString("profilePhoto") ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100&h=100&fit=crop"
                        )
                        _currentUser.value = profile
                        Log.i("TrackingRepository", "Successfully validated credentials via MongoDB!")
                        return@withContext Result.success(profile)
                    } else {
                        return@withContext Result.failure(Exception("Incorrect password. Please verify and try again."))
                    }
                } else {
                    return@withContext Result.failure(Exception("No account found with this email. Please register first."))
                }
            } catch (e: Exception) {
                Log.e("TrackingRepository", "MongoDB authentication failed, falling back: ${e.message}")
            }
        }

        // Try Firebase Authentication
        val auth = firebaseAuth
        if (auth != null) {
            try {
                val taskResult = auth.signInWithEmailAndPassword(cleanEmail, password)
                val firebaseUser = taskResult.result?.user
                if (firebaseUser != null) {
                    val profile = dynamicProfiles.find { it.email.equals(cleanEmail, ignoreCase = true) } ?: UserProfile(
                        uid = firebaseUser.uid,
                        name = firebaseUser.displayName ?: "College User",
                        email = firebaseUser.email ?: cleanEmail,
                        role = UserRole.STUDENT,
                        phoneNumber = "+91 9999999999"
                    )
                    _currentUser.value = profile
                    return@withContext Result.success(profile)
                }
            } catch (e: Exception) {
                Log.w("TrackingRepository", "Firebase auth fallback failed: ${e.message}")
            }
        }

        // In-Memory/Offline validation fallback
        val matchedProfile = dynamicProfiles.find {
            it.email.equals(cleanEmail, ignoreCase = true)
        }

        if (matchedProfile != null) {
            if (password.length >= 6) {
                _currentUser.value = matchedProfile
                Result.success(matchedProfile)
            } else {
                Result.failure(Exception("Incorrect password. Please try again."))
            }
        } else {
            Result.failure(Exception("Invalid credentials. Try registering a new account or sign in with student@college.edu."))
        }
    }

    suspend fun register(
        name: String, 
        email: String, 
        role: UserRole, 
        phone: String, 
        additional: String, 
        password: String
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        val cleanPhone = phone.trim()
        val cleanAdditional = additional.trim()
        
        // Strict Field Validation
        if (cleanName.length < 2) {
            return@withContext Result.failure(Exception("Full name must be at least 2 characters long."))
        }
        if (cleanEmail.isEmpty()) {
            return@withContext Result.failure(Exception("Email address cannot be empty."))
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return@withContext Result.failure(Exception("Please enter a valid email address."))
        }
        if (cleanPhone.isEmpty()) {
            return@withContext Result.failure(Exception("Phone number cannot be empty."))
        }
        if (cleanPhone.length < 10) {
            return@withContext Result.failure(Exception("Please enter a valid phone number (at least 10 digits)."))
        }
        if (role == UserRole.STUDENT && cleanAdditional.isEmpty()) {
            return@withContext Result.failure(Exception("Register Number/ID is required for Students."))
        }
        if (role == UserRole.PARENT && cleanAdditional.length < 2) {
            return@withContext Result.failure(Exception("Child's full name must be at least 2 characters long."))
        }
        if (password.length < 6) {
            return@withContext Result.failure(Exception("Password must be at least 6 characters long."))
        }

        // Try MongoDB Database insert & email verification
        val db = mongoDatabase
        if (db != null) {
            try {
                val usersCol = db.getCollection("users")
                
                // Verify duplicate email
                val filter = Document("email", Document("\$regex", "^" + java.util.regex.Pattern.quote(cleanEmail) + "$").append("\$options", "i"))
                val existing = usersCol.find(filter).first()
                if (existing != null) {
                    return@withContext Result.failure(Exception("An account with this email address is already registered."))
                }
                
                val uid = "user-" + UUID.randomUUID().toString().take(6)
                val avatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100&h=100&fit=crop"
                
                val doc = Document()
                    .append("_id", uid)
                    .append("name", cleanName)
                    .append("email", cleanEmail)
                    .append("role", role.name)
                    .append("phoneNumber", cleanPhone)
                    .append("assignedBusId", "bus-1")
                    .append("studentName", if (role == UserRole.PARENT) cleanAdditional else null)
                    .append("regNo", if (role == UserRole.STUDENT) cleanAdditional else null)
                    .append("password", password)
                    .append("profilePhoto", avatar)
                    
                usersCol.insertOne(doc)
                Log.i("TrackingRepository", "Successfully registered new user in MongoDB!")
                
                val profile = UserProfile(
                    uid = uid,
                    name = cleanName,
                    email = cleanEmail,
                    role = role,
                    phoneNumber = cleanPhone,
                    assignedBusId = "bus-1",
                    studentName = if (role == UserRole.PARENT) cleanAdditional else null,
                    regNo = if (role == UserRole.STUDENT) cleanAdditional else null,
                    profilePhoto = avatar
                )
                
                // Also add to memory fallback list
                dynamicProfiles.add(profile)
                _currentUser.value = profile
                return@withContext Result.success(profile)
            } catch (e: Exception) {
                Log.e("TrackingRepository", "Failed to register user in MongoDB Atlas: ${e.message}", e)
            }
        }

        // Check duplicates in memory
        val existingInMemory = dynamicProfiles.any { it.email.equals(cleanEmail, ignoreCase = true) }
        if (existingInMemory) {
            return@withContext Result.failure(Exception("An account with this email address is already registered."))
        }

        // Local dynamic fallback
        val uid = "user-" + UUID.randomUUID().toString().take(6)
        val avatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100&h=100&fit=crop"
        val profile = UserProfile(
            uid = uid,
            name = cleanName,
            email = cleanEmail,
            role = role,
            phoneNumber = cleanPhone,
            assignedBusId = "bus-1",
            studentName = if (role == UserRole.PARENT) cleanAdditional else null,
            regNo = if (role == UserRole.STUDENT) cleanAdditional else null,
            profilePhoto = avatar
        )
        dynamicProfiles.add(profile)
        _currentUser.value = profile
        Result.success(profile)
    }

    fun logout() {
        _currentUser.value = null
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            Log.e("TrackingRepository", "Error signing out: ${e.message}")
        }
    }

    // --- Driver Controls ---

    suspend fun updateBusStatus(busId: String, status: BusStatus) = withContext(Dispatchers.IO) {
        _buses.value = _buses.value.map {
            if (it.id == busId) {
                val updatedBus = it.copy(status = status)
                updateMongoBus(updatedBus)
                updatedBus
            } else it
        }
        // Save alert if DELAYED or BREAKDOWN
        if (status == BusStatus.BREAKDOWN) {
            triggerSOS(busId, "Mechanical breakdown reported by driver.")
        }
    }

    private var lastAlertTime = 0L

    suspend fun updateDriverGPS(busId: String, lat: Double, lng: Double, speed: Int) = withContext(Dispatchers.IO) {
        _buses.value = _buses.value.map {
            if (it.id == busId) {
                val updatedEta = if (speed > 40) (it.etaMinutes - 1).coerceAtLeast(1) else (it.etaMinutes + 1)
                val updatedBus = it.copy(currentLat = lat, currentLng = lng, speedKmh = speed, etaMinutes = updatedEta)
                updateMongoBus(updatedBus)
                checkProximityAlert(updatedBus)
                updatedBus
            } else it
        }
    }

    private fun checkProximityAlert(bus: Bus) {
        val user = _currentUser.value ?: return
        val assignedBus = user.assignedBusId ?: "bus-1"
        if (bus.id != assignedBus) return

        val stopName = user.savedCampusStop ?: "Hostel Zone"
        val route = _routes.value.find { it.id == bus.routeId } ?: return
        val stop = route.stops.find { it.name.equals(stopName, ignoreCase = true) } ?: return

        val distKm = calculateDistanceInKm(bus.currentLat, bus.currentLng, stop.latitude, stop.longitude)
        val speed = if (bus.speedKmh > 5) bus.speedKmh.toDouble() else 30.0
        val minutesAway = (distKm / speed) * 60.0

        if (minutesAway <= 5.2 && System.currentTimeMillis() - lastAlertTime > 120000) {
            lastAlertTime = System.currentTimeMillis()
            scope.launch {
                val notify = NotificationEntity(
                    id = "proximity-notify-" + UUID.randomUUID().toString().take(4),
                    title = "Proximity Alert: 5m Away",
                    body = "Bus ${bus.number} is approx. 5 minutes away from your saved stop '$stopName' (Distance: ${String.format("%.2f", distKm)} km).",
                    timestamp = getCurrentTimeFormatted(),
                    category = "NEAR_STOP"
                )
                db.notificationDao().insertNotification(notify)
                saveMongoNotification(notify.title, notify.body, "NEAR_STOP")
            }
        }
    }

    private fun calculateDistanceInKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    suspend fun updateSavedCampusStop(stop: String) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        val updatedUser = user.copy(savedCampusStop = stop)
        _currentUser.value = updatedUser
        val db = mongoDatabase
        if (db != null) {
            try {
                val usersCol = db.getCollection("users")
                usersCol.updateOne(
                    Document("_id", user.uid),
                    Document("\$set", Document("savedCampusStop", stop))
                )
            } catch (e: Exception) {
                Log.e("TrackingRepository", "Failed to update saved stop in MongoDB: ${e.message}")
            }
        }
    }

    suspend fun saveScheduledArrival(arrival: ScheduledArrival) = withContext(Dispatchers.IO) {
        _scheduledArrivals.value = _scheduledArrivals.value.filter { it.id != arrival.id } + arrival
        val db = mongoDatabase ?: return@withContext
        try {
            val arrivalsCol = db.getCollection("scheduled_arrivals")
            val doc = Document()
                .append("_id", arrival.id)
                .append("routeId", arrival.routeId)
                .append("busId", arrival.busId)
                .append("stopName", arrival.stopName)
                .append("scheduledTime", arrival.scheduledTime)
                .append("estimatedTime", arrival.estimatedTime)
            arrivalsCol.updateOne(
                Document("_id", arrival.id),
                Document("\$set", doc),
                com.mongodb.client.model.UpdateOptions().upsert(true)
            )
        } catch (e: Exception) {
            Log.e("TrackingRepository", "Failed to save scheduled arrival in MongoDB: ${e.message}")
        }
    }

    suspend fun getScheduledArrivalsForRoute(routeId: String): List<ScheduledArrival> = withContext(Dispatchers.IO) {
        val db = mongoDatabase
        if (db != null) {
            try {
                val arrivalsCol = db.getCollection("scheduled_arrivals")
                val list = mutableListOf<ScheduledArrival>()
                arrivalsCol.find(Document("routeId", routeId)).forEach { doc ->
                    list.add(
                        ScheduledArrival(
                            id = doc.getString("_id") ?: UUID.randomUUID().toString(),
                            routeId = doc.getString("routeId") ?: "",
                            busId = doc.getString("busId") ?: "",
                            stopName = doc.getString("stopName") ?: "",
                            scheduledTime = doc.getString("scheduledTime") ?: "",
                            estimatedTime = doc.getString("estimatedTime")
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    return@withContext list
                }
            } catch (e: Exception) {
                Log.e("TrackingRepository", "Failed to retrieve scheduled arrivals from MongoDB: ${e.message}")
            }
        }
        return@withContext _scheduledArrivals.value.filter { it.routeId == routeId }
    }

    suspend fun triggerSOS(busId: String, message: String) = withContext(Dispatchers.IO) {
        val bus = _buses.value.find { it.id == busId } ?: return@withContext
        val alert = EmergencyAlert(
            id = "sos-" + UUID.randomUUID().toString().take(4),
            busId = busId,
            busNumber = bus.number,
            driverName = bus.driverName,
            latitude = bus.currentLat,
            longitude = bus.currentLng,
            message = message,
            timestamp = getCurrentTimeFormatted(),
            isResolved = false
        )
        _emergencyAlerts.value = listOf(alert) + _emergencyAlerts.value
        saveMongoSOS(alert)

        // Trigger alarm Notification
        val notify = NotificationEntity(
            id = "sos-notify-" + UUID.randomUUID().toString().take(4),
            title = "CRITICAL EMERGENCY ALERT",
            body = "${bus.number} reports SOS: $message",
            timestamp = getCurrentTimeFormatted(),
            category = "EMERGENCY"
        )
        db.notificationDao().insertNotification(notify)
        saveMongoNotification(notify.title, notify.body, "EMERGENCY")
    }

    suspend fun resolveSOS(sosId: String) = withContext(Dispatchers.IO) {
        _emergencyAlerts.value = _emergencyAlerts.value.map {
            if (it.id == sosId) {
                resolveMongoSOS(sosId)
                it.copy(isResolved = true)
            } else it
        }
    }

    // --- QR / Face Attendance ---

    suspend fun scanQRAttendance(studentId: String, busId: String, status: AttendanceStatus, stopName: String): Result<AttendanceRecord> = withContext(Dispatchers.IO) {
        val studentName = if (studentId == "student-1") "Arun Kumar" else "Student User"
        val record = AttendanceEntity(
            id = "att-" + UUID.randomUUID().toString().take(6),
            studentId = studentId,
            studentName = studentName,
            busId = busId,
            date = getCurrentDateFormatted(),
            time = getCurrentTimeFormatted(),
            status = status.name,
            stopName = stopName
        )
        db.attendanceDao().insertRecord(record)

        // Increment bus active boarded count
        _buses.value = _buses.value.map {
            if (it.id == busId) {
                val newCount = if (status == AttendanceStatus.BOARDED) (it.activeBoarded + 1).coerceAtMost(it.totalCapacity) else (it.activeBoarded - 1).coerceAtLeast(0)
                val updatedBus = it.copy(activeBoarded = newCount)
                updateMongoBus(updatedBus)
                updatedBus
            } else it
        }

        // Send Notification to parent
        val title = if (status == AttendanceStatus.BOARDED) "Child Boarded Bus" else "Child Safely De-boarded"
        val body = "Your child $studentName has $status at $stopName stop at ${record.time}."
        val parentNotification = NotificationEntity(
            id = "n-att-" + UUID.randomUUID().toString().take(4),
            title = title,
            body = body,
            timestamp = getCurrentTimeFormatted(),
            category = "ATTENDANCE"
        )
        db.notificationDao().insertNotification(parentNotification)
        saveMongoNotification(title, body, "ATTENDANCE")

        val fullRecord = AttendanceRecord(record.id, record.studentId, record.studentName, record.busId, record.date, record.time, status, record.stopName)
        saveMongoAttendance(fullRecord)
        Result.success(fullRecord)
    }

    // --- Admin Dashboard Actions ---

    suspend fun addBus(number: String, driver: String, phone: String, routeId: String) = withContext(Dispatchers.IO) {
        val route = _routes.value.find { it.id == routeId }
        val newBus = Bus(
            id = "bus-" + UUID.randomUUID().toString().take(4),
            number = number,
            driverId = "driver-gen",
            driverName = driver,
            driverPhone = phone,
            routeId = routeId,
            routeName = route?.name ?: "Adyar Shuttle",
            status = BusStatus.IDLE,
            speedKmh = 0,
            currentLat = 13.0064,
            currentLng = 80.2354,
            etaMinutes = 0,
            nextStop = "Main Gate",
            totalCapacity = 50,
            activeBoarded = 0
        )
        _buses.value = _buses.value + newBus
        saveMongoNewBus(newBus)
    }

    suspend fun getAllUserProfiles(): List<UserProfile> = withContext(Dispatchers.IO) {
        val db = mongoDatabase
        if (db != null) {
            try {
                val usersCol = db.getCollection("users")
                val list = mutableListOf<UserProfile>()
                usersCol.find().forEach { doc ->
                    val roleStr = doc.getString("role") ?: "STUDENT"
                    val role = try { UserRole.valueOf(roleStr) } catch(e: Exception) { UserRole.STUDENT }
                    list.add(
                        UserProfile(
                            uid = doc.getString("_id") ?: doc.getString("uid") ?: UUID.randomUUID().toString(),
                            name = doc.getString("name") ?: "",
                            email = doc.getString("email") ?: "",
                            role = role,
                            phoneNumber = doc.getString("phoneNumber") ?: "",
                            assignedBusId = doc.getString("assignedBusId"),
                            studentName = doc.getString("studentName"),
                            regNo = doc.getString("regNo"),
                            profilePhoto = doc.getString("profilePhoto") ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100&h=100&fit=crop"
                        )
                    )
                }
                if (list.isNotEmpty()) return@withContext list
            } catch (e: Exception) {
                Log.e("TrackingRepository", "Failed to retrieve users from MongoDB: ${e.message}")
            }
        }
        return@withContext mockProfiles
    }

    suspend fun assignStudentToBus(studentId: String, busId: String) = withContext(Dispatchers.IO) {
        val db = mongoDatabase
        if (db != null) {
            try {
                val usersCol = db.getCollection("users")
                usersCol.updateOne(
                    Document("_id", studentId),
                    Document("\$set", Document("assignedBusId", busId))
                )
            } catch (e: Exception) {
                Log.e("TrackingRepository", "Failed to update assigned bus in MongoDB: ${e.message}")
            }
        }
        // Also update local current user if applicable
        val current = _currentUser.value
        if (current != null && current.uid == studentId) {
            _currentUser.value = current.copy(assignedBusId = busId)
        }
    }

    suspend fun addDriverProfile(name: String, email: String, phone: String) = withContext(Dispatchers.IO) {
        val db = mongoDatabase ?: return@withContext
        try {
            val usersCol = db.getCollection("users")
            val uid = "driver-" + UUID.randomUUID().toString().take(4)
            val doc = Document()
                .append("_id", uid)
                .append("name", name)
                .append("email", email)
                .append("role", UserRole.DRIVER.name)
                .append("phoneNumber", phone)
                .append("assignedBusId", "bus-1")
                .append("password", "123456")
                .append("profilePhoto", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=100")
            usersCol.insertOne(doc)
        } catch (e: Exception) {
            Log.e("TrackingRepository", "Failed to add driver in MongoDB: ${e.message}")
        }
    }

    suspend fun addParentProfile(name: String, email: String, phone: String, childName: String) = withContext(Dispatchers.IO) {
        val db = mongoDatabase ?: return@withContext
        try {
            val usersCol = db.getCollection("users")
            val uid = "parent-" + UUID.randomUUID().toString().take(4)
            val doc = Document()
                .append("_id", uid)
                .append("name", name)
                .append("email", email)
                .append("role", UserRole.PARENT.name)
                .append("phoneNumber", phone)
                .append("studentName", childName)
                .append("assignedBusId", "bus-1")
                .append("password", "123456")
                .append("profilePhoto", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100")
            usersCol.insertOne(doc)
        } catch (e: Exception) {
            Log.e("TrackingRepository", "Failed to add parent in MongoDB: ${e.message}")
        }
    }

    suspend fun addStudentProfile(name: String, email: String, phone: String, regNo: String, busId: String) = withContext(Dispatchers.IO) {
        val db = mongoDatabase ?: return@withContext
        try {
            val usersCol = db.getCollection("users")
            val uid = "student-" + UUID.randomUUID().toString().take(4)
            val doc = Document()
                .append("_id", uid)
                .append("name", name)
                .append("email", email)
                .append("role", UserRole.STUDENT.name)
                .append("phoneNumber", phone)
                .append("regNo", regNo)
                .append("assignedBusId", busId)
                .append("password", "123456")
                .append("profilePhoto", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100")
            usersCol.insertOne(doc)
        } catch (e: Exception) {
            Log.e("TrackingRepository", "Failed to add student in MongoDB: ${e.message}")
        }
    }

    suspend fun sendBroadcastNotification(title: String, body: String) = withContext(Dispatchers.IO) {
        val alert = NotificationEntity(
            id = "broad-" + UUID.randomUUID().toString().take(4),
            title = title,
            body = body,
            timestamp = getCurrentTimeFormatted(),
            category = "ANNOUNCEMENT"
        )
        db.notificationDao().insertNotification(alert)
        saveMongoNotification(title, body, "ANNOUNCEMENT")
    }


    // --- AI Smart Assistant with Gemini Integration ---

    suspend fun sendMessageToAi(text: String) = withContext(Dispatchers.IO) {
        // Log query locally
        db.chatDao().insertMessage(ChatEntity(sender = "user", content = text, timestamp = getCurrentTimeFormatted()))

        // Build precise system context based on live data
        val activeBusesContext = _buses.value.joinToString("\n") {
            "Bus ${it.id} (${it.number}) driven by ${it.driverName} (${it.driverPhone}) is on Route '${it.routeName}'. Status: ${it.status.name}, speed: ${it.speedKmh} km/h, current GPS: (${it.currentLat}, ${it.currentLng}), current ETA: ${it.etaMinutes} mins, next stop: ${it.nextStop}, Capacity: ${it.activeBoarded}/${it.totalCapacity}, Battery/Fuel: ${it.batteryPercent}%, Maintenance Due: ${it.maintenanceDue}."
        }

        val activeSOSContext = _emergencyAlerts.value.filter { !it.isResolved }.joinToString("\n") {
            "CRITICAL SOS: Bus ${it.busNumber} has an issue: ${it.message} at ${it.timestamp}."
        }

        val prompt = """
            You are the Smart College Bus tracking AI Assistant. Address the user.
            We are in the year 2026.
            Here is the real-time operational status of college buses and routes:
            $activeBusesContext
            
            Current Emergency Alerts:
            $activeSOSContext
            
            Student information:
            - Logged in profile: ${_currentUser.value?.name ?: "Arun Kumar"}
            - Phone: ${_currentUser.value?.phoneNumber ?: "Unknown"}
            - RegNo: ${_currentUser.value?.regNo ?: "Unknown"}
            - Assigned Bus: ${_currentUser.value?.assignedBusId ?: "bus-1"}
            
            The user says: "$text"
            
            Analyze the user prompt. Answer their query using the real-time context.
            Provide dynamic responses detailing estimated arrival times (ETAs), traffic predictions, suspicious routing checks, battery remaining or performance analyses when asked. Be supportive, concise, and highly professional.
        """.trimIndent()

        // Attempt Gemini API call via Retrofit
        var aiResponse = ""
        val geminiKey = BuildConfig.GEMINI_API_KEY
        if (geminiKey.isNotEmpty() && geminiKey != "MY_GEMINI_API_KEY") {
            try {
                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt))))
                )
                val okHttpClient = OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val service = Retrofit.Builder()
                    .baseUrl("https://generativelanguage.googleapis.com/")
                    .client(okHttpClient)
                    .addConverterFactory(MoshiConverterFactory.create())
                    .build()
                    .create(GeminiApiService::class.java)

                val response = service.generateContent(geminiKey, request)
                aiResponse = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            } catch (e: Exception) {
                Log.e("TrackingRepository", "Gemini API failed, using smart predictive fallback", e)
            }
        }

        // Smart predictive fallback if key is missing or failed
        if (aiResponse.isEmpty()) {
            aiResponse = getPredictiveResponse(text)
        }

        db.chatDao().insertMessage(ChatEntity(sender = "ai", content = aiResponse, timestamp = getCurrentTimeFormatted()))
    }

    private fun getPredictiveResponse(query: String): String {
        val q = query.lowercase()
        val bus1 = _buses.value.find { it.id == "bus-1" }
        val bus2 = _buses.value.find { it.id == "bus-2" }

        return when {
            q.contains("where") || q.contains("live") || q.contains("status") -> {
                "Bus 1 (${bus1?.number}) is currently near ${bus1?.nextStop} travelling at ${bus1?.speedKmh} km/h. It's in '${bus1?.status}' status, ETA is ${bus1?.etaMinutes} minutes to the main block. Bus 2 is near Pallavaram Jn, delayed with a flat tire alert."
            }
            q.contains("eta") || q.contains("arrive") || q.contains("when") -> {
                "Based on traffic analysis, Bus 1 is ${bus1?.etaMinutes} minutes away from its next destination (${bus1?.nextStop}). Bus 2 is experiencing a ${bus2?.etaMinutes} minutes delay due to tyre issues near Pallavaram Jn."
            }
            q.contains("delay") || q.contains("traffic") || q.contains("late") -> {
                "AI Traffic Core reports high density near Pallavaram Jn. Route 2 (Tambaram) has a delay of 28 minutes. Route 1 (Adyar) is clean with steady flow."
            }
            q.contains("driver") || q.contains("performance") || q.contains("selvam") -> {
                "Driver Selvam (Bus 1) has an exceptional score of 4.8/5.0. No speed breaches or harsh breaking triggers detected on today's trip log."
            }
            q.contains("electric") || q.contains("battery") || q.contains("health") -> {
                "Smart Battery Analytics: Bus 1 shows 92% battery remaining, healthy thermal level (31°C). Preventive maintenance scheduled on ${bus1?.maintenanceDue}."
            }
            q.contains("emergency") || q.contains("sos") || q.contains("accident") -> {
                "Yes, a critical SOS alert is active for Bus 2 (${bus2?.number}). The driver mani reported a tyre blowout. Emergency team has been dispatched."
            }
            else -> {
                "I am monitoring our smart college fleet in real time. We have 2 routes running right now. Bus 1 is operating normally with an ETA of 12 mins. Ask me any details about battery status, speed, driver details, or scheduling!"
            }
        }
    }

    // --- Helper Formatting Utilities ---

    private fun getCurrentTimeFormatted(): String {
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
    }

    private fun getCurrentDateFormatted(): String {
        return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
    }

    // --- MongoDB Core Integration & Sync ---

    private fun initMongoDb() {
        scope.launch {
            try {
                val rawUri = BuildConfig.MONGODB_URI
                if (rawUri.isNotEmpty() && !rawUri.contains("YOUR_") && rawUri != "MY_MONGODB_URI") {
                    Log.i("TrackingRepository", "Connecting to MongoDB Atlas... Raw URI length: ${rawUri.length}")
                    val connectionStringStr = if (rawUri.startsWith("mongodb+srv://")) {
                        Log.i("TrackingRepository", "Detected mongodb+srv:// scheme. Performing dynamic DNS-over-HTTPS SRV resolution...")
                        resolveSrvUri(rawUri)
                    } else {
                        rawUri
                    }
                    
                    Log.i("TrackingRepository", "Initializing ConnectionString...")
                    val connectionString = ConnectionString(connectionStringStr)
                    val settings = MongoClientSettings.builder()
                        .applyConnectionString(connectionString)
                        .build()
                    val mongoClient = MongoClients.create(settings)
                    mongoDatabase = mongoClient.getDatabase("smart_college_bus")
                    Log.i("TrackingRepository", "Successfully connected to MongoDB Atlas!")
                    syncInitialDataToMongo()
                } else {
                    Log.w("TrackingRepository", "MongoDB URI is missing or placeholder. Running in local-only simulation mode.")
                }
            } catch (e: Exception) {
                Log.e("TrackingRepository", "Failed to connect to MongoDB Atlas: ${e.message}", e)
            }
        }
    }

    private suspend fun resolveSrvUri(srvUri: String): String = withContext(Dispatchers.IO) {
        if (!srvUri.startsWith("mongodb+srv://")) {
            return@withContext srvUri
        }
        
        try {
            val cleanUri = srvUri.substring("mongodb+srv://".length)
            val lastAt = cleanUri.lastIndexOf('@')
            if (lastAt == -1) return@withContext srvUri
            
            val credentials = cleanUri.substring(0, lastAt)
            val remainder = cleanUri.substring(lastAt + 1)
            
            // Extract domain and query params
            val slashIdx = remainder.indexOf('/')
            val qIdx = remainder.indexOf('?')
            
            val endDomainIdx = when {
                slashIdx != -1 && qIdx != -1 -> minOf(slashIdx, qIdx)
                slashIdx != -1 -> slashIdx
                qIdx != -1 -> qIdx
                else -> remainder.length
            }
            
            val domain = remainder.substring(0, endDomainIdx)
            
            val queryParams = if (qIdx != -1 && qIdx < remainder.length - 1) {
                remainder.substring(qIdx + 1)
            } else ""
            
            Log.i("TrackingRepository", "Resolving SRV records for domain: $domain")
            
            // Step 1: Query SRV
            val srvJson = queryDoh("_mongodb._tcp.$domain", "SRV")
            val srvRegex = """"data"\s*:\s*"([^"]+)"""".toRegex()
            val hosts = srvRegex.findAll(srvJson).map { match ->
                val data = match.groupValues[1]
                val parts = data.trim().split("\\s+".toRegex())
                if (parts.size >= 4) {
                    val port = parts[2]
                    val host = parts[3].removeSuffix(".")
                    "$host:$port"
                } else null
            }.filterNotNull().toList()
            
            if (hosts.isEmpty()) {
                Log.e("TrackingRepository", "No SRV hosts found in DNS response. Falling back to original URI.")
                return@withContext srvUri
            }
            
            val hostsCsv = hosts.joinToString(",")
            Log.i("TrackingRepository", "Resolved SRV hosts: $hostsCsv")
            
            // Step 2: Query TXT options
            val txtJson = queryDoh(domain, "TXT")
            val txtRegex = """"data"\s*:\s*"([^"]+)"""".toRegex()
            val txtOptions = txtRegex.find(txtJson)?.groupValues?.get(1)
                ?.replace("\\u0026", "&")
                ?.replace("\\\"", "")
                ?: ""
                
            // Build traditional connection string
            val srvHostsAndOptions = StringBuilder("mongodb://")
            srvHostsAndOptions.append(credentials).append("@").append(hostsCsv).append("/?")
            
            val optionsList = mutableListOf<String>()
            optionsList.add("ssl=true")
            if (txtOptions.isNotEmpty()) {
                optionsList.add(txtOptions)
            }
            if (queryParams.isNotEmpty()) {
                optionsList.add(queryParams)
            }
            
            srvHostsAndOptions.append(optionsList.joinToString("&"))
            val resolvedUri = srvHostsAndOptions.toString()
            Log.i("TrackingRepository", "Resolved SRV to standard URI successfully!")
            resolvedUri
        } catch (e: Exception) {
            Log.e("TrackingRepository", "Failed to dynamically resolve SRV URI: ${e.message}", e)
            srvUri
        }
    }

    private suspend fun queryDoh(name: String, type: String): String = withContext(Dispatchers.IO) {
        var connection: java.net.HttpURLConnection? = null
        try {
            val url = java.net.URL("https://dns.google/resolve?name=$name&type=$type")
            connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e("TrackingRepository", "DNS query failed for $name ($type): ${e.message}")
            ""
        } finally {
            connection?.disconnect()
        }
    }

    private suspend fun syncInitialDataToMongo() = withContext(Dispatchers.IO) {
        val db = mongoDatabase ?: return@withContext
        try {
            // Seed User Profiles to MongoDB
            val usersCol = db.getCollection("users")
            val usersCount = usersCol.countDocuments()
            if (usersCount == 0L) {
                Log.i("TrackingRepository", "Seeding initial user profiles to MongoDB 'users' collection...")
                mockProfiles.forEach { profile ->
                    val doc = Document()
                        .append("_id", profile.uid)
                        .append("name", profile.name)
                        .append("email", profile.email)
                        .append("role", profile.role.name)
                        .append("phoneNumber", profile.phoneNumber)
                        .append("assignedBusId", profile.assignedBusId ?: "bus-1")
                        .append("studentName", profile.studentName)
                        .append("regNo", profile.regNo)
                        .append("profilePhoto", profile.profilePhoto)
                        .append("password", "123456") // Standard seed password
                    usersCol.insertOne(doc)
                }
            } else {
                Log.i("TrackingRepository", "Found existing users in MongoDB 'users' collection. Count: $usersCount")
            }

            // Seed Buses to MongoDB
            val busesCol = db.getCollection("buses")
            val count = busesCol.countDocuments()
            if (count == 0L) {
                Log.i("TrackingRepository", "Seeding initial buses data to MongoDB...")
                _buses.value.forEach { bus ->
                    val doc = Document()
                        .append("_id", bus.id)
                        .append("number", bus.number)
                        .append("driverId", bus.driverId)
                        .append("driverName", bus.driverName)
                        .append("driverPhone", bus.driverPhone)
                        .append("routeId", bus.routeId)
                        .append("routeName", bus.routeName)
                        .append("status", bus.status.name)
                        .append("speedKmh", bus.speedKmh)
                        .append("currentLat", bus.currentLat)
                        .append("currentLng", bus.currentLng)
                        .append("etaMinutes", bus.etaMinutes)
                        .append("nextStop", bus.nextStop)
                        .append("totalCapacity", bus.totalCapacity)
                        .append("activeBoarded", bus.activeBoarded)
                        .append("batteryPercent", bus.batteryPercent)
                        .append("maintenanceDue", bus.maintenanceDue)
                        .append("occupancy", bus.occupancy)
                    busesCol.insertOne(doc)
                }
            } else {
                Log.i("TrackingRepository", "Found existing buses in MongoDB. Fetching from remote...")
                val fetchedBuses = mutableListOf<Bus>()
                busesCol.find().forEach { doc ->
                    fetchedBuses.add(
                        Bus(
                            id = doc.getString("_id") ?: UUID.randomUUID().toString(),
                            number = doc.getString("number") ?: "",
                            driverId = doc.getString("driverId") ?: "",
                            driverName = doc.getString("driverName") ?: "",
                            driverPhone = doc.getString("driverPhone") ?: "",
                            routeId = doc.getString("routeId") ?: "",
                            routeName = doc.getString("routeName") ?: "",
                            status = BusStatus.valueOf(doc.getString("status") ?: "IDLE"),
                            speedKmh = doc.getInteger("speedKmh") ?: 0,
                            currentLat = doc.getDouble("currentLat") ?: 0.0,
                            currentLng = doc.getDouble("currentLng") ?: 0.0,
                            etaMinutes = doc.getInteger("etaMinutes") ?: 0,
                            nextStop = doc.getString("nextStop") ?: "",
                            totalCapacity = doc.getInteger("totalCapacity") ?: 50,
                            activeBoarded = doc.getInteger("activeBoarded") ?: 0,
                            batteryPercent = doc.getInteger("batteryPercent") ?: 100,
                            maintenanceDue = doc.getString("maintenanceDue") ?: "No Schedule",
                            occupancy = doc.getString("occupancy") ?: "64%"
                        )
                    )
                }
                _buses.value = fetchedBuses
            }
        } catch (e: Exception) {
            Log.e("TrackingRepository", "Error syncing data to/from MongoDB: ${e.message}", e)
        }
    }

    private fun updateMongoBus(bus: Bus) {
        scope.launch {
            val db = mongoDatabase ?: return@launch
            try {
                val busesCol = db.getCollection("buses")
                val filter = Document("_id", bus.id)
                val update = Document("\$set", Document()
                    .append("status", bus.status.name)
                    .append("speedKmh", bus.speedKmh)
                    .append("currentLat", bus.currentLat)
                    .append("currentLng", bus.currentLng)
                    .append("etaMinutes", bus.etaMinutes)
                    .append("nextStop", bus.nextStop)
                    .append("activeBoarded", bus.activeBoarded)
                    .append("occupancy", bus.occupancy)
                )
                busesCol.updateOne(filter, update)
            } catch (e: Exception) {
                Log.e("TrackingRepository", "Failed to update bus in MongoDB: ${e.message}")
            }
        }
    }

    private fun saveMongoSOS(alert: EmergencyAlert) {
        scope.launch {
            val db = mongoDatabase ?: return@launch
            try {
                val sosCol = db.getCollection("emergency_alerts")
                val doc = Document()
                    .append("_id", alert.id)
                    .append("busId", alert.busId)
                    .append("busNumber", alert.busNumber)
                    .append("driverName", alert.driverName)
                    .append("latitude", alert.latitude)
                    .append("longitude", alert.longitude)
                    .append("message", alert.message)
                    .append("timestamp", alert.timestamp)
                    .append("isResolved", alert.isResolved)
                sosCol.insertOne(doc)
            } catch (e: Exception) {
                Log.e("TrackingRepository", "Failed to insert SOS to MongoDB: ${e.message}")
            }
        }
    }

    private fun resolveMongoSOS(sosId: String) {
        scope.launch {
            val db = mongoDatabase ?: return@launch
            try {
                val sosCol = db.getCollection("emergency_alerts")
                sosCol.updateOne(Document("_id", sosId), Document("\$set", Document("isResolved", true)))
            } catch (e: Exception) {
                Log.e("TrackingRepository", "Failed to resolve SOS in MongoDB: ${e.message}")
            }
        }
    }

    private fun saveMongoAttendance(record: AttendanceRecord) {
        scope.launch {
            val db = mongoDatabase ?: return@launch
            try {
                val attCol = db.getCollection("attendance")
                val doc = Document()
                    .append("_id", record.id)
                    .append("studentId", record.studentId)
                    .append("studentName", record.studentName)
                    .append("busId", record.busId)
                    .append("date", record.date)
                    .append("time", record.time)
                    .append("status", record.status.name)
                    .append("stopName", record.stopName)
                attCol.insertOne(doc)
            } catch (e: Exception) {
                Log.e("TrackingRepository", "Failed to save attendance to MongoDB: ${e.message}")
            }
        }
    }

    private fun saveMongoNotification(title: String, body: String, category: String) {
        scope.launch {
            val db = mongoDatabase ?: return@launch
            try {
                val notifyCol = db.getCollection("notifications")
                val doc = Document()
                    .append("title", title)
                    .append("body", body)
                    .append("timestamp", getCurrentTimeFormatted())
                    .append("category", category)
                notifyCol.insertOne(doc)
            } catch (e: Exception) {
                Log.e("TrackingRepository", "Failed to save notification to MongoDB: ${e.message}")
            }
        }
    }

    private fun saveMongoNewBus(bus: Bus) {
        scope.launch {
            val db = mongoDatabase ?: return@launch
            try {
                val busesCol = db.getCollection("buses")
                val doc = Document()
                    .append("_id", bus.id)
                    .append("number", bus.number)
                    .append("driverId", bus.driverId)
                    .append("driverName", bus.driverName)
                    .append("driverPhone", bus.driverPhone)
                    .append("routeId", bus.routeId)
                    .append("routeName", bus.routeName)
                    .append("status", bus.status.name)
                    .append("speedKmh", bus.speedKmh)
                    .append("currentLat", bus.currentLat)
                    .append("currentLng", bus.currentLng)
                    .append("etaMinutes", bus.etaMinutes)
                    .append("nextStop", bus.nextStop)
                    .append("totalCapacity", bus.totalCapacity)
                    .append("activeBoarded", bus.activeBoarded)
                    .append("batteryPercent", bus.batteryPercent)
                    .append("maintenanceDue", bus.maintenanceDue)
                    .append("occupancy", bus.occupancy)
                busesCol.insertOne(doc)
            } catch (e: Exception) {
                Log.e("TrackingRepository", "Failed to insert new bus into MongoDB: ${e.message}")
            }
        }
    }
}

