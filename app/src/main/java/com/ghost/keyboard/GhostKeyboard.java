package com.ghost.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.inputmethodservice.InputMethodService;
import android.media.AudioManager;
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
import android.widget.PopupWindow;
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
    private PopupWindow keyPopup;
    private TextView keyPopupText;

    private Handler deleteHandler = new Handler(Looper.getMainLooper());
    private Runnable deleteRunnable;
    private boolean isDeleting = false;

    private Vibrator vibrator;
    private AudioManager audioManager;

    private int colorBg, colorKey, colorKeySpecial, colorKeyAccent, colorText, colorTextSpecial;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("ghost_kb_prefs", Context.MODE_PRIVATE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        loadTheme();
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
        setupKeyPopup();
        buildAlphaKeyboard();
        return rootView;
    }

    private void setupKeyPopup() {
        View popupView = LayoutInflater.from(this).inflate(R.layout.key_popup, null);
        keyPopupText = popupView.findViewById(R.id.popup_text);
        keyPopup = new PopupWindow(popupView, dpToPx(52), dpToPx(52), false);
        keyPopup.setClippingEnabled(false);
    }

    private int getKeyHeight() {
        int size = prefs.getInt("keyboard_height", 2);
        switch (size) {
            case 1: return dpToPx(46);
            case 3: return dpToPx(62);
            default: return dpToPx(54);
        }
    }

    private void buildAlphaKeyboard() {
        keyboardContainer.removeAllViews();
        keyboardContainer.setBackgroundColor(colorBg);
        String[][] rows = {
            {"q","w","e","r","t","y","u","i","o","p"},
            {"a","s","d","f","g","h","j","k","l"},
            {"SHIFT","z","x","c","v","b","n","m","DEL"},
            {"SYM","SPACE","EMO","ENTER"}
        };
        for (String[] row : rows) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, getKeyHeight());
            rowParams.setMargins(dpToPx(2), dpToPx(3), dpToPx(2), dpToPx(3));
            rowLayout.setLayoutParams(rowParams);
            for (String label : row) rowLayout.addView(makeAlphaKey(label));
            keyboardContainer.addView(rowLayout);
        }
    }

    private TextView makeAlphaKey(String label) {
        TextView key = new TextView(this);
        boolean isSpecial = label.equals("SHIFT") || label.equals("DEL") || label.equals("SYM")
                || label.equals("EMO") || label.equals("ENTER") || label.equals("SPACE");
        String display;
        if (!isSpecial) {
            display = shifted || capsLock ? label.toUpperCase() : label.toLowerCase();
        } else {
            switch (label) {
                case "SHIFT": display = capsLock ? "⇪" : "⇧"; break;
                case "DEL": display = "⌫"; break;
                case "ENTER": display = "↩"; break;
                case "SPACE": display = ""; break;
                case "SYM": display = "?123"; break;
                case "EMO": display = "😊"; break;
                default: display = label;
            }
        }
        key.setText(display);
        key.setTag(label);
        key.setGravity(Gravity.CENTER);
        key.setTextSize(label.equals("SHIFT") || label.equals("DEL") ? 18 : 16);

        int bgColor = label.equals("ENTER") ? colorKeyAccent
                : (label.equals("SHIFT") && (shifted || capsLock)) ? colorKeyAccent
                : isSpecial ? colorKeySpecial : colorKey;
        int txtColor = (label.equals("ENTER") || (label.equals("SHIFT") && (shifted || capsLock)))
                ? colorTextSpecial : colorText;

        key.setTextColor(txtColor);
        key.setBackground(makeRoundedDrawable(bgColor));

        LinearLayout.LayoutParams p;
        switch (label) {
            case "SPACE": p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 4f); break;
            case "SHIFT": case "DEL": p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.5f); break;
            case "SYM": case "ENTER": p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.3f); break;
            default: p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f); break;
        }
        p.setMargins(dpToPx(3), dpToPx(2), dpToPx(3), dpToPx(2));
        key.setLayoutParams(p);

        final int finalBgColor = bgColor;
        final String finalDisplay = display;

        key.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    doFeedback();
                    key.setBackground(makeRoundedDrawable(lighten(finalBgColor, 40)));
                    if (prefs.getBoolean("key_popup", true) && !isSpecial)
                        showKeyPopup(key, finalDisplay);
                    if (label.equals("DEL")) startContinuousDelete();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    key.setBackground(makeRoundedDrawable(finalBgColor));
                    dismissKeyPopup();
                    if (label.equals("DEL")) {
                        stopContinuousDelete();
                        if (!isDeleting) handleAlphaKey(label);
                        isDeleting = false;
                    } else {
                        handleAlphaKey(label);
                    }
                    break;
            }
            return true;
        });
        return key;
    }

    private void doFeedback() {
        boolean vibOn = prefs.getBoolean("vibration", true);
        if (vibOn && vibrator != null && vibrator.hasVibrator()) {
            int strength = prefs.getInt("vibration_strength", 2);
            long ms = strength == 1 ? 15 : strength == 3 ? 45 : 28;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                int amp = strength == 1 ? 60 : strength == 3 ? 200 : 120;
                vibrator.vibrate(VibrationEffect.createOneShot(ms, amp));
            } else {
                vibrator.vibrate(ms);
            }
        }
        boolean soundOn = prefs.getBoolean("sound", false);
        if (soundOn && audioManager != null) {
            int vol = prefs.getInt("sound_volume", 2);
            float fvol = vol == 1 ? 0.1f : vol == 3 ? 1.0f : 0.5f;
            audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, fvol);
        }
    }

    private void handleAlphaKey(String label) {
        InputConnection ic = getCurrentInputConnection();
        switch (label) {
            case "SHIFT":
                long now = System.currentTimeMillis();
                if (now - lastShiftTime < 400) { capsLock = !capsLock; shifted = false; }
                else { if (!capsLock) shifted = !shifted; else capsLock = false; }
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
                currentMode = MODE_NUM_SYM;
                buildNumSymKeyboard();
                break;
            case "EMO":
                currentMode = MODE_EMOJI;
                buildEmojiKeyboard();
                break;
            default:
                if (ic != null) {
                    String txt = shifted || capsLock ? label.toUpperCase() : label.toLowerCase();
                    ic.commitText(txt, 1);
                    if (shifted && !capsLock) { shifted = false; buildAlphaKeyboard(); }
                }
                break;
        }
    }

    private void buildNumSymKeyboard() {
        keyboardContainer.removeAllViews();
        keyboardContainer.setBackgroundColor(colorBg);
        String[][] rows = {
            {"1","2","3","4","5","6","7","8","9","0"},
            {"!","@","#","$","%","^","&","*","(",")"},
            {"-","_","=","+","[","]",";","'",",","."},
            {"ABC","SPACE2","EMO2","ENTER2"}
        };
        for (String[] row : rows) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, getKeyHeight());
            rp.setMargins(dpToPx(2), dpToPx(3), dpToPx(2), dpToPx(3));
            rowLayout.setLayoutParams(rp);
            for (String label : row) rowLayout.addView(makeSymKey(label));
            keyboardContainer.addView(rowLayout);
        }
    }

    private TextView makeSymKey(String label) {
        TextView key = new TextView(this);
        boolean isSpecial = label.equals("ABC") || label.equals("SPACE2") || label.equals("EMO2") || label.equals("ENTER2");
        String display;
        switch (label) {
            case "ABC": display = "ABC"; break;
            case "SPACE2": display = ""; break;
            case "EMO2": display = "😊"; break;
            case "ENTER2": display = "↩"; break;
            default: display = label;
        }
        key.setText(display);
        key.setGravity(Gravity.CENTER);
        key.setTextSize(16);
        int bgColor = label.equals("ENTER2") ? colorKeyAccent : isSpecial ? colorKeySpecial : colorKey;
        int txtColor = label.equals("ENTER2") ? colorTextSpecial : colorText;
        key.setTextColor(txtColor);
        key.setBackground(makeRoundedDrawable(bgColor));
        LinearLayout.LayoutParams p;
        switch (label) {
            case "SPACE2": p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 4f); break;
            case "ABC": case "ENTER2": p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.5f); break;
            default: p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f); break;
        }
        p.setMargins(dpToPx(3), dpToPx(2), dpToPx(3), dpToPx(2));
        key.setLayoutParams(p);
        final int finalBgColor = bgColor;
        final String finalDisplay = display;
        key.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    doFeedback();
                    key.setBackground(makeRoundedDrawable(lighten(finalBgColor, 40)));
                    if (prefs.getBoolean("key_popup", true) && !isSpecial) showKeyPopup(key, finalDisplay);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    key.setBackground(makeRoundedDrawable(finalBgColor));
                    dismissKeyPopup();
                    InputConnection ic = getCurrentInputConnection();
                    switch (label) {
                        case "ABC": currentMode = MODE_ALPHA; buildAlphaKeyboard(); break;
                        case "EMO2": currentMode = MODE_EMOJI; buildEmojiKeyboard(); break;
                        case "ENTER2": if (ic != null) ic.commitText("\n", 1); break;
                        case "SPACE2": if (ic != null) ic.commitText(" ", 1); break;
                        default: if (ic != null) ic.commitText(label, 1); break;
                    }
                    break;
            }
            return true;
        });
        return key;
    }

    private void buildEmojiKeyboard() {
        keyboardContainer.removeAllViews();
        keyboardContainer.setBackgroundColor(colorBg);
        String[] emojis = {
            "😀","😂","🥹","😍","🤩","😎","🥳","😭","😱","🤔",
            "👍","👎","❤️","🔥","✨","🎉","💯","🙏","👏","💪",
            "😴","🤯","🥺","😈","👻","💀","🤖","👾","🎮","🏆",
            "🍕","🍔","🍟","☕","🧃","🍺","🎂","🍎","🍓","🍑",
            "🐶","🐱","🐸","🦊","🐼","🦁","🐻","🐨","🦋","🌸"
        };
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
        LinearLayout.LayoutParams topParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(44));
        topBar.setLayoutParams(topParams);
        TextView backBtn = new TextView(this);
        backBtn.setText("⌨ ABC");
        backBtn.setTextColor(colorText);
        backBtn.setTextSize(14);
        backBtn.setBackground(makeRoundedDrawable(colorKeySpecial));
        backBtn.setPadding(dpToPx(16), 0, dpToPx(16), 0);
        backBtn.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        backParams.setMargins(0, dpToPx(4), 0, dpToPx(4));
        backBtn.setLayoutParams(backParams);
        backBtn.setOnClickListener(vv -> { currentMode = MODE_ALPHA; buildAlphaKeyboard(); });
        topBar.addView(backBtn);
        keyboardContainer.addView(topBar);
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(10);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gridParams.setMargins(dpToPx(4), 0, dpToPx(4), dpToPx(4));
        grid.setLayoutParams(gridParams);
        for (String emoji : emojis) {
            TextView eKey = new TextView(this);
            eKey.setText(emoji);
            eKey.setTextSize(22);
            eKey.setGravity(Gravity.CENTER);
            eKey.setBackground(makeRoundedDrawable(colorKey));
            GridLayout.LayoutParams gp = new GridLayout.LayoutParams();
            gp.width = 0;
            gp.height = dpToPx(48);
            gp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            gp.setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
            eKey.setLayoutParams(gp);
            eKey.setOnTouchListener((vv, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    doFeedback();
                    eKey.setBackground(makeRoundedDrawable(lighten(colorKey, 40)));
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    eKey.setBackground(makeRoundedDrawable(colorKey));
                    InputConnection ic = getCurrentInputConnection();
                    if (ic != null) ic.commitText(emoji, 1);
                }
                return true;
            });
            grid.addView(eKey);
        }
        keyboardContainer.addView(grid);
    }

    private void showKeyPopup(View anchor, String text) {
        if (!prefs.getBoolean("key_popup", true)) return;
        keyPopupText.setText(text.toUpperCase());
        keyPopupText.setTextColor(Color.WHITE);
        keyPopup.getContentView().setBackground(makeRoundedDrawable(colorKeyAccent));
        int[] loc = new int[2];
        anchor.getLocationInWindow(loc);
        int x = loc[0] + anchor.getWidth() / 2 - dpToPx(26);
        int y = loc[1] - dpToPx(56);
        if (keyPopup.isShowing()) keyPopup.update(x, y, dpToPx(52), dpToPx(52));
        else keyPopup.showAtLocation(rootView, Gravity.NO_GRAVITY, x, y);
    }

    private void dismissKeyPopup() {
        if (keyPopup != null && keyPopup.isShowing()) keyPopup.dismiss();
    }

    private void startContinuousDelete() {
        isDeleting = false;
        deleteRunnable = new Runnable() {
            @Override public void run() {
                isDeleting = true;
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) ic.deleteSurroundingText(1, 0);
                deleteHandler.postDelayed(this, 60);
            }
        };
        deleteHandler.postDelayed(deleteRunnable, 500);
    }

    private void stopContinuousDelete() {
        deleteHandler.removeCallbacks(deleteRunnable);
    }

    private android.graphics.drawable.GradientDrawable makeRoundedDrawable(int color) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dpToPx(8));
        return d;
    }

    private int lighten(int color, int amount) {
        return Color.rgb(
            Math.min(255, Color.red(color) + amount),
            Math.min(255, Color.green(color) + amount),
            Math.min(255, Color.blue(color) + amount));
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
