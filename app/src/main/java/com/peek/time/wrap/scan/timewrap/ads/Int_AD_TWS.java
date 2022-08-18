package com.peek.time.wrap.scan.timewrap.ads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.peek.time.wrap.scan.timewrap.R;
import com.peek.time.wrap.scan.timewrap.helpers.Constant;

public class Int_AD_TWS {
    private Context mContext_TWS;
    private String TAG_TWS = "Intad";
    private static Int_AD_TWS mInstance_TWS;

    AdRequest adRequest_TWS;
    private InterstitialAd mInterstitialAd_TWS;
    private int TWS_count = 0;


    private Int_AD_TWS(Context mContext) {
        this.mContext_TWS = mContext;
        adRequest_TWS = new AdRequest.Builder().build();
    }

    public static synchronized Int_AD_TWS getInstance(Context context) {
        if (mInstance_TWS == null) {
            mInstance_TWS = new Int_AD_TWS(context);
        }
        return mInstance_TWS;
    }

    public void loadAd() {
        if (mInterstitialAd_TWS == null && !Constant.isPremiumUser) {
            try {
                InterstitialAd.load(mContext_TWS, mContext_TWS.getString(R.string.interstitial_ad_id), adRequest_TWS,
                        new InterstitialAdLoadCallback() {
                            @Override
                            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                                mInterstitialAd_TWS = interstitialAd;
                                setListener();
                                Log.i(TAG_TWS, "onAdLoaded");
                            }

                            @Override
                            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                                Log.i(TAG_TWS, loadAdError.getMessage());
                                mInterstitialAd_TWS = null;
                            }

                        });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void setListener() {
        if (mInterstitialAd_TWS != null) {
            mInterstitialAd_TWS.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    // Called when fullscreen content is dismissed.
                    Log.d(TAG_TWS, "The ad was dismissed.");
                    loadAd();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    // Called when fullscreen content failed to show.
                    Log.d(TAG_TWS, "The ad failed to show.");

                }

                @Override
                public void onAdShowedFullScreenContent() {
                    // Called when fullscreen content is shown.
                    // Make sure to set your reference to null so you don't
                    // show it a second time.
                    mInterstitialAd_TWS = null;
                    Log.d(TAG_TWS, "The ad was shown.");
                }
            });
        }
    }

    public void showAd_WA(Activity activity) {
        try {
            if (!Constant.isPremiumUser) {
                if (mInterstitialAd_TWS != null) {
                    TWS_count += 1;
                    if (TWS_count % 1 == 0) {
                        mInterstitialAd_TWS.show(activity);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
