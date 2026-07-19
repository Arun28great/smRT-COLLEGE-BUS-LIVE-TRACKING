package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.models.UserRole
import com.example.ui.theme.*
import com.example.ui.viewmodels.TrackingViewModel
import kotlinx.coroutines.delay

// --- SPLASH SCREEN ---

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(2000)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BlueSecondary, BluePrimary)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsBus,
                contentDescription = "App Logo",
                tint = CyanPrimary,
                modifier = Modifier
                    .size(96.dp)
                    .testTag("splash_logo")
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "SMART TRACK",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 3.sp
            )
            Text(
                text = "AI College Bus Fleet",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryDark,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(48.dp))
            CircularProgressIndicator(
                color = CyanPrimary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

// --- ONBOARDING SCREEN ---

@Composable
fun OnboardingScreen(
    onOnboardingFinished: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }

    val slides = listOf(
        Triple(
            "Real-time Tracking",
            "Monitor your assigned college bus in real-time with high-precision GPS positioning and dynamic traffic estimates.",
            "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=300"
        ),
        Triple(
            "AI Assistant ETA",
            "Ask our Gemini AI engine when your bus will arrive, check traffic bottlenecks, or optimize your pick-up schedules instantly.",
            "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=300"
        ),
        Triple(
            "Secure QR Attendance",
            "Fast contactless onboarding. Students scan the QR code to confirm boarding, instantly notifying parents for complete safety.",
            "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=300"
        )
    )

    val currentSlide = slides[step]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Skip Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onOnboardingFinished) {
                    Text("Skip", color = BluePrimary, fontWeight = FontWeight.Bold)
                }
            }

            // Slide Illustration
            Card(
                modifier = Modifier
                    .size(240.dp)
                    .padding(8.dp)
                    .border(1.dp, ThemeBorder, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (step) {
                            0 -> Icons.Default.Map
                            1 -> Icons.Default.SmartToy
                            else -> Icons.Default.QrCodeScanner
                        },
                        contentDescription = "Illustration",
                        tint = BluePrimary,
                        modifier = Modifier.size(80.dp)
                    )
                }
            }

            // Info text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = currentSlide.first,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = currentSlide.second,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryDark,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }

            // Indicator dots and next buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    repeat(slides.size) { i ->
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(width = if (i == step) 20.dp else 8.dp, height = 8.dp)
                                .background(
                                    color = if (i == step) BluePrimary else Color.Gray.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }

                Button(
                    onClick = {
                        if (step < slides.size - 1) {
                            step++
                        } else {
                            onOnboardingFinished()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("onboarding_next"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Text(
                        text = if (step == slides.size - 1) "Get Started" else "Next",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// --- LOGIN SCREEN ---

@Composable
fun LoginScreen(
    viewModel: TrackingViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("student@college.edu") }
    var password by remember { mutableStateOf("password") }

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Sign in to track your college commute safely",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryDark
                )
            }

            // Login Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark, RoundedCornerShape(24.dp))
                    .border(1.5.dp, ThemeBorder, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("College Email", color = TextSecondaryDark) },
                    leadingIcon = { Icon(Icons.Default.Email, "Email", tint = BluePrimary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("email_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark,
                        focusedBorderColor = BluePrimary,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = TextSecondaryDark) },
                    leadingIcon = { Icon(Icons.Default.Lock, "Password", tint = BluePrimary) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark,
                        focusedBorderColor = BluePrimary,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Forgot Password?",
                        color = BluePrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .clickable { onNavigateToForgotPassword() }
                    )
                }

                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = EmergencyRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Button(
                    onClick = {
                        viewModel.loginUser(email, password) {
                            onLoginSuccess()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("login_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // Bottom Sign Up Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Don't have an account? ", color = TextSecondaryDark)
                Text(
                    text = "Register",
                    color = BluePrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToRegister() }
                )
            }
        }
    }
}

// --- REGISTER SCREEN ---

@Composable
fun RegisterScreen(
    viewModel: TrackingViewModel,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.STUDENT) }
    var additionalInfo by remember { mutableStateOf("") } // RegNo for student, child name for parent

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Create Account",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                modifier = Modifier.padding(top = 8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark, RoundedCornerShape(24.dp))
                    .border(1.5.dp, ThemeBorder, RoundedCornerShape(24.dp))
                    .padding(16.dp)
            ) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name", color = TextSecondaryDark) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark, focusedBorderColor = BluePrimary, unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f))
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("College Email Address", color = TextSecondaryDark) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark, focusedBorderColor = BluePrimary, unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f))
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Phone
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number", color = TextSecondaryDark) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark, focusedBorderColor = BluePrimary, unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f))
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Role Selector Chips
                Text("Select App Role:", color = TextSecondaryDark, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(UserRole.STUDENT, UserRole.PARENT, UserRole.DRIVER).forEach { role ->
                        FilterChip(
                            selected = selectedRole == role,
                            onClick = { selectedRole = role },
                            label = { Text(role.name, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = TextSecondaryDark,
                                selectedLabelColor = Color.White,
                                selectedContainerColor = BluePrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dynamic RegNo / Additional Info
                OutlinedTextField(
                    value = additionalInfo,
                    onValueChange = { additionalInfo = it },
                    label = {
                        Text(
                            text = if (selectedRole == UserRole.PARENT) "Child's Full Name" else "Register Number / ID",
                            color = TextSecondaryDark
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark, focusedBorderColor = BluePrimary, unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f))
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Create Password (min 6 char)", color = TextSecondaryDark) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark, focusedBorderColor = BluePrimary, unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f))
                )

                Spacer(modifier = Modifier.height(16.dp))

                errorMessage?.let { error ->
                    Text(text = error, color = EmergencyRed, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        viewModel.registerUser(name, email, selectedRole, phone, additionalInfo, password) {
                            onRegisterSuccess()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("register_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Create Profile", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Already registered? ", color = TextSecondaryDark)
                Text(
                    text = "Login",
                    color = BluePrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }
        }
    }
}

// --- FORGOT PASSWORD SCREEN ---

@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var alertSent by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .background(SurfaceDark, RoundedCornerShape(24.dp))
                .border(1.5.dp, ThemeBorder, RoundedCornerShape(24.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LockReset,
                contentDescription = "Reset Lock",
                tint = BluePrimary,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Reset Password",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Enter your registered college email. We will send an official OTP link to reset your transport dashboard access credentials.",
                fontSize = 12.sp,
                color = TextSecondaryDark,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (!alertSent) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("College Email", color = TextSecondaryDark) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark, focusedBorderColor = BluePrimary, unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f))
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { alertSent = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Text("Send Recovery Link", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, "Success", tint = SuccessGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Instructions have been dispatched to $email! Please inspect your college inbox.",
                            color = SuccessGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Return to Sign In",
                color = BluePrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onNavigateBack() }
                    .padding(8.dp)
            )
        }
    }
}
