package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.LinkAnalysisResult
import com.example.domain.SecurityAnalyzers
import com.example.domain.WifiAnalysisResult
import com.example.localization.LocalStrings
import com.example.localization.LocalizationManager
import com.example.ui.theme.LocalDera3Colors

@Composable
fun ChecklistRow(
    label: String,
    checked: Boolean,
    points: Int,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    val strings = LocalStrings.current
    Surface(
        onClick = { onCheckedChange(!checked) },
        shape = RoundedCornerShape(12.dp),
        color = if (checked) {
            LocalDera3Colors.current.safeContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
        modifier = Modifier.fillMaxWidth().testTag(testTag),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = LocalizationManager.formatString(strings.ptsFormat, points),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (checked) LocalDera3Colors.current.safe else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun StatusBanner(
    text: String,
    isDanger: Boolean,
    isCaution: Boolean = false,
) {
    val colors = LocalDera3Colors.current
    val bg: Color
    val fg: Color
    val icon = when {
        isDanger -> {
            bg = colors.dangerContainer; fg = colors.danger; Icons.Filled.Error
        }
        isCaution -> {
            bg = colors.cautionContainer; fg = colors.caution; Icons.Filled.Warning
        }
        else -> {
            bg = colors.safeContainer; fg = colors.safe; Icons.Filled.CheckCircle
        }
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = text,
                color = fg,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun WifiShieldContent(
    completedChecks: List<String>,
    onToggleCheck: (String, Boolean) -> Unit,
) {
    val strings = LocalStrings.current
    var wifiName by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<WifiAnalysisResult?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = wifiName,
            onValueChange = { wifiName = it },
            label = { Text(strings.wifiInputHint) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("wifi_name_input"),
        )
        Button(
            onClick = {
                if (wifiName.isNotBlank()) {
                    result = SecurityAnalyzers.analyzeWifiName(wifiName)
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("wifi_scan_button"),
        ) {
            Text(strings.scanBtn)
        }

        result?.let {
            StatusBanner(
                text = if (it.isDangerous) strings.wifiDangerResult else strings.wifiSafeResult,
                isDanger = it.isDangerous,
            )
        }

        val checkIds = listOf("wifi_1", "wifi_2", "wifi_3")
        strings.wifiChecks.forEachIndexed { idx, label ->
            val id = checkIds[idx]
            ChecklistRow(
                label = label,
                checked = completedChecks.contains(id),
                points = 5,
                testTag = "check_$id",
                onCheckedChange = { onToggleCheck(id, it) },
            )
        }

        Text(
            text = strings.wifiTip,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun WhatsAppShieldContent(
    completedChecks: List<String>,
    onToggleCheck: (String, Boolean) -> Unit,
) {
    val strings = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = strings.whatsappGuide,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp),
            )
        }

        val checkIds = listOf("wa_1", "wa_2", "wa_3")
        strings.whatsappChecks.forEachIndexed { idx, label ->
            val id = checkIds[idx]
            ChecklistRow(
                label = label,
                checked = completedChecks.contains(id),
                points = 5,
                testTag = "check_$id",
                onCheckedChange = { onToggleCheck(id, it) },
            )
        }

        val btnDone = completedChecks.contains("wa_btn")
        val ptsBadge5 = LocalizationManager.formatString(strings.ptsInlineFormat, 5)
        Button(
            onClick = { onToggleCheck("wa_btn", true) },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("whatsapp_checked_button"),
        ) {
            Text(
                if (btnDone) "✓ ${strings.whatsappCheckedBtn} $ptsBadge5"
                else "${strings.whatsappCheckedBtn} $ptsBadge5"
            )
        }
    }
}

@Composable
fun BreachShieldContent(
    completedChecks: List<String>,
    onToggleCheck: (String, Boolean) -> Unit,
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var checkedEmail by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(strings.breachInputHint) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("breach_email_input"),
        )
        Button(
            onClick = {
                if (email.isNotBlank()) checkedEmail = true
            },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("breach_scan_button"),
        ) {
            Text(strings.scanBtn)
        }

        if (checkedEmail) {
            StatusBanner(text = strings.breachGuideResult, isDanger = false, isCaution = true)
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://haveibeenpwned.com/"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("breach_open_site_button"),
            ) {
                Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(strings.breachOpenSiteBtn)
            }
        }

        val checkIds = listOf("breach_1", "breach_2")
        strings.breachChecks.forEachIndexed { idx, label ->
            val id = checkIds[idx]
            ChecklistRow(
                label = label,
                checked = completedChecks.contains(id),
                points = 5,
                testTag = "check_$id",
                onCheckedChange = { onToggleCheck(id, it) },
            )
        }
    }
}

@Composable
fun PrivacyShieldContent(
    completedChecks: List<String>,
    onToggleCheck: (String, Boolean) -> Unit,
) {
    val strings = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = strings.privacyGuide,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp),
            )
        }

        val checkIds = listOf("priv_1", "priv_2", "priv_3")
        strings.privacyChecks.forEachIndexed { idx, label ->
            val id = checkIds[idx]
            ChecklistRow(
                label = label,
                checked = completedChecks.contains(id),
                points = 5,
                testTag = "check_$id",
                onCheckedChange = { onToggleCheck(id, it) },
            )
        }
    }
}

