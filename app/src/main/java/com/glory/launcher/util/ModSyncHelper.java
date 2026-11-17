package com.glory.launcher.util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.util.Log;

import com.glory.launcher.network.model.LauncherManifest;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ModSyncHelper {
    private static final String TAG = "ModSyncHelper";

    public static class InstalledMod {
        public String sha256;
        public List<String> files = new ArrayList<>();
    }

    public static void syncModsAndLaunch(Activity activity, LauncherManifest manifest, Runnable onReady) {
        ConfigValidator.validateConfigFiles(activity);

        if (manifest == null || manifest.getData() == null || manifest.getMods() == null) {
            onReady.run();
            return;
        }

        File gameDir = activity.getExternalFilesDir(null);
        if (gameDir == null) {
            onReady.run();
            return;
        }

        File stateFile = new File(gameDir, ".mods_state.json");
        Map<String, InstalledMod> installed = loadState(stateFile);

        List<LauncherManifest.ModEntry> toDownload = new ArrayList<>();
        Set<String> activeIds = new HashSet<>();
        for (LauncherManifest.ModEntry m : manifest.getMods()) {
            if (m != null && m.isEnabled() && m.getId() != null) {
                activeIds.add(m.getId());
                InstalledMod cur = installed.get(m.getId());
                if (cur == null || cur.sha256 == null || !cur.sha256.equalsIgnoreCase(m.getSha256())) {
                    toDownload.add(m);
                }
            }
        }

        boolean needsRemoval = false;
        for (String id : installed.keySet()) {
            if (!activeIds.contains(id)) {
                needsRemoval = true;
                break;
            }
        }

        if (toDownload.isEmpty() && !needsRemoval) {
            onReady.run();
            return;
        }

        ProgressDialog pd = new ProgressDialog(activity);
        pd.setTitle("Sincronizando Mods del Servidor");
        pd.setMessage("Aplicando recursos...");
        pd.setCancelable(false);
        pd.show();

        new Thread(() -> {
            try {
                // 1. Remove disabled or deleted mods
                List<String> removeIds = new ArrayList<>();
                for (String id : installed.keySet()) {
                    if (!activeIds.contains(id)) {
                        removeIds.add(id);
                    }
                }
                for (String id : removeIds) {
                    InstalledMod im = installed.remove(id);
                    if (im != null && im.files != null) {
                        for (String rel : im.files) {
                            File f = new File(gameDir, rel);
                            if (f.exists()) f.delete();
                        }
                    }
                }

                // 2. Download & install new/updated mods
                String baseUrl = manifest.getData().getBaseUrl();
                if (baseUrl == null) baseUrl = "http://10.25.60.18:8090/launcher/";
                if (!baseUrl.endsWith("/")) baseUrl += "/";

                File tmpDir = new File(gameDir, ".mods_tmp");
                if (!tmpDir.exists()) tmpDir.mkdirs();

                for (LauncherManifest.ModEntry m : toDownload) {
                    final String modName = m.getName() != null ? m.getName() : m.getId();
                    activity.runOnUiThread(() -> pd.setMessage("Descargando mod: " + modName));

                    // Remove old files if updating
                    InstalledMod old = installed.remove(m.getId());
                    if (old != null && old.files != null) {
                        for (String rel : old.files) {
                            File f = new File(gameDir, rel);
                            if (f.exists()) f.delete();
                        }
                    }

                    String urlStr = baseUrl + m.getFile();
                    File tmpZip = new File(tmpDir, m.getId() + ".zip");
                    downloadFile(urlStr, tmpZip);

                    if (m.getSha256() != null && !m.getSha256().isEmpty()) {
                        String actual = sha256File(tmpZip);
                        if (!m.getSha256().equalsIgnoreCase(actual)) {
                            tmpZip.delete();
                            continue;
                        }
                    }

                    activity.runOnUiThread(() -> pd.setMessage("Instalando mod: " + modName));
                    List<String> extracted = extractModZip(tmpZip, gameDir);
                    tmpZip.delete();

                    InstalledMod im = new InstalledMod();
                    im.sha256 = m.getSha256();
                    im.files = extracted;
                    installed.put(m.getId(), im);
                }
                saveState(stateFile, installed);
            } catch (Exception e) {
                Log.e(TAG, "Mod sync error", e);
            } finally {
                activity.runOnUiThread(() -> {
                    try {
                        if (pd.isShowing()) pd.dismiss();
                    } catch (Exception ignored) {}
                    onReady.run();
                });
            }
        }).start();
    }

    private static Map<String, InstalledMod> loadState(File f) {
        if (!f.exists()) return new HashMap<>();
        try (FileInputStream fis = new FileInputStream(f)) {
            byte[] data = new byte[(int) f.length()];
            int read = fis.read(data);
            String json = new String(data, 0, Math.max(0, read), StandardCharsets.UTF_8);
            Type t = new TypeToken<Map<String, InstalledMod>>(){}.getType();
            Map<String, InstalledMod> m = new Gson().fromJson(json, t);
            return m != null ? m : new HashMap<>();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private static void saveState(File f, Map<String, InstalledMod> state) {
        try (FileOutputStream fos = new FileOutputStream(f)) {
            String json = new Gson().toJson(state);
            fos.write(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            Log.e(TAG, "Failed saving mod state", e);
        }
    }

    private static void downloadFile(String urlStr, File outFile) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        try (InputStream in = new BufferedInputStream(conn.getInputStream());
             FileOutputStream out = new FileOutputStream(outFile)) {
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
        } finally {
            conn.disconnect();
        }
    }

    private static List<String> extractModZip(File zipFile, File gameDir) throws Exception {
        List<String> extracted = new ArrayList<>();
        String baseCanon = gameDir.getCanonicalPath();
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
            ZipEntry entry;
            byte[] buf = new byte[16384];
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String name = entry.getName().replace("\\\\", "/");
                if (name.startsWith("files/")) name = name.substring(6);
                if (name.isEmpty()) continue;
                File target = new File(gameDir, name);
                if (!target.getCanonicalPath().startsWith(baseCanon + File.separator)) continue;
                if (target.getParentFile() != null) target.getParentFile().mkdirs();
                try (FileOutputStream fos = new FileOutputStream(target)) {
                    int n;
                    while ((n = zis.read(buf)) != -1) {
                        fos.write(buf, 0, n);
                    }
                }
                extracted.add(name);
            }
        }
        return extracted;
    }

    private static String sha256File(File f) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new FileInputStream(f)) {
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) != -1) md.update(buf, 0, n);
        }
        byte[] dig = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : dig) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
