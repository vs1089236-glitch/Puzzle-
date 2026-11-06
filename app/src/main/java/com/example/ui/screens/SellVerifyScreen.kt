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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.entity.UserEntity
import com.example.ui.components.PendingVerificationCard
import com.example.ui.components.StatusBadgePill
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.JioPayUiState
import com.example.util.Formatters

@Composable
fun SellVerifyScreen(
    uiState: JioPayUiState,
    currentUser: UserEntity,
    onVerifyOrder: (Long) -> Unit,
    onRejectOrder: (Long, String) -> Unit,
    onPublishSellOrder: (String, Int, String) -> Unit,
    onCancelSellPlan: (Long) -> Unit,
    onOpenAddUpiSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sellAmountText by rememberSaveable { mutableStateOf("500") }
    var sellQuantity by rememberSaveable { mutableIntStateOf(1) }
    var inlineUpiText by rememberSaveable(currentUser.id) { mutableStateOf("") }

    val parsedAmount = sellAmountText.toDoubleOrNull() ?: 0.0
    val totalVaultNeeded = parsedAmount * sellQuantity
    val buyerGetsPerUnit = parsedAmount * 1.08

    val presets = listOf("350", "500", "700", "850", "1000")

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // If incoming buyer payments are waiting, surface them immediately at top
        if (uiState.sellerPendingVerifications.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Verify Incoming Payments (${uiState.sellerPendingVerifications.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = "Action Required",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            items(uiState.sellerPendingVerifications, key = { "verify_${it.id}" }) { pendingPlan ->
                PendingVerificationCard(
                    plan = pendingPlan,
                    onVerifyClick = { onVerifyOrder(pendingPlan.id) },
                    onRejectClick = {
                        onRejectOrder(
                            pendingPlan.id,
                            "Payment not received by seller (${currentUser.primaryUpiId})"
                        )
                    },
                    verifyTag = "seller_verify_${pendingPlan.id}",
                    rejectTag = "seller_reject_${pendingPlan.id}"
                )
            }
        }

        // Primary Sell Money Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_sell_plan_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sell,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Sell Money",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Add your UPI ID & list money from your wallet. Buyers pay your UPI directly.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Linked UPI Status or Inline Quick-Add UPI
                    if (currentUser.primaryUpiId.isBlank()) {
                        OutlinedTextField(
                            value = inlineUpiText,
                            onValueChange = { inlineUpiText = it },
                            label = { Text("Your Receiving UPI ID (e.g. yourname@upi)") },
                            leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sell_inline_upi_input")
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Buyers scan QR to pay this UPI ID",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = onOpenAddUpiSheet,
                                modifier = Modifier.testTag("sell_add_upi_prompt")
                            ) {
                                Text("Or Add Bank/UPI")
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AlternateEmail,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Receiving UPI ID",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = currentUser.primaryUpiId,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontFamily = JetBrainsMonoFontFamily
                                            ),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                OutlinedButton(onClick = onOpenAddUpiSheet) {
                                    Text("Change UPI")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Select Plan Amount",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { preset ->
                            FilterChip(
                                selected = sellAmountText == preset,
                                onClick = { sellAmountText = preset },
                                label = {
                                    Text(
                                        text = "₹$preset",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = JetBrainsMonoFontFamily
                                        )
                                    )
                                },
                                modifier = Modifier.testTag("sell_preset_$preset")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = sellAmountText,
                        onValueChange = { sellAmountText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Plan Amount (₹)") },
                        leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sell_amount_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quantity Stepper
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Number of Plans",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Available Wallet: ${Formatters.formatCompactInr(currentUser.walletBalance)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (sellQuantity > 1) sellQuantity-- },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .testTag("sell_qty_minus")
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease quantity")
                            }
                            Text(
                                text = "$sellQuantity",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            IconButton(
                                onClick = { if (sellQuantity < 20) sellQuantity++ },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .testTag("sell_qty_plus")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase quantity")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Sell Volume", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = Formatters.formatCompactInr(totalVaultNeeded),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = JetBrainsMonoFontFamily
                                    ),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Buyer Receives Per Plan (+8%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = Formatters.formatCompactInr(buyerGetsPerUnit),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = JetBrainsMonoFontFamily
                                    ),
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { onPublishSellOrder(sellAmountText, sellQuantity, inlineUpiText) },
                        enabled = !uiState.isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("publish_sell_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sell,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_publish_sell))
                    }
                }
            }
        }

        // Empty-state notice for incoming payments when none are waiting
        if (uiState.sellerPendingVerifications.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "No incoming buyer payments pending verification. When a buyer pays your UPI (${currentUser.primaryUpiId.ifBlank { "enter above" }}), verify here to transfer balance.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // My Sell Listings History
        item {
            Text(
                text = "My Listed Sell Plans (${uiState.sellerPlans.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(uiState.sellerPlans.take(25), key = { "my_sell_${it.id}" }) { plan ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${plan.planCode} • ${Formatters.formatCompactInr(plan.amount)}",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (plan.pendingBuyerName != null) {
                                "Buyer: ${plan.pendingBuyerName} (${plan.sellerUpiId})"
                            } else {
                                "UPI: ${plan.sellerUpiId}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (plan.status == "AVAILABLE") {
                            TextButton(
                                onClick = { onCancelSellPlan(plan.id) },
                                modifier = Modifier.testTag("cancel_sell_plan_${plan.id}")
                            ) {
                                Text(
                                    text = "Cancel",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        StatusBadgePill(status = plan.status)
                    }
                }
            }
        }
    }
}
