package com.example.medsync.utils;

import static androidx.core.content.ContextCompat.getSystemService;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.medsync.R;
import com.example.medsync.activities.receptionist.Dashboard;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;
import com.bumptech.glide.Glide;

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

    public static void redirectToRoleBasedDashboard(Activity activity, FirebaseUser user) {
        if (user == null || activity == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String role = document.getString("role");
                        Class<?> targetActivity = null;

                        if (role != null) {
                            switch (role) {
                                case "C": targetActivity = com.example.medsync.activities.careTaker.Dashboard.class; break;
                                case "P": targetActivity = com.example.medsync.activities.patient.Dashboard.class; break;
                                case "D": targetActivity = com.example.medsync.activities.doctor.Dashboard.class; break;
                                case "R": targetActivity = com.example.medsync.activities.receptionist.Dashboard.class; break;
                                case "A": targetActivity = com.example.medsync.activities.assistant.Dashboard.class; break;
                            }
                        }

                        if (targetActivity != null) {
                            Intent intent = new Intent(activity, targetActivity);
                            activity.startActivity(intent);
                            activity.overridePendingTransition(0, 0); // Stops screen flicker

                            activity.finish(); // Close the calling activity (Login/Signup)
                        } else {
                            Toast.makeText(activity, "Unknown role: " + role, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(activity, "Profile not found in database.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ViewUtils", "Error fetching role", e);
                    Toast.makeText(activity, "Failed to fetch user data.", Toast.LENGTH_SHORT).show();
                });
    }

    public static void setupNavbar(Activity activity, String name, String subtext, Uri photoUri, boolean hasNotifications, String role) {
        TextView tvName = activity.findViewById(R.id.tvUserName);
        TextView tvSubtext = activity.findViewById(R.id.tvUserSubtext);
        TextView tvInitial = activity.findViewById(R.id.tvUserInitial);
        ImageView ivProfile = activity.findViewById(R.id.ivUserProfile);
        View profileContainer = activity.findViewById(R.id.profileContainer);
        View dot = activity.findViewById(R.id.notificationDot);

        if (tvName != null) tvName.setText(name);
        if (tvSubtext != null) tvSubtext.setText(subtext);

        // Logic Fix: Only show ImageView if photoUri is NOT null
        if (photoUri != null && ivProfile != null) {
            ivProfile.setVisibility(View.VISIBLE);
            if (tvInitial != null) tvInitial.setVisibility(View.GONE);

            Glide.with(activity.getApplicationContext())
                    .load(photoUri)
                    .placeholder(R.drawable.circle_background)
                    .error(R.drawable.circle_background)
                    .circleCrop()
                    .into(ivProfile);
        } else {
            // Fallback to Initial text
            if (ivProfile != null) ivProfile.setVisibility(View.GONE);
            if (tvInitial != null) {
                tvInitial.setVisibility(View.VISIBLE);
                if (name != null && !name.isEmpty()) {
                    tvInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
                }
            }
        }

        // Profile Redirection from Navbar
        if (profileContainer != null) {
            profileContainer.setOnClickListener(v -> {
                Class<?> targetActivity = null;
                if (role != null) {
                    switch (role) {
                        case "C": targetActivity = com.example.medsync.activities.careTaker.Profile.class; break;
                        case "R": targetActivity = com.example.medsync.activities.receptionist.Profile.class; break;
                        case "D": targetActivity = com.example.medsync.activities.doctor.Profile.class; break;
                        case "A": targetActivity = com.example.medsync.activities.assistant.Profile.class; break;
                        case "P": targetActivity = com.example.medsync.activities.patient.Profile.class; break;
                    }
                }

                if (targetActivity != null) {
                    Intent intent = new Intent(activity, targetActivity);
                    activity.startActivity(intent);
                    activity.overridePendingTransition(0, 0);
                } else {
                    Toast.makeText(activity, "Profile coming soon for " + role, Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (dot != null) {
            dot.setVisibility(hasNotifications ? View.VISIBLE : View.GONE);
        }
    }
    public static void setupFooter(Activity activity, String activeTab, String role) {
        // 1. Find all buttons
        ImageButton btnHome = activity.findViewById(R.id.btnNavHome);
        ImageButton btnRoleBased = activity.findViewById(R.id.btnNavRoleBased);
        ImageButton btnProfile = activity.findViewById(R.id.btnNavProfile);
        ImageButton btnEmergency = activity.findViewById(R.id.btnNavEmergency);

        // 2. Define Colors
        int activeColor = ContextCompat.getColor(activity, R.color.dark_green);
        int inactiveColor = ContextCompat.getColor(activity, R.color.grey);
        int emergencyColor = ContextCompat.getColor(activity, android.R.color.holo_red_dark);

        // 3. Set Dynamic Icon based on Role
        if (role != null && btnRoleBased != null) {
            switch (role) {
                case "P":
                case "D":
                case "C":
                case "A":
                    btnRoleBased.setImageResource(R.drawable.ic_analytics);
                    break;
                case "R":
                    btnRoleBased.setImageResource(R.drawable.ic_hospital);
                    break;
            }
        }

        // 4. RESET all colors to inactive
        if (btnHome != null) btnHome.setColorFilter(inactiveColor);
        if (btnRoleBased != null) btnRoleBased.setColorFilter(inactiveColor);
        if (btnProfile != null) btnProfile.setColorFilter(inactiveColor);
        if (btnEmergency != null) btnEmergency.setColorFilter(emergencyColor); // Emergency stays red

        // 5. APPLY Active Color based on current state
        if (activeTab != null) {
            switch (activeTab.toLowerCase()) {
                case "home":
                    if (btnHome != null) btnHome.setColorFilter(activeColor);
                    break;
                case "rolebased": // Matches "roleBased".toLowerCase()
                    if (btnRoleBased != null) btnRoleBased.setColorFilter(activeColor);
                    break;
                case "profile":
                    if (btnProfile != null) btnProfile.setColorFilter(activeColor);
                    break;
            }
        }

        // 6. Navigation Logic
        btnHome.setOnClickListener(v -> {
            if (!activeTab.equals("home")) {
                FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    redirectToRoleBasedDashboard(activity, user);
                }
            }
        });

        btnRoleBased.setOnClickListener(v -> {
            // Only navigate if we aren't already on this tab
            if (!"rolebased".equals(activeTab)) {

                Class<?> targetActivity = null;if (role != null) {
                    switch (role) {
                        case "R":
                            targetActivity = com.example.medsync.activities.receptionist.Hospital.class;
                            break;
                        // case "D": targetActivity = DoctorActivity.class; break;
                    }
                }

                if (targetActivity != null) {
                    // Check if we are already in the target activity to prevent loops
                    if (!activity.getClass().equals(targetActivity)) {
                        Intent intent = new Intent(activity, targetActivity);
                        // Add flags to prevent building a massive backstack
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        activity.startActivity(intent);
                        activity.overridePendingTransition(0, 0);
                        activity.finish();
                    }
                } else {
                    Toast.makeText(activity, "Feature coming soon for " + role, Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnProfile.setOnClickListener(v -> {
            if (!activeTab.equalsIgnoreCase("profile")) {
                Class<?> targetActivity = null;
                if (role != null) {
                    switch (role) {
                        case "R":targetActivity = com.example.medsync.activities.receptionist.Profile.class;break;
                        case "P": targetActivity = com.example.medsync.activities.patient.Profile.class;break;
                        case "C": targetActivity = com.example.medsync.activities.careTaker.Profile.class;break;
                        case "A": targetActivity = com.example.medsync.activities.assistant.Profile.class;break;
                        case "D": targetActivity = com.example.medsync.activities.doctor.Profile.class;break;

                    }
                }

                if (targetActivity != null) {
                    Intent intent = new Intent(activity, targetActivity);
                    activity.startActivity(intent);
                    activity.overridePendingTransition(0, 0); // Stops screen flicker
                    activity.finish();
                } else {
                    Toast.makeText(activity, "Feature coming soon for " + role, Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnEmergency.setOnClickListener(v -> {
            Toast.makeText(activity, "Initiating Emergency Protocol...", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:102"));
            activity.startActivity(intent);
        });
    }




    public interface SaveListener {
        void onSave(String newValue);
    }
    public static void setupEditableInfoCard(Activity activity, View root, int iconResId, String hint, String initialValue,SaveListener saveListener) {
        EditText editTextInput = root.findViewById(R.id.edit_text_input);
        ImageView btnEdit = root.findViewById(R.id.btn_edit);
        ImageView btnSave = root.findViewById(R.id.btn_save);
        ImageView iconError = root.findViewById(R.id.icon_error);
        ImageView iconStart = root.findViewById(R.id.icon_start); // Get the start icon reference

        // Set the custom icon passed from the Activity
        if (iconStart != null) {
            iconStart.setImageResource(iconResId);
        }

        editTextInput.setHint(hint);
        editTextInput.setText(initialValue);

        btnEdit.setOnClickListener(v -> setInputState(activity, root, "EDITING"));

        btnSave.setOnClickListener(v -> {
            setInputState(activity, root, "SAVING");
            hideKeyboard(activity, editTextInput);

            // 2. Trigger the custom logic passed from the Activity
            if (saveListener != null) {
                saveListener.onSave(editTextInput.getText().toString());
            }
        });

        iconError.setOnClickListener(v -> setInputState(activity, root, "EDITING"));

        // Initialize to IDLE
        setInputState(activity, root, "IDLE");
    }
    public static void setInputState(Activity activity, View root, String state) {
        EditText editTextInput = root.findViewById(R.id.edit_text_input);
        ImageView btnEdit = root.findViewById(R.id.btn_edit);
        ImageView btnSave = root.findViewById(R.id.btn_save);
        ProgressBar progressSaving = root.findViewById(R.id.progress_saving);
        ImageView iconError = root.findViewById(R.id.icon_error);

        btnEdit.setVisibility(View.GONE);
        btnSave.setVisibility(View.GONE);
        progressSaving.setVisibility(View.GONE);
        iconError.setVisibility(View.GONE);

        switch (state) {
            case "IDLE":
                btnEdit.setVisibility(View.VISIBLE);
                makeEditable(activity, editTextInput, false);
                break;
            case "EDITING":
                btnSave.setVisibility(View.VISIBLE);
                makeEditable(activity, editTextInput, true);
                break;
            case "SAVING":
                progressSaving.setVisibility(View.VISIBLE);
                makeEditable(activity, editTextInput, false);
                break;
            case "ERROR":
                iconError.setVisibility(View.VISIBLE);
                makeEditable(activity, editTextInput, false);
                break;
        }
    }

    private static void makeEditable(Activity activity, EditText et, boolean canEdit) {
        et.setFocusable(canEdit);
        et.setFocusableInTouchMode(canEdit);
        et.setCursorVisible(canEdit);
        if (canEdit) {
            et.requestFocus();
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private static void simulateNetworkCall(Activity activity, View root) {

    }
    public static void setupRatingBar(android.widget.RatingBar ratingBar, double rating, long reviewCount) {
            // 1. Cast double to float for the RatingBar
            ratingBar.setRating((float) rating);

            // 2. Find the rating value TextView (using root view to find siblings)
            TextView tv_rating_value = ratingBar.getRootView().findViewById(R.id.tv_rating_value);

            if (tv_rating_value != null) {
                // 3. Logic: If rating is valid, show it; otherwise show 0.0
                if (rating > 0) {
                    tv_rating_value.setText(String.valueOf((float) rating));
                } else {
                    tv_rating_value.setText("0.0");
                }
            }

            // 4. Handle Review Count
            TextView tvReviewCount = ratingBar.getRootView().findViewById(R.id.tv_review_count);
            if (tvReviewCount != null) {
                if (reviewCount > 0) {
                    tvReviewCount.setText(String.valueOf(reviewCount));
                } else {
                    tvReviewCount.setText("No");
                }
            }
        }
    private static void hideKeyboard(Activity activity, View view) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    public static boolean isValidUid(String uid){
        if(uid ==null){
            return false;
        }
        return !uid.trim().isEmpty();
    }
}