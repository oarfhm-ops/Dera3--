package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.ai.AiAssistantState
import com.example.data.ai.ChatMessage
import com.example.data.ai.ChatRole
import com.example.domain.CallerOrigin
import com.example.domain.FakeAppAnalysisResult
import com.example.domain.PasswordStrength
import com.example.domain.SecurityAnalyzers
import com.example.localization.LocalStrings
import com.example.localization.LocalizationManager
import com.example.ui.theme.LocalDera3Colors
import com.example.ui.theme.MonoFontFamily

@Composable
fun HoneypotShieldContent(
    completedChecks: List<String>,
    honeypotLogs: List<String>,
    onTriggerHoneypot: (isSimulation: Boolean) -> Unit,
) {
    val strings = LocalStrings.current
    val isActive = completedChecks.contains("honeypot_active")

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = strings.honeypotDesc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (isActive) {
            StatusBanner(text = strings.honeypotActiveBadge, isDanger = false)
        }

        Button(
            onClick = { onTriggerHoneypot(false) },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("honeypot_activate_button"),
        ) {
            Icon(Icons.Filled.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("${strings.honeypotActivateBtn} ${LocalizationManager.formatString(strings.ptsInlineFormat, 10)}")
        }

        OutlinedButton(
            onClick = { onTriggerHoneypot(true) },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("honeypot_simulate_button"),
        ) {
            Text(strings.honeypotSimulateBtn)
        }

        Text(
            text = strings.honeypotLogsTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        if (honeypotLogs.isEmpty()) {
            Text(
                text = strings.honeypotEmptyLogs,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            honeypotLogs.take(5).forEach { log ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = log,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFontFamily),
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun PasswordShieldContent(
    completedChecks: List<String>,
    onAwardCheck: (String) -> Unit,
) {
    val strings = LocalStrings.current
    val clipboard = LocalClipboardManager.current
    var passwordInput by remember { mutableStateOf("") }
    var strength by remember { mutableStateOf<PasswordStrength?>(null) }
    var generatedPassword by remember { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = passwordInput,
            onValueChange = { passwordInput = it },
            label = { Text(strings.passwordInputHint) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("password_shield_input"),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    if (passwordInput.isNotEmpty()) {
                        strength = SecurityAnalyzers.evaluatePasswordStrength(passwordInput)
                        if (!completedChecks.contains("pw_check")) onAwardCheck("pw_check")
                    }
                },
                modifier = Modifier.weight(1f).height(48.dp).testTag("password_check_button"),
            ) {
                Text(strings.passwordCheckBtn)
            }

            FilledTonalButton(
                onClick = {
                    generatedPassword = SecurityAnalyzers.generateStrongPassword(12)
                    copied = false
                    if (!completedChecks.contains("pw_gen")) onAwardCheck("pw_gen")
                },
                modifier = Modifier.weight(1f).height(48.dp).testTag("password_generate_button"),
            ) {
                Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(strings.passwordGenerateBtn, maxLines = 1)
            }
        }

        strength?.let { s ->
            when (s) {
                PasswordStrength.WEAK -> StatusBanner(strings.passwordWeak, isDanger = true)
                PasswordStrength.MEDIUM -> StatusBanner(strings.passwordMedium, isDanger = false, isCaution = true)
                PasswordStrength.STRONG -> StatusBanner(strings.passwordStrong, isDanger = false)
            }
        }

        generatedPassword?.let { pw ->
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.passwordGeneratedLabel,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            text = pw,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                    IconButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(pw))
                            copied = true
                        },
                        modifier = Modifier.testTag("password_copy_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = strings.copyBtn,
                        )
                    }
                }
            }
            if (copied) {
                Text(
                    text = strings.copiedToast,
                    color = LocalDera3Colors.current.safe,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
fun FakeAppsShieldContent(
    completedChecks: List<String>,
    onAwardScan: () -> Unit,
    onFakeAppDetected: (String) -> Unit = {},
) {
    val strings = LocalStrings.current
    var appName by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<FakeAppAnalysisResult?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = appName,
            onValueChange = { appName = it },
            label = { Text(strings.fakeAppInputHint) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("fake_app_input"),
        )
        Button(
            onClick = {
                if (appName.isNotBlank()) {
                    val res = SecurityAnalyzers.analyzeAppName(appName)
                    result = res
                    if (!completedChecks.contains("fake_app_scan")) onAwardScan()
                    if (res.isFakeOrModded) onFakeAppDetected(appName.trim())
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("fake_app_scan_button"),
        ) {
            Text("${strings.scanBtn} ${LocalizationManager.formatString(strings.ptsInlineFormat, 5)}")
        }

        result?.let {
            StatusBanner(
                text = if (it.isFakeOrModded) strings.fakeAppDanger else strings.fakeAppSafe,
                isDanger = it.isFakeOrModded,
            )
        }

        Text(
            text = strings.fakeAppGuide,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ScamCallsShieldContent(
    completedChecks: List<String>,
    onAwardScan: () -> Unit,
    onScamCallerDetected: (String) -> Unit = {},
) {
    val strings = LocalStrings.current
    var phone by remember { mutableStateOf("") }
    var origin by remember { mutableStateOf<CallerOrigin?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text(strings.scamCallInputHint) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("scam_call_input"),
        )
        Button(
            onClick = {
                if (phone.isNotBlank()) {
                    val detectedOrigin = SecurityAnalyzers.analyzeCallerNumber(phone)
                    origin = detectedOrigin
                    if (!completedChecks.contains("scam_call_scan")) onAwardScan()
                    if (detectedOrigin == CallerOrigin.FOREIGN_OR_UNKNOWN) {
                        onScamCallerDetected(phone.trim())
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("scam_call_scan_button"),
        ) {
            Text("${strings.scanBtn} ${LocalizationManager.formatString(strings.ptsInlineFormat, 5)}")
        }

        origin?.let {
            StatusBanner(
                text = if (it == CallerOrigin.EGYPTIAN) strings.scamCallEgyptian else strings.scamCallForeign,
                isDanger = it == CallerOrigin.FOREIGN_OR_UNKNOWN,
                isCaution = it == CallerOrigin.EGYPTIAN,
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = strings.scamCallGuide,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiAssistantShieldContent(
    aiState: AiAssistantState,
    chatMessages: List<ChatMessage> = emptyList(),
    onAskQuestion: (String) -> Unit,
    onOpenFullChat: () -> Unit = {},
) {
    val strings = LocalStrings.current
    var question by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = strings.aiPoweredBy,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        FilledTonalButton(
            onClick = onOpenFullChat,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("ai_open_full_chat_button"),
        ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(strings.securityAssistantOpenFullChatBtn, fontWeight = FontWeight.Bold)
        }

        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            label = { Text(strings.aiInputHint) },
            placeholder = { Text(strings.aiExampleHint) },
            modifier = Modifier.fillMaxWidth().testTag("ai_question_input"),
        )

        Button(
            onClick = {
                if (question.isNotBlank()) {
                    onAskQuestion(question)
                }
            },
            enabled = aiState !is AiAssistantState.Loading,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("ai_ask_button"),
        ) {
            if (aiState is AiAssistantState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("${strings.aiAskBtn} ${LocalizationManager.formatString(strings.ptsInlineFormat, 5)}")
            }
        }

        if (chatMessages.isNotEmpty()) {
            val latestAssistant = chatMessages.lastOrNull { it.role == ChatRole.ASSISTANT && it.text.isNotBlank() }
            if (latestAssistant != null && aiState is AiAssistantState.Idle) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = latestAssistant.text,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }

        // 4 Quick Ready Questions
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            strings.aiQuickQuestions.forEachIndexed { idx, prompt ->
                AssistChip(
                    onClick = {
                        question = prompt
                        onAskQuestion(prompt)
                    },
                    label = { Text(prompt) },
                    modifier = Modifier.testTag("ai_quick_chip_$idx"),
                )
            }
        }

        // Streaming / Success / Error + Offline Fallback UI
        when (aiState) {
            AiAssistantState.Idle -> Unit
            AiAssistantState.Loading -> Unit
            is AiAssistantState.Streaming -> {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = aiState.text,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }
            is AiAssistantState.Success -> {
                if (aiState.isOfflineFallback) {
                    Surface(
                        color = LocalDera3Colors.current.cautionContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudOff,
                                contentDescription = null,
                                tint = LocalDera3Colors.current.caution,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = strings.aiOfflineNotice,
                                color = LocalDera3Colors.current.caution,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = aiState.text,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }
            is AiAssistantState.Error -> {
                StatusBanner(text = aiState.errorMessage, isDanger = true)
                Surface(
                    color = LocalDera3Colors.current.cautionContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = strings.aiOfflineNotice,
                            color = LocalDera3Colors.current.caution,
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = aiState.offlineFallbackText,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}
