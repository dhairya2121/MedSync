package com.example.medsync.utils;

import static android.opengl.ETC1.isValid;

import static com.example.medsync.utils.ViewUtils.isValidUid;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.medsync.R;
import com.example.medsync.activities.LoginActivity;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
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
    private static final long CACHE_EXPIRY_MS = 10 * 60 * 1000; // 10 minutes
    private static final String KEY_CACHE_TIME = "cache_time";

    private void saveProfileCache(String name, String email, String phone) {
        getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit()
                .putString(KEY_NAME, name)
                .putString(KEY_EMAIL, email)
                .putString(KEY_PHONE, phone)
                .putLong(KEY_CACHE_TIME, System.currentTimeMillis())
                .apply();
    }

    private boolean isCacheValid() {
        long savedTime = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                .getLong(KEY_CACHE_TIME, 0);
        return (System.currentTimeMillis() - savedTime) < CACHE_EXPIRY_MS;
    }

    private void loadFromCache(SharedPreferences prefs) {
        String cachedName  = prefs.getString(KEY_NAME, "Set Name");
        String cachedEmail = prefs.getString(KEY_EMAIL, "");
        String cachedPhone = prefs.getString(KEY_PHONE, "Add Phone");
        bindProfileViews(cachedName, cachedEmail, cachedPhone);
    }

    // ✅ Call this whenever a field is successfully updated to keep cache in sync
    public void invalidateProfileCache() {
        getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit()
                .remove(KEY_CACHE_TIME) // Forces re-fetch next open
                .apply();
    }
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;

    protected abstract String getCollectionName();
    protected abstract String getUserRole();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Note: Make sure activity_receptionist_profile.xml uses generic IDs
        // if this is truly a "Base" layout, or handle IDs in child classes.
        setContentView(R.layout.activity_receptionist_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();

        applyEdgeToEdgePadding(findViewById(R.id.main));
        setupBaseActivityFooter("profile", getUserRole());

        setupLogout();
        if (user != null) {
            loadAuthProfileData();
        }
    }
    private void bindProfileViews(String name, String email, String phone) {
        ViewUtils.setupEditableInfoCard(this, findViewById(R.id.receptionist_name_card),
                R.drawable.ic_filled_user, "Full Name",
                (name != null && !name.isEmpty()) ? name : "Set Name",
                val -> {
                    updateAuthName(val, findViewById(R.id.receptionist_name_card));
                    invalidateProfileCache(); // ✅ Bust cache on update
                });

        ViewUtils.setupEditableInfoCard(this, findViewById(R.id.receptionist_email_card),
                R.drawable.ic_mail, "Email Address", email,
                val -> {
                    updateAuthEmail(val, findViewById(R.id.receptionist_email_card));
                    invalidateProfileCache();
                });

        ViewUtils.setupEditableInfoCard(this, findViewById(R.id.receptionist_phone_card),
                R.drawable.ic_contact_book, "+91 9999 9999 99",
                (isValidUid(phone)) ? phone : "Add Phone",
                val -> {
                    updateAuthPhone(val, findViewById(R.id.receptionist_phone_card));
                    invalidateProfileCache();
                });

        View passwordCard = findViewById(R.id.receptionist_password_card);
        setupPasswordField(passwordCard);
        ViewUtils.setupEditableInfoCard(this, passwordCard,
                R.drawable.ic_passkey, "Password", "********",
                val -> updateAuthPassword(val, passwordCard)); // Password never cached
    }
    private void loadAuthProfileData() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        if (isCacheValid()) {
            loadFromCache(prefs);
        } else {
            String authName  = user.getDisplayName();
            String authEmail = user.getEmail();
            String authPhone = user.getPhoneNumber();
            saveProfileCache(authName, authEmail, authPhone);
            bindProfileViews(authName, authEmail, authPhone);
        }

        db.collection(getCollectionName()).document(user.getUid()).get()
                .addOnSuccessListener(this::onExtraDataLoaded)
                .addOnFailureListener(e -> Log.e("Profile", "Firestore failed", e));
    }
    private void updateAuthName(String newName, View card) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newName).build();

        user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
            ViewUtils.setInputState(this, card, task.isSuccessful() ? "IDLE" : "ERROR");
            if (task.isSuccessful()) {
                Toast.makeText(this, "Name updated", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateAuthEmail(String newEmail, View card) {
        user.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener(task -> {
            ViewUtils.setInputState(this, card, task.isSuccessful() ? "IDLE" : "ERROR");
            if (task.isSuccessful()) {

                Toast.makeText(this, "Verification sent to " + newEmail, Toast.LENGTH_LONG).show();
                Toast.makeText(this," Please Login again",Toast.LENGTH_SHORT).show();
                invalidateProfileCache();
                mAuth.signOut();
                startActivity(new Intent(this, LoginActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                finish();
            } else {
                Exception ex = task.getException();
                Log.e("EmailUpdate", "Failed: " + (ex != null ? ex.getMessage() : "null"));
                handleAuthError(ex);
            }
        });
    }
    private void reauthenticateAndUpdateEmail(String newEmail, View card) {
        // Prompt user for password first
        EditText passInput = new EditText(this);
        passInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passInput.setHint("Enter current password");

        new AlertDialog.Builder(this)
                .setTitle("Re-authenticate")
                .setView(passInput)
                .setPositiveButton("Confirm", (d, w) -> {
                    String password = passInput.getText().toString();
                    AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);

                    user.reauthenticate(credential).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            updateAuthEmail(newEmail, card); // now call safely
                        } else {
                            Toast.makeText(this, "Re-auth failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateAuthPassword(String newPassword, View card) {
        user.updatePassword(newPassword).addOnCompleteListener(task -> {
            ViewUtils.setInputState(this, card, task.isSuccessful() ? "IDLE" : "ERROR");
            if (task.isSuccessful()) {
                Toast.makeText(this, "Password changed", Toast.LENGTH_SHORT).show();
            } else {
                handleAuthError(task.getException());
            }
        });
    }

    private void updateAuthPhone(String newPhone, View card) {
        newPhone=newPhone.trim();
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(newPhone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential credential) {
                        updateUserPhoneCredential(credential, card);
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        ViewUtils.setInputState(BaseProfileActivity.this, card, "ERROR");
                        // This is where "BILLING NOT ENABLED" triggers if using a non-test number
                        Toast.makeText(BaseProfileActivity.this, "Failed: Use Test Numbers or Enable Billing", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                        mVerificationId = verificationId;
                        mResendToken = token;
                        showOtpDialog(card);
                    }
                })
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void showOtpDialog(View card) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter OTP");
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("Verify", (dialog, which) -> {
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, input.getText().toString());
            updateUserPhoneCredential(credential, card);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> ViewUtils.setInputState(this, card, "IDLE"));
        builder.show();
    }

    private void updateUserPhoneCredential(PhoneAuthCredential credential, View card) {
        user.updatePhoneNumber(credential).addOnCompleteListener(task -> {
            ViewUtils.setInputState(this, card, task.isSuccessful() ? "IDLE" : "ERROR");
            if (task.isSuccessful()) Toast.makeText(this, "Phone updated", Toast.LENGTH_SHORT).show();
        });
    }

    private void handleAuthError(Exception e) {
        if (e instanceof com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
            Toast.makeText(this, "Please re-login to change sensitive data", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Error: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupPasswordField(View card) {
        EditText et = card.findViewById(R.id.edit_text_input);
        if (et != null) et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    }

    private void setupLogout() {
        View logoutBtn = findViewById(R.id.logout);
        if (logoutBtn != null) {
            logoutBtn.setOnClickListener(v -> {
                invalidateProfileCache(); // ✅ Clear stale cache on logout
                mAuth.signOut();
                startActivity(new Intent(this, LoginActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                finish();
            });
        }
    }

    protected void onExtraDataLoaded(DocumentSnapshot doc) {}
}