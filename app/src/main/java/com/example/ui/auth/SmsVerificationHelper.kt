package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.example.localization.AppLanguage
import com.example.localization.Dera3Strings
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

object PhoneNumberFormatter {

    fun normalizeDigits(raw: String): String {
        val sb = StringBuilder(raw.length)
        for (ch in raw) {
            when (ch) {
                in '٠'..'٩' -> sb.append(('0'.code + (ch.code - '٠'.code)).toChar())
                in '۰'..'۹' -> sb.append(('0'.code + (ch.code - '۰'.code)).toChar())
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toE164(raw: String): String? {
        val ascii = normalizeDigits(raw).trim()
        if (ascii.isEmpty()) return null

        val cleaned = buildString {
            ascii.forEachIndexed { idx, ch ->
                when {
                    ch == '+' && idx == 0 -> append(ch)
                    ch.isDigit() -> append(ch)
                }
            }
        }
        if (cleaned.isEmpty()) return null

        val normalized = when {
            cleaned.startsWith("+") -> {
                val digits = cleaned.drop(1)
                if (digits.length in 8..15 && !digits.startsWith("0")) "+$digits" else null
            }
            cleaned.startsWith("00") -> {
                val digits = cleaned.drop(2)
                if (digits.length in 8..15 && !digits.startsWith("0")) "+$digits" else null
            }
            // Egyptian local mobile format: 010/011/012/015 + 8 digits
            cleaned.length == 11 &&
                (cleaned.startsWith("010") ||
                    cleaned.startsWith("011") ||
                    cleaned.startsWith("012") ||
                    cleaned.startsWith("015")) -> "+2$cleaned"
            // Egyptian format missing '+'
            cleaned.length == 12 &&
                (cleaned.startsWith("2010") ||
                    cleaned.startsWith("2011") ||
                    cleaned.startsWith("2012") ||
                    cleaned.startsWith("2015")) -> "+$cleaned"
            // Saudi local mobile format: 05 + 8 digits
            cleaned.length == 10 && cleaned.startsWith("05") -> "+966${cleaned.drop(1)}"
            cleaned.length in 10..15 && !cleaned.startsWith("0") -> "+$cleaned"
            else -> null
        }

        return normalized
    }

    fun normalizeOtpDigits(raw: String): String {
        return normalizeDigits(raw).filter { it.isDigit() }.take(6)
    }

    fun extractOtpFromSms(message: String): String? {
        val normalized = normalizeDigits(message)
        val match = Regex("""\b(\d{6})\b""").find(normalized)
        return match?.groupValues?.getOrNull(1)
    }

    fun maskPhone(raw: String): String {
        val trimmed = normalizeDigits(raw).trim()
        if (trimmed.length <= 6) return trimmed
        val prefixLen = if (trimmed.startsWith("+")) 5 else 4
        val prefix = trimmed.take(prefixLen)
        val suffix = trimmed.takeLast(2)
        val hiddenCount = (trimmed.length - prefixLen - 2).coerceAtLeast(3)
        return "$prefix${"•".repeat(hiddenCount)}$suffix"
    }
}

sealed interface SmsVerificationStatus {
    data object Idle : SmsVerificationStatus
    data object SendingCode : SmsVerificationStatus
    data class CodeSent(
        val verificationId: String,
        val autoRetrievalTimedOut: Boolean = false,
    ) : SmsVerificationStatus
    data object VerifyingCode : SmsVerificationStatus
    data class Verified(val phoneE164: String) : SmsVerificationStatus
    data class Error(
        val message: String,
        val fallbackUnlocked: Boolean = false,
    ) : SmsVerificationStatus
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

class SmsVerificationController(
    private val scope: CoroutineScope,
) {
    private val _status = MutableStateFlow<SmsVerificationStatus>(SmsVerificationStatus.Idle)
    val status: StateFlow<SmsVerificationStatus> = _status.asStateFlow()

    private val _resendSeconds = MutableStateFlow(0)
    val resendSeconds: StateFlow<Int> = _resendSeconds.asStateFlow()

    private val _fallbackUnlocked = MutableStateFlow(false)
    val fallbackUnlocked: StateFlow<Boolean> = _fallbackUnlocked.asStateFlow()

    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var countdownJob: Job? = null

    fun resetError() {
        val currentId = storedVerificationId
        _status.value = if (currentId != null) {
            SmsVerificationStatus.CodeSent(currentId)
        } else {
            SmsVerificationStatus.Idle
        }
    }

    fun sendVerificationCode(
        activity: Activity?,
        rawPhone: String,
        language: AppLanguage,
        strings: Dera3Strings,
        forceResend: Boolean = false,
        onAutoVerified: (String) -> Unit,
    ) {
        val e164 = PhoneNumberFormatter.toE164(rawPhone)
        if (e164 == null) {
            _status.value = SmsVerificationStatus.Error(
                message = strings.smsPhoneFormatError,
                fallbackUnlocked = false,
            )
            return
        }

        if (_status.value is SmsVerificationStatus.SendingCode ||
            _status.value is SmsVerificationStatus.VerifyingCode
        ) {
            return
        }

        _status.value = SmsVerificationStatus.SendingCode

        val auth = Firebase.auth
        auth.setLanguageCode(if (language == AppLanguage.AR) "ar" else "en")

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d(TAG, "SMS onVerificationCompleted (auto/instant)")
                applyPhoneCredential(
                    credential = credential,
                    phoneE164 = e164,
                    strings = strings,
                    onVerified = onAutoVerified,
                )
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.w(TAG, "SMS onVerificationFailed: ${e.message}", e)
                val code = (e as? FirebaseAuthException)?.errorCode.orEmpty()
                val msgLower = e.message.orEmpty().lowercase()
                val isConfigOrEmulatorRestriction =
                    e is FirebaseAuthMissingActivityForRecaptchaException ||
                        code == "ERROR_OPERATION_NOT_ALLOWED" ||
                        code == "ERROR_APP_NOT_AUTHORIZED" ||
                        code == "ERROR_MISSING_CLIENT_IDENTIFIER" ||
                        msgLower.contains("region") ||
                        msgLower.contains("sms") ||
                        msgLower.contains("recaptcha") ||
                        msgLower.contains("play integrity") ||
                        msgLower.contains("blocked") ||
                        msgLower.contains("network")

                val message = when {
                    e is FirebaseAuthInvalidCredentialsException -> strings.smsPhoneFormatError
                    e is FirebaseTooManyRequestsException -> strings.smsQuotaExceededError
                    isConfigOrEmulatorRestriction -> strings.smsProviderDisabledError
                    else -> e.localizedMessage ?: strings.smsGenericFailedError
                }

                val unlockFallback = e !is FirebaseAuthInvalidCredentialsException
                if (unlockFallback) {
                    _fallbackUnlocked.value = true
                }
                _status.value = SmsVerificationStatus.Error(
                    message = message,
                    fallbackUnlocked = unlockFallback,
                )
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken,
            ) {
                Log.d(TAG, "SMS onCodeSent: verificationId received")
                storedVerificationId = verificationId
                resendToken = token
                _status.value = SmsVerificationStatus.CodeSent(verificationId)
                startResendCountdown()
            }

            override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
                storedVerificationId = verificationId
                if (_status.value is SmsVerificationStatus.CodeSent) {
                    _status.value = SmsVerificationStatus.CodeSent(
                        verificationId = verificationId,
                        autoRetrievalTimedOut = true,
                    )
                }
            }
        }

        val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(e164)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setCallbacks(callbacks)

        if (activity != null) {
            optionsBuilder.setActivity(activity)
        }
        val activeToken = resendToken
        if (forceResend && activeToken != null) {
            optionsBuilder.setForceResendingToken(activeToken)
        }

        try {
            PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
        } catch (e: Exception) {
            Log.w(TAG, "PhoneAuthProvider.verifyPhoneNumber exception", e)
            _fallbackUnlocked.value = true
            _status.value = SmsVerificationStatus.Error(
                message = strings.smsProviderDisabledError,
                fallbackUnlocked = true,
            )
        }
    }

    fun verifyManualCode(
        rawCode: String,
        rawPhone: String,
        strings: Dera3Strings,
        onVerified: (String) -> Unit,
    ) {
        val code = PhoneNumberFormatter.normalizeOtpDigits(rawCode)
        val e164 = PhoneNumberFormatter.toE164(rawPhone) ?: rawPhone.trim()
        val verificationId = storedVerificationId

        if (code.length != 6) {
            _status.value = SmsVerificationStatus.Error(
                message = strings.smsInvalidCodeError,
                fallbackUnlocked = _fallbackUnlocked.value,
            )
            return
        }

        if (verificationId != null) {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            applyPhoneCredential(
                credential = credential,
                phoneE164 = e164,
                strings = strings,
                onVerified = onVerified,
            )
            return
        }

        if (_fallbackUnlocked.value && code == DEMO_FALLBACK_CODE) {
            _status.value = SmsVerificationStatus.Verified(e164)
            onVerified(e164)
            return
        }

        _status.value = SmsVerificationStatus.Error(
            message = strings.smsInvalidCodeError,
            fallbackUnlocked = _fallbackUnlocked.value,
        )
    }

    private fun applyPhoneCredential(
        credential: PhoneAuthCredential,
        phoneE164: String,
        strings: Dera3Strings,
        onVerified: (String) -> Unit,
    ) {
        scope.launch {
            _status.value = SmsVerificationStatus.VerifyingCode
            try {
                val user = Firebase.auth.currentUser
                if (user != null) {
                    user.updatePhoneNumber(credential).await()
                }
                _status.value = SmsVerificationStatus.Verified(phoneE164)
                onVerified(phoneE164)
            } catch (e: FirebaseAuthUserCollisionException) {
                Log.w(TAG, "Phone already linked to another account", e)
                _status.value = SmsVerificationStatus.Error(
                    message = strings.smsCollisionError,
                    fallbackUnlocked = false,
                )
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                Log.w(TAG, "Invalid SMS verification code", e)
                _status.value = SmsVerificationStatus.Error(
                    message = strings.smsInvalidCodeError,
                    fallbackUnlocked = _fallbackUnlocked.value,
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed applying phone credential", e)
                _status.value = SmsVerificationStatus.Error(
                    message = e.localizedMessage ?: strings.smsInvalidCodeError,
                    fallbackUnlocked = _fallbackUnlocked.value,
                )
            }
        }
    }

    private fun startResendCountdown() {
        countdownJob?.cancel()
        countdownJob = scope.launch {
            for (sec in 60 downTo 0) {
                _resendSeconds.value = sec
                if (sec > 0) delay(1000L)
            }
        }
    }

    companion object {
        private const val TAG = "SmsVerification"
        const val DEMO_FALLBACK_CODE = "123456"
    }
}
