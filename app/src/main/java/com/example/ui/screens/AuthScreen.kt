package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.StatusFeedbackBanner
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.AuthMode
import com.example.ui.viewmodel.JioPayUiState

@Composable
fun AuthScreen(
    uiState: JioPayUiState,
    onSetAuthMode: (AuthMode) -> Unit,
    onOtpMobileChanged: (String) -> Unit,
    onSendOtp: () -> Unit,
    onEnteredOtpChanged: (String) -> Unit,
    onAutoFillOtp: () -> Unit,
    onOtpNewUserNameChanged: (String) -> Unit,
    onOtpNewUserUpiChanged: (String) -> Unit,
    onVerifyOtpLogin: () -> Unit,
    onPasswordLogin: (String, String) -> Unit,
    onRegisterUser: (String, String, String, String, String) -> Unit,
    onDismissBanners: () -> Unit
) {
    var passwordIdentifier by rememberSaveable { mutableStateOf("") }
    var passwordValue by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    var regName by rememberSaveable { mutableStateOf("") }
    var regMobile by rememberSaveable { mutableStateOf("") }
    var regEmail by rememberSaveable { mutableStateOf("") }
    var regPassword by rememberSaveable { mutableStateOf("") }
    var regUpi by rememberSaveable { mutableStateOf("") }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Hero Banner with Fintech Branding
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_banner),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xCC081A36),
                                    Color(0xEE0B1F3A)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Jio Pay Logo",
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(id = R.string.app_name),
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Instant UPI Money Exchange",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF059669).copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "Earn +8% Commission Profit on Every Buy Plan",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFD1FAE5),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StatusFeedbackBanner(
                    statusMessage = uiState.statusBanner,
                    errorMessage = uiState.errorBanner,
                    onDismiss = onDismissBanners
                )

                // Segmented Auth Mode Selector: OTP Login | Password | Register
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    val modes = listOf(
                        AuthMode.OTP to stringResource(R.string.auth_tab_otp),
                        AuthMode.PASSWORD to stringResource(R.string.auth_tab_password),
                        AuthMode.REGISTER to stringResource(R.string.auth_tab_register)
                    )
                    modes.forEachIndexed { index, (mode, label) ->
                        SegmentedButton(
                            selected = uiState.authMode == mode,
                            onClick = { onSetAuthMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("auth_tab_${mode.name.lowercase()}")
                        ) {
                            Text(label, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        when (uiState.authMode) {
                            AuthMode.OTP -> {
                                Text(
                                    text = "Login with Mobile OTP",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Enter your 10-digit mobile number to receive a 6-digit verification OTP.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = uiState.otpSession.mobile,
                                    onValueChange = onOtpMobileChanged,
                                    label = { Text(stringResource(R.string.label_mobile_number)) },
                                    placeholder = { Text("9876543210") },
                                    leadingIcon = {
                                        Icon(Icons.Default.PhoneAndroid, contentDescription = null)
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("otp_mobile_input")
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                if (!uiState.otpSession.isOtpSent) {
                                    Button(
                                        onClick = onSendOtp,
                                        enabled = !uiState.isBusy,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 50.dp)
                                            .testTag("send_otp_button")
                                    ) {
                                        Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.action_send_otp))
                                    }
                                } else {
                                    // Live In-App OTP Code Banner
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "SMS Verification Code (${uiState.otpSession.secondsRemaining}s)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Text(
                                                    text = uiState.otpSession.generatedOtp,
                                                    style = MaterialTheme.typography.headlineSmall.copy(
                                                        fontFamily = JetBrainsMonoFontFamily,
                                                        letterSpacing = 4.sp
                                                    ),
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            TextButton(
                                                onClick = onAutoFillOtp,
                                                modifier = Modifier.testTag("autofill_otp_button")
                                            ) {
                                                Text("Auto-Fill")
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = uiState.otpSession.enteredOtp,
                                        onValueChange = onEnteredOtpChanged,
                                        label = { Text("Enter 6-Digit OTP") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Pin, contentDescription = null)
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("otp_code_input")
                                    )

                                    AnimatedVisibility(visible = uiState.otpSession.isNewUser) {
                                        Column {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            OutlinedTextField(
                                                value = uiState.otpSession.newUserName,
                                                onValueChange = onOtpNewUserNameChanged,
                                                label = { Text("Your Full Name (New Account)") },
                                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                                singleLine = true,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("otp_new_name_input")
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            OutlinedTextField(
                                                value = uiState.otpSession.newUserUpi,
                                                onValueChange = onOtpNewUserUpiChanged,
                                                label = { Text("Your UPI ID (Optional, e.g. name@upi)") },
                                                leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
                                                singleLine = true,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("otp_new_upi_input")
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = onSendOtp,
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 50.dp)
                                                .testTag("resend_otp_button")
                                        ) {
                                            Text("Resend")
                                        }
                                        Button(
                                            onClick = onVerifyOtpLogin,
                                            enabled = !uiState.isBusy,
                                            modifier = Modifier
                                                .weight(1.6f)
                                                .heightIn(min = 50.dp)
                                                .testTag("verify_otp_button")
                                        ) {
                                            if (uiState.isBusy) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            } else {
                                                Text(stringResource(R.string.action_verify_otp))
                                            }
                                        }
                                    }
                                }
                            }

                            AuthMode.PASSWORD -> {
                                Text(
                                    text = "Login with Password",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Sign in using your registered mobile number or email and password.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = passwordIdentifier,
                                    onValueChange = { passwordIdentifier = it },
                                    label = { Text(stringResource(R.string.label_mobile_or_email)) },
                                    placeholder = { Text("Enter 10-digit mobile or email") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("password_identifier_input")
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = passwordValue,
                                    onValueChange = { passwordValue = it },
                                    label = { Text(stringResource(R.string.label_password)) },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                            )
                                        }
                                    },
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("password_input")
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = { onPasswordLogin(passwordIdentifier, passwordValue) },
                                    enabled = !uiState.isBusy,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 50.dp)
                                        .testTag("login_password_button")
                                ) {
                                    Text(stringResource(R.string.action_login_password))
                                }
                            }

                            AuthMode.REGISTER -> {
                                Text(
                                    text = "Create New Account",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Register to buy plans with +8% profit or add your UPI ID to sell money.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = regName,
                                    onValueChange = { regName = it },
                                    label = { Text(stringResource(R.string.label_full_name)) },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("register_name_input")
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = regMobile,
                                    onValueChange = { regMobile = it.filter { ch -> ch.isDigit() }.take(10) },
                                    label = { Text(stringResource(R.string.label_mobile_number)) },
                                    leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("register_mobile_input")
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = regUpi,
                                    onValueChange = { regUpi = it },
                                    label = { Text(stringResource(R.string.label_upi_id)) },
                                    placeholder = { Text("yourname@upi") },
                                    leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("register_upi_input")
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = regPassword,
                                    onValueChange = { regPassword = it },
                                    label = { Text("Create Password (min 4 chars)") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("register_password_input")
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = { onRegisterUser(regName, regMobile, regEmail, regPassword, regUpi) },
                                    enabled = !uiState.isBusy,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 50.dp)
                                        .testTag("register_submit_button")
                                ) {
                                    Text(stringResource(R.string.action_create_account))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
