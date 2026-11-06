package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.JioPayDatabase
import com.example.data.preferences.SessionPreferences
import com.example.data.repository.JioPayRepository
import com.example.ui.components.ProfileSwitcherSheet
import com.example.ui.components.StatusFeedbackBanner
import com.example.ui.screens.AddAccountSheet
import com.example.ui.screens.AddMoneySheet
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BuyMarketScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.SellVerifyScreen
import com.example.ui.screens.UpiQrCheckoutSheet
import com.example.ui.screens.WalletAccountsScreen
import com.example.ui.screens.WithdrawSheet
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.JioPayTheme
import com.example.ui.viewmodel.JioPayViewModel
import kotlinx.serialization.Serializable

@Serializable
sealed interface JioPayRoute

@Serializable
data object BuyMarketRoute : JioPayRoute

@Serializable
data object SellVerifyRoute : JioPayRoute

@Serializable
data object WalletAccountsRoute : JioPayRoute

@Serializable
data object LogsRoute : JioPayRoute

@Serializable
data object AdminRoute : JioPayRoute

private data class NavTabItem(
    val route: JioPayRoute,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JioPayTheme {
                JioPayRoot()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JioPayRoot() {
    val viewModel: JioPayViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = JioPayDatabase.getInstance(app)
                val repo = JioPayRepository(db)
                val sessionPrefs = SessionPreferences(app)
                JioPayViewModel(repo, sessionPrefs)
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isSessionLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val currentUser = uiState.currentUser
    if (currentUser == null) {
        AuthScreen(
            uiState = uiState,
            onSetAuthMode = viewModel::setAuthMode,
            onOtpMobileChanged = viewModel::updateOtpMobile,
            onSendOtp = viewModel::sendOtp,
            onEnteredOtpChanged = viewModel::updateEnteredOtp,
            onAutoFillOtp = viewModel::autoFillGeneratedOtp,
            onOtpNewUserNameChanged = viewModel::updateOtpNewUserName,
            onOtpNewUserUpiChanged = viewModel::updateOtpNewUserUpi,
            onVerifyOtpLogin = viewModel::verifyOtpAndLogin,
            onPasswordLogin = viewModel::loginWithPassword,
            onRegisterUser = viewModel::registerUser,
            onDismissBanners = viewModel::clearBanners
        )
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    var showAddAccountSheet by rememberSaveable { mutableStateOf(false) }
    var showAddMoneySheet by rememberSaveable { mutableStateOf(false) }
    var showWithdrawSheet by rememberSaveable { mutableStateOf(false) }
    var showProfileSheet by rememberSaveable { mutableStateOf(false) }

    val navItems = remember(
        currentUser.isAdmin,
        uiState.sellerPendingVerifications.size,
        uiState.allPendingVerifications.size
    ) {
        buildList {
            add(
                NavTabItem(
                    route = BuyMarketRoute,
                    label = "Buy",
                    selectedIcon = Icons.Filled.ShoppingBag,
                    unselectedIcon = Icons.Outlined.ShoppingBag,
                    testTag = "nav_tab_buy"
                )
            )
            add(
                NavTabItem(
                    route = SellVerifyRoute,
                    label = "Sell",
                    selectedIcon = Icons.Filled.Sell,
                    unselectedIcon = Icons.Outlined.Sell,
                    badgeCount = uiState.sellerPendingVerifications.size,
                    testTag = "nav_tab_sell"
                )
            )
            add(
                NavTabItem(
                    route = WalletAccountsRoute,
                    label = "Wallet",
                    selectedIcon = Icons.Filled.AccountBalanceWallet,
                    unselectedIcon = Icons.Outlined.AccountBalanceWallet,
                    testTag = "nav_tab_wallet"
                )
            )
            add(
                NavTabItem(
                    route = LogsRoute,
                    label = "Logs",
                    selectedIcon = Icons.Filled.History,
                    unselectedIcon = Icons.Outlined.History,
                    testTag = "nav_tab_logs"
                )
            )
            if (currentUser.isAdmin) {
                add(
                    NavTabItem(
                        route = AdminRoute,
                        label = "Admin",
                        selectedIcon = Icons.Filled.Shield,
                        unselectedIcon = Icons.Outlined.Shield,
                        badgeCount = uiState.allPendingVerifications.size,
                        testTag = "nav_tab_admin"
                    )
                )
            }
        }
    }

    fun navigateToTab(route: JioPayRoute) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val useNavigationRail = maxWidth >= 840.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = currentUser.primaryUpiId.ifBlank { currentUser.mobile },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFontFamily
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    actions = {
                        Surface(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable { showProfileSheet = true }
                                .testTag("profile_switcher_button"),
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (currentUser.isAdmin) Icons.Default.Shield else Icons.Default.Person,
                                    contentDescription = if (currentUser.isAdmin) "Admin Profile & Switch Account" else "My Profile & Sign Out",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentUser.fullName.substringBefore(" "),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        navItems.forEach { item ->
                            val selected = currentDestination?.hasRoute(item.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(item.route) },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (item.badgeCount > 0) {
                                                Badge {
                                                    Text("${item.badgeCount}")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        navItems.forEach { item ->
                            val selected = currentDestination?.hasRoute(item.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(item.route) },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (item.badgeCount > 0) {
                                                Badge {
                                                    Text("${item.badgeCount}")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    }
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    StatusFeedbackBanner(
                        statusMessage = uiState.statusBanner,
                        errorMessage = uiState.errorBanner,
                        onDismiss = viewModel::clearBanners,
                        modifier = Modifier.widthIn(max = 720.dp)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(max = 720.dp)
                            .fillMaxWidth()
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = BuyMarketRoute,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable<BuyMarketRoute> {
                                BuyMarketScreen(
                                    uiState = uiState,
                                    currentUser = currentUser,
                                    onSelectFilter = viewModel::selectPlanFilter,
                                    onBuyPlanClick = viewModel::openBuyCheckout,
                                    onOpenAddMoney = { showAddMoneySheet = true },
                                    onOpenWithdraw = { showWithdrawSheet = true },
                                    onSwitchToSellTab = { navigateToTab(SellVerifyRoute) }
                                )
                            }

                            composable<SellVerifyRoute> {
                                SellVerifyScreen(
                                    uiState = uiState,
                                    currentUser = currentUser,
                                    onVerifyOrder = viewModel::verifyAndTransferOrder,
                                    onRejectOrder = viewModel::rejectOrderAsFailed,
                                    onPublishSellOrder = viewModel::createSellOrders,
                                    onCancelSellPlan = viewModel::cancelSellPlan,
                                    onOpenAddUpiSheet = { showAddAccountSheet = true }
                                )
                            }

                            composable<WalletAccountsRoute> {
                                WalletAccountsScreen(
                                    uiState = uiState,
                                    currentUser = currentUser,
                                    onOpenAddAccount = { showAddAccountSheet = true },
                                    onOpenAddMoney = { showAddMoneySheet = true },
                                    onOpenWithdraw = { showWithdrawSheet = true },
                                    onSetPrimaryAccount = viewModel::setPrimaryAccount,
                                    onDeleteAccount = viewModel::deleteAccount
                                )
                            }

                            composable<LogsRoute> {
                                LogsScreen(
                                    uiState = uiState,
                                    currentUser = currentUser,
                                    onSelectFilter = viewModel::selectUserTxnFilter
                                )
                            }

                            composable<AdminRoute> {
                                if (currentUser.isAdmin) {
                                    AdminScreen(
                                        uiState = uiState,
                                        currentUser = currentUser,
                                        onSelectAdminTxnFilter = viewModel::selectAdminTxnFilter,
                                        onVerifyOrder = viewModel::verifyAndTransferOrder,
                                        onRejectOrder = viewModel::rejectOrderAsFailed,
                                        onReplenishTier = { tier, qty ->
                                            viewModel.createSellOrders(tier, qty)
                                        },
                                        onOpenTopUpVault = { showAddMoneySheet = true },
                                        onToggleBlockUser = viewModel::adminToggleBlockUser,
                                        onAdjustUserWallet = viewModel::adminAdjustUserWallet,
                                        onUpdateUserAccount = viewModel::adminUpdateUserAccount,
                                        onDeleteUserAccount = viewModel::adminDeleteUserAccount
                                    )
                                } else {
                                    LaunchedEffect(Unit) {
                                        navController.navigate(BuyMarketRoute) {
                                            popUpTo(BuyMarketRoute) { inclusive = true }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Sheets
    val checkoutPlan = uiState.activeCheckoutPlan
    if (checkoutPlan != null) {
        UpiQrCheckoutSheet(
            plan = checkoutPlan,
            isBusy = uiState.isBusy,
            onSubmitPayment = { utr ->
                viewModel.submitBuyPayment(checkoutPlan.id, utr)
            },
            onReportFailed = { reason ->
                viewModel.reportFailedPaymentCheckout(checkoutPlan.id, reason)
            },
            onDismiss = viewModel::dismissBuyCheckout
        )
    }

    if (showAddAccountSheet) {
        AddAccountSheet(
            currentUser = currentUser,
            isBusy = uiState.isBusy,
            onSaveAccount = { type, upi, holder, bank, accNum, ifsc, primary ->
                viewModel.addPaymentAccount(
                    accountType = type,
                    upiId = upi,
                    holderName = holder,
                    bankName = bank,
                    accountNumber = accNum,
                    ifscCode = ifsc,
                    makePrimary = primary,
                    onComplete = { showAddAccountSheet = false }
                )
            },
            onDismiss = { showAddAccountSheet = false }
        )
    }

    if (showAddMoneySheet) {
        AddMoneySheet(
            currentUser = currentUser,
            isBusy = uiState.isBusy,
            onConfirmAddMoney = { amountText, sourceLabel, utr ->
                viewModel.addMoneyToWallet(
                    amountText = amountText,
                    sourceLabel = sourceLabel,
                    utr = utr,
                    onComplete = { showAddMoneySheet = false }
                )
            },
            onDismiss = { showAddMoneySheet = false }
        )
    }

    if (showWithdrawSheet) {
        WithdrawSheet(
            currentUser = currentUser,
            accounts = uiState.paymentAccounts,
            isBusy = uiState.isBusy,
            onConfirmWithdraw = { amountText, destination ->
                viewModel.withdrawMoney(
                    amountText = amountText,
                    destination = destination,
                    onComplete = { showWithdrawSheet = false }
                )
            },
            onOpenAddAccount = { showAddAccountSheet = true },
            onDismiss = { showWithdrawSheet = false }
        )
    }

    if (showProfileSheet) {
        ProfileSwitcherSheet(
            currentUser = currentUser,
            allUsers = uiState.allUsers,
            onSwitchUser = viewModel::switchUserSession,
            onSignOut = viewModel::signOut,
            onDismiss = { showProfileSheet = false }
        )
    }
}
