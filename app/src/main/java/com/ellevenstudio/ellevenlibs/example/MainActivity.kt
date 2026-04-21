package com.ellevenstudio.ellevenlibs.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ellevenstudio.eads.EAdUnitId
import com.ellevenstudio.eads.EAdsBanner
import com.ellevenstudio.eads.EAds
import com.ellevenstudio.eads.EAdsInterstitial
import com.ellevenstudio.eads.EAdsOpenApp
import com.ellevenstudio.eads.EAdsRewarded
import com.ellevenstudio.estore.EStore
import com.ellevenstudio.estore.EStoreConfig
import com.ellevenstudio.estore.EStoreProductConfig
import com.ellevenstudio.estore.EStoreProductType
import com.ellevenstudio.estore.EStoreFeature
import com.ellevenstudio.estore.EStoreTheme
import com.ellevenstudio.estore.paywalls.*
import com.ellevenstudio.egate.EGate
import com.ellevenstudio.egate.EGateConfig
import com.ellevenstudio.egate.EGateOverlay
import com.ellevenstudio.esupabaseanalytics.ESupabaseAnalytics
import com.ellevenstudio.esupabaseanalytics.ESupabaseAnalyticsConfig
import android.util.Log
import com.ellevenstudio.ellevenlibs.EllevenLibs
import com.ellevenstudio.ellevenlibs.example.ui.theme.EllevenLibsTheme
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("Example", "App started - EllevenLibs v${EllevenLibs.VERSION}")

        // Configure EAds
        EAds.configure(
            context = this,
            banner = EAdUnitId(
                debug = "ca-app-pub-3940256099942544/6300978111",
                production = "ca-app-pub-xxxxxxxxxxxxx/1111111111"
            ),
            interstitial = EAdUnitId(
                debug = "ca-app-pub-3940256099942544/1033173712",
                production = "ca-app-pub-xxxxxxxxxxxxx/2222222222"
            ),
            rewarded = EAdUnitId(
                debug = "ca-app-pub-3940256099942544/5224354917",
                production = "ca-app-pub-xxxxxxxxxxxxx/3333333333"
            ),
            openApp = EAdUnitId(
                debug = "ca-app-pub-3940256099942544/9257395921",
                production = "ca-app-pub-xxxxxxxxxxxxx/4444444444"
            )
        )

        // Configure EStore
        EStore.configure(
            context = this,
            config = EStoreConfig(
                products = listOf(
                    EStoreProductConfig(
                        id = "com.ellevenstudio.example.monthly",
                        type = EStoreProductType.Subscription,
                        localizedTitles = mapOf("en" to "Monthly Premium", "hy" to "\u0531\u0574\u057D\u0561\u056F\u0561\u0576 Premium"),
                        localizedDescriptions = mapOf("en" to "Monthly access to all premium features", "hy" to "\u0531\u0574\u057D\u0561\u056F\u0561\u0576 \u0570\u0561\u057D\u0561\u0576\u0565\u056C\u056B\u0578\u0582\u0569\u0575\u0578\u0582\u0576")
                    ),
                    EStoreProductConfig(
                        id = "com.ellevenstudio.example.yearly",
                        type = EStoreProductType.Subscription,
                        localizedTitles = mapOf("en" to "Yearly Premium"),
                        localizedDescriptions = mapOf("en" to "Yearly access to all premium features")
                    ),
                    EStoreProductConfig(
                        id = "com.ellevenstudio.example.lifetime",
                        type = EStoreProductType.OneTime,
                        localizedTitles = mapOf("en" to "Lifetime Premium"),
                        localizedDescriptions = mapOf("en" to "Lifetime access to all premium features")
                    ),
                    EStoreProductConfig(
                        id = "com.ellevenstudio.example.coins100",
                        type = EStoreProductType.Consumable(amount = 100),
                        localizedTitles = mapOf("en" to "100 Coins"),
                        localizedDescriptions = mapOf("en" to "Buy 100 coins")
                    ),
                    EStoreProductConfig(
                        id = "com.ellevenstudio.example.coins500",
                        type = EStoreProductType.Consumable(amount = 500),
                        localizedTitles = mapOf("en" to "500 Coins"),
                        localizedDescriptions = mapOf("en" to "Buy 500 coins")
                    )
                ),
                features = listOf(
                    EStoreFeature(icon = "🚫", title = "Ad Free", subtitle = "Enjoy an ad-free experience"),
                    EStoreFeature(icon = "📁", title = "Unlimited Projects", subtitle = "Create without limits"),
                    EStoreFeature(icon = "☁️", title = "Cloud Sync", subtitle = "Access your data everywhere"),
                    EStoreFeature(icon = "⚡", title = "Priority Support", subtitle = "Get help within hours"),
                    EStoreFeature(icon = "✨", title = "Exclusive Content", subtitle = "Access premium-only features"),
                    EStoreFeature(icon = "👨‍👩‍👧‍👦", title = "Family Sharing", subtitle = "Share with up to 6 family members"),
                ),
                theme = EStoreTheme(
                    primaryColor = Color(0xFF6750A4),
                    accentColor = Color(0xFFFF9800)
                )
            )
        )

        // Configure EGate
        EGate.configure(
            context = this,
            config = EGateConfig(
                maxPlays = 5,
                localizedTitles = mapOf(
                    "en" to "Play Limit Reached",
                    "hy" to "\u053D\u0561\u0572\u0565\u0580\u056B \u057D\u0561\u0570\u0574\u0561\u0576\u0561\u0583\u0561\u056F\u0568 \u057D\u057A\u0561\u057C\u057E\u0561\u056E \u0567"
                ),
                localizedMessages = mapOf(
                    "en" to "Upgrade to premium or watch an ad to continue playing.",
                    "hy" to "\u0541\u0565\u057C\u0584 \u0562\u0561\u0566\u0561\u0576\u0561\u0563\u0580\u0561\u0576\u0564\u0561\u056F\u056B \u0569\u0561\u0580\u0574\u0561\u0581\u0574\u0561\u0576 \u056F\u0561\u0574 \u0564\u056B\u057F\u0565\u0584 \u0563\u0578\u057E\u0561\u0566\u0564\u0589"
                ),
                localizedPremiumButtonTexts = mapOf(
                    "en" to "Go Premium",
                    "hy" to "\u054A\u0580\u0565\u0574\u056B\u0578\u0582\u0574"
                ),
                localizedAdButtonTexts = mapOf(
                    "en" to "Watch Ad to Continue",
                    "hy" to "\u0534\u056B\u057F\u0565\u0584 \u0563\u0578\u057E\u0561\u0566\u0564"
                ),
                localizedDismissButtonTexts = mapOf(
                    "en" to "Later",
                    "hy" to "\u0540\u0565\u057F\u0578"
                )
            )
        )

        // Configure ESupabaseAnalytics — replace with your own Supabase project URL + anon key.
        // The app is expected to pass the same credentials it already uses for its own
        // Supabase calls so events land in the existing project.
        ESupabaseAnalytics.configure(
            context = this,
            config = ESupabaseAnalyticsConfig(
                supabaseUrl = "https://your-project.supabase.co",
                anonKey = "REPLACE_WITH_YOUR_ANON_KEY"
            )
        )
        ESupabaseAnalytics.track("app_open")

        enableEdgeToEdge()
        setContent {
            EllevenLibsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ExampleScreen(
                        activity = this,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun ExampleScreen(
    activity: ComponentActivity,
    modifier: Modifier = Modifier
) {
    val isPremium by EStore.isPremium.collectAsState()
    val products by EStore.products.collectAsState()
    val purchaseInfo by EStore.purchaseInfo.collectAsState()
    val allPurchaseInfos by EStore.allPurchaseInfos.collectAsState()
    val gateCount by EGate.currentCount.collectAsState()
    val gateActive by EGate.shouldShowGate.collectAsState()
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    var showPaywall by remember { mutableIntStateOf(0) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // MARK: - EGate Test
            SectionHeader("EGate - Play Limit Test")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    InfoRow("Plays", "$gateCount / ${EGate.config.maxPlays}")
                    InfoRow("Gate Active", if (gateActive) "Yes" else "No")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { EGate.recordPlay() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Play Game (Record Play)")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { EGate.reset() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset Play Count")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // MARK: - Header
            Text(
                text = "EllevenLibs Example",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Version: ${EllevenLibs.VERSION}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Premium: ${if (isPremium) "Yes" else "No"}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isPremium) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Coins: ${EStore.consumableBalance("com.ellevenstudio.example.coins100") + EStore.consumableBalance("com.ellevenstudio.example.coins500")}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // MARK: - EAds Banner
            SectionHeader("EAds - Banner")
            Card(modifier = Modifier.fillMaxWidth()) {
                EAdsBanner(modifier = Modifier.fillMaxWidth())
            }

            Spacer(modifier = Modifier.height(16.dp))

            // MARK: - EAds Fullscreen
            SectionHeader("EAds - Fullscreen")
            Button(
                onClick = {
                    EAdsInterstitial.show(activity) {
                        Log.i("Example","Interstitial dismissed")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Show Interstitial")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    EAdsRewarded.show(
                        activity = activity,
                        onReward = { reward ->
                            Log.i("Example","Reward earned: ${reward.amount} ${reward.type}")
                        },
                        onDismiss = {
                            Log.i("Example","Rewarded ad dismissed")
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Show Rewarded Ad")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    EAdsOpenApp.show(activity)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Show Open App Ad")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    EAdsOpenApp.attachToAppLifecycle { activity }
                    Log.i("Example","Open app ad attached to lifecycle")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Attach Open App to Lifecycle")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // MARK: - EStore Products
            SectionHeader("EStore - Products")
            if (products.isEmpty()) {
                Text(
                    text = "No products loaded",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                products.forEach { product ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.displayName,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = product.localizedDescription,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (product.subscriptionPeriod != null) {
                                    Text(
                                        text = "Period: ${product.subscriptionPeriod}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                EStore.purchase(activity, product.id)
                            }) {
                                Text(product.displayPrice.ifEmpty { "Buy" })
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // MARK: - EStore Actions
            SectionHeader("EStore - Actions")
            Button(
                onClick = { EStore.restore() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Restore Purchases")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    EStore.clearTestPurchases()
                    Log.i("Example","Test purchases cleared")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear Test Purchases")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // MARK: - Paywalls
            SectionHeader("EStore - Paywalls")
            for (i in 1..9) {
                Button(
                    onClick = { showPaywall = i },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Paywall $i")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Button(
                onClick = { showPaywall = 10 },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Coin Store (Paywall 10)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            SectionHeader("EStore - Fancy Paywalls")
            val fancyNames = listOf("Animated Gradient", "Floating Particles", "Glassmorphism", "Dark Luxury", "3D Interactive")
            fancyNames.forEachIndexed { index, name ->
                Button(
                    onClick = { showPaywall = 11 + index },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Paywall ${11 + index} - $name")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // MARK: - Active Purchase Info
            if (purchaseInfo != null) {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader("EStore - Active Purchase")
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        allPurchaseInfos.forEachIndexed { index, info ->
                            if (index > 0) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                            InfoRow("Product(s)", info.productIds.joinToString())
                            InfoRow("Purchase Date", dateFormat.format(info.purchaseDate))
                            InfoRow("Type", when (info.type) {
                                is EStoreProductType.Subscription -> "Subscription"
                                is EStoreProductType.OneTime -> "One-Time"
                                is EStoreProductType.Consumable -> "Consumable"
                            })
                            InfoRow("Auto-Renewing", if (info.isAutoRenewing) "Yes" else "No")
                            info.expirationDate?.let { InfoRow("Expiration", dateFormat.format(it)) }
                            info.orderId?.let { InfoRow("Order ID", it) }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Paywall overlays
        when (showPaywall) {
            1 -> EPaywall1(activity = activity, onDismiss = { showPaywall = 0 })
            2 -> EPaywall2(activity = activity, onDismiss = { showPaywall = 0 })
            3 -> EPaywall3(activity = activity, onDismiss = { showPaywall = 0 })
            4 -> EPaywall4(activity = activity, onDismiss = { showPaywall = 0 })
            5 -> EPaywall5(activity = activity, onDismiss = { showPaywall = 0 })
            6 -> EPaywall6(activity = activity, onDismiss = { showPaywall = 0 })
            7 -> EPaywall7(activity = activity, onDismiss = { showPaywall = 0 })
            8 -> EPaywall8(activity = activity, onDismiss = { showPaywall = 0 })
            9 -> EPaywall9(activity = activity, onDismiss = { showPaywall = 0 })
            10 -> EPaywall10(activity = activity, onDismiss = { showPaywall = 0 })
            11 -> EPaywall11(activity = activity, onDismiss = { showPaywall = 0 })
            12 -> EPaywall12(activity = activity, onDismiss = { showPaywall = 0 })
            13 -> EPaywall13(activity = activity, onDismiss = { showPaywall = 0 })
            14 -> EPaywall14(activity = activity, onDismiss = { showPaywall = 0 })
            15 -> EPaywall15(activity = activity, onDismiss = { showPaywall = 0 })
        }

        // EGate overlay
        EGateOverlay(activity = activity)
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
