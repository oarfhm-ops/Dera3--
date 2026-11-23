package com.example.data.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.localization.AppLanguage
import com.example.localization.LocalizationManager
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "Dera3AiRepo"
private const val AI_TIMEOUT_MS = 60_000L

enum class ChatRole {
    USER,
    ASSISTANT,
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val isOfflineFallback: Boolean = false,
    val errorMessage: String? = null,
    val offlineFallbackText: String? = null,
)

sealed interface AiAssistantState {
    data object Idle : AiAssistantState
    data object Loading : AiAssistantState
    data class Streaming(val text: String) : AiAssistantState
    data class Success(val text: String, val isOfflineFallback: Boolean = false) : AiAssistantState
    data class Error(val errorMessage: String, val offlineFallbackText: String) : AiAssistantState
}

class Dera3AiRepository(private val context: Context) {

    private val modelAr by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "أنت المساعد الأمني الذكي لتطبيق DERA3 (درعك الرقمي). " +
                        "تخاطب المستخدمين غير المتخصصين وكبار السن والأسر باللهجة المصرية البسيطة والواضحة والودودة. " +
                        "مهمتك تقديم نصائح فورية ومخصصة للحماية من النصب الإلكتروني والاحتيال والروابط الخبيثة وسرقة الحسابات. " +
                        "أجب دائماً في 5 خطوات عملية مرقمة أو أقل، مع مراعاة سياق المحادثة السابقة ودرجة أمان المستخدم إن وُجدت، وبدون مصطلحات تقنية معقدة."
                )
            }
        )
    }

    private val modelEn by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "You are the DERA3 (Your Digital Shield) Security Assistant, a friendly cybersecurity advisor for everyday users and seniors. " +
                        "Help users spot online security scams, phishing links, fake calls, and compromised accounts with instant, tailored advice. " +
                        "Always reply in 5 clear, actionable numbered steps or fewer, taking prior conversation turns and the user's safety score context into account without complex jargon."
                )
            }
        )
    }

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun askStream(question: String, language: AppLanguage): Flow<AiAssistantState> =
        sendChatMessageStream(
            history = emptyList(),
            question = question,
            language = language,
        )

    fun sendChatMessageStream(
        history: List<ChatMessage>,
        question: String,
        language: AppLanguage,
        safetyScore: Int? = null,
    ): Flow<AiAssistantState> = flow {
        val cleanQuestion = question.trim()
        if (cleanQuestion.isEmpty()) return@flow

        val strings = LocalizationManager.stringsFor(language)

        if (!isNetworkAvailable()) {
            emit(
                AiAssistantState.Success(
                    text = offlineAdviceFor(cleanQuestion, language),
                    isOfflineFallback = true,
                )
            )
            return@flow
        }

        emit(AiAssistantState.Loading)
        val activeModel = if (language == AppLanguage.AR) modelAr else modelEn
        val sdkHistory = buildSdkHistory(history)
        val wirePrompt = if (sdkHistory.isEmpty() && safetyScore != null) {
            "[DERA3 Safety Score: $safetyScore/100]\n$cleanQuestion"
        } else {
            cleanQuestion
        }

        val accumulated = StringBuilder()
        try {
            val chat = activeModel.startChat(history = sdkHistory)
            withTimeout(AI_TIMEOUT_MS) {
                chat.sendMessageStream(wirePrompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(AiAssistantState.Streaming(accumulated.toString()))
                    }
                }
            }
            if (accumulated.isNotBlank()) {
                emit(AiAssistantState.Success(accumulated.toString(), isOfflineFallback = false))
            } else {
                emit(
                    AiAssistantState.Error(
                        errorMessage = strings.securityAssistantEmptyError,
                        offlineFallbackText = offlineAdviceFor(cleanQuestion, language),
                    )
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini AI chat request timed out", e)
            emit(
                AiAssistantState.Error(
                    errorMessage = strings.securityAssistantTimeoutError,
                    offlineFallbackText = offlineAdviceFor(cleanQuestion, language),
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI Logic chat request failed", e)
            val msg = e.localizedMessage?.takeIf { it.isNotBlank() } ?: strings.securityAssistantGenericError
            emit(
                AiAssistantState.Error(
                    errorMessage = msg,
                    offlineFallbackText = offlineAdviceFor(cleanQuestion, language),
                )
            )
        }
    }

    fun offlineAdviceFor(question: String, language: AppLanguage): String {
        val q = question.lowercase()
        if (language == AppLanguage.EN) {
            return when {
                q.contains("link") || q.contains("url") || q.contains("click") || q.contains("sms") || q.contains("parcel") || q.contains("delivery") ->
                    "1. Never tap unfamiliar links from SMS, delivery notices, or WhatsApp.\n2. Check that the site starts with https:// and has no spelling mistakes.\n3. Avoid shortened links (bit.ly / tinyurl) or small payment fee requests.\n4. Paste the URL into Shield 6 (Predictive Link Scanner) in DERA3 before opening."
                q.contains("whatsapp") || q.contains("hack") || q.contains("gold") ->
                    "1. Open WhatsApp -> Linked Devices and remove any unknown device.\n2. Turn on Two-Step Verification with a 6-digit PIN.\n3. Uninstall WhatsApp Gold or any modded APK immediately.\n4. Never share your 6-digit SMS activation code."
                q.contains("password") || q.contains("pin") ->
                    "1. Use at least 12 characters with uppercase, lowercase, numbers, and symbols.\n2. Never reuse the same password across email, banking, and social apps.\n3. Use Shield 8 in DERA3 to generate a strong random password."
                q.contains("call") || q.contains("scam") || q.contains("bank") || q.contains("otp") ->
                    "1. Banks NEVER call asking for OTP codes or card PINs.\n2. Hang up immediately if someone pressures you urgently.\n3. Call the official bank number printed on your card.\n4. Block and report the caller in Shield 10."
                q.contains("wifi") || q.contains("wi-fi") ->
                    "1. Avoid banking or entering passwords on free café/airport Wi-Fi.\n2. Forget old open networks on your phone.\n3. Verify the exact network name with staff."
                else ->
                    "1. Never share OTP verification codes with anyone, even if they claim to be bank support.\n2. Avoid unknown links and unofficial APK downloads.\n3. Keep Two-Step Verification enabled on WhatsApp and Email.\n4. Review camera/mic permissions regularly in Settings."
            }
        }

        return when {
            q.contains("لينك") || q.contains("رابط") || q.contains("url") || q.contains("شحنة") || q.contains("رسوم") ->
                "١. متفتحش أي لينك غريب جالك في رسالة SMS أو واتساب أو طلب دفع رسوم شحنة.\n" +
                    "٢. اتأكد إن الرابط بيبدأ بـ https:// وإن اسم الموقع مكتوب صح من غير غلطات.\n" +
                    "٣. احذر الروابط المختصرة زي bit.ly أو اللي بتوعد بجوائز.\n" +
                    "٤. انسخ اللينك وافحصه الأول في (درع التنبؤ - فحص اللينكات) رقم 6 في التطبيق."

            q.contains("واتس") || q.contains("مخترق") || q.contains("متراقب") || q.contains("ذهب") || q.contains("whatsapp") ->
                "١. افتح واتساب فوراً ← الأجهزة المرتبطة، وامسح أي جهاز متعرفوش.\n" +
                    "٢. شغّل التحقق بخطوتين من الإعدادات ← الحساب برقم سري ليك.\n" +
                    "٣. لو عندك واتساب ذهبي أو معدل امسحه ونزل الأصلي من المتجر الرسمي.\n" +
                    "٤. أوعى تدي كود تفعيل الواتساب (6 أرقام) لأي شخص."

            q.contains("كلمة سر") || q.contains("باسورد") || q.contains("password") ->
                "١. اعمل كلمة سر لا تقل عن 12 حرف فيها حروف كبيرة وصغيرة وأرقام ورموز.\n" +
                    "٢. متستخدمش نفس كلمة السر في الإيميل والبنك والفيسبوك.\n" +
                    "٣. ابعد عن تاريخ ميلادك أو رقم موبايلك في كلمة السر.\n" +
                    "٤. استخدم (درع كلمة السر) رقم 8 لتوليد كلمة سر قوية فوراً."

            q.contains("مكالمة") || q.contains("نصب") || q.contains("بنك") || q.contains("كود") || q.contains("otp") ->
                "١. البنك عمره ما يطلب كود OTP أو رقم الكارت في التليفون.\n" +
                    "٢. لو حد قالك أنا من البنك وطلب كود، اقفل السكة فوراً.\n" +
                    "٣. اتصل إنت بالبنك على الرقم الرسمي المكتوب على ضهر الكارت.\n" +
                    "٤. اعمل حظر وبلاغ للرقم فوراً من (درع مكالمات النصب) رقم 10."

            q.contains("واي فاي") || q.contains("wifi") || q.contains("شبكة") ->
                "١. متدخلش على تطبيق البنك أو تكتب كلمات سر على واي فاي مفتوح في كافيه.\n" +
                    "٢. امسح الشبكات المفتوحة القديمة من إعدادات الواي فاي في موبايلك.\n" +
                    "٣. استخدم بيانات الموبايل لما تعمل أي معاملة مالية مهمة."

            q.contains("كبار") || q.contains("ماما") || q.contains("بابا") || q.contains("تيتة") ->
                "١. احفظ رقم حد من العيلة في (درع كبار السن) للاتصال به قبل أي تصرف.\n" +
                    "٢. ابعت لأهلك رسالة التحذير الجاهزة إن البنك مش بيطلب أكواد في المكالمات."

            else ->
                "١. أوعى تشارك أي كود تفعيل OTP مع أي حد حتى لو قالك من خدمة العملاء.\n" +
                    "٢. متفتحش لينكات جوائز أو عروض وهمية وماتنزلش تطبيقات من بره المتجر.\n" +
                    "٣. شغّل التحقق بخطوتين على الواتساب والإيميل.\n" +
                    "٤. افحص الدروع الـ 12 في DERA3 لرفع درجة أمانك إلى 100."
        }
    }

    companion object {
        fun buildSdkHistory(messages: List<ChatMessage>, maxTurns: Int = 12): List<Content> {
            val cleanMessages = messages
                .filter { !it.isStreaming && it.errorMessage == null && it.text.isNotBlank() }
                .takeLast(maxTurns)

            // Ensure Gemini chat history starts cleanly with a user turn when non-empty.
            val firstUserIdx = cleanMessages.indexOfFirst { it.role == ChatRole.USER }
            if (firstUserIdx < 0) return emptyList()

            return cleanMessages.drop(firstUserIdx).map { msg ->
                val role = if (msg.role == ChatRole.USER) "user" else "model"
                content(role) { text(msg.text) }
            }
        }
    }
}
