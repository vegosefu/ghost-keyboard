package com.ghost.keyboard;

import android.inputmethodservice.InputMethodService;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.TextView;

public class GhostKeyboard extends InputMethodService {

    private boolean shifted = false;
    private View keyboardView;

    @Override
    public View onCreateInputView() {
        keyboardView = getLayoutInflater().inflate(R.layout.keyboard_view, null);
        setupKeys(keyboardView);
        return keyboardView;
    }

    private void setupKeys(View v) {
        int[] ids = {
            R.id.key_q, R.id.key_w, R.id.key_e, R.id.key_r, R.id.key_t,
            R.id.key_y, R.id.key_u, R.id.key_i, R.id.key_o, R.id.key_p,
            R.id.key_a, R.id.key_s, R.id.key_d, R.id.key_f, R.id.key_g,
            R.id.key_h, R.id.key_j, R.id.key_k, R.id.key_l,
            R.id.key_z, R.id.key_x, R.id.key_c, R.id.key_v, R.id.key_b,
            R.id.key_n, R.id.key_m
        };
        for (int id : ids) {
            TextView key = v.findViewById(id);
            if (key == null) continue;
            key.setOnTouchListener((view, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    InputConnection ic = getCurrentInputConnection();
                    if (ic != null) {
                        String txt = ((TextView) view).getText().toString();
                        ic.commitText(shifted ? txt.toUpperCase() : txt, 1);
                        if (shifted) { shifted = false; updateShift(v); }
                    }
                }
                return true;
            });
        }
        v.findViewById(R.id.key_space).setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) ic.commitText(" ", 1);
            }
            return true;
        });
        v.findViewById(R.id.key_del).setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) ic.deleteSurroundingText(1, 0);
            }
            return true;
        });
        v.findViewById(R.id.key_enter).setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) ic.commitText("\n", 1);
            }
            return true;
        });
        v.findViewById(R.id.key_shift).setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                shifted = !shifted;
                updateShift(v);
            }
            return true;
        });
        v.findViewById(R.id.key_num).setOnTouchListener((view, event) -> true);
    }

    private void updateShift(View v) {
        int[] ids = {
            R.id.key_q, R.id.key_w, R.id.key_e, R.id.key_r, R.id.key_t,
            R.id.key_y, R.id.key_u, R.id.key_i, R.id.key_o, R.id.key_p,
            R.id.key_a, R.id.key_s, R.id.key_d, R.id.key_f, R.id.key_g,
            R.id.key_h, R.id.key_j, R.id.key_k, R.id.key_l,
            R.id.key_z, R.id.key_x, R.id.key_c, R.id.key_v, R.id.key_b,
            R.id.key_n, R.id.key_m
        };
        for (int id : ids) {
            TextView key = v.findViewById(id);
            if (key == null) continue;
            String t = key.getText().toString();
            key.setText(shifted ? t.toUpperCase() : t.toLowerCase());
        }
    }
}
