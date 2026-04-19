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
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class GhostKeyboard extends InputMethodService {

    private static final int MODE_ALPHA = 0;
    private static final int MODE_NUM_SYM = 1;
    private static final int MODE_EMOJI = 2;

    private int currentMode = MODE_ALPHA;
    private boolean shifted = false;
    private boolean capsLock = false;
    private long lastShiftTime = 0;

    private View rootView;
    private LinearLayout keyboardContainer;
    private SharedPreferences prefs;

    private Handler deleteHandler = new Handler(Looper.getMainLooper());
    private Runnable deleteRunnable;
    private boolean isDeleting = false;
    private boolean deleteDown = false;

    private Vibrator vibrator;
    private AudioManager audioManager;

    private int colorBg, colorKey, colorKeySpecial, colorKeyAccent, colorText, colorTextSpecial;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("ghost_kb_prefs", Context.MODE_PRIVATE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
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
    }

    @Override
    public View onCreateInputView() {
        loadTheme();
        rootView = LayoutInflater.from(this).inflate(R.layout.keyboard_root, null);
        keyboardContainer = rootView.findViewById(R.id.keyboard_container);
        buildAlphaKeyboard();
        return rootView;
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
        String[][] rows = {
            {"q","w","e","r","t","y","u","i","o","p"},
            {"a","s","d","f","g","h","j","k","l"},
            {"SHIFT","z","x","c","v","b","n","m","DEL"},
            {"SYM","SPACE","EMO","ENTER"}
        };
        for (String[] row : rows) {
            LinearLayout rowLayout = makeRow();
            for (String label : row) rowLayout.addView(makeKey(label, getKeyHeight()));
            keyboardContainer.addView(rowLayout);
        }
    }

    private void buildNumSymKeyboard() {
        currentMode = MODE_NUM_SYM;
        keyboardContainer.removeAllViews();
        keyboardContainer.setBackgroundColor(colorBg);
        String[][] rows = {
            {"1","2","3","4","5","6","7","8","9","0"},
            {"!","@","#","$","%","^","&","*","(",")"},
            {"-","_","=","+","[","]",";","'",",","."},
            {"ABC","SPACE","EMO","ENTER"}
        };
        for (String[] row : rows) {
            LinearLayout rowLayout = makeRow();
            for (String label : row) rowLayout.addView(makeKey(label, getKeyHeight()));
            keyboardContainer.addView(rowLayout);
        }
    }

    private void buildEmojiKeyboard() {
        currentMode = MODE_EMOJI;
        keyboardContainer.removeAllViews();
        keyboardContainer.setBackgroundColor(colorBg);
        LinearLayout topBar = makeRow();
        TextView backBtn = new TextView(this);
        backBtn.setText("⌨ ABC");
        backBtn.setTextColor(colorText);
        backBtn.setTextSize(14);
        backBtn.setBackground(makeRoundedBg(colorKeySpecial));
        backBtn.setPadding(dpToPx(16), 0, dpToPx(16), 0);
        backBtn.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(44));
        bp.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        backBtn.setLayoutParams(bp);
        backBtn.setOnClickListener(v -> buildAlphaKeyboard());
        topBar.addView(backBtn);
        keyboardContainer.addView(topBar);
        String[] emojis = {
            "😀","😂","🥹","😍","🤩","😎","🥳","😭","😱","🤔",
            "👍","👎","❤️","🔥","✨","🎉","💯","🙏","👏","💪",
            "😴","🤯","🥺","😈","👻","💀","🤖","👾","🎮","🏆",
            "🍕","🍔","🍟","☕","🧃","🍺","🎂","🍎","🍓","🍑",
            "🐶","🐱","🐸","🦊","🐼","🦁","🐻","🐨","🦋","🌸"
        };
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(10);
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gp.setMargins(dpToPx(4), 0, dpToPx(4), dpToPx(4));
        grid.setLayoutParams(gp);
        for (String emoji : emojis) {
            TextView eKey = new TextView(this);
            eKey.setText(emoji);
            eKey.setTextSize(22);
            eKey.setGravity(Gravity.CENTER);
            eKey.setBackground(makeRoundedBg(colorKey));
            GridLayout.LayoutParams ekp = new GridLayout.LayoutParams();
            ekp.width = 0;
            ekp.height = dpToPx(48);
            ekp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            ekp.setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
            eKey.setLayoutParams(ekp);
            eKey.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    doFeedback();
                    InputConnection ic = getCurrentInputConnection();
                    if (ic != null) ic.commitText(emoji, 1);
                }
                return true;
            });
            grid.addView(eKey);
        }
        keyboardContainer.addView(grid);
    }

    private TextView makeKey(String label, int height) {
        TextView key = new TextView(this);
        key.setText(getDisplay(label));
        key.setGravity(Gravity.CENTER);
        key.setTextSize(16);
        boolean isAccent = label.equals("ENTER");
        boolean isSpecial = label.equals("SHIFT") || label.equals("DEL") || label.equals("SYM")
                || label.equals("EMO") || label.equals("ABC") || label.equals("SPACE") || isAccent;
        int bg = isAccent ? colorKeyAccent : isSpecial ? colorKeySpecial : colorKey;
        int fg = isAccent ? colorTextSpecial : colorText;
        key.setTextColor(fg);
        key.setBackground(makeRoundedBg(bg));
        key.setLayoutParams(getKeyParams(label, height));
        key.setOnTouchListener((v, event) -> {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                doFeedback();
                if (label.equals("DEL")) {
                    deleteDown = true;
                    isDeleting = false;
                    startContinuousDelete();
                }
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                if (label.equals("DEL")) {
                    deleteDown = false;
                    stopContinuousDelete();
                    if (!isDeleting) {
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) ic.deleteSurroundingText(1, 0);
                    }
                    isDeleting = false;
                } else {
                    handleKey(label);
                }
            }
            return true;
        });
        return key;
    }

    private String getDisplay(String label) {
        switch (label) {
            case "SHIFT": return "⇧";
            case "DEL": return "⌫";
            case "ENTER": return "↩";
            case "SPACE": return "";
            case "SYM": return "?123";
            case "EMO": return "😊";
            case "ABC": return "ABC";
            default: return shifted || capsLock ? label.toUpperCase() : label.toLowerCase();
        }
    }

    private LinearLayout.LayoutParams getKeyParams(String label, int height) {
        LinearLayout.LayoutParams p;
        switch (label) {
            case "SPACE": p = new LinearLayout.LayoutParams(0, height, 4f); break;
            case "SHIFT": case "DEL": p = new LinearLayout.LayoutParams(0, height, 1.5f); break;
            case "SYM": case "ABC": case "ENTER": p = new LinearLayout.LayoutParams(0, height, 1.3f); break;
            default: p = new LinearLayout.LayoutParams(0, height, 1f); break;
        }
        p.setMargins(dpToPx(3), dpToPx(2), dpToPx(3), dpToPx(2));
        return p;
    }

    private LinearLayout makeRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(dpToPx(2), dpToPx(3), dpToPx(2), dpToPx(3));
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
            case "EMO":
                buildEmojiKeyboard();
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
        boolean vibOn = prefs.getBoolean("vibration", true);
        if (vibOn && vibrator != null && vibrator.hasVibrator()) {
            int strength = prefs.getInt("vibration_strength", 50);
            long ms = 20 + (long)(strength / 100f * 60);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int amp = 80 + (int)(strength / 100f * 175);
                vibrator.vibrate(VibrationEffect.createOneShot(ms, amp));
            } else {
                vibrator.vibrate(ms);
            }
        }
        boolean soundOn = prefs.getBoolean("sound", false);
        if (soundOn && audioManager != null) {
            int vol = prefs.getInt("sound_volume", 50);
            audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, vol / 100f);
        }
    }

    private void startContinuousDelete() {
        deleteRunnable = new Runnable() {
            @Override public void run() {
                if (!deleteDown) return;
                isDeleting = true;
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) ic.deleteSurroundingText(1, 0);
                deleteHandler.postDelayed(this, 50);
            }
        };
        deleteHandler.postDelayed(deleteRunnable, 400);
    }

    private void stopContinuousDelete() {
        if (deleteRunnable != null) deleteHandler.removeCallbacks(deleteRunnable);
    }

    private android.graphics.drawable.GradientDrawable makeRoundedBg(int color) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dpToPx(8));
        return d;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
