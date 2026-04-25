package com.ghost.keyboard;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsActivity extends Activity {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("ghost_kb_prefs", MODE_PRIVATE);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#1C1B1F"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(32), dp(20), dp(48));
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("👻 Ghost Keyboard");
        title.setTextSize(28);
        title.setTextColor(Color.parseColor("#D0BCFF"));
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Settings");
        subtitle.setTextSize(14);
        subtitle.setTextColor(Color.parseColor("#9E9E9E"));
        subtitle.setPadding(0, dp(2), 0, dp(28));
        root.addView(subtitle);

        // SETUP
        root.addView(sectionLabel("⚙️  SETUP"));
        LinearLayout setupCard = makeCard();
        TextView enableHint = new TextView(this);
        enableHint.setText("Tap below to enable Ghost Keyboard in Android settings, then select it as your input method.");
        enableHint.setTextColor(Color.parseColor("#9E9E9E"));
        enableHint.setTextSize(13);
        enableHint.setPadding(0, 0, 0, dp(12));
        setupCard.addView(enableHint);
        setupCard.addView(makeAccentButton("Open Input Method Settings", v ->
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))));
        root.addView(setupCard);
        root.addView(spacer(20));

        // THEME
        root.addView(sectionLabel("🎨  THEME"));
        LinearLayout themeCard = makeCard();
        String[] themeIds = {"dark", "light", "amoled", "material"};
        String[] themeNames = {"🌙  Dark Blue", "☀️  Light", "⬛  AMOLED Black", "💜  Material You"};
        String current = prefs.getString("theme", "dark");
        for (int i = 0; i < themeIds.length; i++) {
            final String tid = themeIds[i];
            boolean selected = tid.equals(current);
            LinearLayout row = makeRow();
            TextView lbl = new TextView(this);
            lbl.setText(themeNames[i]);
            lbl.setTextSize(16);
            lbl.setTextColor(selected ? Color.parseColor("#D0BCFF") : Color.parseColor("#E6E1E5"));
            lbl.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView check = new TextView(this);
            check.setText(selected ? "✓" : "");
            check.setTextColor(Color.parseColor("#D0BCFF"));
            check.setTextSize(18);
            row.addView(lbl);
            row.addView(check);
            row.setOnClickListener(v -> { prefs.edit().putString("theme", tid).apply(); recreate(); });
            themeCard.addView(row);
            if (i < themeIds.length - 1) themeCard.addView(divider());
        }
        root.addView(themeCard);
        root.addView(spacer(20));

        // KEY POPUP
        root.addView(sectionLabel("🔤  KEY POPUP"));
        LinearLayout popupCard = makeCard();
        popupCard.addView(makeSwitchRow("Show key preview on press",
                prefs.getBoolean("key_popup", true),
                (btn, checked) -> prefs.edit().putBoolean("key_popup", checked).apply()));
        root.addView(popupCard);
        root.addView(spacer(20));

        // HAPTIC
        root.addView(sectionLabel("📳  HAPTIC FEEDBACK"));
        LinearLayout hapticCard = makeCard();
        hapticCard.addView(makeSwitchRow("Haptic on keypress",
                prefs.getBoolean("haptic", true),
                (btn, checked) -> prefs.edit().putBoolean("haptic", checked).apply()));
        hapticCard.addView(divider());
        // intensity selector: 1=Light 2=Medium 3=Strong
        TextView hapticIntLabel = new TextView(this);
        hapticIntLabel.setText("Intensity");
        hapticIntLabel.setTextSize(15);
        hapticIntLabel.setTextColor(Color.parseColor("#E6E1E5"));
        hapticIntLabel.setPadding(0, dp(10), 0, dp(8));
        hapticCard.addView(hapticIntLabel);
        String[] hapticLevels = {"Light", "Medium", "Strong"};
        int[] hapticVals = {1, 2, 3};
        int currentHaptic = prefs.getInt("haptic_intensity", 2);
        LinearLayout hapticRow = new LinearLayout(this);
        hapticRow.setOrientation(LinearLayout.HORIZONTAL);
        hapticRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        for (int i = 0; i < hapticLevels.length; i++) {
            final int hval = hapticVals[i];
            boolean sel = (currentHaptic == hval);
            TextView btn = new TextView(this);
            btn.setText(hapticLevels[i]);
            btn.setGravity(Gravity.CENTER);
            btn.setTextSize(14);
            btn.setTextColor(sel ? Color.parseColor("#1C1B1F") : Color.parseColor("#E6E1E5"));
            android.graphics.drawable.GradientDrawable bd = new android.graphics.drawable.GradientDrawable();
            bd.setColor(sel ? Color.parseColor("#D0BCFF") : Color.parseColor("#3B383E"));
            bd.setCornerRadius(dp(10));
            btn.setBackground(bd);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, dp(44), 1f);
            bp.setMargins(dp(4), 0, dp(4), dp(8));
            btn.setLayoutParams(bp);
            btn.setOnClickListener(v -> { prefs.edit().putInt("haptic_intensity", hval).apply(); recreate(); });
            hapticRow.addView(btn);
        }
        hapticCard.addView(hapticRow);
        root.addView(hapticCard);
        root.addView(spacer(20));

        // SOUND
        root.addView(sectionLabel("🔊  KEY SOUND"));
        LinearLayout soundCard = makeCard();
        soundCard.addView(makeSwitchRow("Play sound on keypress",
                prefs.getBoolean("sound", false),
                (btn, checked) -> prefs.edit().putBoolean("sound", checked).apply()));
        soundCard.addView(divider());
        soundCard.addView(makeSeekRow("Volume", "Quiet", "Loud",
                prefs.getInt("sound_volume", 2), 1, 3,
                val -> prefs.edit().putInt("sound_volume", val).apply()));
        root.addView(soundCard);
        root.addView(spacer(20));

        // KEYBOARD SIZE
        root.addView(sectionLabel("📐  KEYBOARD SIZE"));
        LinearLayout sizeCard = makeCard();
        String[] sizeNames = {"Small", "Normal", "Large"};
        int currentSize = prefs.getInt("keyboard_height", 2);
        LinearLayout sizeRow = new LinearLayout(this);
        sizeRow.setOrientation(LinearLayout.HORIZONTAL);
        sizeRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        for (int i = 0; i < sizeNames.length; i++) {
            final int sizeVal = i + 1;
            boolean sel = (currentSize == sizeVal);
            TextView btn = new TextView(this);
            btn.setText(sizeNames[i]);
            btn.setGravity(Gravity.CENTER);
            btn.setTextSize(14);
            btn.setTextColor(sel ? Color.parseColor("#1C1B1F") : Color.parseColor("#E6E1E5"));
            android.graphics.drawable.GradientDrawable bd = new android.graphics.drawable.GradientDrawable();
            bd.setColor(sel ? Color.parseColor("#D0BCFF") : Color.parseColor("#3B383E"));
            bd.setCornerRadius(dp(10));
            btn.setBackground(bd);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, dp(44), 1f);
            bp.setMargins(dp(4), 0, dp(4), 0);
            btn.setLayoutParams(bp);
            btn.setOnClickListener(v -> { prefs.edit().putInt("keyboard_height", sizeVal).apply(); recreate(); });
            sizeRow.addView(btn);
        }
        sizeCard.addView(sizeRow);
        root.addView(sizeCard);
        root.addView(spacer(20));

        // ABOUT
        root.addView(sectionLabel("ℹ️  ABOUT"));
        LinearLayout aboutCard = makeCard();
        TextView about = new TextView(this);
        about.setText("Ghost Keyboard v2.1\nMaterial You design • Haptic feedback\nThemes • Symbols • Long press alts\nURL mode • Resizable keyboard");
        about.setTextColor(Color.parseColor("#9E9E9E"));
        about.setTextSize(13);
        about.setLineSpacing(dp(4), 1f);
        aboutCard.addView(about);
        root.addView(aboutCard);

        setContentView(scroll);
    }

    private TextView sectionLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(11);
        tv.setTextColor(Color.parseColor("#D0BCFF"));
        tv.setTypeface(null, Typeface.BOLD);
        tv.setLetterSpacing(0.08f);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(dp(4), 0, 0, dp(8));
        tv.setLayoutParams(p);
        return tv;
    }

    private LinearLayout makeCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(12), dp(16), dp(12));
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(Color.parseColor("#2B2930"));
        d.setCornerRadius(dp(16));
        card.setBackground(d);
        card.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return card;
    }

    private LinearLayout makeRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private View makeSwitchRow(String label, boolean checked, CompoundButton.OnCheckedChangeListener listener) {
        LinearLayout row = makeRow();
        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextSize(16);
        lbl.setTextColor(Color.parseColor("#E6E1E5"));
        lbl.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Switch sw = new Switch(this);
        sw.setChecked(checked);
        sw.setOnCheckedChangeListener(listener);
        row.addView(lbl);
        row.addView(sw);
        return row;
    }

    private View makeSeekRow(String label, String minLabel, String maxLabel,
                              int value, int min, int max, SeekListener listener) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(0, dp(8), 0, dp(4));
        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextSize(15);
        lbl.setTextColor(Color.parseColor("#E6E1E5"));
        col.addView(lbl);
        LinearLayout seekRow = new LinearLayout(this);
        seekRow.setOrientation(LinearLayout.HORIZONTAL);
        seekRow.setGravity(Gravity.CENTER_VERTICAL);
        seekRow.setPadding(0, dp(6), 0, 0);
        TextView minTv = new TextView(this);
        minTv.setText(minLabel);
        minTv.setTextSize(12);
        minTv.setTextColor(Color.parseColor("#9E9E9E"));
        minTv.setPadding(0, 0, dp(8), 0);
        SeekBar seek = new SeekBar(this);
        seek.setMax(max - min);
        seek.setProgress(value - min);
        seek.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) { listener.onValue(p + min); }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        TextView maxTv = new TextView(this);
        maxTv.setText(maxLabel);
        maxTv.setTextSize(12);
        maxTv.setTextColor(Color.parseColor("#9E9E9E"));
        maxTv.setPadding(dp(8), 0, 0, 0);
        seekRow.addView(minTv);
        seekRow.addView(seek);
        seekRow.addView(maxTv);
        col.addView(seekRow);
        return col;
    }

    interface SeekListener { void onValue(int val); }

    private View makeAccentButton(String text, View.OnClickListener listener) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextColor(Color.parseColor("#1C1B1F"));
        btn.setTextSize(15);
        btn.setGravity(Gravity.CENTER);
        btn.setTypeface(null, Typeface.BOLD);
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(Color.parseColor("#D0BCFF"));
        d.setCornerRadius(dp(12));
        btn.setBackground(d);
        btn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        btn.setOnClickListener(listener);
        return btn;
    }

    private View divider() {
        View v = new View(this);
        v.setBackgroundColor(Color.parseColor("#3B383E"));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        p.setMargins(0, dp(4), 0, dp(4));
        v.setLayoutParams(p);
        return v;
    }

    private View spacer(int dpSize) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(dpSize)));
        return v;
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
