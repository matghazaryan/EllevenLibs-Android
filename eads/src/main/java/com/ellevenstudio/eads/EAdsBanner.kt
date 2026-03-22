package com.ellevenstudio.eads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * A Jetpack Compose composable that displays a Google AdMob banner ad.
 *
 * Usage:
 *     EAdsBanner()
 *     EAdsBanner(adSize = AdSize.LARGE_BANNER)
 */
@Composable
fun EAdsBanner(
    modifier: Modifier = Modifier,
    adSize: AdSize = AdSize.BANNER
) {
    val adUnitId = EAds.bannerAdUnitId ?: return

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(adSize)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
