package com.example

import com.example.domain.SafeBrowserBlockReason
import com.example.domain.SafeBrowserFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeBrowserFilterTest {

    @Test
    fun resolveInputToUrl_normalizesDomainsQueriesAndEmptyInput() {
        assertEquals(SafeBrowserFilter.DEFAULT_HOME_URL, SafeBrowserFilter.resolveInputToUrl("   "))
        assertEquals("https://ar.wikipedia.org", SafeBrowserFilter.resolveInputToUrl("ar.wikipedia.org"))
        assertEquals("https://www.ekb.eg/home", SafeBrowserFilter.resolveInputToUrl("https://www.ekb.eg/home"))

        val searchUrl = SafeBrowserFilter.resolveInputToUrl("cyber security tips")
        assertTrue(searchUrl.startsWith("https://duckduckgo.com/?kp=1&q="))
        assertTrue(searchUrl.contains("cyber"))
    }

    @Test
    fun evaluateNavigation_blocksSensitiveAdultGamblingAndOnionSites() {
        val adultVerdict = SafeBrowserFilter.evaluateNavigation("https://www.pornhub.com/video")
        assertFalse(adultVerdict.isAllowed)
        assertEquals(SafeBrowserBlockReason.SENSITIVE_CONTENT, adultVerdict.reason)

        val gamblingVerdict = SafeBrowserFilter.evaluateNavigation("1xbet.com")
        assertFalse(gamblingVerdict.isAllowed)
        assertEquals(SafeBrowserBlockReason.SENSITIVE_CONTENT, gamblingVerdict.reason)

        val onionVerdict = SafeBrowserFilter.evaluateNavigation("https://examplehidden.onion")
        assertFalse(onionVerdict.isAllowed)
        assertEquals(SafeBrowserBlockReason.SENSITIVE_CONTENT, onionVerdict.reason)

        val tokenInSubdomain = SafeBrowserFilter.evaluateNavigation("https://free-casino-bonus.example.org")
        assertFalse(tokenInSubdomain.isAllowed)
        assertEquals(SafeBrowserBlockReason.SENSITIVE_CONTENT, tokenInSubdomain.reason)
    }

    @Test
    fun evaluateNavigation_respectsSensitiveFilterToggle() {
        val disabledFilter = SafeBrowserFilter.evaluateNavigation(
            rawUrl = "https://www.bet365.com",
            sensitiveFilterEnabled = false,
        )
        assertTrue(disabledFilter.isAllowed)
        assertEquals(SafeBrowserBlockReason.NONE, disabledFilter.reason)
    }

    @Test
    fun evaluateNavigation_blocksUnsafeSchemesAndPhishingUrls() {
        val jsScheme = SafeBrowserFilter.evaluateNavigation("javascript:alert(1)")
        assertFalse(jsScheme.isAllowed)
        assertEquals(SafeBrowserBlockReason.UNSAFE_SCHEME, jsScheme.reason)

        val fileScheme = SafeBrowserFilter.evaluateNavigation("file:///etc/passwd")
        assertFalse(fileScheme.isAllowed)
        assertEquals(SafeBrowserBlockReason.UNSAFE_SCHEME, fileScheme.reason)

        val rawIpPhishing = SafeBrowserFilter.evaluateNavigation("https://192.168.1.50/login")
        assertFalse(rawIpPhishing.isAllowed)
        assertEquals(SafeBrowserBlockReason.PHISHING_RISK, rawIpPhishing.reason)

        val typosquatPhishing = SafeBrowserFilter.evaluateNavigation("https://facebok-security.com/verify")
        assertFalse(typosquatPhishing.isAllowed)
        assertEquals(SafeBrowserBlockReason.PHISHING_RISK, typosquatPhishing.reason)

        val insecureShortener = SafeBrowserFilter.evaluateNavigation("http://bit.ly/free-gift")
        assertFalse(insecureShortener.isAllowed)
        assertEquals(SafeBrowserBlockReason.PHISHING_RISK, insecureShortener.reason)
    }

    @Test
    fun evaluateNavigation_allowsTrustedCleanWebsites() {
        val wiki = SafeBrowserFilter.evaluateNavigation("https://ar.wikipedia.org/wiki/Security")
        assertTrue(wiki.isAllowed)
        assertEquals(SafeBrowserBlockReason.NONE, wiki.reason)

        val ekb = SafeBrowserFilter.evaluateNavigation("https://www.ekb.eg")
        assertTrue(ekb.isAllowed)
        assertEquals(SafeBrowserBlockReason.NONE, ekb.reason)
    }

    @Test
    fun shouldBlockAdRequest_blocksKnownAdDomainsAndThirdPartyAdPathsOnlyWhenEnabled() {
        assertTrue(
            SafeBrowserFilter.shouldBlockAdRequest(
                requestUrl = "https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js",
                mainPageHost = "ar.wikipedia.org",
                adBlockEnabled = true,
            )
        )
        assertTrue(
            SafeBrowserFilter.shouldBlockAdRequest(
                requestUrl = "https://cdn.doubleclick.net/pixel.png",
                mainPageHost = "www.ekb.eg",
                adBlockEnabled = true,
            )
        )
        assertTrue(
            SafeBrowserFilter.shouldBlockAdRequest(
                requestUrl = "https://tracker.thirdparty.net/prebid/bid.js",
                mainPageHost = "ar.wikipedia.org",
                adBlockEnabled = true,
            )
        )
        assertFalse(
            SafeBrowserFilter.shouldBlockAdRequest(
                requestUrl = "https://ar.wikipedia.org/static/images/logo.png",
                mainPageHost = "ar.wikipedia.org",
                adBlockEnabled = true,
            )
        )
        assertFalse(
            SafeBrowserFilter.shouldBlockAdRequest(
                requestUrl = "https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js",
                mainPageHost = "ar.wikipedia.org",
                adBlockEnabled = false,
            )
        )
    }
}
