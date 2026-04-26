# Legacy AIDL Billing API (kept for older callers; current is below)
-keep class com.android.vending.billing.** { *; }

# Google Play Billing Client 7+ — keep public surface and internals used via
# reflection by the Play Services billing module. Apps using
# proguard-android-optimize.txt will otherwise see queryProductDetails return
# empty results in release builds while debug works fine.
-keep class com.android.billingclient.api.** { *; }
-keep interface com.android.billingclient.api.** { *; }
-keep class com.google.android.gms.internal.play_billing.** { *; }

# Keep EStore's own surface so R8 -repackageclasses/inlining doesn't sever
# the Billing listener and data classes that ferry ProductDetails out.
-keep class com.ellevenstudio.estore.** { *; }
