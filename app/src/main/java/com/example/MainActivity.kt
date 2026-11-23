package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.ai.Dera3AiRepository
import com.example.data.firebase.configureAppCheck
import com.example.data.model.AppPreferences
import com.example.data.model.UiState
import com.example.data.preferences.PreferencesRepository
import com.example.data.repository.Dera3Repository
import com.example.localization.AppLanguage
import com.example.localization.LocalStrings
import com.example.localization.LocalizationManager
import com.example.localization.ProvideDera3Localization
import com.example.notifications.Dera3NotificationHelper
import com.example.ui.auth.signOutUser
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OtpVerificationScreen
import com.example.ui.screens.RegistrationScreen
import com.example.ui.screens.StatusBanner
import com.example.ui.theme.Dera3Theme
import com.example.viewmodel.MainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

private const val ADMIN_EMAIL = "oarfhm@gmail.com"

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    private var openAlertsFromNotificationState by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        openAlertsFromNotificationState = intent?.getBooleanExtra(
            Dera3NotificationHelper.EXTRA_OPEN_ALERTS_SHEET,
            false,
        ) == true

        enableEdgeToEdge()
        setContent {
            Dera3AppRoot(openAlertsFromNotification = openAlertsFromNotificationState)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(Dera3NotificationHelper.EXTRA_OPEN_ALERTS_SHEET, false)) {
            openAlertsFromNotificationState = true
        }
    }
}

