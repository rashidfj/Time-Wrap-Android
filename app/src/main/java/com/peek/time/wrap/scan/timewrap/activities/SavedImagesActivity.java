package com.peek.time.wrap.scan.timewrap.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.ActionMode;
import android.view.View;
import android.widget.Toast;

import com.peek.time.wrap.scan.timewrap.MainActivity;
import com.peek.time.wrap.scan.timewrap.R;
import com.peek.time.wrap.scan.timewrap.adapters.SavedAdapter;
import com.peek.time.wrap.scan.timewrap.databinding.ActivityMainBinding;
import com.peek.time.wrap.scan.timewrap.databinding.ActivitySavedImagesBinding;
import com.peek.time.wrap.scan.timewrap.model.SavedModel;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
        File directory = new File(SavedImagesActivity.this.getExternalFilesDir("Photos"), "Time Wrap");
        File path = directory.getAbsoluteFile();

        execute(path);
    }

    private void clickListeners() {
        binding.back.setOnClickListener(view -> onBackPressed());
    }


    private void execute(File path) {
        if (!path.exists()) {
            return;
        }

        new Thread(() -> {
            File[] imageFiles = path.listFiles();
            imagesList.clear();

            if (imageFiles != null && imageFiles.length > 0) {
                List<File> statusFiles = Arrays.asList(imageFiles);

                try {
                    Collections.sort(statusFiles, new Comparator<File>() {
                        public int compare(File f1, File f2) {
                            try {
                                return String.valueOf(f2.lastModified()).compareTo(String.valueOf(f1.lastModified()));
                            } catch (Exception e) {
                                return 1;
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }

                for (File file : statusFiles) {
                    String fileName = file.getName();

                    double length2 = (double) (file.length() / 1024);
                    double length3 = length2 / 1024;
                    DecimalFormat dtime = new DecimalFormat("#.##");

                    double file_size = Double.parseDouble((String.valueOf(dtime.format(length3))));

                    String date = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

                    SavedModel status = new SavedModel(file, file.getAbsolutePath(), String.valueOf(file_size), date);

                    imagesList.add(status);

                }

                handler.post(() -> {
                    GridLayoutManager layoutManager = new GridLayoutManager(SavedImagesActivity.this, 2);
                    binding.rvSave.setLayoutManager(layoutManager);
                    adapters = new SavedAdapter(imagesList, SavedImagesActivity.this);
                    binding.rvSave.setAdapter(adapters);
                    adapters.notifyDataSetChanged();
                });
            }
        }).start();


    }
}