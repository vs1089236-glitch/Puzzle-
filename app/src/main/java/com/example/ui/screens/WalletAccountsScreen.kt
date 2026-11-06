package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.entity.PaymentAccountEntity
import com.example.data.local.entity.UserEntity
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.JioPayUiState
import com.example.util.Formatters

@Composable
fun WalletAccountsScreen(
    uiState: JioPayUiState,
    currentUser: UserEntity,
    onOpenAddAccount: () -> Unit,
    onOpenAddMoney: () -> Unit,
    onOpenWithdraw: () -> Unit,
    onSetPrimaryAccount: (Long) -> Unit,
    onDeleteAccount: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Premium Digital Wallet Hero Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wallet_hero_card"),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_banner),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xDC081A36),
                                        Color(0xEC102B57)
                                    )
                                )
                            )
                    )

                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "JIO PAY DIGITAL WALLET",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.78f),
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFF10B981).copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = if (currentUser.isAdmin) "MASTER SELLER" else "VERIFIED WALLET",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA7F3D0),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = Formatters.formatInr(currentUser.walletBalance),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("wallet_screen_balance")
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Primary UPI: ${currentUser.primaryUpiId.ifBlank { "Not linked yet — tap Add UPI" }}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = Color(0xFFBAE6FD)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "8% Commission Profit Earned",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = "+${Formatters.formatInr(currentUser.totalCommissionEarned)}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily
                                    ),
                                    color = Color(0xFF6EE7B7),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Primary Wallet & Account Actions Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenAddMoney,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                        .testTag("open_add_money_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(stringResource(R.string.action_add_money), maxLines = 1)
                }

                OutlinedButton(
                    onClick = onOpenWithdraw,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                        .testTag("open_withdraw_button")
                ) {
                    Icon(Icons.Default.ArrowOutward, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(stringResource(R.string.action_withdraw), maxLines = 1)
                }
            }
        }

        // 3. Linked UPI & Bank Accounts Header + Add Account CTA
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Linked UPI & Bank Accounts (${uiState.paymentAccounts.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Used for withdrawals & receiving buyer payments when selling money",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onOpenAddAccount,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("open_add_account_button")
            ) {
                Icon(Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_add_account))
            }
        }

        if (uiState.paymentAccounts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AlternateEmail,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No UPI ID or Bank Account Linked",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap 'Add UPI / Bank' above to link your UPI ID (e.g. yourname@upi) so you can withdraw or sell money.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(uiState.paymentAccounts, key = { it.id }) { account ->
                PaymentAccountCard(
                    account = account,
                    onSetPrimary = { onSetPrimaryAccount(account.id) },
                    onDelete = { onDeleteAccount(account.id) }
                )
            }
        }
    }
}

