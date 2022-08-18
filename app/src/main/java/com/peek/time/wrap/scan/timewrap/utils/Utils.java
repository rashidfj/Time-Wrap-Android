package com.peek.time.wrap.scan.timewrap.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import com.peek.time.wrap.scan.timewrap.BuildConfig;
import com.peek.time.wrap.scan.timewrap.R;
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

    public static void openPrivacy_TWS(Activity activity_TWS, String link_TWS) {
        try {
            activity_TWS.startActivity(
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(link_TWS)
                    )
            );
        } catch (RuntimeException e) {
            Toast.makeText(activity_TWS, "No Link Found", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    public static void shareApp_TWS(Activity activity) {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.app_name));
            String shareMessage = "\nLet me recommend you this application\n\n";
            shareMessage = shareMessage + "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID + "\n\n";
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
            activity.startActivity(Intent.createChooser(shareIntent, "choose one"));
        } catch (Exception e) {
            //e.toString();
        }
    }

}
