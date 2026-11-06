package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.UserEntity
import com.example.ui.components.PendingVerificationCard
import com.example.ui.components.TransactionRowCard
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.JioPayUiState
import com.example.ui.viewmodel.TxnFilter
import com.example.util.Formatters

@Composable
fun AdminScreen(
    uiState: JioPayUiState,
    currentUser: UserEntity,
    onSelectAdminTxnFilter: (TxnFilter) -> Unit,
    onVerifyOrder: (Long) -> Unit,
    onRejectOrder: (Long, String) -> Unit,
    onReplenishTier: (String, Int) -> Unit,
    onOpenTopUpVault: () -> Unit,
    onToggleBlockUser: (UserEntity) -> Unit,
    onAdjustUserWallet: (Long, String, String, String, () -> Unit) -> Unit,
    onUpdateUserAccount: (Long, String, String, String, String, String?, () -> Unit) -> Unit,
    onDeleteUserAccount: (UserEntity, () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var userSearchQuery by rememberSaveable { mutableStateOf("") }
    var managingUserId by rememberSaveable { mutableStateOf<Long?>(null) }

    val managedUsers = remember(uiState.allUsers, userSearchQuery) {
        val nonAdmins = uiState.allUsers.filter { !it.isAdmin }
        val q = userSearchQuery.trim().lowercase()
        if (q.isEmpty()) {
            nonAdmins
        } else {
            nonAdmins.filter {
                it.fullName.lowercase().contains(q) ||
                    it.mobile.contains(q) ||
                    it.primaryUpiId.lowercase().contains(q)
            }
        }
    }

    val managingUser = remember(uiState.allUsers, managingUserId) {
        managingUserId?.let { id -> uiState.allUsers.firstOrNull { it.id == id } }
    }

    val successfulTxns = remember(uiState.allTransactions) {
        uiState.allTransactions.filter { it.status == "SUCCESS" }
    }
    val failedTxns = remember(uiState.allTransactions) {
        uiState.allTransactions.filter { it.status == "FAILED" }
    }
    val totalSuccessVolume = remember(successfulTxns) {
        successfulTxns.sumOf { it.netAmount }
    }
    val totalFailedVolume = remember(failedTxns) {
        failedTxns.sumOf { it.baseAmount }
    }
    val totalCommissionDistributed = remember(successfulTxns) {
        successfulTxns.sumOf { it.commissionAmount }
    }

    val filteredAdminTxns = remember(uiState.allTransactions, uiState.selectedAdminTxnFilter) {
        val status = uiState.selectedAdminTxnFilter.statusMatch
        if (status == null) {
            uiState.allTransactions
        } else {
            uiState.allTransactions.filter { it.status == status }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Admin Master Wallet & Identity Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_header_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ADMIN CONTROL PANEL",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            onClick = onOpenTopUpVault,
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.16f),
                            modifier = Modifier.testTag("admin_topup_vault_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Add Vault",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${currentUser.fullName} • ${currentUser.primaryUpiId}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily
                        ),
                        color = Color(0xFF93C5FD)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "Master Vault Balance",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                            Text(
                                text = Formatters.formatInr(currentUser.walletBalance),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("admin_wallet_balance")
                            )
                        }
                        Text(
                            text = "${uiState.allUsers.size} Total Users",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = Color(0xFF6EE7B7),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. 2x2 KPI Grid: Successful | Failed | Pending | 8% Commission
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminMetricCard(
                        title = "Successful Txns",
                        countText = "${successfulTxns.size}",
                        subText = Formatters.formatCompactInr(totalSuccessVolume),
                        icon = Icons.Default.CheckCircle,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_metric_success")
                    )
                    AdminMetricCard(
                        title = "Failed Txns",
                        countText = "${failedTxns.size}",
                        subText = Formatters.formatCompactInr(totalFailedVolume),
                        icon = Icons.Default.ErrorOutline,
                        accentColor = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_metric_failed")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminMetricCard(
                        title = "Pending Verify",
                        countText = "${uiState.allPendingVerifications.size}",
                        subText = "Awaiting confirm",
                        icon = Icons.Default.HourglassTop,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_metric_pending")
                    )
                    AdminMetricCard(
                        title = "8% Comm. Paid",
                        countText = Formatters.formatCompactInr(totalCommissionDistributed),
                        subText = "Buyer rewards",
                        icon = Icons.Default.TrendingUp,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_metric_commission")
                    )
                }
            }
        }

        // 3. Pending Payment Verification Queue
        item {
            Text(
                text = "Pending Payment Verifications (${uiState.allPendingVerifications.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (uiState.allPendingVerifications.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "All buyer payments are settled. New buy orders appear here immediately for verification.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(uiState.allPendingVerifications, key = { "admin_pending_${it.id}" }) { plan ->
                PendingVerificationCard(
                    plan = plan,
                    onVerifyClick = { onVerifyOrder(plan.id) },
                    onRejectClick = {
                        onRejectOrder(
                            plan.id,
                            "Admin verification failed — payment not received at ${plan.sellerUpiId}"
                        )
                    },
                    verifyTag = "admin_verify_${plan.id}",
                    rejectTag = "admin_fail_${plan.id}"
                )
            }
        }

        // 4. Exclusive User Accounts & Access Control
        item {
            Column {
                Text(
                    text = "User Accounts & Full Control (${managedUsers.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Block/unblock users, adjust wallet balance, edit UPI/password, or remove accounts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = userSearchQuery,
                    onValueChange = { userSearchQuery = it },
                    placeholder = { Text("Search user by name, mobile, or UPI...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_user_search_input")
                )
            }
        }

        if (managedUsers.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No matching user accounts found.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(managedUsers, key = { "admin_user_${it.id}" }) { user ->
                AdminUserControlCard(
                    user = user,
                    onToggleBlock = { onToggleBlockUser(user) },
                    onManageClick = { managingUserId = user.id }
                )
            }
        }

        // 5. Quick Plan Restock Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Quick Restock Plans (+5 Units)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Add 5 new plans to the marketplace using ${currentUser.primaryUpiId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("350", "500", "700", "850", "1000").forEach { tier ->
                            OutlinedButton(
                                onClick = { onReplenishTier(tier, 5) },
                                modifier = Modifier.heightIn(min = 42.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "₹$tier",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. All Platform Transactions (Successful & Failed Audit Log)
        item {
            Column {
                Text(
                    text = "All Platform Transactions (${filteredAdminTxns.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TxnFilter.entries.forEach { filter ->
                        val count = if (filter.statusMatch == null) {
                            uiState.allTransactions.size
                        } else {
                            uiState.allTransactions.count { it.status == filter.statusMatch }
                        }
                        FilterChip(
                            selected = uiState.selectedAdminTxnFilter == filter,
                            onClick = { onSelectAdminTxnFilter(filter) },
                            label = { Text("${filter.label} ($count)") },
                            modifier = Modifier
                                .heightIn(min = 42.dp)
                                .testTag("admin_txn_filter_${filter.name.lowercase()}")
                        )
                    }
                }
            }
        }

        items(filteredAdminTxns, key = { "admin_txn_${it.id}" }) { txn ->
            TransactionRowCard(
                txn = txn,
                viewerUserId = null,
                showBothParties = true
            )
        }
    }

    if (managingUser != null) {
        AdminManageUserSheet(
            user = managingUser,
            isBusy = uiState.isBusy,
            onAdjustWallet = { mode, amountText, reason ->
                onAdjustUserWallet(managingUser.id, mode, amountText, reason) {}
            },
            onUpdateAccount = { fullName, mobile, email, upi, newPass ->
                onUpdateUserAccount(managingUser.id, fullName, mobile, email, upi, newPass) {}
            },
            onToggleBlock = {
                onToggleBlockUser(managingUser)
            },
            onDeleteAccount = {
                onDeleteUserAccount(managingUser) {
                    managingUserId = null
                }
            },
            onDismiss = { managingUserId = null }
        )
    }
}

@Composable
private fun AdminUserControlCard(
    user: UserEntity,
    onToggleBlock: () -> Unit,
    onManageClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_user_card_${user.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (user.isBlocked) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (user.isBlocked) MaterialTheme.colorScheme.errorContainer
                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (user.isBlocked) Icons.Default.Block else Icons.Default.Person,
                            contentDescription = null,
                            tint = if (user.isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = user.fullName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
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
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${user.mobile} • ${user.primaryUpiId.ifBlank { "No UPI" }}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Formatters.formatCompactInr(user.walletBalance),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "8%: +${Formatters.formatCompactInr(user.totalCommissionEarned)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onToggleBlock,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("admin_block_user_${user.id}"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (user.isBlocked) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (user.isBlocked) {
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                        }
                    )
                ) {
                    Icon(
                        imageVector = if (user.isBlocked) Icons.Default.CheckCircle else Icons.Default.Block,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (user.isBlocked) "Unblock" else "Block User", maxLines = 1)
                }

                Button(
                    onClick = onManageClick,
                    modifier = Modifier
                        .weight(1.2f)
                        .heightIn(min = 48.dp)
                        .testTag("admin_manage_user_${user.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Manage Account", maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun AdminMetricCard(
    title: String,
    countText: String,
    subText: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = countText,
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(
                text = subText,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

