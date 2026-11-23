package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.view.ViewGroup
import android.webkit.SafeBrowsingResponse
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.SafeBrowserBlockReason
import com.example.domain.SafeBrowserFilter
import com.example.domain.UrlSafetyVerdict
import com.example.localization.AppLanguage
import com.example.localization.LocalAppLanguage
import com.example.localization.LocalStrings
import com.example.localization.LocalizationManager
import com.example.ui.screens.StatusBanner
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.LocalDera3Colors
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavySurface
import java.io.ByteArrayInputStream

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SafeBrowserShieldContent(
    completedChecks: List<String>,
    onAwardUse: () -> Unit,
    onPhishingBlocked: (String) -> Unit = {},
) {
    val strings = LocalStrings.current
    val dera3Colors = LocalDera3Colors.current
    val language = LocalAppLanguage.current

    var urlInput by remember { mutableStateOf("") }
    var adBlockEnabled by remember { mutableStateOf(true) }
    var sensitiveFilterEnabled by remember { mutableStateOf(true) }
    var adsBlockedTotal by remember { mutableIntStateOf(0) }
    var sitesBlockedTotal by remember { mutableIntStateOf(0) }
    var activeBrowserUrl by remember { mutableStateOf<String?>(null) }
    var inlineBlockedVerdict by remember { mutableStateOf<UrlSafetyVerdict?>(null) }

    val launchUrl: (String) -> Unit = { rawTarget ->
        val verdict = SafeBrowserFilter.evaluateNavigation(
            rawUrl = rawTarget,
            sensitiveFilterEnabled = sensitiveFilterEnabled,
        )
        if (!completedChecks.contains("safe_browser_use")) {
            onAwardUse()
        }
        if (verdict.isAllowed) {
            inlineBlockedVerdict = null
            activeBrowserUrl = verdict.normalizedUrl
        } else {
            sitesBlockedTotal++
            inlineBlockedVerdict = verdict
            if (verdict.reason == SafeBrowserBlockReason.PHISHING_RISK) {
                onPhishingBlocked(verdict.normalizedUrl)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (completedChecks.contains("safe_browser_use")) {
            StatusBanner(text = strings.safeBrowser.activeBadge, isDanger = false)
        }

        // Protection Toggles
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Block,
                        contentDescription = null,
                        tint = dera3Colors.safe,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = strings.safeBrowser.adBlockToggle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = adBlockEnabled,
                        onCheckedChange = { adBlockEnabled = it },
                        modifier = Modifier.testTag("safe_browser_adblock_switch"),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = strings.safeBrowser.sensitiveFilterToggle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = sensitiveFilterEnabled,
                        onCheckedChange = { sensitiveFilterEnabled = it },
                        modifier = Modifier.testTag("safe_browser_sensitive_switch"),
                    )
                }
            }
        }

        // Live Session Telemetry
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                color = dera3Colors.safeContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = LocalizationManager.formatString(strings.safeBrowser.adsBlockedFormat, adsBlockedTotal),
                    style = MaterialTheme.typography.labelLarge,
                    color = dera3Colors.safe,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
            Surface(
                color = dera3Colors.dangerContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = LocalizationManager.formatString(strings.safeBrowser.sitesBlockedFormat, sitesBlockedTotal),
                    style = MaterialTheme.typography.labelLarge,
                    color = dera3Colors.danger,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }

        // URL or Safe Search Bar
        OutlinedTextField(
            value = urlInput,
            onValueChange = {
                urlInput = it
                inlineBlockedVerdict = null
            },
            label = { Text(strings.safeBrowser.urlHint) },
            leadingIcon = { Icon(Icons.Filled.Public, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Go,
            ),
            keyboardActions = KeyboardActions(
                onGo = { launchUrl(urlInput) },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("safe_browser_url_input"),
        )

        Button(
            onClick = { launchUrl(urlInput) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("safe_browser_open_button"),
        ) {
            Icon(Icons.Filled.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${strings.safeBrowser.openBtn} ${LocalizationManager.formatString(strings.ptsInlineFormat, 10)}",
                fontWeight = FontWeight.Bold,
            )
        }

        // Inline Blocked Verdict Banner if user typed a sensitive/phishing URL directly
        inlineBlockedVerdict?.let { verdict ->
            val reasonText = when (verdict.reason) {
                SafeBrowserBlockReason.SENSITIVE_CONTENT -> strings.safeBrowser.blockedSensitiveReason
                SafeBrowserBlockReason.PHISHING_RISK -> strings.safeBrowser.blockedPhishingReason
                SafeBrowserBlockReason.UNSAFE_SCHEME -> strings.safeBrowser.blockedSchemeReason
                SafeBrowserBlockReason.NONE -> ""
            }
            StatusBanner(
                text = "${strings.safeBrowser.blockedTitle}: $reasonText",
                isDanger = true,
            )
        }

        Text(
            text = strings.safeBrowser.quickBookmarksTitle,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            SafeBrowserFilter.defaultBookmarks.forEachIndexed { idx, bookmark ->
                val label = if (language == AppLanguage.AR) bookmark.titleAr else bookmark.titleEn
                AssistChip(
                    onClick = {
                        urlInput = bookmark.url
                        launchUrl(bookmark.url)
                    },
                    label = { Text(label) },
                    modifier = Modifier.testTag("safe_browser_bookmark_$idx"),
                )
            }
        }

        Text(
            text = strings.safeBrowser.guide,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    activeBrowserUrl?.let { targetUrl ->
        InAppSafeBrowserDialog(
            initialUrl = targetUrl,
            adBlockEnabled = adBlockEnabled,
            sensitiveFilterEnabled = sensitiveFilterEnabled,
            onToggleAdBlock = { adBlockEnabled = it },
            onToggleSensitiveFilter = { sensitiveFilterEnabled = it },
            onAdBlocked = { adsBlockedTotal++ },
            onSiteBlocked = { verdict ->
                sitesBlockedTotal++
                if (verdict.reason == SafeBrowserBlockReason.PHISHING_RISK) {
                    onPhishingBlocked(verdict.normalizedUrl)
                }
            },
            onDismiss = { activeBrowserUrl = null },
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InAppSafeBrowserDialog(
    initialUrl: String,
    adBlockEnabled: Boolean,
    sensitiveFilterEnabled: Boolean,
    onToggleAdBlock: (Boolean) -> Unit,
    onToggleSensitiveFilter: (Boolean) -> Unit,
    onAdBlocked: () -> Unit,
    onSiteBlocked: (UrlSafetyVerdict) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var addressText by remember(initialUrl) { mutableStateOf(initialUrl) }
    var currentMainHost by remember(initialUrl) {
        mutableStateOf(SafeBrowserFilter.evaluateNavigation(initialUrl, sensitiveFilterEnabled).host)
    }
    var loadingProgress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var adsBlockedInBrowser by remember { mutableIntStateOf(0) }
    var sitesBlockedInBrowser by remember { mutableIntStateOf(0) }
    var blockedVerdict by remember { mutableStateOf<UrlSafetyVerdict?>(null) }

    val navigateTo: (String) -> Unit = { rawInput ->
        val verdict = SafeBrowserFilter.evaluateNavigation(
            rawUrl = rawInput,
            sensitiveFilterEnabled = sensitiveFilterEnabled,
        )
        addressText = verdict.normalizedUrl
        if (verdict.isAllowed) {
            blockedVerdict = null
            currentMainHost = verdict.host
            webViewRef?.loadUrl(verdict.normalizedUrl)
        } else {
            sitesBlockedInBrowser++
            onSiteBlocked(verdict)
            blockedVerdict = verdict
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        BackHandler(enabled = true) {
            val wv = webViewRef
            if (blockedVerdict != null) {
                blockedVerdict = null
            } else if (wv != null && wv.canGoBack()) {
                wv.goBack()
            } else {
                onDismiss()
            }
        }

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Surface(
                    color = NavyDeep,
                    shadowElevation = 4.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Top Navigation + URL Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("safe_browser_close_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = strings.safeBrowser.closeDesc,
                                    tint = Color.White,
                                )
                            }

                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                OutlinedTextField(
                                    value = addressText,
                                    onValueChange = { addressText = it },
                                    singleLine = true,
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (addressText.startsWith("https://")) {
                                                Icons.Filled.Lock
                                            } else {
                                                Icons.Filled.Shield
                                            },
                                            contentDescription = null,
                                            tint = if (addressText.startsWith("https://")) EmeraldSafe else Color.White,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Uri,
                                        imeAction = ImeAction.Go,
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onGo = { navigateTo(addressText) },
                                    ),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = MonoFontFamily,
                                        color = Color.White,
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("safe_browser_dialog_url_input"),
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (blockedVerdict != null) {
                                        navigateTo(addressText)
                                    } else {
                                        webViewRef?.reload()
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("safe_browser_refresh_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = strings.safeBrowser.refreshDesc,
                                    tint = Color.White,
                                )
                            }
                        }

                        // Controls & Live Blocker Counters Strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            IconButton(
                                onClick = { webViewRef?.goBack() },
                                enabled = canGoBack,
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = strings.safeBrowser.backDesc,
                                    tint = if (canGoBack) Color.White else Color.White.copy(alpha = 0.35f),
                                )
                            }

                            IconButton(
                                onClick = { webViewRef?.goForward() },
                                enabled = canGoForward,
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = strings.safeBrowser.forwardDesc,
                                    tint = if (canGoForward) Color.White else Color.White.copy(alpha = 0.35f),
                                )
                            }

                            FilterChip(
                                selected = adBlockEnabled,
                                onClick = { onToggleAdBlock(!adBlockEnabled) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Block,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                                label = {
                                    Text(
                                        LocalizationManager.formatString(
                                            strings.safeBrowser.adsBlockedFormat,
                                            adsBlockedInBrowser,
                                        )
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = NavySurface,
                                    labelColor = Color.White.copy(alpha = 0.8f),
                                    selectedContainerColor = EmeraldSafe.copy(alpha = 0.22f),
                                    selectedLabelColor = EmeraldSafe,
                                    selectedLeadingIconColor = EmeraldSafe,
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("safe_browser_dialog_adblock_chip"),
                            )

                            FilterChip(
                                selected = sensitiveFilterEnabled,
                                onClick = { onToggleSensitiveFilter(!sensitiveFilterEnabled) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Security,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                                label = {
                                    Text(
                                        LocalizationManager.formatString(
                                            strings.safeBrowser.sitesBlockedFormat,
                                            sitesBlockedInBrowser,
                                        )
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = NavySurface,
                                    labelColor = Color.White.copy(alpha = 0.8f),
                                    selectedContainerColor = Color(0xFF00D4FF).copy(alpha = 0.22f),
                                    selectedLabelColor = Color(0xFF00D4FF),
                                    selectedLeadingIconColor = Color(0xFF00D4FF),
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("safe_browser_dialog_filter_chip"),
                            )
                        }

                        if (loadingProgress in 1..99 && blockedVerdict == null) {
                            LinearProgressIndicator(
                                progress = { loadingProgress / 100f },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding(),
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.javaScriptCanOpenWindowsAutomatically = false
                            settings.setSupportMultipleWindows(false)
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                settings.safeBrowsingEnabled = true
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    loadingProgress = newProgress
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    if (!url.isNullOrBlank()) {
                                        addressText = url
                                        currentMainHost = SafeBrowserFilter.evaluateNavigation(
                                            rawUrl = url,
                                            sensitiveFilterEnabled = sensitiveFilterEnabled,
                                        ).host
                                    }
                                    canGoBack = view?.canGoBack() == true
                                    canGoForward = view?.canGoForward() == true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    canGoBack = view?.canGoBack() == true
                                    canGoForward = view?.canGoForward() == true
                                    if (adBlockEnabled) {
                                        view?.evaluateJavascript(SafeBrowserFilter.cosmeticAdHideScript, null)
                                    }
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                ): Boolean {
                                    val target = request?.url?.toString() ?: return false
                                    val verdict = SafeBrowserFilter.evaluateNavigation(
                                        rawUrl = target,
                                        sensitiveFilterEnabled = sensitiveFilterEnabled,
                                    )
                                    if (!verdict.isAllowed) {
                                        sitesBlockedInBrowser++
                                        onSiteBlocked(verdict)
                                        blockedVerdict = verdict
                                        return true
                                    }
                                    currentMainHost = verdict.host
                                    return false
                                }

                                override fun shouldInterceptRequest(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                ): WebResourceResponse? {
                                    val reqUrl = request?.url?.toString() ?: return null
                                    if (SafeBrowserFilter.shouldBlockAdRequest(
                                            requestUrl = reqUrl,
                                            mainPageHost = currentMainHost,
                                            adBlockEnabled = adBlockEnabled,
                                        )
                                    ) {
                                        view?.post {
                                            adsBlockedInBrowser++
                                            onAdBlocked()
                                        }
                                        return WebResourceResponse(
                                            "text/plain",
                                            "utf-8",
                                            ByteArrayInputStream(ByteArray(0)),
                                        )
                                    }
                                    return null
                                }

                                override fun onSafeBrowsingHit(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    threatType: Int,
                                    callback: SafeBrowsingResponse?,
                                ) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                                        callback?.backToSafety(true)
                                    }
                                    val target = request?.url?.toString().orEmpty()
                                    val verdict = UrlSafetyVerdict(
                                        normalizedUrl = target,
                                        host = target,
                                        isAllowed = false,
                                        reason = SafeBrowserBlockReason.PHISHING_RISK,
                                    )
                                    sitesBlockedInBrowser++
                                    onSiteBlocked(verdict)
                                    blockedVerdict = verdict
                                }
                            }

                            webViewRef = this
                            val initialVerdict = SafeBrowserFilter.evaluateNavigation(
                                rawUrl = initialUrl,
                                sensitiveFilterEnabled = sensitiveFilterEnabled,
                            )
                            if (initialVerdict.isAllowed) {
                                loadUrl(initialVerdict.normalizedUrl)
                            } else {
                                blockedVerdict = initialVerdict
                            }
                        }
                    },
                    onRelease = { releasedWebView ->
                        releasedWebView.stopLoading()
                        releasedWebView.destroy()
                        if (webViewRef == releasedWebView) {
                            webViewRef = null
                        }
                    },
                )

                // Blocked Content Interstitial Overlay
                blockedVerdict?.let { verdict ->
                    val reasonDesc = when (verdict.reason) {
                        SafeBrowserBlockReason.SENSITIVE_CONTENT -> strings.safeBrowser.blockedSensitiveReason
                        SafeBrowserBlockReason.PHISHING_RISK -> strings.safeBrowser.blockedPhishingReason
                        SafeBrowserBlockReason.UNSAFE_SCHEME -> strings.safeBrowser.blockedSchemeReason
                        SafeBrowserBlockReason.NONE -> ""
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("safe_browser_blocked_interstitial"),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(CrimsonAlert.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.GppBad,
                                    contentDescription = strings.safeBrowser.blockedTitle,
                                    tint = CrimsonAlert,
                                    modifier = Modifier.size(40.dp),
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = strings.safeBrowser.blockedTitle,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = CrimsonAlert,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = reasonDesc,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.4f)),
                            ) {
                                Text(
                                    text = verdict.host.ifBlank { verdict.normalizedUrl },
                                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFontFamily),
                                    color = CrimsonAlert,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                )
                            }
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    blockedVerdict = null
                                    val wv = webViewRef
                                    if (wv != null && wv.canGoBack()) {
                                        wv.goBack()
                                    } else {
                                        navigateTo(SafeBrowserFilter.DEFAULT_HOME_URL)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSafe),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("safe_browser_back_to_safety_button"),
                            ) {
                                Text(
                                    text = strings.safeBrowser.backToSafetyBtn,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyDeep,
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                            ) {
                                Text(strings.safeBrowser.closeDesc)
                            }
                        }
                    }
                }
            }
        }
    }
}
