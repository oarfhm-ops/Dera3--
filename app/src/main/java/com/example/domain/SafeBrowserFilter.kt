package com.example.domain

import java.net.URI
import java.net.URLEncoder

enum class SafeBrowserBlockReason {
    NONE,
    UNSAFE_SCHEME,
    SENSITIVE_CONTENT,
    PHISHING_RISK,
}

data class UrlSafetyVerdict(
    val normalizedUrl: String,
    val host: String,
    val isAllowed: Boolean,
    val reason: SafeBrowserBlockReason,
)

data class SafeBookmark(
    val titleAr: String,
    val titleEn: String,
    val url: String,
)

object SafeBrowserFilter {

    const val DEFAULT_HOME_URL = "https://duckduckgo.com/?kp=1"

    val defaultBookmarks: List<SafeBookmark> = listOf(
        SafeBookmark("محرك البحث الآمن", "Safe Search", "https://duckduckgo.com/?kp=1"),
        SafeBookmark("ويكيبيديا", "Wikipedia", "https://ar.wikipedia.org"),
        SafeBookmark("بنك المعرفة المصري", "Egyptian Knowledge Bank", "https://www.ekb.eg"),
        SafeBookmark("فحص تسريب الإيميل", "Have I Been Pwned", "https://haveibeenpwned.com"),
    )

    private val blockedSensitiveDomains = setOf(
        "pornhub.com", "xvideos.com", "xnxx.com", "redtube.com", "youporn.com",
        "xhamster.com", "onlyfans.com", "chaturbate.com", "brazzers.com", "bangbros.com",
        "spankbang.com", "eporner.com", "tube8.com", "beeg.com", "rule34.xxx",
        "bet365.com", "1xbet.com", "melbet.com", "stake.com", "pokerstars.com",
        "888casino.com", "betway.com", "linebet.com", "mostbet.com",
    )

    private val blockedSensitiveHostTokens = listOf(
        "porn", "xxx", "xnxx", "xvideos", "hentai", "nsfw", "escort",
        "chaturbate", "onlyfans", "casino", "gambling", "sportsbet", "1xbet",
        "bet365", "melbet", "إباحي", "سكس", "قمار", "مراهنات",
    )

