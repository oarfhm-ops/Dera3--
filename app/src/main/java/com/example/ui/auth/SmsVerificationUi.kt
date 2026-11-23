package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.localization.LocalAppLanguage
import com.example.localization.LocalStrings
import com.example.localization.LocalizationManager
import com.example.ui.theme.LocalDera3Colors
import com.example.ui.theme.MonoFontFamily

@Composable
fun SmsVerificationContent(
    initialPhoneNumber: String,
    isSaving: Boolean,
    autoSendOnOpen: Boolean,
    onVerified: (verifiedPhoneE164: String) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
) {
    val strings = LocalStrings.current
    val dera3Colors = LocalDera3Colors.current
    val language = LocalAppLanguage.current
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    val controller = remember { SmsVerificationController(scope) }

    val status by controller.status.collectAsStateWithLifecycle()
    val resendSeconds by controller.resendSeconds.collectAsStateWithLifecycle()
    val fallbackUnlocked by controller.fallbackUnlocked.collectAsStateWithLifecycle()

    var currentPhone by remember(initialPhoneNumber) {
        mutableStateOf(PhoneNumberFormatter.toE164(initialPhoneNumber) ?: initialPhoneNumber.trim())
    }
    var editedPhone by remember(initialPhoneNumber) { mutableStateOf(currentPhone) }
    var isEditingPhone by remember { mutableStateOf(currentPhone.isBlank()) }
    var code by remember { mutableStateOf("") }
    var initialSendAttempted by remember { mutableStateOf(false) }

    LaunchedEffect(autoSendOnOpen, currentPhone) {
        if (autoSendOnOpen && !initialSendAttempted && PhoneNumberFormatter.toE164(currentPhone) != null) {
            initialSendAttempted = true
            controller.sendVerificationCode(
                activity = activity,
                rawPhone = currentPhone,
                language = language,
                strings = strings,
                forceResend = false,
                onAutoVerified = onVerified,
            )
        }
    }

    val isBusy = isSaving ||
        status is SmsVerificationStatus.SendingCode ||
        status is SmsVerificationStatus.VerifyingCode

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Sms,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp),
            )
        }

        Text(
            text = title ?: strings.otpTitle,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Text(
            text = subtitle ?: strings.otpSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        // Phone Display / Inline Edit Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = currentPhone.ifBlank { "—" },
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            editedPhone = currentPhone
                            isEditingPhone = !isEditingPhone
                        },
                        enabled = !isBusy,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("sms_edit_phone_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(strings.smsEditPhoneBtn)
                    }
                }

                AnimatedVisibility(visible = isEditingPhone) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editedPhone,
                            onValueChange = { editedPhone = it },
                            label = { Text(strings.smsPhoneInputHint) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sms_phone_edit_input"),
                        )
                        Button(
                            onClick = {
                                val normalized = PhoneNumberFormatter.toE164(editedPhone)
                                if (normalized != null) {
                                    currentPhone = normalized
                                    editedPhone = normalized
                                    isEditingPhone = false
                                    code = ""
                                }
                                controller.sendVerificationCode(
                                    activity = activity,
                                    rawPhone = normalized ?: editedPhone,
                                    language = language,
                                    strings = strings,
                                    forceResend = false,
                                    onAutoVerified = onVerified,
                                )
                            },
                            enabled = !isBusy,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("sms_save_and_send_button"),
                        ) {
                            Text(strings.smsSavePhoneBtn, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Live SMS Dispatch Status Banner
        when (val st = status) {
            SmsVerificationStatus.Idle -> Unit
            SmsVerificationStatus.SendingCode -> {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = strings.smsSendingProgress,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            is SmsVerificationStatus.CodeSent -> {
                Surface(
                    color = dera3Colors.safeContainer,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, dera3Colors.safe.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sms_code_sent_banner"),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = dera3Colors.safe,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = LocalizationManager.formatString(
                                    strings.smsCodeSentBannerFormat,
                                    PhoneNumberFormatter.maskPhone(currentPhone),
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = dera3Colors.safe,
                            )
                        }
                        if (!st.autoRetrievalTimedOut) {
                            Text(
                                text = strings.smsAutoRetrievingNote,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            SmsVerificationStatus.VerifyingCode -> {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = strings.smsVerifyingProgress,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            is SmsVerificationStatus.Verified -> {
                Surface(
                    color = dera3Colors.safeContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = dera3Colors.safe,
                        )
                        Text(
                            text = strings.smsVerifySuccessMsg,
                            color = dera3Colors.safe,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            is SmsVerificationStatus.Error -> {
                Surface(
                    color = dera3Colors.dangerContainer,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, dera3Colors.danger.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sms_error_banner"),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WarningAmber,
                            contentDescription = null,
                            tint = dera3Colors.danger,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = st.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = dera3Colors.danger,
                        )
                    }
                }
            }
        }

        if (fallbackUnlocked) {
            Surface(
                color = dera3Colors.cautionContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = strings.smsFallbackAvailableNote,
                    color = dera3Colors.caution,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        // Visual 6-digit OTP Boxes (always LTR for digit order)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                for (i in 0 until 6) {
                    val ch = code.getOrNull(i)?.toString().orEmpty()
                    val isFocusedSlot = code.length == i
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isFocusedSlot) 2.dp else 1.dp,
                            color = if (isFocusedSlot) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                        ),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = ch.ifEmpty { "•" },
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFontFamily),
                                fontWeight = FontWeight.Bold,
                                color = if (ch.isNotEmpty()) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                },
                            )
                        }
                    }
                }
            }
        }

        // Accessible numeric input field
        OutlinedTextField(
            value = code,
            onValueChange = { raw ->
                code = PhoneNumberFormatter.normalizeOtpDigits(raw)
                if (status is SmsVerificationStatus.Error) {
                    controller.resetError()
                }
            },
            label = { Text(strings.otpHint) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = MaterialTheme.typography.titleLarge.copy(
                fontFamily = MonoFontFamily,
                textAlign = TextAlign.Center,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("otp_code_input"),
        )

        // Verify Code CTA
        Button(
            onClick = {
                controller.verifyManualCode(
                    rawCode = code,
                    rawPhone = currentPhone,
                    strings = strings,
                    onVerified = onVerified,
                )
            },
            enabled = !isBusy && code.length == 6,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag("otp_verify_button"),
            shape = RoundedCornerShape(14.dp),
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(strings.verifyBtn, fontWeight = FontWeight.Bold)
            }
        }

        // Send / Resend SMS Button with Cooldown
        OutlinedButton(
            onClick = {
                val hasSentOnce = status is SmsVerificationStatus.CodeSent || resendSeconds > 0
                controller.sendVerificationCode(
                    activity = activity,
                    rawPhone = currentPhone,
                    language = language,
                    strings = strings,
                    forceResend = hasSentOnce,
                    onAutoVerified = onVerified,
                )
            },
            enabled = !isBusy && resendSeconds == 0 && !isEditingPhone,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("otp_resend_button"),
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = when {
                    resendSeconds > 0 -> LocalizationManager.formatString(
                        strings.smsResendCountdownFormat,
                        resendSeconds,
                    )
                    status is SmsVerificationStatus.Idle && !initialSendAttempted -> strings.smsSendCodeBtn
                    else -> strings.smsResendCodeBtn
                },
                fontWeight = FontWeight.SemiBold,
            )
        }

        Text(
            text = strings.smsCarrierRatesNote,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsVerificationBottomSheet(
    initialPhoneNumber: String,
    isSaving: Boolean,
    onVerified: (verifiedPhoneE164: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("sms_verification_sheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            SmsVerificationContent(
                initialPhoneNumber = initialPhoneNumber,
                isSaving = isSaving,
                autoSendOnOpen = false,
                onVerified = { verifiedPhone ->
                    onVerified(verifiedPhone)
                    onDismiss()
                },
                title = strings.smsChangePhoneTitle,
                subtitle = strings.smsChangePhoneSubtitle,
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}
