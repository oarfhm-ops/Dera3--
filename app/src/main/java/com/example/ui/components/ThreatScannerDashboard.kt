package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.ShieldConfig
import com.example.data.model.ShieldId
import com.example.data.model.UserProfile
import com.example.localization.LocalStrings
import com.example.localization.LocalizationManager
import com.example.ui.theme.AmberCaution
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyanBright
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceElevated
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Immutable
data class ShieldAuditSummary(
    val scorePercent: Int,
    val securedCount: Int,
    val atRiskCount: Int,
    val pausedCount: Int,
    val nextRiskShield: ShieldId?,
)

private fun requiredChecksForShield(shield: ShieldId): List<String> = when (shield) {
    ShieldId.WIFI -> listOf("wifi_1", "wifi_2", "wifi_3")
    ShieldId.WHATSAPP -> listOf("wa_1", "wa_2", "wa_3", "wa_btn")
    ShieldId.BREACH -> listOf("breach_1", "breach_2")
    ShieldId.PRIVACY -> listOf("priv_1", "priv_2", "priv_3")
    ShieldId.ELDERLY -> listOf("elderly_contact")
    ShieldId.LINK_SCANNER -> listOf("link_scan")
    ShieldId.HONEYPOT -> listOf("honeypot_active")
    ShieldId.PASSWORD -> listOf("pw_check", "pw_gen")
    ShieldId.FAKE_APPS -> listOf("fake_app_scan")
    ShieldId.SCAM_CALLS -> listOf("scam_call_scan")
    ShieldId.AI_ASSISTANT -> listOf("ai_ask_1")
    ShieldId.SAFE_BROWSER -> listOf("safe_browser_use")
}

fun buildShieldAuditSummary(
    userProfile: UserProfile,
    shieldConfig: ShieldConfig,
): ShieldAuditSummary {
    val scoreInt = userProfile.securityScore.toInt().coerceIn(0, 100)
    val completed = userProfile.completedChecks.toSet()
    val disabled = shieldConfig.disabledShieldIds.toSet()

    var secured = 0
    var atRisk = 0
    var paused = 0
    var firstRisk: ShieldId? = null

    ShieldId.entries.forEach { shield ->
        if (disabled.contains(shield.key)) {
            paused++
        } else {
            val required = requiredChecksForShield(shield)
            if (required.all { completed.contains(it) }) {
                secured++
            } else {
                atRisk++
                if (firstRisk == null) {
                    firstRisk = shield
                }
            }
        }
    }

    return ShieldAuditSummary(
        scorePercent = scoreInt,
        securedCount = secured,
        atRiskCount = atRisk,
        pausedCount = paused,
        nextRiskShield = firstRisk,
    )
}

