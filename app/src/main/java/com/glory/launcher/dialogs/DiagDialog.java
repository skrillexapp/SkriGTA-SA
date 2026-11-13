package com.glory.launcher.dialogs;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class DiagDialog extends Dialog {

    private static final String[] BISECT_SWITCHES = {
            "nohook_pipelines",
            "no_weapon_hooks",
            "no_radar_sync",
            "no_chat_touch",
            "no_chat",
            "no_money_sync",
            "no_weapon_hud",
            "no_perf_logger"
    };

    private static final String[] BISECT_DESCS = {
            "nohook_pipelines (Desactivar hooks gráficos)",
            "no_weapon_hooks (Desactivar hooks de armas)",
            "no_radar_sync (Desactivar sincronización de radar)",
            "no_chat_touch (Desactivar toque en chat)",
            "no_chat (Ocultar chat del juego)",
            "no_money_sync (Desactivar sincronización de dinero)",
            "no_weapon_hud (Desactivar HUD de armas JNI)",
            "no_perf_logger (Desactivar log de rendimiento)"
    };

    private TextView logTextView;

    public static String getGameDirectory() {
        File f1 = new File("/storage/emulated/0/GloryMobile/");
        if (f1.exists()) return f1.getAbsolutePath();
        File f2 = new File("/sdcard/GloryMobile/");
        if (f2.exists()) return f2.getAbsolutePath();
        return "/storage/emulated/0/GloryMobile/";
    }

    public DiagDialog(@NonNull Context context) {
        super(context);
    }

    public static void show(Context context) {
        DiagDialog dialog = new DiagDialog(context);
        dialog.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(0xEE12101A));
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        ScrollView scrollView = new ScrollView(getContext());
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);

        // Header Title
        TextView title = new TextView(getContext());
        title.setText("Panel de Diagnóstico");
        title.setTextColor(Color.parseColor("#DAA520"));
        title.setTextSize(18);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 24);
        root.addView(title);

        // Section: Symbols Info
        TextView symTitle = new TextView(getContext());
        symTitle.setText("Información de compilación:");
        symTitle.setTextColor(Color.WHITE);
        symTitle.setTextSize(14);
        symTitle.setPadding(0, 8, 0, 8);
        root.addView(symTitle);

        TextView symInfo = new TextView(getContext());
        String infoText = "Version Code: 113 (GLORYRP)\n" +
                "Architectures: arm64-v8a, armeabi-v7a\n" +
                "Symbols Directory: symbols/release/\n" +
                "Data Dir: " + getGameDirectory();
        symInfo.setText(infoText);
        symInfo.setTextColor(Color.parseColor("#CCCCCC"));
        symInfo.setTextSize(12);
        symInfo.setPadding(16, 12, 16, 12);
        symInfo.setBackgroundColor(0x33FFFFFF);
        root.addView(symInfo);

        Button copySymBtn = new Button(getContext());
        copySymBtn.setText("Copiar info de compilación");
        copySymBtn.setTextColor(Color.BLACK);
        copySymBtn.setBackgroundColor(Color.parseColor("#DAA520"));
        copySymBtn.setOnClickListener(v -> {
            copyToClipboard("GloryRP Build & Symbols", infoText);
            Toast.makeText(getContext(), "Información copiada", Toast.LENGTH_SHORT).show();
        });
        root.addView(copySymBtn);

        // Section: Bisect Switches
        TextView bisectTitle = new TextView(getContext());
        bisectTitle.setText("\nInterruptores de diagnóstico:");
        bisectTitle.setTextColor(Color.WHITE);
        bisectTitle.setTextSize(14);
        bisectTitle.setPadding(0, 16, 0, 8);
        root.addView(bisectTitle);

        File gameDir = new File(getGameDirectory());
        for (int i = 0; i < BISECT_SWITCHES.length; i++) {
            final String flagName = BISECT_SWITCHES[i];
            String flagDesc = BISECT_DESCS[i];
            final File flagFile = new File(gameDir, flagName);

            CheckBox cb = new CheckBox(getContext());
            cb.setText(flagDesc);
            cb.setTextColor(Color.parseColor("#EEEEEE"));
            cb.setTextSize(12);
            cb.setChecked(flagFile.exists());
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                try {
                    if (isChecked) {
                        flagFile.createNewFile();
                        Toast.makeText(getContext(), "Activado: " + flagName, Toast.LENGTH_SHORT).show();
                    } else {
                        if (flagFile.exists()) flagFile.delete();
                        Toast.makeText(getContext(), "Desactivado: " + flagName, Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(getContext(), "Error al guardar bandera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
            root.addView(cb);
        }

        // Section: Diag Log
        TextView logSectionTitle = new TextView(getContext());
        logSectionTitle.setText("\nContenido de diag_log.txt:");
        logSectionTitle.setTextColor(Color.WHITE);
        logSectionTitle.setTextSize(14);
        logSectionTitle.setPadding(0, 16, 0, 8);
        root.addView(logSectionTitle);

        logTextView = new TextView(getContext());
        logTextView.setTextColor(Color.parseColor("#00FF66"));
        logTextView.setTextSize(11);
        logTextView.setPadding(16, 16, 16, 16);
        logTextView.setBackgroundColor(0xFF0A0910);
        root.addView(logTextView);

        loadDiagLog();

        LinearLayout btnRow = new LinearLayout(getContext());
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, 16, 0, 16);

        Button copyLogBtn = new Button(getContext());
        copyLogBtn.setText("Copiar Log");
        copyLogBtn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        copyLogBtn.setOnClickListener(v -> {
            copyToClipboard("diag_log.txt", logTextView.getText().toString());
            Toast.makeText(getContext(), "Log copiado", Toast.LENGTH_SHORT).show();
        });
        btnRow.addView(copyLogBtn);

        Button refreshBtn = new Button(getContext());
        refreshBtn.setText("Actualizar");
        refreshBtn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        refreshBtn.setOnClickListener(v -> {
            loadDiagLog();
            Toast.makeText(getContext(), "Log actualizado", Toast.LENGTH_SHORT).show();
        });
        btnRow.addView(refreshBtn);

        Button clearBtn = new Button(getContext());
        clearBtn.setText("Limpiar Log");
        clearBtn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        clearBtn.setOnClickListener(v -> {
            File logFile = new File(getGameDirectory(), "diag_log.txt");
            if (logFile.exists()) {
                try (FileWriter fw = new FileWriter(logFile, false)) {
                    fw.write("");
                } catch (IOException ignored) {}
            }
            loadDiagLog();
            Toast.makeText(getContext(), "Log limpiado", Toast.LENGTH_SHORT).show();
        });
        btnRow.addView(clearBtn);

        root.addView(btnRow);

        Button closeBtn = new Button(getContext());
        closeBtn.setText("Cerrar panel");
        closeBtn.setBackgroundColor(Color.parseColor("#444444"));
        closeBtn.setTextColor(Color.WHITE);
        closeBtn.setOnClickListener(v -> dismiss());
        root.addView(closeBtn);

        scrollView.addView(root);
        setContentView(scrollView);
    }

    private void loadDiagLog() {
        File logFile = new File(getGameDirectory(), "diag_log.txt");
        if (!logFile.exists()) {
            logTextView.setText("diag_log.txt aún no existe.\nRuta: " + logFile.getAbsolutePath());
            return;
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            if (sb.length() == 0) {
                logTextView.setText("El log está vacío.");
            } else {
                logTextView.setText(sb.toString());
            }
        } catch (Exception e) {
            logTextView.setText("Error al leer log: " + e.getMessage());
        }
    }

    private void copyToClipboard(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText(label, text);
            clipboard.setPrimaryClip(clip);
        }
    }
}
