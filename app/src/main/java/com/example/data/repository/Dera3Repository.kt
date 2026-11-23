package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AlertAck
import com.example.data.model.DetectedThreat
import com.example.data.model.PhishingAlert
import com.example.data.model.PhishingCategory
import com.example.data.model.SecurityBadge
import com.example.data.model.ShieldConfig
import com.example.data.model.ThreatSeverity
import com.example.data.model.UserBadge
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

object ShieldScorePoints {
    val pointsByCheckId: Map<String, Int> = mapOf(
        // Shield 1: Wi-Fi (15 pts)
        "wifi_1" to 5,
        "wifi_2" to 5,
        "wifi_3" to 5,
        // Shield 2: WhatsApp (20 pts)
        "wa_1" to 5,
        "wa_2" to 5,
        "wa_3" to 5,
        "wa_btn" to 5,
        // Shield 3: Data Breach (10 pts)
        "breach_1" to 5,
        "breach_2" to 5,
        // Shield 4: Privacy / Surveillance (15 pts)
        "priv_1" to 5,
        "priv_2" to 5,
        "priv_3" to 5,
        // Shield 5: Elderly (10 pts)
        "elderly_contact" to 10,
        // Shield 6: Link Scanner (10 pts)
        "link_scan" to 10,
        // Shield 7: Honeypot (10 pts)
        "honeypot_active" to 10,
        // Shield 8: Password (10 pts)
        "pw_check" to 5,
        "pw_gen" to 5,
        // Shield 9: Fake Apps (5 pts)
        "fake_app_scan" to 5,
        // Shield 10: Scam Calls (5 pts)
        "scam_call_scan" to 5,
        // Shield 11: AI Assistant (up to 15 pts across first 3 questions)
        "ai_ask_1" to 5,
        "ai_ask_2" to 5,
        "ai_ask_3" to 5,
        // Shield 12: Safe Browser (10 pts)
        "safe_browser_use" to 10,
    )

    fun calculateScore(completedChecks: List<String>): Long {
        val total = completedChecks.distinct().sumOf { pointsByCheckId[it] ?: 5 }
        return total.coerceIn(0, 100).toLong()
    }
}