@Composable
fun Dera3AppRoot(openAlertsFromNotification: Boolean = false) {
    val context = LocalContext.current
    val prefsRepo = remember(context) { PreferencesRepository(context.applicationContext) }
    val preferences by prefsRepo.preferencesFlow.collectAsStateWithLifecycle(initialValue = AppPreferences())
    val scope = rememberCoroutineScope()
    val localizationManager = remember(prefsRepo, scope) { LocalizationManager(prefsRepo, scope) }

    val currentUser by Firebase.auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    ProvideDera3Localization(localizationManager = localizationManager) {
        Dera3Theme(darkTheme = preferences.darkMode) {
            val user = currentUser
            if (user == null) {
                AuthScreen(
                    isDarkMode = preferences.darkMode,
                    onToggleDarkMode = {
                        scope.launch { prefsRepo.setDarkMode(!preferences.darkMode) }
                    },
                    onToggleLanguage = { localizationManager.toggleLanguage() },
                    onAuthSuccess = {},
                )
            } else {
                val uid = user.uid
                val isAdmin = user.email.equals(ADMIN_EMAIL, ignoreCase = true) && user.isEmailVerified
                key(uid) {
                    AuthenticatedAppContent(
                        currentUser = user,
                        isAdminUser = isAdmin,
                        prefsRepo = prefsRepo,
                        localizationManager = localizationManager,
                        openAlertsFromNotification = openAlertsFromNotification,
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedAppContent(
    currentUser: FirebaseUser,
    isAdminUser: Boolean,
    prefsRepo: PreferencesRepository,
    localizationManager: LocalizationManager,
    openAlertsFromNotification: Boolean = false,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: MainViewModel = viewModel(
        key = currentUser.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MainViewModel(
                    repository = Dera3Repository(db),
                    aiRepository = Dera3AiRepository(app),
                    prefsRepository = prefsRepo,
                    localizationManager = localizationManager,
                    currentUserId = currentUser.uid,
                    isAdminUser = isAdminUser,
                )
            }
        }
    )

    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val badgesState by viewModel.badgesState.collectAsStateWithLifecycle()
    val phishingNotificationsState by viewModel.phishingNotificationsState.collectAsStateWithLifecycle()
    val adminPhishingAlertsState by viewModel.adminPhishingAlertsState.collectAsStateWithLifecycle()
    val shieldConfig by viewModel.shieldConfig.collectAsStateWithLifecycle()
    val allUsersState by viewModel.allUsersState.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isChatGenerating by viewModel.isChatGenerating.collectAsStateWithLifecycle()
    val impersonatingUser by viewModel.impersonatingUser.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    val isSavingAuthStep by viewModel.isSavingAuthStep.collectAsStateWithLifecycle()

    var showAdminScreen by rememberSaveable(currentUser.uid) { mutableStateOf(isAdminUser) }
    val snackbarHostState = remember { SnackbarHostState() }
    val strings = LocalStrings.current
    var knownBadgeIds by remember(currentUser.uid, impersonatingUser?.userId) { mutableStateOf<Set<String>?>(null) }
    var knownAlertIds by remember(currentUser.uid, impersonatingUser?.userId) { mutableStateOf<Set<String>?>(null) }

    LaunchedEffect(preferences.language) {
        Dera3NotificationHelper.ensureChannels(context, preferences.language)
    }

    LaunchedEffect(preferences.notificationsEnabled) {
        Dera3NotificationHelper.syncFcmAlertSubscription(preferences.notificationsEnabled)
    }

    LaunchedEffect(badgesState, preferences.notificationsEnabled) {
        val currentBadges = (badgesState as? UiState.Success)?.data ?: return@LaunchedEffect
        val currentIds = currentBadges.map { it.badgeId }.toSet()
        val previous = knownBadgeIds
        if (previous != null && impersonatingUser == null) {
            val newlyUnlocked = currentBadges.firstOrNull { !previous.contains(it.badgeId) }
            if (newlyUnlocked != null) {
                val title = LocalizationManager.resolveLocalizedText(
                    preferences.language,
                    newlyUnlocked.titleAr,
                    newlyUnlocked.titleEn,
                )
                if (preferences.notificationsEnabled) {
                    Dera3NotificationHelper.showBadgeUnlockedNotification(
                        context = context,
                        badgeId = newlyUnlocked.badgeId,
                        badgeTitle = title,
                        language = preferences.language,
                    )
                }
                snackbarHostState.showSnackbar(
                    LocalizationManager.formatString(strings.badgeNewCelebrationFormat, title)
                )
            }
        }
        knownBadgeIds = currentIds
    }

    LaunchedEffect(phishingNotificationsState, preferences.notificationsEnabled) {
        val currentAlerts = (phishingNotificationsState as? UiState.Success)?.data ?: return@LaunchedEffect
        val currentIds = currentAlerts.map { "${it.isPersonalDetection}_${it.id}" }.toSet()
        val previous = knownAlertIds
        if (previous != null && preferences.notificationsEnabled && impersonatingUser == null) {
            val newUnreadAlerts = currentAlerts.filter {
                !it.read && !previous.contains("${it.isPersonalDetection}_${it.id}")
            }
            newUnreadAlerts.take(3).forEach { alertItem ->
                Dera3NotificationHelper.showSecurityAlertNotification(
                    context = context,
                    item = alertItem,
                    language = preferences.language,
                )
            }
            val firstUnread = newUnreadAlerts.firstOrNull()
            if (firstUnread != null) {
                val title = LocalizationManager.resolveLocalizedText(
                    preferences.language,
                    firstUnread.titleAr,
                    firstUnread.titleEn,
                )
                snackbarHostState.showSnackbar(
                    LocalizationManager.formatString(strings.phishingToastNewFormat, title)
                )
            }
        }
        knownAlertIds = currentIds
    }

    LaunchedEffect(actionError) {
        val err = actionError
        if (!err.isNullOrBlank()) {
            snackbarHostState.showSnackbar(err)
            viewModel.clearActionError()
        }
    }

    val performSignOut: () -> Unit = {
        signOutUser(credentialManager, scope)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = profileState) {
            UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                if (isAdminUser && showAdminScreen && impersonatingUser == null) {
                    AdminScreen(
                        allUsersState = allUsersState,
                        adminPhishingAlertsState = adminPhishingAlertsState,
                        shieldConfig = shieldConfig,
                        preferences = preferences,
                        hasOwnProfile = false,
                        onBackToMyHome = {},
                        onEnterAsUser = { user -> viewModel.enterAsUser(user) },
                        onToggleShield = { key, enabled -> viewModel.setShieldEnabled(key, enabled) },
                        onPublishPhishingAlert = viewModel::publishPhishingAlert,
                        onTogglePhishingAlertActive = viewModel::setPhishingAlertActive,
                        onDeletePhishingAlert = viewModel::deletePhishingAlert,
                        onToggleDarkMode = viewModel::toggleDarkMode,
                        onToggleLanguage = viewModel::toggleLanguage,
                        onSignOut = performSignOut,
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        StatusBanner(text = state.message, isDanger = true)
                    }
                }
            }
            is UiState.Success -> {
                val ownProfile = state.data
                val previewTarget = impersonatingUser
                when {
                    previewTarget != null -> {
                        HomeScreen(
                            userProfile = previewTarget,
                            badgesState = badgesState,
                            phishingNotificationsState = phishingNotificationsState,
                            shieldConfig = shieldConfig,
                            preferences = preferences,
                            aiState = aiState,
                            chatMessages = chatMessages,
                            isChatGenerating = isChatGenerating,
                            isAdminUser = isAdminUser,
                            isImpersonating = true,
                            isSavingAuthStep = isSavingAuthStep,
                            onExitImpersonation = viewModel::exitImpersonation,
                            onOpenAdminPanel = { viewModel.exitImpersonation() },
                            onToggleDarkMode = viewModel::toggleDarkMode,
                            onToggleLanguage = viewModel::toggleLanguage,
                            onSetNotifications = viewModel::setNotificationsEnabled,
                            onSetSound = viewModel::setSoundEnabled,
                            onSignOut = performSignOut,
                            onToggleCheck = viewModel::setCheckCompleted,
                            onSaveEmergencyContact = viewModel::saveEmergencyContact,
                            onTriggerHoneypot = viewModel::triggerHoneypot,
                            onAskAi = viewModel::sendSecurityChatMessage,
                            onRetryChatMessage = viewModel::retryChatMessage,
                            onClearChat = viewModel::clearSecurityChat,
                            onAcknowledgeNotification = viewModel::acknowledgeNotification,
                            onMarkAllNotificationsRead = viewModel::markAllNotificationsRead,
                            onReportThreat = viewModel::reportDetectedThreat,
                        )
                    }
                    isAdminUser && showAdminScreen -> {
                        AdminScreen(
                            allUsersState = allUsersState,
                            adminPhishingAlertsState = adminPhishingAlertsState,
                            shieldConfig = shieldConfig,
                            preferences = preferences,
                            hasOwnProfile = ownProfile != null,
                            onBackToMyHome = { showAdminScreen = false },
                            onEnterAsUser = { user -> viewModel.enterAsUser(user) },
                            onToggleShield = { key, enabled -> viewModel.setShieldEnabled(key, enabled) },
                            onPublishPhishingAlert = viewModel::publishPhishingAlert,
                            onTogglePhishingAlertActive = viewModel::setPhishingAlertActive,
                            onDeletePhishingAlert = viewModel::deletePhishingAlert,
                            onToggleDarkMode = viewModel::toggleDarkMode,
                            onToggleLanguage = viewModel::toggleLanguage,
                            onSignOut = performSignOut,
                        )
                    }
                    ownProfile == null -> {
                        RegistrationScreen(
                            defaultName = currentUser.displayName.orEmpty(),
                            defaultEmail = currentUser.email.orEmpty(),
                            isSaving = isSavingAuthStep,
                            errorMessage = actionError,
                            onCreateAccount = viewModel::createProfile,
                            onSignOut = performSignOut,
                        )
                    }
                    !ownProfile.phoneVerified && !isAdminUser -> {
                        OtpVerificationScreen(
                            phoneNumber = ownProfile.phoneNumber,
                            isSaving = isSavingAuthStep,
                            onVerified = viewModel::markPhoneVerified,
                            onSignOut = performSignOut,
                        )
                    }
                    else -> {
                        HomeScreen(
                            userProfile = ownProfile,
                            badgesState = badgesState,
                            phishingNotificationsState = phishingNotificationsState,
                            shieldConfig = shieldConfig,
                            preferences = preferences,
                            aiState = aiState,
                            chatMessages = chatMessages,
                            isChatGenerating = isChatGenerating,
                            isAdminUser = isAdminUser,
                            isImpersonating = false,
                            isSavingAuthStep = isSavingAuthStep,
                            onExitImpersonation = {},
                            onOpenAdminPanel = { showAdminScreen = true },
                            onToggleDarkMode = viewModel::toggleDarkMode,
                            onToggleLanguage = viewModel::toggleLanguage,
                            onSetNotifications = viewModel::setNotificationsEnabled,
                            onSetSound = viewModel::setSoundEnabled,
                            onSignOut = performSignOut,
                            onToggleCheck = viewModel::setCheckCompleted,
                            onSaveEmergencyContact = viewModel::saveEmergencyContact,
                            onTriggerHoneypot = viewModel::triggerHoneypot,
                            onAskAi = viewModel::sendSecurityChatMessage,
                            onRetryChatMessage = viewModel::retryChatMessage,
                            onClearChat = viewModel::clearSecurityChat,
                            onAcknowledgeNotification = viewModel::acknowledgeNotification,
                            onMarkAllNotificationsRead = viewModel::markAllNotificationsRead,
                            onReportThreat = viewModel::reportDetectedThreat,
                            onVerifyPhoneViaSms = viewModel::markPhoneVerified,
                            openAlertsOnLaunch = openAlertsFromNotification,
                            onShowToastMessage = { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            },
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