@Composable
fun ThreatScannerDashboard(
    userProfile: UserProfile,
    shieldConfig: ShieldConfig,
    onFocusShield: (ShieldId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()

    val audit = remember(userProfile.securityScore, userProfile.completedChecks, shieldConfig.disabledShieldIds) {
        buildShieldAuditSummary(userProfile, shieldConfig)
    }

    var isScanning by remember { mutableStateOf(false) }
    var hasCompletedScan by remember { mutableStateOf(false) }

    val statusColor by animateColorAsState(
        targetValue = when {
            isScanning -> CyanBright
            audit.scorePercent < 40 -> CrimsonAlert
            audit.scorePercent < 80 -> AmberCaution
            else -> EmeraldSafe
        },
        animationSpec = tween(durationMillis = 350),
        label = "scanner_status_color",
    )

    val statusLabel = when {
        isScanning -> strings.threatScanningLabel
        audit.scorePercent < 40 -> strings.threatStatusCritical
        audit.scorePercent < 80 -> strings.threatStatusModerate
        else -> strings.threatStatusProtected
    }

    val statusIcon: ImageVector = when {
        isScanning -> Icons.Filled.Radar
        audit.scorePercent < 40 -> Icons.Filled.GppBad
        audit.scorePercent < 80 -> Icons.Filled.GppMaybe
        else -> Icons.Filled.GppGood
    }

    val advice = when {
        audit.scorePercent < 40 -> strings.scoreLowAdvice
        audit.scorePercent < 80 -> strings.scoreMidAdvice
        else -> strings.scoreHighAdvice
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("security_score_card"),
        shape = RoundedCornerShape(26.dp),
        color = NavyDeep,
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f)),
        shadowElevation = 6.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("threat_scanner_dashboard")
                .background(
                    Brush.linearGradient(
                        colors = listOf(NavyDeep, NavySurface, NavySurfaceElevated)
                    )
                )
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                statusColor.copy(alpha = 0.22f),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.28f, size.height * 0.32f),
                            radius = size.maxDimension * 0.65f,
                        ),
                        radius = size.maxDimension * 0.65f,
                        center = Offset(size.width * 0.28f, size.height * 0.32f),
                    )
                }
                .padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Header row with title and status pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(statusColor.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Radar,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = strings.threatScannerTitle,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = strings.threatScannerSubtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.72f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    Surface(
                        color = statusColor.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.55f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = statusLabel,
                                tint = statusColor,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                // Responsive Circular Gauge + Security Details
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val useWideLayout = maxWidth >= 420.dp
                    if (useWideLayout) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            CircularSecurityGauge(
                                scorePercent = audit.scorePercent,
                                accentColor = statusColor,
                                isScanning = isScanning,
                                outOfLabel = strings.securityScoreOutOf,
                                scoreAccessibilityLabel = "${strings.securityScoreTitle}: ${audit.scorePercent} ${strings.securityScoreOutOf}",
                                gaugeSize = 148.dp,
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    text = advice,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f),
                                )
                                ScannerMetricsRow(
                                    securedCount = audit.securedCount,
                                    atRiskCount = audit.atRiskCount,
                                    pausedCount = audit.pausedCount,
                                    securedLabel = strings.threatProtectedShieldsLabel,
                                    atRiskLabel = strings.threatOpenRisksLabel,
                                    pausedLabel = strings.threatPausedShieldsLabel,
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            CircularSecurityGauge(
                                scorePercent = audit.scorePercent,
                                accentColor = statusColor,
                                isScanning = isScanning,
                                outOfLabel = strings.securityScoreOutOf,
                                scoreAccessibilityLabel = "${strings.securityScoreTitle}: ${audit.scorePercent} ${strings.securityScoreOutOf}",
                                gaugeSize = 154.dp,
                            )
                            Text(
                                text = advice,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center,
                            )
                            ScannerMetricsRow(
                                securedCount = audit.securedCount,
                                atRiskCount = audit.atRiskCount,
                                pausedCount = audit.pausedCount,
                                securedLabel = strings.threatProtectedShieldsLabel,
                                atRiskLabel = strings.threatOpenRisksLabel,
                                pausedLabel = strings.threatPausedShieldsLabel,
                            )
                        }
                    }
                }

                // Scan result feedback banner after triggering Scan Threats
                AnimatedVisibility(visible = hasCompletedScan && !isScanning) {
                    val allSafe = audit.atRiskCount == 0
                    val bannerColor = if (allSafe) EmeraldSafe else AmberCaution
                    Surface(
                        color = bannerColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, bannerColor.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = if (allSafe) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber,
                                contentDescription = null,
                                tint = bannerColor,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = if (allSafe) {
                                    strings.threatScanCompleteAllSafe
                                } else {
                                    LocalizationManager.formatString(strings.threatScanCompleteFoundFormat, audit.atRiskCount)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                // Action Buttons (min 48dp touch targets)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = {
                            if (!isScanning) {
                                scope.launch {
                                    isScanning = true
                                    delay(1150L)
                                    isScanning = false
                                    hasCompletedScan = true
                                }
                            }
                        },
                        enabled = !isScanning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = statusColor,
                            contentColor = NavyDeep,
                            disabledContainerColor = CyanBright.copy(alpha = 0.4f),
                            disabledContentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("threat_scan_now_button"),
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = strings.threatScanningLabel,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = strings.threatScanNowBtn,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    val nextRisk = audit.nextRiskShield
                    if (nextRisk != null) {
                        val nextShieldTitle = strings.shieldTitles.getOrElse(nextRisk.index) { nextRisk.name }
                        OutlinedButton(
                            onClick = { onFocusShield(nextRisk) },
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.32f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("threat_focus_next_button"),
                        ) {
                            Text(
                                text = LocalizationManager.formatString(strings.threatNextActionFormat, nextShieldTitle),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CircularSecurityGauge(
    scorePercent: Int,
    accentColor: Color,
    isScanning: Boolean,
    outOfLabel: String,
    scoreAccessibilityLabel: String,
    gaugeSize: Dp,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = scorePercent.coerceIn(0, 100) / 100f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 240f),
        label = "circular_safety_progress",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "radar_sweep")
    val radarAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "radar_angle",
    )

    Box(
        modifier = Modifier
            .size(gaugeSize)
            .semantics { contentDescription = scoreAccessibilityLabel }
            .testTag("threat_circular_progress"),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 12.dp.toPx()
            val inset = strokeWidth / 2f + 10.dp.toPx()
            val arcDiameter = size.minDimension - inset * 2f
            val topLeft = Offset(
                x = (size.width - arcDiameter) / 2f,
                y = (size.height - arcDiameter) / 2f,
            )
            val arcSize = Size(arcDiameter, arcDiameter)

            // Outer precision telemetry tick marks
            val center = Offset(size.width / 2f, size.height / 2f)
            val tickOuterRadius = size.minDimension / 2f - 1.dp.toPx()
            val tickInnerRadius = tickOuterRadius - 5.dp.toPx()
            val tickCount = 24
            for (i in 0 until tickCount) {
                val angleDeg = -90f + i * (360f / tickCount)
                val rad = angleDeg * (PI / 180.0)
                val start = Offset(
                    x = center.x + (tickInnerRadius * cos(rad)).toFloat(),
                    y = center.y + (tickInnerRadius * sin(rad)).toFloat(),
                )
                val end = Offset(
                    x = center.x + (tickOuterRadius * cos(rad)).toFloat(),
                    y = center.y + (tickOuterRadius * sin(rad)).toFloat(),
                )
                val tickFilled = (i.toFloat() / tickCount) <= animatedProgress
                drawLine(
                    color = if (tickFilled) accentColor.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.18f),
                    start = start,
                    end = end,
                    strokeWidth = if (i % 6 == 0) 2.2.dp.toPx() else 1.2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            // Inner circular track
            drawArc(
                color = Color.White.copy(alpha = 0.14f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            // Animated safety score circular progress arc
            if (animatedProgress > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.72f),
                            accentColor,
                            CyanBright.copy(alpha = 0.9f),
                            accentColor,
                        ),
                        center = center,
                    ),
                    startAngle = -90f,
                    sweepAngle = (animatedProgress * 360f).coerceAtLeast(6f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }

            // Active radar sweep overlay while scanning
            if (isScanning) {
                drawArc(
                    color = CyanBright.copy(alpha = 0.9f),
                    startAngle = radarAngle - 90f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth * 0.5f, cap = StrokeCap.Round),
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = "$scorePercent%",
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = MonoFontFamily),
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = outOfLabel,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
private fun ScannerMetricsRow(
    securedCount: Int,
    atRiskCount: Int,
    pausedCount: Int,
    securedLabel: String,
    atRiskLabel: String,
    pausedLabel: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ScannerMetricPill(
            value = "$securedCount",
            label = securedLabel,
            accentColor = EmeraldSafe,
            modifier = Modifier.weight(1f),
        )
        ScannerMetricPill(
            value = "$atRiskCount",
            label = atRiskLabel,
            accentColor = if (atRiskCount == 0) EmeraldSafe else CrimsonAlert,
            modifier = Modifier.weight(1f),
        )
        ScannerMetricPill(
            value = "$pausedCount",
            label = pausedLabel,
            accentColor = AmberCaution,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ScannerMetricPill(
    value: String,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                color = accentColor,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}
