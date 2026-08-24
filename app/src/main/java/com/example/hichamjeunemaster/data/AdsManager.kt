package com.example.hichamjeunemaster.data

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdsManager {
    private const val TAG = "AdsManager"
    
    // IDs fournis par l'utilisateur
    const val BANNER_ID = "ca-app-pub-8659111696182234/9783829163"
    const val INTERSTITIAL_ID = "ca-app-pub-8659111696182234/3250210405"

    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false

    fun initialize(context: Context) {
        MobileAds.initialize(context) { status ->
            Log.d(TAG, "AdMob Initialized: $status")
            loadInterstitial(context)
        }
    }

    fun loadInterstitial(context: Context) {
        if (isInterstitialLoading || interstitialAd != null) return
        
        isInterstitialLoading = true
        val adRequest = AdRequest.Builder().build()
        
        InterstitialAd.load(
            context,
            INTERSTITIAL_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial loaded")
                    interstitialAd = ad
                    isInterstitialLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Interstitial failed to load: ${error.message}")
                    interstitialAd = null
                    isInterstitialLoading = false
                }
            }
        )
    }

    fun showInterstitial(activity: Activity, onAdClosed: () -> Unit = {}) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial dismissed")
                    interstitialAd = null
                    loadInterstitial(activity)
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                    Log.e(TAG, "Interstitial failed to show: ${error.message}")
                    interstitialAd = null
                    onAdClosed()
                }
            }
            interstitialAd?.show(activity)
        } else {
            Log.d(TAG, "Interstitial not ready")
            loadInterstitial(activity)
            onAdClosed()
        }
    }
}
