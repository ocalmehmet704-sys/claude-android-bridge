package com.claude.bridge;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int PERM_REQ = 101;

    private TextView statusText;
    private View statusDot;
    private Button mainButton;
    private LinearLayout permissionsContainer;

    static class PermEntry {
        String label, desc, perm;
        int type; // 0=dangerous 1=normal(auto) 2=special
        PermEntry(String l, String d, String p, int t) {
            label = l; desc = d; perm = p; type = t;
        }
    }

    private final List<PermEntry> permList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildPermList();
        setContentView(buildUI());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        updateUI();
    }

    // ── UI ──────────────────────────────────────────────────────────────────

    private ScrollView buildUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#0D1117"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16);
        root.setPadding(p, p, p, p);

        root.addView(buildHeader());
        root.addView(buildStatusCard(), matchLP(0, dp(12)));
        mainButton = buildButton();
        LinearLayout.LayoutParams btnLp = matchLP(dp(52), dp(24));
        root.addView(mainButton, btnLp);
        root.addView(buildSectionLabel("İZİNLER"), matchLP(0, dp(8)));

        permissionsContainer = new LinearLayout(this);
        permissionsContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(permissionsContainer);

        scroll.addView(root);
        return scroll;
    }

    private LinearLayout buildHeader() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = matchLP(0, dp(16));
        h.setLayoutParams(lp);

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_app);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));

        TextView title = new TextView(this);
        title.setText("Claude Bridge");
        title.setTextColor(0xFFF0F6FC);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tlp.setMarginStart(dp(12));
        title.setLayoutParams(tlp);

        h.addView(icon);
        h.addView(title);
        return h;
    }

    private LinearLayout buildStatusCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(roundRect(0xFF161B22, dp(10)));
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout dotRow = new LinearLayout(this);
        dotRow.setOrientation(LinearLayout.HORIZONTAL);
        dotRow.setGravity(Gravity.CENTER_VERTICAL);

        statusDot = new View(this);
        statusDot.setBackground(oval(Color.GRAY));
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(10), dp(10));
        dotLp.setMarginEnd(dp(8));
        statusDot.setLayoutParams(dotLp);

        TextView dotLabel = smallGray("Servis Durumu");
        dotRow.addView(statusDot);
        dotRow.addView(dotLabel);

        statusText = new TextView(this);
        statusText.setTextColor(0xFFF0F6FC);
        statusText.setTextSize(15);
        statusText.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        stLp.topMargin = dp(6);
        statusText.setLayoutParams(stLp);

        card.addView(dotRow);
        card.addView(statusText);
        return card;
    }

    private Button buildButton() {
        Button btn = new Button(this);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(15);
        btn.setAllCaps(false);
        btn.setOnClickListener(v -> handleMainButton());
        return btn;
    }

    private TextView buildSectionLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFF8B949E);
        tv.setTextSize(11);
        tv.setLetterSpacing(0.1f);
        return tv;
    }

    // ── UPDATE ───────────────────────────────────────────────────────────────

    private void updateUI() {
        boolean acc = isAccessibilityEnabled();
        boolean stor = isStorageGranted();

        // Status dot
        statusDot.setBackground(oval(acc ? 0xFF3FB950 : 0xFFF85149));
        statusText.setText(acc
                ? (stor ? "Aktif — Bildirimler izleniyor" : "Erişilebilirlik aktif, depolama izni eksik")
                : "Erişilebilirlik kapalı — Ayarlardan etkinleştirin");

        // Main button
        String btnText;
        int btnColor;
        if (!acc) {
            btnText = "Erişilebilirliği Aç";
            btnColor = 0xFF238636;
        } else if (!stor) {
            btnText = "Depolama İznini Ver";
            btnColor = 0xFF9E6A03;
        } else {
            btnText = "Erişilebilirlik Ayarları";
            btnColor = 0xFF21262D;
        }
        mainButton.setText(btnText);
        mainButton.setBackground(roundRect(btnColor, dp(8)));

        // Permission rows
        permissionsContainer.removeAllViews();
        for (PermEntry e : permList) addPermRow(e);
    }

    private void addPermRow(PermEntry e) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(roundRect(0xFF161B22, dp(8)));
        row.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams lp = matchLP(0, dp(4));
        row.setLayoutParams(lp);

        // Type dot
        int dotColor;
        if (e.type == 0) dotColor = 0xFFF85149;
        else if (e.type == 1) dotColor = 0xFF58A6FF;
        else dotColor = 0xFFE3B341;
        View dot = new View(this);
        dot.setBackground(oval(dotColor));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(8), dp(8));
        dlp.setMarginEnd(dp(10));
        dot.setLayoutParams(dlp);

        // Labels
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView name = new TextView(this);
        name.setText(e.label);
        name.setTextColor(0xFFF0F6FC);
        name.setTextSize(13);
        name.setTypeface(Typeface.DEFAULT_BOLD);

        TextView desc = smallGray(e.desc);
        labels.addView(name);
        labels.addView(desc);

        // Status chip
        String status = getPermStatus(e);
        TextView chip = new TextView(this);
        chip.setText(status);
        chip.setTextSize(10);
        chip.setTextColor(Color.WHITE);
        chip.setPadding(dp(6), dp(2), dp(6), dp(2));
        int chipColor;
        switch (status) {
            case "VERİLDİ":   chipColor = 0xFF1f4d2a; break;
            case "OTOMATİK":  chipColor = 0xFF1a3a5c; break;
            default:           chipColor = 0xFF4d1414; break;
        }
        chip.setBackground(roundRect(chipColor, dp(4)));

        row.setOnClickListener(v -> handlePermClick(e));
        row.addView(dot);
        row.addView(labels);
        row.addView(chip);
        permissionsContainer.addView(row);
    }

    // ── PERMISSIONS ──────────────────────────────────────────────────────────

    private void buildPermList() {
        // Special (via system settings)
        permList.add(new PermEntry("Erişilebilirlik",      "UI olayları ve bildirimler",        null, 2));
        permList.add(new PermEntry("Tüm Dosyalar",          "Tüm depolama okuma/yazma",          "android.permission.MANAGE_EXTERNAL_STORAGE", 2));
        permList.add(new PermEntry("Sistem Üstü Pencere",  "Diğer uygulamaların üzerinde göster","android.permission.SYSTEM_ALERT_WINDOW", 2));

        // Dangerous
        permList.add(new PermEntry("Bildirim İzni",        "Bildirim gönder",                   "android.permission.POST_NOTIFICATIONS", 0));
        permList.add(new PermEntry("SMS Oku",              "SMS mesajlarını oku",               Manifest.permission.READ_SMS, 0));
        permList.add(new PermEntry("SMS Al",               "Gelen SMS dinle",                   Manifest.permission.RECEIVE_SMS, 0));
        permList.add(new PermEntry("Depolama (Oku)",       "Dosya okuma",                       Manifest.permission.READ_EXTERNAL_STORAGE, 0));
        permList.add(new PermEntry("Depolama (Yaz)",       "Dosya yazma",                       Manifest.permission.WRITE_EXTERNAL_STORAGE, 0));
        permList.add(new PermEntry("Rehber",               "Kişi listesini oku",                Manifest.permission.READ_CONTACTS, 0));
        permList.add(new PermEntry("Arama Geçmişi",        "Çağrı günlüğünü oku",              Manifest.permission.READ_CALL_LOG, 0));
        permList.add(new PermEntry("Telefon Durumu",       "Telefon bilgilerini oku",           Manifest.permission.READ_PHONE_STATE, 0));
        permList.add(new PermEntry("Giden Aramalar",       "Çıkan aramaları yakala",            Manifest.permission.PROCESS_OUTGOING_CALLS, 0));
        permList.add(new PermEntry("Mikrofon",             "Ses kayıt",                         Manifest.permission.RECORD_AUDIO, 0));
        permList.add(new PermEntry("Kamera",               "Fotoğraf/video",                    Manifest.permission.CAMERA, 0));
        permList.add(new PermEntry("Hassas Konum",         "GPS konumu",                        Manifest.permission.ACCESS_FINE_LOCATION, 0));
        permList.add(new PermEntry("Yaklaşık Konum",       "Ağ konumu",                         Manifest.permission.ACCESS_COARSE_LOCATION, 0));
        if (Build.VERSION.SDK_INT >= 31) {
            permList.add(new PermEntry("Bluetooth",        "Yakın cihazları tara",              "android.permission.BLUETOOTH_CONNECT", 0));
            permList.add(new PermEntry("Bluetooth Tara",   "BLE tarama",                        "android.permission.BLUETOOTH_SCAN", 0));
        }

        // Normal (auto-granted)
        permList.add(new PermEntry("İnternet",             "Ağ erişimi",                        Manifest.permission.INTERNET, 1));
        permList.add(new PermEntry("Önyükleme",            "Cihaz açılışında başla",            Manifest.permission.RECEIVE_BOOT_COMPLETED, 1));
        permList.add(new PermEntry("Ön Plan Servis",       "Arka plan servisi",                 Manifest.permission.FOREGROUND_SERVICE, 1));
        permList.add(new PermEntry("Titreşim",             "Titreşim kontrolü",                 Manifest.permission.VIBRATE, 1));
        permList.add(new PermEntry("Uyanık Tut",           "CPU uykuya alma",                   Manifest.permission.WAKE_LOCK, 1));
        permList.add(new PermEntry("NFC",                  "NFC iletişimi",                     Manifest.permission.NFC, 1));
        permList.add(new PermEntry("APK Yükle",            "Uygulama yükleme izni",             "android.permission.REQUEST_INSTALL_PACKAGES", 1));
    }

    private String getPermStatus(PermEntry e) {
        if (e.type == 1) return "OTOMATİK";

        if (e.perm == null) return isAccessibilityEnabled() ? "VERİLDİ" : "GEREKLİ";

        if (e.perm.equals("android.permission.MANAGE_EXTERNAL_STORAGE")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                return Environment.isExternalStorageManager() ? "VERİLDİ" : "GEREKLİ";
            return "OTOMATİK";
        }
        if (e.perm.equals("android.permission.SYSTEM_ALERT_WINDOW"))
            return Settings.canDrawOverlays(this) ? "VERİLDİ" : "GEREKLİ";

        return checkSelfPermission(e.perm) == PackageManager.PERMISSION_GRANTED ? "VERİLDİ" : "GEREKLİ";
    }

    private void handlePermClick(PermEntry e) {
        if (e.type == 2) {
            if (e.perm == null) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            } else if (e.perm.equals("android.permission.MANAGE_EXTERNAL_STORAGE")) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivity(i);
                }
            } else if (e.perm.equals("android.permission.SYSTEM_ALERT_WINDOW")) {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            }
        } else if (e.type == 0 && e.perm != null) {
            requestPermissions(new String[]{e.perm}, PERM_REQ);
        }
    }

    private void handleMainButton() {
        if (!isAccessibilityEnabled()) {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } else if (!isStorageGranted()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            } else {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERM_REQ);
            }
        } else {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private boolean isAccessibilityEnabled() {
        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        for (AccessibilityServiceInfo i : am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK))
            if (i.getId().contains(getPackageName())) return true;
        return false;
    }

    private boolean isStorageGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            return Environment.isExternalStorageManager();
        return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private GradientDrawable roundRect(int color, float radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private GradientDrawable oval(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        return d;
    }

    private TextView smallGray(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFF8B949E);
        tv.setTextSize(11);
        return tv;
    }

    private LinearLayout.LayoutParams matchLP(int height, int bottomMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                height == 0 ? LinearLayout.LayoutParams.WRAP_CONTENT : height);
        lp.bottomMargin = bottomMargin;
        return lp;
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