class Dera3Repository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        if (userId.isBlank()) return flowOf(null)
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, path)
                }
                throw error
            }
    }

    fun observeUserBadges(userId: String): Flow<List<UserBadge>> {
        if (userId.isBlank()) return flowOf(emptyList())
        val path = "users/$userId/badges"
        return db.collection("users").document(userId)
            .collection("badges")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    UserBadge::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.threshold }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeShieldConfig(): Flow<ShieldConfig> {
        if (auth.currentUser == null) return flowOf(ShieldConfig())
        val path = "app_config/shields"
        return db.collection("app_config").document("shields")
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        ShieldConfig::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: ShieldConfig()
                } else {
                    ShieldConfig()
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, path)
                }
                // Fallback to default all-enabled config on read error if non-cancellation
                emit(ShieldConfig())
            }
    }

    fun observeAllUsers(): Flow<List<UserProfile>> = flow {
        val currentEmail = auth.currentUser?.email
        if (auth.currentUser == null) {
            emit(emptyList())
            return@flow
        }
        Log.d("Dera3Repo", "Observing all users for admin: $currentEmail")
        val path = "users"
        db.collection("users")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun createUserProfile(
        fullName: String,
        email: String,
        phoneNumber: String,
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val docPath = "users/$uid"
        return try {
            val activeUid = requireUserId()
            val cleanName = fullName.trim().take(100)
            val cleanEmail = email.trim().take(150)
            val cleanPhone = phoneNumber.trim().take(30)
            require(cleanName.isNotEmpty()) { "Full name is required" }
            require(cleanEmail.length >= 3) { "Valid email is required" }

            val payload = mapOf(
                "userId" to activeUid,
                "fullName" to cleanName,
                "email" to cleanEmail,
                "phoneNumber" to cleanPhone,
                "phoneVerified" to false,
                "securityScore" to 0L,
                "completedChecks" to emptyList<String>(),
                "emergencyContact" to "",
                "honeypotLogs" to emptyList<String>(),
                "passwordMasked" to "••••••••",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            db.collection("users").document(activeUid).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun verifyPhoneOtp(
        userId: String,
        verifiedPhoneNumber: String? = null,
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val activeUid = requireUserId()
            check(activeUid == userId) { "User mismatch" }
            val updates = mutableMapOf<String, Any>(
                "phoneVerified" to true,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            val cleanPhone = verifiedPhoneNumber?.trim()?.take(30).orEmpty()
            if (cleanPhone.isNotEmpty()) {
                updates["phoneNumber"] = cleanPhone
            }
            db.collection("users").document(userId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun setCheckCompleted(
        userId: String,
        checkId: String,
        completed: Boolean,
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val activeUid = requireUserId()
            check(activeUid == userId) { "User mismatch" }
            val docRef = db.collection("users").document(userId)
            val newScore = db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val existing = (snap.get("completedChecks") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?.toMutableList()
                    ?: mutableListOf()
                if (completed) {
                    if (!existing.contains(checkId) && existing.size < 50) {
                        existing.add(checkId)
                    }
                } else {
                    existing.remove(checkId)
                }
                val calculated = ShieldScorePoints.calculateScore(existing)
                tx.update(
                    docRef,
                    mapOf(
                        "completedChecks" to existing,
                        "securityScore" to calculated,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
                calculated
            }.await()
            syncBadgesForScore(userId, newScore)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun awardAiQuestionPoint(userId: String): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val activeUid = requireUserId()
            check(activeUid == userId) { "User mismatch" }
            val docRef = db.collection("users").document(userId)
            val newScore = db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val existing = (snap.get("completedChecks") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?.toMutableList()
                    ?: mutableListOf()
                val nextSlot = listOf("ai_ask_1", "ai_ask_2", "ai_ask_3").firstOrNull { !existing.contains(it) }
                if (nextSlot != null && existing.size < 50) {
                    existing.add(nextSlot)
                    val calculated = ShieldScorePoints.calculateScore(existing)
                    tx.update(
                        docRef,
                        mapOf(
                            "completedChecks" to existing,
                            "securityScore" to calculated,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                    calculated
                } else {
                    (snap.get("securityScore") as? Number)?.toLong() ?: 0L
                }
            }.await()
            syncBadgesForScore(userId, newScore)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveEmergencyContact(userId: String, phone: String): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val activeUid = requireUserId()
            check(activeUid == userId) { "User mismatch" }
            val cleanPhone = phone.trim().take(30)
            require(cleanPhone.isNotEmpty()) { "Emergency phone number cannot be empty" }
            val docRef = db.collection("users").document(userId)
            val newScore = db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val existing = (snap.get("completedChecks") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?.toMutableList()
                    ?: mutableListOf()
                if (!existing.contains("elderly_contact") && existing.size < 50) {
                    existing.add("elderly_contact")
                }
                val calculated = ShieldScorePoints.calculateScore(existing)
                tx.update(
                    docRef,
                    mapOf(
                        "emergencyContact" to cleanPhone,
                        "completedChecks" to existing,
                        "securityScore" to calculated,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
                calculated
            }.await()
            syncBadgesForScore(userId, newScore)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun logHoneypotEvent(userId: String, logEntry: String): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val activeUid = requireUserId()
            check(activeUid == userId) { "User mismatch" }
            val docRef = db.collection("users").document(userId)
            val newScore = db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val existingChecks = (snap.get("completedChecks") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?.toMutableList()
                    ?: mutableListOf()
                if (!existingChecks.contains("honeypot_active") && existingChecks.size < 50) {
                    existingChecks.add("honeypot_active")
                }
                val existingLogs = (snap.get("honeypotLogs") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?.toMutableList()
                    ?: mutableListOf()
                existingLogs.add(0, logEntry.take(160))
                val trimmedLogs = existingLogs.take(50)
                val calculated = ShieldScorePoints.calculateScore(existingChecks)
                tx.update(
                    docRef,
                    mapOf(
                        "honeypotLogs" to trimmedLogs,
                        "completedChecks" to existingChecks,
                        "securityScore" to calculated,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
                calculated
            }.await()
            syncBadgesForScore(userId, newScore)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    private suspend fun syncBadgesForScore(userId: String, score: Long) {
        for (badge in SecurityBadge.entries) {
            if (score >= badge.threshold) {
                val badgePath = "users/$userId/badges/${badge.badgeId}"
                try {
                    val badgeRef = db.collection("users").document(userId)
                        .collection("badges").document(badge.badgeId)
                    val existing = badgeRef.get().await()
                    if (!existing.exists()) {
                        val payload = mapOf(
                            "badgeId" to badge.badgeId,
                            "userId" to userId,
                            "threshold" to badge.threshold.toLong(),
                            "titleEn" to badge.titleEn,
                            "titleAr" to badge.titleAr,
                            "unlockedAt" to FieldValue.serverTimestamp(),
                        )
                        badgeRef.set(payload).await()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.CREATE, badgePath)
                }
            }
        }
    }

    suspend fun setShieldEnabled(shieldKey: String, enabled: Boolean): Result<Unit> {
        val docPath = "app_config/shields"
        return try {
            val activeUid = requireUserId()
            val docRef = db.collection("app_config").document("shields")
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val currentDisabled = if (snap.exists()) {
                    (snap.get("disabledShieldIds") as? List<*>)
                        ?.filterIsInstance<String>()
                        ?.toMutableSet()
                        ?: mutableSetOf()
                } else {
                    mutableSetOf()
                }
                if (enabled) {
                    currentDisabled.remove(shieldKey)
                } else {
                    currentDisabled.add(shieldKey)
                }
                val payload = mapOf(
                    "disabledShieldIds" to currentDisabled.take(12),
                    "updatedBy" to activeUid,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                if (snap.exists()) {
                    tx.update(docRef, payload)
                } else {
                    tx.set(docRef, payload)
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun observeActivePhishingAlerts(): Flow<List<PhishingAlert>> {
        if (auth.currentUser == null) return flowOf(emptyList())
        val path = "phishing_alerts"
        return db.collection("phishing_alerts")
            .whereEqualTo("active", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    PhishingAlert::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeAllPhishingAlertsAdmin(): Flow<List<PhishingAlert>> {
        if (auth.currentUser == null) return flowOf(emptyList())
        val path = "phishing_alerts"
        return db.collection("phishing_alerts")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    PhishingAlert::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeAlertAcks(userId: String): Flow<List<AlertAck>> {
        if (userId.isBlank() || auth.currentUser == null) return flowOf(emptyList())
        val path = "users/$userId/alert_acks"
        return db.collection("users").document(userId)
            .collection("alert_acks")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    AlertAck::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeDetectedThreats(userId: String): Flow<List<DetectedThreat>> {
        if (userId.isBlank() || auth.currentUser == null) return flowOf(emptyList())
        val path = "users/$userId/detected_threats"
        return db.collection("users").document(userId)
            .collection("detected_threats")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    DetectedThreat::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.detectedAt?.seconds ?: it.updatedAt?.seconds ?: 0L }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun acknowledgeGlobalAlert(
        userId: String,
        alertId: String,
        read: Boolean,
        dismissed: Boolean,
    ): Result<Unit> {
        val cleanAlertId = alertId.trim()
        val docPath = "users/$userId/alert_acks/$cleanAlertId"
        return try {
            val activeUid = requireUserId()
            check(activeUid == userId) { "User mismatch" }
            require(cleanAlertId.isNotEmpty() && cleanAlertId.length <= 128) { "Invalid alertId" }
            val docRef = db.collection("users").document(userId)
                .collection("alert_acks").document(cleanAlertId)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                if (snap.exists()) {
                    tx.update(
                        docRef,
                        mapOf(
                            "read" to read,
                            "dismissed" to dismissed,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                } else {
                    tx.set(
                        docRef,
                        mapOf(
                            "alertId" to cleanAlertId,
                            "userId" to userId,
                            "read" to read,
                            "dismissed" to dismissed,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun acknowledgeDetectedThreat(
        userId: String,
        threatId: String,
        read: Boolean,
        dismissed: Boolean,
    ): Result<Unit> {
        val cleanThreatId = threatId.trim()
        val docPath = "users/$userId/detected_threats/$cleanThreatId"
        return try {
            val activeUid = requireUserId()
            check(activeUid == userId) { "User mismatch" }
            require(cleanThreatId.isNotEmpty() && cleanThreatId.length <= 128) { "Invalid threatId" }
            db.collection("users").document(userId)
                .collection("detected_threats").document(cleanThreatId)
                .update(
                    mapOf(
                        "read" to read,
                        "dismissed" to dismissed,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordDetectedThreat(
        userId: String,
        shieldKey: String,
        category: PhishingCategory,
        severity: ThreatSeverity,
        indicator: String,
        titleAr: String,
        titleEn: String,
        messageAr: String,
        messageEn: String,
    ): Result<Unit> {
        val threatId = "threat_${System.currentTimeMillis()}"
        val docPath = "users/$userId/detected_threats/$threatId"
        return try {
            val activeUid = requireUserId()
            check(activeUid == userId) { "User mismatch" }
            val cleanTitleAr = titleAr.trim().take(120)
            val cleanTitleEn = titleEn.trim().take(120)
            val cleanMessageAr = messageAr.trim().take(500)
            val cleanMessageEn = messageEn.trim().take(500)
            val cleanShield = shieldKey.trim().take(32)
            require(cleanTitleAr.isNotEmpty() && cleanTitleEn.isNotEmpty()) { "Title is required" }
            require(cleanMessageAr.isNotEmpty() && cleanMessageEn.isNotEmpty()) { "Message is required" }
            require(cleanShield.isNotEmpty()) { "Shield key is required" }

            val payload = mapOf(
                "userId" to activeUid,
                "shieldKey" to cleanShield,
                "category" to category.name,
                "severity" to severity.name,
                "indicator" to indicator.trim().take(180),
                "titleAr" to cleanTitleAr,
                "titleEn" to cleanTitleEn,
                "messageAr" to cleanMessageAr,
                "messageEn" to cleanMessageEn,
                "read" to false,
                "dismissed" to false,
                "detectedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            db.collection("users").document(activeUid)
                .collection("detected_threats").document(threatId)
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun publishPhishingAlert(
        titleAr: String,
        titleEn: String,
        messageAr: String,
        messageEn: String,
        category: PhishingCategory,
        severity: ThreatSeverity,
        indicator: String,
    ): Result<Unit> {
        val alertId = "alert_${System.currentTimeMillis()}"
        val docPath = "phishing_alerts/$alertId"
        return try {
            val activeUid = requireUserId()
            val cleanTitleAr = titleAr.trim().take(120)
            val cleanTitleEn = titleEn.trim().take(120)
            val cleanMessageAr = messageAr.trim().take(500)
            val cleanMessageEn = messageEn.trim().take(500)
            require(cleanTitleAr.isNotEmpty() && cleanTitleEn.isNotEmpty()) { "Alert titles are required" }
            require(cleanMessageAr.isNotEmpty() && cleanMessageEn.isNotEmpty()) { "Alert descriptions are required" }

            val payload = mapOf(
                "titleAr" to cleanTitleAr,
                "titleEn" to cleanTitleEn,
                "messageAr" to cleanMessageAr,
                "messageEn" to cleanMessageEn,
                "category" to category.name,
                "severity" to severity.name,
                "indicator" to indicator.trim().take(180),
                "active" to true,
                "createdBy" to activeUid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            db.collection("phishing_alerts").document(alertId)
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun setPhishingAlertActive(alertId: String, active: Boolean): Result<Unit> {
        val cleanId = alertId.trim()
        val docPath = "phishing_alerts/$cleanId"
        return try {
            requireUserId()
            require(cleanId.isNotEmpty() && cleanId.length <= 128) { "Invalid alert ID" }
            db.collection("phishing_alerts").document(cleanId)
                .update(
                    mapOf(
                        "active" to active,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deletePhishingAlert(alertId: String): Result<Unit> {
        val cleanId = alertId.trim()
        val docPath = "phishing_alerts/$cleanId"
        return try {
            requireUserId()
            require(cleanId.isNotEmpty() && cleanId.length <= 128) { "Invalid alert ID" }
            db.collection("phishing_alerts").document(cleanId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
