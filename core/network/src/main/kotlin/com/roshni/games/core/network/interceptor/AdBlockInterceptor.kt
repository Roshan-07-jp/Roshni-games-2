package com.roshni.games.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdBlockInterceptor @Inject constructor() : Interceptor {

    private val blockedHosts = setOf(
        // Google AdSense and DoubleClick
        "googleadservices.com",
        "googlesyndication.com",
        "doubleclick.net",
        "googletagmanager.com",
        "googletagservices.com",

        // Facebook/Meta ads
        "facebook.com/tr",
        "facebook.net",

        // Amazon ads
        "amazon-adsystem.com",

        // Other common ad networks
        "outbrain.com",
        "taboola.com",
        "adnxs.com",
        "rubiconproject.com",
        "pubmatic.com",
        "openx.net",
        "contextweb.com",
        "criteo.com",
        "smartadserver.com",
        "smaato.com",
        "inmobi.com",
        "chartboost.com",
        "unity3d.com",
        "vungle.com",
        "applovin.com",
        "ironsrc.com",
        "pangle.com",
        "mintegral.com",
        "adxcorp.kr",
        "adxbid.info",
        "adcolony.com",
        "tapjoy.com",
        "fyber.com",
        "admob.com",
        "mopub.com",
        "startapp.com",
        "ogury.com",
        "verve.com",
        "smaato.net",
        "bidmachine.ml",
        "mytracker.ru",
        "yandex.ru/ads",
        "between-digital.com",
        "adtelligent.com",
        "adform.net",
        "adition.com",
        "adtech.de",
        "advertising.com",
        "aol.com/aim",
        "atdmt.com",
        "blismedia.com",
        "casalemedia.com",
        "collective-media.net",
        "districtm.ca",
        "exponential.com",
        "gumgum.com",
        "indexexchange.com",
        "kargo.com",
        "lijit.com",
        "media.net",
        "moatads.com",
        "quantcast.com",
        "quantserve.com",
        "revcontent.com",
        "rhythmone.com",
        "sharethrough.com",
        "sovrn.com",
        "spotx.tv",
        "spotxchange.com",
        "teads.tv",
        "tremorvideo.com",
        "tribalfusion.com",
        "undertone.com",
        "videologygroup.com",
        "yieldbot.com",
        "yieldmo.com",
        "zemanta.com"
    )

    private val blockedKeywords = setOf(
        "ads",
        "advertisement",
        "banner",
        "popup",
        "tracking",
        "analytics",
        "beacon",
        "pixel",
        "impression",
        "click_tracking"
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()

        // Check if the URL contains blocked hosts
        val isBlockedHost = blockedHosts.any { host ->
            url.contains(host, ignoreCase = true)
        }

        // Check if the URL contains blocked keywords
        val isBlockedKeyword = blockedKeywords.any { keyword ->
            url.contains(keyword, ignoreCase = true)
        }

        if (isBlockedHost || isBlockedKeyword) {
            Timber.d("AdBlock: Blocked request to $url")
            throw IOException("Ad blocked")
        }

        return chain.proceed(request)
    }
}