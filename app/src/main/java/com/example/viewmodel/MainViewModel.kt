package com.example.viewmodel

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiAssistantState
import com.example.data.ai.ChatMessage
import com.example.data.ai.ChatRole
import com.example.data.ai.Dera3AiRepository
import com.example.data.model.AppPreferences
import com.example.data.model.PhishingAlert
import com.example.data.model.PhishingCategory
import com.example.data.model.PhishingNotificationItem
import com.example.data.model.ShieldConfig
import com.example.data.model.ShieldId
import com.example.data.model.ThreatSeverity
import com.example.data.model.UiState
import com.example.data.model.UserBadge
import com.example.data.model.UserProfile
import com.example.data.preferences.PreferencesRepository
import com.example.data.repository.Dera3Repository
import com.example.localization.AppLanguage
import com.example.localization.LocalizationManager
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(
    private val repository: Dera3Repository,
    private val aiRepository: Dera3AiRepository,
    private val prefsRepository: PreferencesRepository,
    private val localizationManager: LocalizationManager,
    private val currentUserId: String,
    val isAdminUser: Boolean,
) : ViewModel() {

    val preferences: StateFlow<AppPreferences> = prefsRepository.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = AppPreferences()
        )

    val userProfileState: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing user profile", rootError)
            emit(UiState.Error(rootError.message ?: localizationManager.currentStrings.errorLoadProfile))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val shieldConfig: StateFlow<ShieldConfig> = repository.observeShieldConfig()
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing shield config", error)
            emit(ShieldConfig())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = ShieldConfig()
        )

    val allUsersState: StateFlow<UiState<List<UserProfile>>> = if (isAdminUser) {
        repository.observeAllUsers()
            .map<List<UserProfile>, UiState<List<UserProfile>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing all users", rootError)
                emit(UiState.Error(rootError.message ?: localizationManager.currentStrings.errorLoadUsers))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )
    } else {
        MutableStateFlow(UiState.Success(emptyList()))
    }

    private val _aiState = MutableStateFlow<AiAssistantState>(AiAssistantState.Idle)
    val aiState: StateFlow<AiAssistantState> = _aiState.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatGenerating = MutableStateFlow(false)
    val isChatGenerating: StateFlow<Boolean> = _isChatGenerating.asStateFlow()

    private val _impersonatingUser = MutableStateFlow<UserProfile?>(null)
    val impersonatingUser: StateFlow<UserProfile?> = _impersonatingUser.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val badgesState: StateFlow<UiState<List<UserBadge>>> = _impersonatingUser
        .map { impersonated -> impersonated?.userId?.ifBlank { impersonated.id } ?: currentUserId }
        .flatMapLatest { targetUid ->
            repository.observeUserBadges(targetUid)
                .map<List<UserBadge>, UiState<List<UserBadge>>> { UiState.Success(it) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing badges", rootError)
                    emit(UiState.Error(rootError.message ?: localizationManager.currentStrings.errorLoadBadges))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val phishingNotificationsState: StateFlow<UiState<List<PhishingNotificationItem>>> = _impersonatingUser
        .map { impersonated -> impersonated?.userId?.ifBlank { impersonated.id } ?: currentUserId }
        .flatMapLatest { targetUid ->
            combine(
                repository.observeActivePhishingAlerts(),
                repository.observeAlertAcks(targetUid),
                repository.observeDetectedThreats(targetUid),
            ) { globalAlerts, acks, threats ->
                val ackMap = acks.associateBy { it.alertId.ifBlank { it.id } }
                val globalItems = globalAlerts.map { alert ->
                    val ack = ackMap[alert.id]
                    val category = PhishingCategory.fromRaw(alert.category)
                    PhishingNotificationItem(
                        id = alert.id,
                        isPersonalDetection = false,
                        titleAr = alert.titleAr,
                        titleEn = alert.titleEn,
                        messageAr = alert.messageAr,
                        messageEn = alert.messageEn,
                        category = category,
                        severity = ThreatSeverity.fromRaw(alert.severity),
                        indicator = alert.indicator,
                        targetShield = category.targetShield,
                        read = ack?.read ?: false,
                        dismissed = ack?.dismissed ?: false,
                        timestampSeconds = alert.updatedAt?.seconds ?: alert.createdAt?.seconds ?: 0L,
                    )
                }
                val personalItems = threats.map { threat ->
                    val category = PhishingCategory.fromRaw(threat.category)
                    val matchedShield = ShieldId.entries.firstOrNull { it.key == threat.shieldKey }
                        ?: category.targetShield
                    PhishingNotificationItem(
                        id = threat.id,
                        isPersonalDetection = true,
                        titleAr = threat.titleAr,
                        titleEn = threat.titleEn,
                        messageAr = threat.messageAr,
                        messageEn = threat.messageEn,
                        category = category,
                        severity = ThreatSeverity.fromRaw(threat.severity),
                        indicator = threat.indicator,
                        targetShield = matchedShield,
                        read = threat.read,
                        dismissed = threat.dismissed,
                        timestampSeconds = threat.detectedAt?.seconds ?: threat.updatedAt?.seconds ?: 0L,
                    )
                }
                val merged = (personalItems + globalItems)
                    .sortedByDescending { it.timestampSeconds }
                UiState.Success(merged) as UiState<List<PhishingNotificationItem>>
            }.catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing phishing notifications", rootError)
                emit(UiState.Error(rootError.message ?: localizationManager.currentStrings.errorLoadAlerts))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val adminPhishingAlertsState: StateFlow<UiState<List<PhishingAlert>>> = if (isAdminUser) {
        repository.observeAllPhishingAlertsAdmin()
            .map<List<PhishingAlert>, UiState<List<PhishingAlert>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing admin phishing alerts", rootError)
                emit(UiState.Error(rootError.message ?: localizationManager.currentStrings.errorLoadAlerts))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )
    } else {
        MutableStateFlow(UiState.Success(emptyList()))
    }

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _isSavingAuthStep = MutableStateFlow(false)
    val isSavingAuthStep: StateFlow<Boolean> = _isSavingAuthStep.asStateFlow()

    private var aiJob: Job? = null

    fun toggleDarkMode() {
        viewModelScope.launch {
            prefsRepository.setDarkMode(!preferences.value.darkMode)
        }
    }

    fun toggleLanguage() {
        localizationManager.toggleLanguage()
    }

    fun setLanguage(language: AppLanguage) {
        localizationManager.setLanguage(language)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setNotificationsEnabled(enabled)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setSoundEnabled(enabled)
        }
    }

    fun clearActionError() {
        _actionError.value = null
    }

    fun createProfile(fullName: String, email: String, phone: String) {
        viewModelScope.launch {
            _isSavingAuthStep.value = true
            val result = repository.createUserProfile(fullName, email, phone)
            _isSavingAuthStep.value = false
            result.onFailure { _actionError.value = it.localizedMessage }
        }
    }

    fun markPhoneVerified(verifiedPhoneNumber: String? = null) {
        viewModelScope.launch {
            _isSavingAuthStep.value = true
            val result = repository.verifyPhoneOtp(currentUserId, verifiedPhoneNumber)
            _isSavingAuthStep.value = false
            if (result.isSuccess) playFeedbackTone()
            result.onFailure { _actionError.value = it.localizedMessage }
        }
    }

    fun setCheckCompleted(checkId: String, completed: Boolean) {
        if (_impersonatingUser.value != null) return
        viewModelScope.launch {
            val result = repository.setCheckCompleted(currentUserId, checkId, completed)
            if (result.isSuccess && completed) playFeedbackTone()
            result.onFailure { _actionError.value = it.localizedMessage }
        }
    }

    fun saveEmergencyContact(phone: String, onSuccess: () -> Unit) {
        if (_impersonatingUser.value != null) return
        viewModelScope.launch {
            val result = repository.saveEmergencyContact(currentUserId, phone)
            if (result.isSuccess) {
                playFeedbackTone()
                onSuccess()
            } else {
                _actionError.value = result.exceptionOrNull()?.localizedMessage
            }
        }
    }

    fun triggerHoneypot(isSimulation: Boolean) {
        if (_impersonatingUser.value != null) return
        viewModelScope.launch {
            val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val stamp = fmt.format(Date())
            val strings = localizationManager.currentStrings
            val entry = if (isSimulation) {
                LocalizationManager.formatString(strings.honeypotLogProbeTemplate, stamp)
            } else {
                LocalizationManager.formatString(strings.honeypotLogActivatedTemplate, stamp)
            }
            val result = repository.logHoneypotEvent(currentUserId, entry)
            if (result.isSuccess) playFeedbackTone()
            result.onFailure { _actionError.value = it.localizedMessage }
        }
    }

    fun askAiAssistant(question: String) {
        sendSecurityChatMessage(question)
    }

    fun sendSecurityChatMessage(question: String) {
        val clean = question.trim()
        if (clean.isEmpty()) return
        aiJob?.cancel()

        val priorHistory = _chatMessages.value
        val userMsg = ChatMessage(role = ChatRole.USER, text = clean)
        val assistantMsgId = java.util.UUID.randomUUID().toString()
        val assistantPlaceholder = ChatMessage(
            id = assistantMsgId,
            role = ChatRole.ASSISTANT,
            text = "",
            isStreaming = true,
        )
        _chatMessages.value = priorHistory + userMsg + assistantPlaceholder
        _isChatGenerating.value = true

        aiJob = viewModelScope.launch {
            try {
                val lang = preferences.value.language
                val targetUser = _impersonatingUser.value
                    ?: (userProfileState.value as? UiState.Success)?.data
                val safetyScore = targetUser?.securityScore?.toInt()

                aiRepository.sendChatMessageStream(
                    history = priorHistory,
                    question = clean,
                    language = lang,
                    safetyScore = safetyScore,
                ).collect { state ->
                    _aiState.value = state
                    when (state) {
                        AiAssistantState.Idle -> Unit
                        AiAssistantState.Loading -> Unit
                        is AiAssistantState.Streaming -> {
                            updateAssistantMessage(assistantMsgId) {
                                it.copy(text = state.text, isStreaming = true)
                            }
                        }
                        is AiAssistantState.Success -> {
                            updateAssistantMessage(assistantMsgId) {
                                it.copy(
                                    text = state.text,
                                    isStreaming = false,
                                    isOfflineFallback = state.isOfflineFallback,
                                    errorMessage = null,
                                )
                            }
                            if (_impersonatingUser.value == null) {
                                repository.awardAiQuestionPoint(currentUserId)
                                playFeedbackTone()
                            }
                        }
                        is AiAssistantState.Error -> {
                            updateAssistantMessage(assistantMsgId) {
                                it.copy(
                                    text = "",
                                    isStreaming = false,
                                    errorMessage = state.errorMessage,
                                    offlineFallbackText = state.offlineFallbackText,
                                )
                            }
                        }
                    }
                }
            } finally {
                _isChatGenerating.value = false
            }
        }
    }

    fun retryChatMessage(failedAssistantMessage: ChatMessage) {
        val current = _chatMessages.value
        val failedIdx = current.indexOfFirst { it.id == failedAssistantMessage.id }
        if (failedIdx <= 0) return
        val precedingUser = current.subList(0, failedIdx).lastOrNull { it.role == ChatRole.USER } ?: return
        val precedingUserIdx = current.indexOfLast { it.id == precedingUser.id }
        if (precedingUserIdx < 0) return
        _chatMessages.value = current.take(precedingUserIdx)
        sendSecurityChatMessage(precedingUser.text)
    }

    fun clearSecurityChat() {
        aiJob?.cancel()
        _isChatGenerating.value = false
        _chatMessages.value = emptyList()
        _aiState.value = AiAssistantState.Idle
    }

    private fun updateAssistantMessage(
        messageId: String,
        transform: (ChatMessage) -> ChatMessage,
    ) {
        _chatMessages.value = _chatMessages.value.map { msg ->
            if (msg.id == messageId) transform(msg) else msg
        }
    }

    fun setShieldEnabled(shieldKey: String, enabled: Boolean) {
        if (!isAdminUser) return
        viewModelScope.launch {
            val result = repository.setShieldEnabled(shieldKey, enabled)
            if (result.isSuccess) playFeedbackTone()
            result.onFailure { _actionError.value = it.localizedMessage }
        }
    }

    fun enterAsUser(user: UserProfile) {
        if (!isAdminUser) return
        _impersonatingUser.value = user
    }

    fun exitImpersonation() {
        _impersonatingUser.value = null
    }

    fun acknowledgeNotification(
        item: PhishingNotificationItem,
        read: Boolean,
        dismissed: Boolean,
    ) {
        if (_impersonatingUser.value != null) return
        viewModelScope.launch {
            val result = if (item.isPersonalDetection) {
                repository.acknowledgeDetectedThreat(currentUserId, item.id, read, dismissed)
            } else {
                repository.acknowledgeGlobalAlert(currentUserId, item.id, read, dismissed)
            }
            result.onFailure { _actionError.value = it.localizedMessage }
        }
    }

    fun markAllNotificationsRead() {
        if (_impersonatingUser.value != null) return
        val items = (phishingNotificationsState.value as? UiState.Success)?.data.orEmpty()
        val unread = items.filter { !it.read }
        if (unread.isEmpty()) return
        viewModelScope.launch {
            unread.forEach { item ->
                val res = if (item.isPersonalDetection) {
                    repository.acknowledgeDetectedThreat(currentUserId, item.id, read = true, dismissed = item.dismissed)
                } else {
                    repository.acknowledgeGlobalAlert(currentUserId, item.id, read = true, dismissed = item.dismissed)
                }
                res.onFailure { _actionError.value = it.localizedMessage }
            }
        }
    }

    fun reportDetectedThreat(
        shieldKey: String,
        category: PhishingCategory,
        severity: ThreatSeverity,
        indicator: String,
        titleAr: String,
        titleEn: String,
        messageAr: String,
        messageEn: String,
    ) {
        if (_impersonatingUser.value != null) return
        viewModelScope.launch {
            val result = repository.recordDetectedThreat(
                userId = currentUserId,
                shieldKey = shieldKey,
                category = category,
                severity = severity,
                indicator = indicator,
                titleAr = titleAr,
                titleEn = titleEn,
                messageAr = messageAr,
                messageEn = messageEn,
            )
            if (result.isSuccess && preferences.value.notificationsEnabled) {
                playFeedbackTone()
            }
            result.onFailure { _actionError.value = it.localizedMessage }
        }
    }

    fun publishPhishingAlert(
        titleAr: String,
        titleEn: String,
        messageAr: String,
        messageEn: String,
        category: PhishingCategory,
        severity: ThreatSeverity,
        indicator: String,
    ) {
        if (!isAdminUser) return
        viewModelScope.launch {
            val result = repository.publishPhishingAlert(
                titleAr = titleAr,
                titleEn = titleEn,
                messageAr = messageAr,
                messageEn = messageEn,
                category = category,
                severity = severity,
                indicator = indicator,
            )
            if (result.isSuccess) playFeedbackTone()
            result.onFailure { _actionError.value = it.localizedMessage }
        }
    }

    fun setPhishingAlertActive(alertId: String, active: Boolean) {
        if (!isAdminUser) return
        viewModelScope.launch {
            val result = repository.setPhishingAlertActive(alertId, active)
            if (result.isSuccess) playFeedbackTone()
            result.onFailure { _actionError.value = it.localizedMessage }
        }
    }

    fun deletePhishingAlert(alertId: String) {
        if (!isAdminUser) return
        viewModelScope.launch {
            val result = repository.deletePhishingAlert(alertId)
            result.onFailure { _actionError.value = it.localizedMessage }
        }
    }

    private fun playFeedbackTone() {
        if (!preferences.value.soundEnabled) return
        try {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 55)
            tone.startTone(ToneGenerator.TONE_PROP_ACK, 110)
        } catch (e: Exception) {
            Log.d(TAG, "Tone playback skipped: ${e.message}")
        }
    }

    private companion object {
        const val TAG = "MainViewModel"
    }
}
