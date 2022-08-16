package com.peek.time.wrap.scan.timewrap;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.peek.time.wrap.scan.timewrap.activities.SavedImagesActivity;
import com.peek.time.wrap.scan.timewrap.activities.ScanActivity;
import com.peek.time.wrap.scan.timewrap.activities.SplashActivity;
import com.peek.time.wrap.scan.timewrap.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initItems();
        clickListeners();
    }

    private void initItems() {

    }

    private void clickListeners() {
        binding.scanView.setOnClickListener(view -> startActivity(new Intent(MainActivity.this, ScanActivity.class)));
        binding.savedFilesView.setOnClickListener(view -> startActivity(new Intent(MainActivity.this, SavedImagesActivity.class)));
    }
}