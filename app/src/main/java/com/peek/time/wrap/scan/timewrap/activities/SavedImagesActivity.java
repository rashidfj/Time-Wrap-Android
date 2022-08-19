package com.peek.time.wrap.scan.timewrap.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.view.ActionMode;

import com.peek.time.wrap.scan.timewrap.R;
import com.peek.time.wrap.scan.timewrap.adapters.SavedAdapter;
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
    private final List<SavedModel> mList = new ArrayList<>();
    SavedAdapter savedAdapter;
    Handler handler = new Handler();
    public static ActionMode actionModeTWS;
    private final double divideBy = 1024;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySavedImagesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initItems();
        clickListeners();
    }

    private void initItems() {
        try {
            File directory = new File(SavedImagesActivity.this.getExternalFilesDir("Photos"), getString(R.string.app_name));
            File path = directory.getAbsoluteFile();
            startFileScan(path);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    private void clickListeners() {
        binding.back.setOnClickListener(view -> onBackPressed());
    }


    @SuppressLint("NotifyDataSetChanged")
    private void startFileScan(File path) {
        try {
            if (!path.exists()) {
                return;
            }

            new Thread(() -> {
                File[] imageFiles = path.listFiles();
                mList.clear();

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

                    for (File mFile : statusFiles) {
                        double l2 = mFile.length() / divideBy;
                        double l3 = l2 / divideBy;
                        DecimalFormat decimalFormat = new DecimalFormat("#.##");
                        double fileSize = Double.parseDouble((decimalFormat.format(l3)));
                        String date = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
                        SavedModel status = new SavedModel(mFile, mFile.getAbsolutePath(), String.valueOf(fileSize), date);

                        mList.add(status);

                    }

                    handler.post(() -> {
                        GridLayoutManager layoutManager = new GridLayoutManager(SavedImagesActivity.this, 2);
                        binding.rvSave.setLayoutManager(layoutManager);
                        savedAdapter = new SavedAdapter(mList, SavedImagesActivity.this);
                        binding.rvSave.setAdapter(savedAdapter);
                        savedAdapter.notifyDataSetChanged();
                    });
                }
            }).start();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }


    }
}