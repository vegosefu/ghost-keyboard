package com.ghost.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.inputmethodservice.InputMethodService;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GhostKeyboard extends InputMethodService {

    private static final int MODE_ALPHA = 0;
    private static final int MODE_NUM_SYM = 1;

    private int currentMode = MODE_ALPHA;
    private boolean shifted = false;
    private boolean capsLock = false;
    private long lastShiftTime = 0;

    private View rootView;
    private LinearLayout keyboardContainer;
    private SharedPreferences prefs;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Handler deleteHandler = new Handler(Looper.getMainLooper());
    // executor dedicat pentru vibratii - nu mai cream thread nou per tasta
    private final ExecutorService vibExecutor = Executors.newSingleThreadExecutor();

    private Runnable deleteRunnable;
    private boolean isDeleting = false;
    private boolean deleteDown = false;

    private Vibrator vibrator;
    private AudioManager audioManager;

    // cache pentru prefs ca sa nu citim din disk la fiecare tasta
    private boolean prefVibOn;
    private int prefVibStrength;
    private boolean prefSoundOn;
    private int prefSoundVol;

    private int colorBg, colorKey, colorKeySpecial, colorKeyAccent, colorText, colorTextSpecial;

    // culori pressed state
    private int colorKeyPressed, colorKeySpecialPressed;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("ghost_kb_prefs", Context.MODE_PRIVATE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        reloadPrefsCache();
    }

    private void reloadPrefsCache() {
        prefVibOn = prefs.getBoolean("vibration", true);
        prefVibStrength = prefs.getInt("vibration_strength", 50);
        prefSoundOn = prefs.getBoolean("sound", false);
        prefSoundVol = prefs.getInt("sound_volume", 50);
    }

    private void loadTheme() {
        String theme = prefs.getString("theme", "dark");
        switch (theme) {
            case "light":
                colorBg = Color.parseColor("#F1F3F4");
                colorKey = Color.WHITE;
                colorKeySpecial = Color.parseColor("#DADCE0");
                colorKeyAccent = Color.parseColor("#1A73E8");
                colorText = Color.parseColor("#202124");
                colorTextSpecial = Color.WHITE;
                break;
            case "amoled":
                colorBg = Color.BLACK;
                colorKey = Color.parseColor("#1A1A1A");
                colorKeySpecial = Color.parseColor("#111111");
                colorKeyAccent = Color.parseColor("#BB86FC");
                colorText = Color.WHITE;
                colorTextSpecial = Color.BLACK;
                break;
            case "material":
                colorBg = Color.parseColor("#1C1B1F");
                colorKey = Color.parseColor("#2B2930");
                colorKeySpecial = Color.parseColor("#3B383E");
                colorKeyAccent = Color.parseColor("#D0BCFF");
                colorText = Color.parseColor("#E6E1E5");
                colorTextSpecial = Color.parseColor("#1C1B1F");
                break;
            default:
                colorBg = Color.parseColor("#1A1A2E");
                colorKey = Color.parseColor("#16213E");
                colorKeySpecial = Color.parseColor("#0F3460");
                colorKeyAccent = Color.parseColor("#E94560");
                colorText = Color.parseColor("#EAEAEA");
                colorTextSpecial = Color.WHITE;
                break;
        }
        colorKeyPressed = lighten(colorKey, 0.25f);
        colorKeySpecialPressed = lighten(colorKeySpecial, 0.25f);
    }

    private int lighten(int color, float factor) {
        int r = Math.min(255, (int)(Color.red(color) + (255 - Color.red(color)) * factor));
        int g = Math.min(255, (int)(Color.green(color) + (255 - Color.green(color)) * factor));
        int b = Math.min(255, (int)(Color.blue(color) + (255 - Color.blue(color)) * factor));
        return Color.rgb(r, g, b);
    }

    @Override
    public View onCreateInputView() {
        loadTheme();
        reloadPrefsCache();
        rootView = LayoutInflater.from(this).inflate(R.layout.keyboard_root, null);
        keyboardContainer = rootView.findViewById(R.id.keyboard_container);
        buildAlphaKeyboard();
        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        vibExecutor.shutdown();
    }

    private int getKeyHeight() {
        int size = prefs.getInt("keyboard_height", 50);
        int dp = 42 + (int)(size / 100f * 26);
        return dpToPx(dp);
    }

    private void buildAlphaKeyboard() {
        currentMode = MODE_ALPHA;
        keyboardContainer.removeAllViews();
        keyboardContainer.setBackgroundColor(colorBg);
        int h = getKeyHeight();
        String[][] rows = {
            {"q","w","e","r","t","y","u","i","o","p"},
            {"a","s","d","f","g","h","j","k","l"},
            {"SHIFT","z","x","c","v","b","n","m","DEL"},
            {"SYM","SPACE","ENTER"}
        };
        for (String[] row : rows) {
            LinearLayout rowLayout = makeRow();
            for (String label : row) rowLayout.addView(makeKey(label, h));
            keyboardContainer.addView(rowLayout);
        }
    }

    private void buildNumSymKeyboard() {
        currentMode = MODE_NUM_SYM;
        keyboardContainer.removeAllViews();
        keyboardContainer.setBackgroundColor(colorBg);
        int h = getKeyHeight();
        String[][] rows = {
            {"1","2","3","4","5","6","7","8","9","0"},
            {"!","@","#","$","%","^","&","*","(",")"},
            {"-","_","=","+","[","]",";","'",",","."},
            {"ABC","SPACE","ENTER"}
        };
        for (String[] row : rows) {
            LinearLayout rowLayout = makeRow();
            for (String label : row) rowLayout.addView(makeKey(label, h));
            keyboardContainer.addView(rowLayout);
        }
    }

    private TextView makeKey(String label, int height) {
        TextView key = new TextView(this);
        key.setText(getDisplay(label));
        key.setGravity(Gravity.CENTER);
        key.setTextSize(16);

        boolean isAccent = label.equals("ENTER");
        boolean isSpecial = label.equals("SHIFT") || label.equals("DEL") || label.equals("SYM")
                || label.equals("ABC") || label.equals("SPACE") || isAccent;

        int bgNormal = isAccent ? colorKeyAccent : isSpecial ? colorKeySpecial : colorKey;
        int bgPressed = isAccent ? lighten(colorKeyAccent, 0.2f) : isSpecial ? colorKeySpecialPressed : colorKeyPressed;
        int fg = isAccent ? colorTextSpecial : colorText;

        key.setTextColor(fg);
        key.setBackground(makeRoundedBg(bgNormal));
        key.setLayoutParams(getKeyParams(label, height));

        // identifica daca e tasta care face rebuild (nu punem in coada, o tratam special)
        boolean isRebuildKey = label.equals("SHIFT") || label.equals("SYM") || label.equals("ABC");

        key.setOnTouchListener((v, event) -> {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                // feedback imediat pe ACTION_DOWN - fara nicio intarziere
                doFeedback();
                key.setBackground(makeRoundedBg(bgPressed));
                key.setScaleX(0.92f);
                key.setScaleY(0.92f);

                if (label.equals("DEL")) {
                    deleteDown = true;
                    isDeleting = false;
                    startContinuousDelete();
                }
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                key.setBackground(makeRoundedBg(bgNormal));
                key.setScaleX(1f);
                key.setScaleY(1f);

                if (label.equals("DEL")) {
                    deleteDown = false;
                    stopContinuousDelete();
                    if (!isDeleting) {
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) ic.deleteSurroundingText(1, 0);
                    }
                    isDeleting = false;
                } else if (isRebuildKey) {
                    // rebuilduim dupa ce touch event s-a terminat complet
                    mainHandler.post(() -> handleKey(label));
                } else {
                    // taste normale: direct, fara post, zero delay
                    handleKey(label);
                }
            }
            return true;
        });
        return key;
    }

    private String getDisplay(String label) {
        switch (label) {
            case "SHIFT": return capsLock ? "⇪" : shifted ? "⇧" : "⇧";
            case "DEL": return "⌫";
            case "ENTER": return "↩";
            case "SPACE": return "";
            case "SYM": return "?123";
            case "ABC": return "ABC";
            default: return shifted || capsLock ? label.toUpperCase() : label.toLowerCase();
        }
    }

    private LinearLayout.LayoutParams getKeyParams(String label, int height) {
        LinearLayout.LayoutParams p;
        switch (label) {
            case "SPACE": p = new LinearLayout.LayoutParams(0, height, 4f); break;
            case "SHIFT": case "DEL": p = new LinearLayout.LayoutParams(0, height, 1.5f); break;
            case "SYM": case "ABC": case "ENTER": p = new LinearLayout.LayoutParams(0, height, 1.5f); break;
            default: p = new LinearLayout.LayoutParams(0, height, 1f); break;
        }
        p.setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
        return p;
    }

    private LinearLayout makeRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
        row.setLayoutParams(p);
        return row;
    }

    private void handleKey(String label) {
        InputConnection ic = getCurrentInputConnection();
        switch (label) {
            case "SHIFT":
                long now = System.currentTimeMillis();
                if (now - lastShiftTime < 400) {
                    capsLock = !capsLock;
                    shifted = false;
                } else {
                    if (!capsLock) shifted = !shifted;
                    else { capsLock = false; shifted = false; }
                }
                lastShiftTime = now;
                buildAlphaKeyboard();
                break;
            case "DEL":
                if (ic != null) ic.deleteSurroundingText(1, 0);
                break;
            case "ENTER":
                if (ic != null) ic.commitText("\n", 1);
                break;
            case "SPACE":
                if (ic != null) ic.commitText(" ", 1);
                break;
            case "SYM":
                buildNumSymKeyboard();
                break;
            case "ABC":
                buildAlphaKeyboard();
                break;
            default:
                if (ic != null) {
                    String txt = shifted || capsLock ? label.toUpperCase() : label.toLowerCase();
                    ic.commitText(txt, 1);
                    if (shifted && !capsLock) {
                        shifted = false;
                        buildAlphaKeyboard();
                    }
                }
                break;
        }
    }

    private void doFeedback() {
        // folosim executor dedicat - nu mai creeam thread nou per tasta
        if (prefVibOn) {
            final int strength = prefVibStrength;
            vibExecutor.execute(() -> {
                try {
                    if (vibrator != null && vibrator.hasVibrator()) {
                        long ms = 18 + (long)(strength / 100f * 30);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            int amp = 60 + (int)(strength / 100f * 140);
                            vibrator.vibrate(VibrationEffect.createOneShot(ms, amp));
                        } else {
                            vibrator.vibrate(ms);
                        }
                    }
                } catch (Exception ignored) {}
            });
        }
        if (prefSoundOn && audioManager != null) {
            final float vol = prefSoundVol / 100f;
            try {
                audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, vol);
            } catch (Exception ignored) {}
        }
    }

    private void startContinuousDelete() {
        deleteRunnable = new Runnable() {
            @Override public void run() {
                if (!deleteDown) return;
                isDeleting = true;
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) ic.deleteSurroundingText(1, 0);
                deleteHandler.postDelayed(this, 45);
            }
        };
        deleteHandler.postDelayed(deleteRunnable, 350);
    }

    private void stopContinuousDelete() {
        if (deleteRunnable != null) deleteHandler.removeCallbacks(deleteRunnable);
    }

    private android.graphics.drawable.GradientDrawable makeRoundedBg(int color) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dpToPx(10));
        return d;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