@Composable
fun ElderlyShieldContent(
    savedEmergencyContact: String,
    onSaveEmergencyContact: (String, () -> Unit) -> Unit,
) {
    val strings = LocalStrings.current
    val clipboard = LocalClipboardManager.current
    var emergencyPhone by remember(savedEmergencyContact) { mutableStateOf(savedEmergencyContact) }
    var savedBanner by remember { mutableStateOf(false) }
    var copiedBanner by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = emergencyPhone,
            onValueChange = { emergencyPhone = it },
            label = { Text(strings.elderlyEmergencyHint) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("elderly_phone_input"),
        )
        Button(
            onClick = {
                if (emergencyPhone.isNotBlank()) {
                    onSaveEmergencyContact(emergencyPhone) { savedBanner = true }
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("elderly_save_button"),
        ) {
            Text("${strings.elderlySaveBtn} ${LocalizationManager.formatString(strings.ptsInlineFormat, 10)}")
        }
        if (savedBanner) {
            StatusBanner(text = strings.elderlySavedMsg, isDanger = false)
        }

        Surface(
            color = LocalDera3Colors.current.cautionContainer,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "\"${strings.elderlyReadyMessage}\"",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(strings.elderlyReadyMessage))
                        copiedBanner = true
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("elderly_copy_button"),
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (copiedBanner) strings.copiedToast else strings.elderlyCopyMsgBtn)
                }
            }
        }

        Text(
            text = strings.elderlyGuide,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun LinkScannerShieldContent(
    completedChecks: List<String>,
    onScanCompleted: () -> Unit,
    onPhishingDetected: (String) -> Unit = {},
) {
    val strings = LocalStrings.current
    var urlInput by remember { mutableStateOf("") }
    var analysis by remember { mutableStateOf<LinkAnalysisResult?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            label = { Text(strings.linkInputHint) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("link_scanner_input"),
        )
        Button(
            onClick = {
                if (urlInput.isNotBlank()) {
                    val res = SecurityAnalyzers.analyzeLink(urlInput)
                    analysis = res
                    if (!completedChecks.contains("link_scan")) {
                        onScanCompleted()
                    }
                    if (res.isDangerous) {
                        onPhishingDetected(urlInput.trim())
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("link_scan_button"),
        ) {
            Text("${strings.linkScanBtn} ${LocalizationManager.formatString(strings.ptsInlineFormat, 10)}")
        }

        analysis?.let { res ->
            StatusBanner(
                text = if (res.isDangerous) strings.linkDangerVerdict else strings.linkSafeVerdict,
                isDanger = res.isDangerous,
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                res.checks.forEach { check ->
                    val label = strings.linkCheckLabels.getOrElse(check.index) { "" }
                    val fg = if (check.passed) LocalDera3Colors.current.safe else LocalDera3Colors.current.danger
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (check.passed) Icons.Filled.CheckCircle else Icons.Filled.Error,
                            contentDescription = null,
                            tint = fg,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = fg)
                    }
                }
            }
        }
    }
}
