package com.peek.time.wrap.scan.timewrap.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

import com.peek.time.wrap.scan.timewrap.R;
import com.peek.time.wrap.scan.timewrap.databinding.ActivityImageOpenBinding;
import com.peek.time.wrap.scan.timewrap.databinding.ActivitySavedImagesBinding;

public class ImageOpenActivity extends AppCompatActivity {

    ActivityImageOpenBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityImageOpenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initItems();
        clickListeners();
    }

    private void initItems() {
        binding.ivIcon.setImageBitmap(BitmapFactory.decodeFile(getIntent().getStringExtra("STR_IMAGE")));
    }

    private void clickListeners() {
        binding.back.setOnClickListener(view -> onBackPressed());
    }
}