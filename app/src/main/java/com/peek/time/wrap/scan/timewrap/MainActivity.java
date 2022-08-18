package com.peek.time.wrap.scan.timewrap;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;

import com.peek.time.wrap.scan.timewrap.activities.SavedImagesActivity;
import com.peek.time.wrap.scan.timewrap.activities.ScanActivity;
import com.peek.time.wrap.scan.timewrap.ads.Int_AD_TWS;
import com.peek.time.wrap.scan.timewrap.databinding.ActivityMainBinding;
import com.peek.time.wrap.scan.timewrap.model.SavedModel;
import com.peek.time.wrap.scan.timewrap.utils.Utils;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    private LinearLayout llRateUs, llShareApp, llMoreApps, llPrivacyPolicy;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initItems();
        clickListeners();
    }

    private void initItems() {
        View navHeader = binding.navView.getHeaderView(0);
        llRateUs = navHeader.findViewById(R.id.ll_rate_us);
        llShareApp = navHeader.findViewById(R.id.ll_share_apps);
        llMoreApps = navHeader.findViewById(R.id.ll_more_apps);
        llPrivacyPolicy = navHeader.findViewById(R.id.ll_privacy_policy);
        Int_AD_TWS.getInstance(this).loadAd();
    }

    private void clickListeners() {
        binding.scanView.setOnClickListener(view -> startActivity(new Intent(MainActivity.this, ScanActivity.class)));
        binding.savedFilesView.setOnClickListener(view -> startActivity(new Intent(MainActivity.this, SavedImagesActivity.class)));

        binding.toolbar.toggleDrawer.setOnClickListener(view -> {
            toggleDrawable();
        });
        llRateUs.setOnClickListener(view -> {
            showRateUsDialog();
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        });
        llMoreApps.setOnClickListener(view -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/developer?id=Peek+International")));
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        });
        llPrivacyPolicy.setOnClickListener(view -> {

            Utils.openPrivacy_TWS(MainActivity.this,getString(R.string.privacy_policy_link));
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        });
        llShareApp.setOnClickListener(view -> {
            Utils.shareApp_TWS(MainActivity.this);
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        });

    }

    private void showRateUsDialog() {
        androidx.appcompat.app.AlertDialog.Builder alert = new androidx.appcompat.app.AlertDialog.Builder(
                new ContextThemeWrapper(MainActivity.this, R.style.CustomAlertDialog));

        alert.setTitle("Rate Us?");
        alert.setMessage("Are you sure you want rate our app?");
        alert.setPositiveButton(android.R.string.yes, (dialog, which) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName()))));
        alert.setNegativeButton(android.R.string.no, (dialog, which) -> {
            dialog.cancel();
        });
        alert.show();
    }

    private void toggleDrawable() {
        try {
            if (binding.drawerLayout.isDrawerVisible(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START);

            } else {
                binding.drawerLayout.openDrawer(GravityCompat.START);

            }

        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }
}