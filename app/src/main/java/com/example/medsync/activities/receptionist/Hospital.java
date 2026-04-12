package com.example.medsync.activities.receptionist;

import static com.example.medsync.utils.ViewUtils.isValidUid;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RatingBar;
import android.widget.Toast;

import com.example.medsync.R;
import com.example.medsync.utils.BaseActivity;
import com.example.medsync.utils.ViewUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class Hospital extends BaseActivity {
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private String cachedHospitalId;
    private ListenerRegistration receptionistListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hospital);
        applyEdgeToEdgePadding(findViewById(R.id.main));
        setupBaseActivityNavbar("R", "Receptionist");
        setupBaseActivityFooter("rolebased", "R");

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        View nameCard = findViewById(R.id.hospital_name_card_container);
        View phoneCard = findViewById(R.id.hospital_phone_card_container);
        View landlineCard = findViewById(R.id.hospital_landline_card_container);
        View emailCard = findViewById(R.id.hospital_email_card_container);
        View addressCard = findViewById(R.id.hospital_address_card_container);

        // Safe binding for RatingBar
        View ratingCard = findViewById(R.id.hospital_rating_card_container);
        RatingBar ratingBar = (ratingCard != null) ? ratingCard.findViewById(R.id.rating_bar) : null;

        if (user != null) {
            receptionistListener = db.collection("receptionists").document(user.getUid())
                    .addSnapshotListener((v, error) -> {
                        if (error != null) {
                            Log.e("Hospital", "Listen failed.", error);
                            return;
                        }

                        if (v != null && v.exists()) {
                            String newId = v.getString("hospital_id");

                            if (isValidUid(newId) && !newId.equals(cachedHospitalId)) {
                                cachedHospitalId = newId;
                                fetchHospitalDetails(cachedHospitalId, nameCard, phoneCard, landlineCard, emailCard, addressCard,ratingCard, ratingBar);
                            }
                            else {
                                navigateToSearch();
                            }
                        } else {
                            Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    public void fetchHospitalDetails(String hospitalId, View nameCard, View phoneCard, View landlineCard, View emailCard, View addressCard,View ratingCard, RatingBar ratingBar) {
        if (!isValidUid(hospitalId)) {
            Log.e("Hospital", "fetchHospitalDetails called with empty ID");
            navigateToSearch();
            return;
        }

        db.collection("hospitals").document(hospitalId).get()
                .addOnSuccessListener(v -> {
                    if (v.exists()) {
                        // Populate UI Cards
                        ViewUtils.setupEditableInfoCard(this, nameCard, R.drawable.ic_filled_user, "Hospital Name",
                                v.contains("legal_name")? v.getString("legal_name"): "New Hospital",
                                newValue -> updateDataEntry("legal_name", newValue, nameCard));

                        ViewUtils.setupEditableInfoCard(this, addressCard, R.drawable.ic_location_pin, "Address",
                                v.contains("address")? v.getString("address"): "New address",
                                newValue -> updateDataEntry("address", newValue, addressCard));

                        ViewUtils.setupEditableInfoCard(this, phoneCard, R.drawable.ic_contact_book, "Phone",
                                v.contains("phone")? v.getString("phone"): "9999999999",

                                newValue -> updateDataEntry("phone", newValue, phoneCard));

                        ViewUtils.setupEditableInfoCard(this, landlineCard, R.drawable.ic_landline, "Landline",
                                v.contains("landline")? v.getString("landline"): "999999",

                                newValue -> updateDataEntry("landline", newValue, landlineCard));

                        ViewUtils.setupEditableInfoCard(this, emailCard, R.drawable.ic_mail, "Email",
                                v.contains("email")? v.getString("email"):"hospital@aimsn.ac.in",
                                newValue -> updateDataEntry("email", newValue, emailCard));

                        // Safer number handling
                        if (ratingBar != null) {
                            Double rating = v.getDouble("rating");
                            Long count = v.getLong("reviewCount");

                            // If there are no reviews, it's often better to hide the card entirely
                            if (rating ==null || count==null){
                                ViewUtils.setupRatingBar(ratingBar,0.0,0);
                            } else {
                                ratingCard.setVisibility(View.VISIBLE);
                                ViewUtils.setupRatingBar(ratingBar,rating,count);
                            }
                        }
                    } else {
                        navigateToSearch();
                    }
                })
                .addOnFailureListener(e -> Log.e("Hospital", "Error fetching details", e));
    }

    public void updateDataEntry(String key, String value, View cardRoot) {
        if (user != null && isValidUid(cachedHospitalId)) {
            db.collection("hospitals").document(cachedHospitalId)
                    .update(key, value)
                    .addOnSuccessListener(aVoid -> {
                        ViewUtils.setInputState(this, cardRoot, "IDLE");
                        Toast.makeText(this, "Updated successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        ViewUtils.setInputState(this, cardRoot, "ERROR");
                        Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
                    });
        } else {
            navigateToSearch();
        }
    }

    private void navigateToSearch() {
        Intent intent = new Intent(Hospital.this, SearchHospitals.class);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop listening to database changes when activity is closed to save battery/data
        if (receptionistListener != null) {
            receptionistListener.remove();
        }
    }
}