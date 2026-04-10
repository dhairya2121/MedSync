package com.example.medsync.utils;

import android.content.Context;
import android.graphics.Color;
import com.google.android.material.button.MaterialButton;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

public class ViewUtils {

    public static void setLoading(Context context, boolean isLoading, MaterialButton btn, String loadingText, String defaultText) {
        if (isLoading) {
            CircularProgressDrawable progressDrawable = new CircularProgressDrawable(context);
            progressDrawable.setStrokeWidth(5f);
            progressDrawable.setCenterRadius(20f);
            progressDrawable.setColorSchemeColors(Color.WHITE);
            progressDrawable.start();

            btn.setIconTint(null);
            btn.setIcon(progressDrawable);
            btn.setText(loadingText);
            btn.setEnabled(false);
        } else {
            btn.setIcon(null);
            btn.setText(defaultText);
            btn.setEnabled(true);
        }
    }
}