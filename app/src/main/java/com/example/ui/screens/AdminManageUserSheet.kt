package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.UserEntity
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManageUserSheet(
    user: UserEntity,
    isBusy: Boolean,
    onAdjustWallet: (mode: String, amountText: String, reason: String) -> Unit,
    onUpdateAccount: (
        fullName: String,
        mobile: String,
        email: String,
        primaryUpiId: String,
        newPassword: String?
    ) -> Unit,
    onToggleBlock: () -> Unit,
    onDeleteAccount: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var walletMode by rememberSaveable(user.id) { mutableStateOf("CREDIT") }
    var walletAmountText by rememberSaveable(user.id) { mutableStateOf("500") }
    var walletNoteText by rememberSaveable(user.id) { mutableStateOf("") }

    var fullName by rememberSaveable(user.id, user.fullName) { mutableStateOf(user.fullName) }
    var mobile by rememberSaveable(user.id, user.mobile) { mutableStateOf(user.mobile) }
    var email by rememberSaveable(user.id, user.email) { mutableStateOf(user.email) }
    var primaryUpiId by rememberSaveable(user.id, user.primaryUpiId) { mutableStateOf(user.primaryUpiId) }
    var newPassword by rememberSaveable(user.id) { mutableStateOf("") }

    var confirmDelete by rememberSaveable(user.id) { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Manage User Account",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${user.fullName} • ${user.mobile}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = JetBrainsMonoFontFamily
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (user.isBlocked) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                ) {
                    Text(
                        text = if (user.isBlocked) "BLOCKED" else "ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (user.isBlocked) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Current Wallet Balance Pill
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Current Wallet Balance",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = Formatters.formatInr(user.walletBalance),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("admin_sheet_user_balance")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 1: Wallet Balance Control
            Text(
                text = "1. Wallet Balance Control",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val walletModes = listOf(
                    "CREDIT" to "+ Add",
                    "DEBIT" to "- Deduct",
                    "SET" to "= Set Exact"
                )
                walletModes.forEachIndexed { idx, (code, label) ->
                    SegmentedButton(
                        selected = walletMode == code,
                        onClick = { walletMode = code },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = walletModes.size),
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .testTag("admin_wallet_mode_${code.lowercase()}")
                    ) {
                        Text(label, maxLines = 1)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("500", "1000", "5000", "10000").forEach { preset ->
                    FilterChip(
                        selected = walletAmountText == preset,
                        onClick = { walletAmountText = preset },
                        label = {
                            Text(
                                text = "₹$preset",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                )
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = walletAmountText,
                onValueChange = { walletAmountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = { Text("Amount (₹)") },
                leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_wallet_amount_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = walletNoteText,
                onValueChange = { walletNoteText = it },
                label = { Text("Adjustment Note (Optional)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_wallet_note_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { onAdjustWallet(walletMode, walletAmountText, walletNoteText) },
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("admin_apply_wallet_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Apply Wallet Update")
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            // Section 2: Edit Profile, UPI & Password
            Text(
                text = "2. Edit Profile, UPI & Password",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_edit_name_input")
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = mobile,
                onValueChange = { mobile = it.filter { ch -> ch.isDigit() }.take(10) },
                label = { Text("10-Digit Mobile Number") },
                leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_edit_mobile_input")
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_edit_email_input")
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = primaryUpiId,
                onValueChange = { primaryUpiId = it },
                label = { Text("Primary UPI ID") },
                leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_edit_upi_input")
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New Password (optional — min 4 chars)") },
                leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_edit_password_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    onUpdateAccount(
                        fullName,
                        mobile,
                        email,
                        primaryUpiId,
                        newPassword.ifBlank { null }
                    )
                },
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("admin_save_user_btn")
            ) {
                Text("Save Account Changes")
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            // Section 3: Block / Unblock & Delete Account
            Text(
                text = "3. Account Access & Removal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onToggleBlock,
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("admin_sheet_block_btn"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (user.isBlocked) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                ),
                border = BorderStroke(
                    1.2.dp,
                    if (user.isBlocked) {
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f)
                    } else {
                        MaterialTheme.colorScheme.error.copy(alpha = 0.65f)
                    }
                )
            ) {
                Icon(
                    imageVector = if (user.isBlocked) Icons.Default.CheckCircle else Icons.Default.Block,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (user.isBlocked) "Unblock User Account" else "Block User Account")
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!confirmDelete) {
                TextButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 46.dp)
                        .testTag("admin_delete_user_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Delete User Account Permanently",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Permanently delete ${user.fullName}'s account?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Removes login access, linked UPI/bank accounts, and unsold plans.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { confirmDelete = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = onDeleteAccount,
                                enabled = !isBusy,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("admin_confirm_delete_btn")
                            ) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}
