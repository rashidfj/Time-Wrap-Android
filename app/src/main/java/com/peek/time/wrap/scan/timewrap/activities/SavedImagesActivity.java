package com.peek.time.wrap.scan.timewrap.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.ActionMode;

import com.peek.time.wrap.scan.timewrap.MainActivity;
import com.peek.time.wrap.scan.timewrap.R;
import com.peek.time.wrap.scan.timewrap.adapters.SavedAdapter;
import com.peek.time.wrap.scan.timewrap.databinding.ActivityMainBinding;
import com.peek.time.wrap.scan.timewrap.databinding.ActivitySavedImagesBinding;
import com.peek.time.wrap.scan.timewrap.model.SavedModel;

import java.util.ArrayList;
import java.util.List;

public class SavedImagesActivity extends AppCompatActivity {

    ActivitySavedImagesBinding binding;
    private final List<SavedModel> imagesList = new ArrayList<>();
    SavedAdapter adapters;
    Handler handler = new Handler();
    public static ActionMode statusActionMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySavedImagesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initItems();
        clickListeners();
    }

    private void initItems() {


    }

    private void clickListeners() {
        binding.back.setOnClickListener(view -> onBackPressed());
    }
}