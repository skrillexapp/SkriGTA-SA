package com.glory.launcher.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ConfigValidator {
    public static void validateConfigFiles(Context context) {
        File externalFilesDir = context.getExternalFilesDir(null);
        if (externalFilesDir == null) return;

        File file = new File(externalFilesDir, "SAMP/settings.ini");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            copyAsset(context.getAssets(), "settings.ini", file.toString());
        }

        String[] touchCfgs = new String[]{
            "data/TouchDefaultPhone3x2.cfg",
            "data/TouchDefaultPhoneWidescreen.cfg",
            "data/TouchDefaultTablet4x3.cfg",
            "data/TouchDefaultTabletWidescreen.cfg"
        };
        for (String cfg : touchCfgs) {
            File target = new File(externalFilesDir, cfg);
            if (!target.exists() || target.length() == 0) {
                if (target.getParentFile() != null) target.getParentFile().mkdirs();
                copyAsset(context.getAssets(), cfg, target.toString());
            }
        }

        // Undo v1.0.1 TouchDefaultPhoneWidescreen2.cfg + restore SAMP-safe gta_sa.set
        SharedPreferences prefs = context.getSharedPreferences("skrigta_fixes", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("touch_fix_v2", false)) {
            File badOverlay = new File(externalFilesDir, "data/TouchDefaultPhoneWidescreen2.cfg");
            if (badOverlay.exists()) {
                badOverlay.delete();
            }
            File gtaset = new File(externalFilesDir, "gta_sa.set");
            copyAsset(context.getAssets(), "gta_sa.set", gtaset.toString());
            prefs.edit().putBoolean("touch_fix_v2", true).apply();
        }
    }

    static boolean copyAsset(AssetManager assetManager, String str, String str2) {
        try {
            InputStream open = assetManager.open(str);
            new File(str2).createNewFile();
            FileOutputStream fileOutputStream = new FileOutputStream(str2);
            copyFile(open, fileOutputStream);
            open.close();
            fileOutputStream.flush();
            fileOutputStream.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static void copyFile(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] bArr = new byte[1024];
        while (true) {
            int read = inputStream.read(bArr);
            if (read != -1) {
                outputStream.write(bArr, 0, read);
            } else {
                return;
            }
        }
    }
}
