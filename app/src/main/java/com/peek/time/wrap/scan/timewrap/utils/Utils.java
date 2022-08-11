package com.peek.time.wrap.scan.timewrap.utils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.peek.time.wrap.scan.timewrap.helpers.Constant;

import java.io.File;

public class Utils {

    public static void makeDir(Activity activity) {
        File file = new File(Constant.MEDIA_FOLDER);
        if (!file.exists()) {
            file.mkdirs();
            activity.sendBroadcast(new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE", Uri.fromFile(file)));
        }
    }
}
