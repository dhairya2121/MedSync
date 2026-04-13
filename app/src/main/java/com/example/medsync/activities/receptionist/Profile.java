//package com.example.medsync.activities.receptionist;
////reuse the activity_base_profile and make a
//import static com.example.medsync.utils.ViewUtils.isValidUid;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.text.InputType;
//import android.util.Log;
//import android.view.View;
//import android.widget.EditText;
//import android.widget.Toast;
//
//import com.example.medsync.R;
//import com.example.medsync.activities.LoginActivity;
//import com.example.medsync.utils.BaseActivity;
//import com.example.medsync.utils.ViewUtils;
//import com.google.android.material.button.MaterialButton;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.firestore.FirebaseFirestore;
//
//public class Profile extends BaseActivity {
//    private FirebaseAuth mAuth;
//    private FirebaseUser user;
//    private FirebaseFirestore db;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_receptionist_profile);
//        applyEdgeToEdgePadding(findViewById(R.id.main));
//        setupBaseActivityFooter("profile", "R");
//
//        mAuth = FirebaseAuth.getInstance();
//        user = mAuth.getCurrentUser();
//        db = FirebaseFirestore.getInstance();
//
//        // Bind UI Containers
//        View nameCard = findViewById(R.id.receptionist_name_card);
//        View emailCard = findViewById(R.id.receptionist_email_card);
//        View phoneCard = findViewById(R.id.receptionist_phone_card);
//        View passwordCard = findViewById(R.id.receptionist_password_card);
//        View profileImageBtn = findViewById(R.id.btn_profile_image);
//        MaterialButton logoutBtn = findViewById(R.id.logout);
//
//        // Setup Logout
//        logoutBtn.setOnClickListener(v -> {
//            mAuth.signOut();
//            Intent loginIntent = new Intent(this, LoginActivity.class);
//            loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//            startActivity(loginIntent);
//            finish();
//        });
//
//        // Setup Profile Image Click (Placeholder for Gallery Intent)
//        profileImageBtn.setOnClickListener(v -> {
//            Toast.makeText(this, "Upload feature coming soon!", Toast.LENGTH_SHORT).show();
//        });
//
//        // Initialize Password Card to be protected (dots)
//        setupPasswordField(passwordCard);
//
//        if (user != null) {
//            fetchReceptionistDetails(user.getUid(), nameCard, emailCard, phoneCard, passwordCard);
//        }
//    }
//
//    private void setupPasswordField(View passwordCard) {
//        EditText et = passwordCard.findViewById(R.id.edit_text_input);
//        if (et != null) {
//            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
//        }
//    }
//
//    public void fetchReceptionistDetails(String uid, View nameCard, View emailCard, View phoneCard, View passwordCard) {
//        db.collection("receptionists").document(uid).get()
//                .addOnSuccessListener(doc -> {
//                    if (doc.exists()) {
//                        // Setup Name Card
//                        ViewUtils.setupEditableInfoCard(this, nameCard, R.drawable.ic_filled_user, "Full Name",
//                                doc.getString("name") != null ? doc.getString("name") : user.getDisplayName(),
//                                newValue -> updateDataEntry("name", newValue, nameCard));
//
//                        // Setup Email Card
//                        ViewUtils.setupEditableInfoCard(this, emailCard, R.drawable.ic_mail, "Email Address",
//                                doc.getString("email") != null ? doc.getString("email") : user.getEmail(),
//                                newValue -> updateDataEntry("email", newValue, emailCard));
//
//                        // Setup Phone Card
//                        ViewUtils.setupEditableInfoCard(this, phoneCard, R.drawable.ic_contact_book, "Phone Number",
//                                doc.getString("phone") != null ? doc.getString("phone") : "",
//                                newValue -> updateDataEntry("phone", newValue, phoneCard));
//
//                        // Setup Password Card (Note: Firestore shouldn't store plain text passwords, but adding as requested)
//                        ViewUtils.setupEditableInfoCard(this, passwordCard, R.drawable.ic_passkey, "Password",
//                                "********", // Show masked by default
//                                newValue -> updateDataEntry("password", newValue, passwordCard));
//                    }
//                })
//                .addOnFailureListener(e -> Log.e("Profile", "Error fetching profile", e));
//    }
//
//    public void updateDataEntry(String key, String value, View cardRoot) {
//        if (user != null) {
//            db.collection("receptionists").document(user.getUid())
//                    .update(key, value)
//                    .addOnSuccessListener(aVoid -> {
//                        ViewUtils.setInputState(this, cardRoot, "IDLE");
//                        Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
//                    })
//                    .addOnFailureListener(e -> {
//                        ViewUtils.setInputState(this, cardRoot, "ERROR");
//                        Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
//                    });
//        }
//    }
//}

package com.example.medsync.activities.receptionist;

import com.example.medsync.utils.BaseProfileActivity;
import com.google.firebase.firestore.DocumentSnapshot;

public class Profile extends BaseProfileActivity {

    @Override
    protected String getCollectionName() {
        return "receptionists";
    }

    @Override
    protected String getUserRole() {
        return "R";
    }

}