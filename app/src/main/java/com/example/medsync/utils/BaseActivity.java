package com.example.medsync.utils;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.net.Uri;
import android.view.View;

public abstract class BaseActivity extends AppCompatActivity {

    protected FirebaseUser currentUser;
    protected String userRole; // You can fetch this from SharedPreferences or Intent

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }
    protected void applyEdgeToEdgePadding(View rootView) {
        if (rootView == null) return;

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    // This method will be called by child activities to setup the common UI
    protected void setupBaseActivityNavbar(String role, String subtext) {
        if (currentUser != null) {
            String name = currentUser.getDisplayName();
            Uri photo = currentUser.getPhotoUrl();

            // Setup Navbar
            ViewUtils.setupNavbar(BaseActivity.this, name, subtext, photo, false, role);
        }
    }
    protected void setupBaseActivityFooter(String activeTab, String role){
        if (currentUser != null) {
            // Setup Footer
            ViewUtils.setupFooter(BaseActivity.this, activeTab, role);
        }
    }
}
