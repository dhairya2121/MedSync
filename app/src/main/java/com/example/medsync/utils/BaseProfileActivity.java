package com.example.medsync.utils;

import static com.example.medsync.utils.ViewUtils.isValidUid;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.medsync.R;
import com.example.medsync.activities.LoginActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.TimeUnit;

public abstract class BaseProfileActivity extends BaseActivity {
    protected FirebaseAuth mAuth;
    protected FirebaseFirestore db;
    protected FirebaseUser user;

    private static final String PREF_NAME = "profile_cache";
    private static final String KEY_NAME = "cached_name";
    private static final String KEY_EMAIL = "cached_email";
    private static final String KEY_PHONE = "cached_phone";
    private static final long CACHE_EXPIRY_MS = 10 * 60 * 1000;
    private static final String KEY_CACHE_TIME = "cache_time";

    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;

    protected abstract String getCollectionName();
    protected abstract String getUserRole();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();
    }

    private void saveProfileCache(String name, String email, String phone) {
        getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit()
                .putString(KEY_NAME, name)
                .putString(KEY_EMAIL, email)
                .putString(KEY_PHONE, phone)
                .putLong(KEY_CACHE_TIME, System.currentTimeMillis())
                .apply();
    }

    private boolean isCacheValid() {
        long savedTime = getSharedPreferences(PREF_NAME, MODE_PRIVATE).getLong(KEY_CACHE_TIME, 0);
        return (System.currentTimeMillis() - savedTime) < CACHE_EXPIRY_MS;
    }

    protected void invalidateProfileCache() {
        getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit().remove(KEY_CACHE_TIME).apply();
    }

    // --- DATA LOADING ---
    protected void loadAuthProfileData(View nameCard, View emailCard, View phoneCard, View passCard) {
        if (user == null) return;

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        if (isCacheValid()) {
            bindViewsFromData(prefs.getString(KEY_NAME, ""), prefs.getString(KEY_EMAIL, ""),
                    prefs.getString(KEY_PHONE, ""), nameCard, emailCard, phoneCard, passCard);
        } else {
            String n = user.getDisplayName();
            String e = user.getEmail();
            String p = user.getPhoneNumber();
            saveProfileCache(n, e, p);
            bindViewsFromData(n, e, p, nameCard, emailCard, phoneCard, passCard);
        }

        db.collection(getCollectionName()).document(user.getUid()).get()
                .addOnSuccessListener(this::onExtraDataLoaded);
    }

    protected void bindViewsFromData(String name, String email, String phone, View nameCard, View emailCard, View phoneCard, View passCard) {
        ViewUtils.setupEditableInfoCard(this, nameCard, R.drawable.ic_filled_user, "Full Name",
                (name != null && !name.isEmpty()) ? name : "", val -> {
                    ViewUtils.setInputState(this, nameCard, "LOADING");
                    updateAuthName(val, nameCard);
                });

        ViewUtils.setupEditableInfoCard(this, emailCard, R.drawable.ic_mail, "Email", email, val -> {
            ViewUtils.setInputState(this, emailCard, "LOADING");
            updateAuthEmail(val, emailCard);
        });

        ViewUtils.setupEditableInfoCard(this, phoneCard, R.drawable.ic_contact_book, "Phone",
                (isValidUid(phone)) ? phone : "Add Phone", val -> {
                    ViewUtils.setInputState(this, phoneCard, "LOADING");
                    updateAuthPhone(val, phoneCard);
                });

        setupPasswordField(passCard);
        ViewUtils.setupEditableInfoCard(this, passCard, R.drawable.ic_passkey, "Password", "", val -> {
            ViewUtils.setInputState(this, passCard, "LOADING");
            updateAuthPassword(val, passCard);
        });
    }

    // --- UPDATE LOGIC ---

    protected void updateAuthName(String newName, View card) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newName).build();

        user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                db.collection(getCollectionName()).document(user.getUid()).update("name", newName)
                        .addOnCompleteListener(dbTask -> {
                            ViewUtils.setInputState(this, card, dbTask.isSuccessful() ? "IDLE" : "ERROR");
                            if (dbTask.isSuccessful()) {
                                invalidateProfileCache();
                                Toast.makeText(this, "Name Updated", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                ViewUtils.setInputState(this, card, "ERROR");
            }
        });
    }

    protected void updateAuthEmail(String newEmail, View card) {
        // Sync DB first
        db.collection(getCollectionName()).document(user.getUid()).update("email", newEmail)
                .addOnCompleteListener(dbTask -> {
                    if (dbTask.isSuccessful()) {
                        user.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                ViewUtils.setInputState(this, card, "IDLE");
                                invalidateProfileCache();
                                mAuth.signOut();
                                Toast.makeText(this, "Verify link sent to " + newEmail + ". Please login again.", Toast.LENGTH_LONG).show();
                                finishToLogin();
                            } else if (task.getException() instanceof FirebaseAuthRecentLoginRequiredException) {
                                showReauthenticateDialog(newEmail, card);
                            } else {
                                ViewUtils.setInputState(this, card, "ERROR");
                                handleAuthError(task.getException());
                            }
                        });
                    } else {
                        ViewUtils.setInputState(this, card, "ERROR");
                    }
                });
    }

    private void showReauthenticateDialog(String newEmail, View card) {
        EditText passInput = new EditText(this);
        passInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle("Security Check")
                .setMessage("Please enter password to verify identity.")
                .setView(passInput)
                .setPositiveButton("Verify", (d, w) -> {
                    AuthCredential cred = EmailAuthProvider.getCredential(user.getEmail(), passInput.getText().toString());
                    user.reauthenticate(cred).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) updateAuthEmail(newEmail, card);
                        else ViewUtils.setInputState(this, card, "ERROR");
                    });
                })
                .setNegativeButton("Cancel", (d, w) -> ViewUtils.setInputState(this, card, "IDLE"))
                .show();
    }

    protected void updateAuthPhone(String newPhone, View card) {
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(newPhone.trim())
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override public void onVerificationCompleted(PhoneAuthCredential c) { updateUserPhoneCredential(c, card); }
                    @Override public void onVerificationFailed(FirebaseException e) {
                        ViewUtils.setInputState(BaseProfileActivity.this, card, "ERROR");
                        Toast.makeText(BaseProfileActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                    @Override public void onCodeSent(String vId, PhoneAuthProvider.ForceResendingToken t) {
                        mVerificationId = vId;
                        showOtpDialog(card);
                    }
                }).build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void showOtpDialog(View card) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this).setTitle("Enter OTP").setView(input)
                .setPositiveButton("Verify", (d, w) -> {
                    updateUserPhoneCredential(PhoneAuthProvider.getCredential(mVerificationId, input.getText().toString()), card);
                })
                .setNegativeButton("Cancel", (d, w) -> ViewUtils.setInputState(this, card, "IDLE"))
                .show();
    }

    private void updateUserPhoneCredential(PhoneAuthCredential credential, View card) {
        user.updatePhoneNumber(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                db.collection(getCollectionName()).document(user.getUid()).update("phone", user.getPhoneNumber())
                        .addOnCompleteListener(dbTask -> {
                            ViewUtils.setInputState(this, card, dbTask.isSuccessful() ? "IDLE" : "ERROR");
                            invalidateProfileCache();
                        });
            } else {
                ViewUtils.setInputState(this, card, "ERROR");
                handleAuthError(task.getException());
            }
        });
    }

    protected void updateAuthPassword(String newPass, View card) {
        user.updatePassword(newPass).addOnCompleteListener(task -> {
            ViewUtils.setInputState(this, card, task.isSuccessful() ? "IDLE" : "ERROR");
            if (!task.isSuccessful()) {
                handleAuthError(task.getException());
            }
        });
    }

    // --- HELPERS ---
    protected void setupPasswordField(View card) {
        EditText et = card.findViewById(R.id.edit_text_input);
        if (et != null) et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    }

    protected void setupLogout(View logoutBtn) {
        if (logoutBtn != null) logoutBtn.setOnClickListener(v -> {
            invalidateProfileCache();
            mAuth.signOut();
            finishToLogin();
        });
    }

    private void finishToLogin() {
        startActivity(new Intent(this, LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    protected void exitHospitalLogic(View btn, String role) {
        String col = role.equals("D") ? "doctors" : "assistants";
        db.collection(col).document(user.getUid()).update("hospital_id", "")
                .addOnSuccessListener(a -> {
                    invalidateProfileCache();
                    Toast.makeText(this, "Exited Hospital", Toast.LENGTH_SHORT).show();
                });
        btn.setEnabled(false);
    }

    private void handleAuthError(Exception e) {
        Toast.makeText(this, "Auth Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }

    protected void onExtraDataLoaded(DocumentSnapshot doc) {}
}