@Composable
private fun PaymentAccountCard(
    account: PaymentAccountEntity,
    onSetPrimary: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("payment_account_card_${account.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (account.isPrimary) 1.5.dp else 1.dp,
            color = if (account.isPrimary) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (account.isPrimary) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (account.accountType == "BANK") {
                        Icons.Default.AccountBalance
                    } else {
                        Icons.Default.AlternateEmail
                    },
                    contentDescription = account.accountType,
                    tint = if (account.isPrimary) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = account.upiId,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily
                        ),
                        fontWeight = FontWeight.Bold
                    )
                    if (account.isPrimary) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Primary",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Text(
                    text = if (account.accountType == "BANK") {
                        "${account.bankName} ${account.accountNumberMasked} • ${account.ifscCode}"
                    } else {
                        "${account.accountHolderName} • ${account.bankName}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!account.isPrimary) {
                TextButton(
                    onClick = onSetPrimary,
                    modifier = Modifier.testTag("set_primary_account_${account.id}")
                ) {
                    Text("Set Primary")
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_account_${account.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Remove account",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountSheet(
    currentUser: UserEntity,
    isBusy: Boolean,
    onSaveAccount: (
        accountType: String,
        upiId: String,
        holderName: String,
        bankName: String,
        accountNumber: String,
        ifscCode: String,
        makePrimary: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var accountType by rememberSaveable { mutableStateOf("UPI") }
    var upiId by rememberSaveable { mutableStateOf("") }
    var holderName by rememberSaveable { mutableStateOf(currentUser.fullName) }
    var bankName by rememberSaveable { mutableStateOf("") }
    var accountNumber by rememberSaveable { mutableStateOf("") }
    var ifscCode by rememberSaveable { mutableStateOf("") }
    var makePrimary by rememberSaveable { mutableStateOf(true) }

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
            Text(
                text = "Add UPI ID or Bank Account",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Link your UPI ID so buyers can pay you directly when you sell money, or for instant withdrawals.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf("UPI" to "UPI ID", "BANK" to "Bank + UPI")
                options.forEachIndexed { idx, (code, label) ->
                    SegmentedButton(
                        selected = accountType == code,
                        onClick = { accountType = code },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = options.size),
                        modifier = Modifier
                            .heightIn(min = 46.dp)
                            .testTag("account_type_$code")
                    ) {
                        Text(label)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = upiId,
                onValueChange = { upiId = it },
                label = { Text("UPI ID (e.g. yourname@upi or name@ybl)") },
                leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_upi_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = holderName,
                onValueChange = { holderName = it },
                label = { Text("Account Holder Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_holder_input")
            )

            if (accountType == "BANK") {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Bank Name (e.g. HDFC Bank, SBI)") },
                    leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_bank_name_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Account Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_bank_number_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = ifscCode,
                    onValueChange = { ifscCode = it.uppercase() },
                    label = { Text("IFSC Code (e.g. HDFC0001234)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_ifsc_input")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Set as Primary Receiving UPI",
                    style = MaterialTheme.typography.titleSmall
                )
                Switch(
                    checked = makePrimary,
                    onCheckedChange = { makePrimary = it },
                    modifier = Modifier.testTag("make_primary_switch")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onSaveAccount(
                        accountType,
                        upiId,
                        holderName,
                        bankName,
                        accountNumber,
                        ifscCode,
                        makePrimary
                    )
                },
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("save_account_button")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Account")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMoneySheet(
    currentUser: UserEntity,
    isBusy: Boolean,
    onConfirmAddMoney: (amountText: String, sourceLabel: String, utr: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by rememberSaveable { mutableStateOf("1000") }
    var utrText by rememberSaveable { mutableStateOf(Formatters.generateUtr()) }
    val quickAmounts = listOf("500", "1000", "5000", "10000")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Add Money to Wallet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Top up your Jio Pay wallet via UPI or linked bank account.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickAmounts.forEach { amt ->
                    FilterChip(
                        selected = amountText == amt,
                        onClick = { amountText = amt },
                        label = {
                            Text(
                                text = "₹$amt",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
                label = { Text("Deposit Amount (₹)") },
                leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_money_amount_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = utrText,
                onValueChange = { utrText = it },
                label = { Text("Payment Reference / UTR") },
                leadingIcon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_money_utr_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val source = currentUser.primaryUpiId.ifBlank { "UPI Instant Deposit" }
                    onConfirmAddMoney(amountText, source, utrText)
                },
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("submit_add_money_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Text("Add Money Now")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawSheet(
    currentUser: UserEntity,
    accounts: List<PaymentAccountEntity>,
    isBusy: Boolean,
    onConfirmWithdraw: (amountText: String, destination: PaymentAccountEntity?) -> Unit,
    onOpenAddAccount: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by rememberSaveable { mutableStateOf("500") }
    var selectedAccountId by remember(accounts) {
        mutableStateOf(accounts.firstOrNull { it.isPrimary }?.id ?: accounts.firstOrNull()?.id)
    }
    val selectedAccount = accounts.firstOrNull { it.id == selectedAccountId } ?: accounts.firstOrNull()

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
            Text(
                text = "Withdraw Wallet Balance",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Available Balance: ${Formatters.formatInr(currentUser.walletBalance)}",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (accounts.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "No Linked UPI or Bank Account",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Add a UPI ID or Bank Account first to withdraw your funds.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                onDismiss()
                                onOpenAddAccount()
                            }
                        ) {
                            Text("Add UPI / Bank Account")
                        }
                    }
                }
            } else {
                Text(
                    text = "Select Destination Account",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                accounts.forEach { acc ->
                    val isSelected = selectedAccount?.id == acc.id
                    Surface(
                        onClick = { selectedAccountId = acc.id },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        },
                        border = if (isSelected) {
                            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = acc.upiId,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = JetBrainsMonoFontFamily
                                    ),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${acc.bankName} ${acc.accountNumberMasked}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Withdrawal Amount (₹)") },
                    leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("withdraw_amount_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onConfirmWithdraw(amountText, selectedAccount) },
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("submit_withdraw_button")
                ) {
                    Text("Withdraw to ${selectedAccount?.upiId ?: "Account"}")
                }
            }
        }
    }
}