    private val adAndTrackerDomains = setOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "adservice.google.com",
        "pagead2.googlesyndication.com",
        "admob.com",
        "amazon-adsystem.com",
        "adnxs.com",
        "ads.pubmatic.com",
        "pubmatic.com",
        "criteo.com",
        "criteo.net",
        "outbrain.com",
        "taboola.com",
        "mgid.com",
        "revcontent.com",
        "propellerads.com",
        "popads.net",
        "popcash.net",
        "exoclick.com",
        "juicyads.com",
        "trafficjunky.com",
        "adcolony.com",
        "applovin.com",
        "unityads.unity3d.com",
        "openx.net",
        "rubiconproject.com",
        "casalemedia.com",
        "smartadserver.com",
        "bidswitch.net",
        "media.net",
        "zedo.com",
        "adsafeprotected.com",
        "moatads.com",
        "scorecardresearch.com",
        "quantserve.com",
        "hotjar.com",
        "mixpanel.com",
        "mc.yandex.ru",
        "analytics.tiktok.com",
        "ads-twitter.com",
        "adroll.com",
        "chartbeat.com",
        "demdex.net",
        "krxd.net",
    )

    private val adPathMarkers = listOf(
        "/pagead/",
        "/adserver",
        "/adservice",
        "/popunder",
        "/prebid",
        "googlesyndication.com",
        "doubleclick.net",
        "/tr?id=",
    )

    val cosmeticAdHideScript: String = """
        (function() {
          if (document.getElementById('dera3-adblock-style')) return;
          var style = document.createElement('style');
          style.id = 'dera3-adblock-style';
          style.innerHTML = 'ins.adsbygoogle, [id^="div-gpt-ad"], .adsbygoogle, .ad-banner, .ad-slot, [data-ad-slot], [class*="taboola"], [id*="taboola"], [class*="outbrain"] { display: none !important; height: 0 !important; min-height: 0 !important; visibility: hidden !important; }';
          (document.head || document.documentElement).appendChild(style);
        })();
    """.trimIndent()

    fun resolveInputToUrl(rawInput: String): String {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return DEFAULT_HOME_URL

        val lower = trimmed.lowercase()
        val dangerousSchemes = listOf("javascript:", "file:", "content:", "intent:", "data:", "vbscript:")
        if (dangerousSchemes.any { lower.startsWith(it) }) {
            return trimmed
        }

        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return trimmed
        }

        val looksLikeDomain = !trimmed.contains(" ") &&
            trimmed.contains(".") &&
            !trimmed.startsWith(".") &&
            !trimmed.endsWith(".")

        return if (looksLikeDomain) {
            "https://$trimmed"
        } else {
            "https://duckduckgo.com/?kp=1&q=${URLEncoder.encode(trimmed, "UTF-8")}"
        }
    }

    fun evaluateNavigation(
        rawUrl: String,
        sensitiveFilterEnabled: Boolean = true,
    ): UrlSafetyVerdict {
        val resolved = resolveInputToUrl(rawUrl)
        val lower = resolved.lowercase()

        val uri = try {
            URI(resolved)
        } catch (_: Exception) {
            null
        }
        val scheme = uri?.scheme?.lowercase().orEmpty()
        val host = uri?.host?.lowercase().orEmpty()

        if (scheme != "https" && scheme != "http") {
            return UrlSafetyVerdict(
                normalizedUrl = resolved,
                host = host,
                isAllowed = false,
                reason = SafeBrowserBlockReason.UNSAFE_SCHEME,
            )
        }

        if (host.isBlank()) {
            return UrlSafetyVerdict(
                normalizedUrl = resolved,
                host = "",
                isAllowed = false,
                reason = SafeBrowserBlockReason.UNSAFE_SCHEME,
            )
        }

        if (sensitiveFilterEnabled) {
            val isOnion = host.endsWith(".onion")
            val matchesBlockedDomain = blockedSensitiveDomains.any { blocked ->
                host == blocked || host.endsWith(".$blocked")
            }
            val matchesSensitiveToken = blockedSensitiveHostTokens.any { token ->
                host.contains(token) || lower.contains("/$token") || lower.contains("=$token")
            }
            if (isOnion || matchesBlockedDomain || matchesSensitiveToken) {
                return UrlSafetyVerdict(
                    normalizedUrl = resolved,
                    host = host,
                    isAllowed = false,
                    reason = SafeBrowserBlockReason.SENSITIVE_CONTENT,
                )
            }
        }

        // Phishing & malicious link protection
        val linkAnalysis = SecurityAnalyzers.analyzeLink(resolved)
        val noHttps = !linkAnalysis.checks[0].passed
        val isShortener = !linkAnalysis.checks[1].passed
        val hasPhishingKeyword = !linkAnalysis.checks[2].passed
        val hasRawIp = !linkAnalysis.checks[3].passed
        val hasTyposquat = !linkAnalysis.checks[4].passed
        val badShape = !linkAnalysis.checks[5].passed
        val failedCount = linkAnalysis.checks.count { !it.passed }

        val highRiskPhishing = hasRawIp ||
            hasTyposquat ||
            badShape ||
            (isShortener && (noHttps || hasPhishingKeyword)) ||
            failedCount >= 3

        if (highRiskPhishing) {
            return UrlSafetyVerdict(
                normalizedUrl = resolved,
                host = host,
                isAllowed = false,
                reason = SafeBrowserBlockReason.PHISHING_RISK,
            )
        }

        return UrlSafetyVerdict(
            normalizedUrl = resolved,
            host = host,
            isAllowed = true,
            reason = SafeBrowserBlockReason.NONE,
        )
    }

    fun shouldBlockAdRequest(
        requestUrl: String,
        mainPageHost: String?,
        adBlockEnabled: Boolean,
    ): Boolean {
        if (!adBlockEnabled) return false
        val lower = requestUrl.trim().lowercase()
        if (lower.isEmpty()) return false

        val host = try {
            URI(requestUrl).host?.lowercase().orEmpty()
        } catch (_: Exception) {
            ""
        }

        if (host.isNotEmpty() && adAndTrackerDomains.any { adDomain ->
                host == adDomain || host.endsWith(".$adDomain")
            }
        ) {
            return true
        }

        val isThirdParty = mainPageHost.isNullOrBlank() ||
            (host.isNotEmpty() && host != mainPageHost.lowercase() && !host.endsWith(".${mainPageHost.lowercase()}"))

        if (isThirdParty && adPathMarkers.any { lower.contains(it) }) {
            return true
        }

        return false
    }
}
