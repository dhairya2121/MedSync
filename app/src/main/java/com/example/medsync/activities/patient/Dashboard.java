package com.example.medsync.activities.patient;

import static com.example.medsync.utils.ViewUtils.isValidUid;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.medsync.R;
import com.example.medsync.model.Report;
import com.example.medsync.model.Treatment;
import com.example.medsync.utils.BaseActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Dashboard extends BaseActivity {
    FirebaseAuth mAuth;
    FirebaseUser user;
    FirebaseFirestore db;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_dashboard);
        applyEdgeToEdgePadding(findViewById(R.id.main));
        setupBaseActivityNavbar("P", "Patient Dashboard");
        setupBaseActivityFooter("home", "P");

        mAuth=FirebaseAuth.getInstance();
        user=mAuth.getCurrentUser();
        db=FirebaseFirestore.getInstance();
        updateNavbarUI(user);
        //handle the RECENT HOSPITAL cart at Top (HERO)
        MaterialCardView RecentHospitalCardContainer = findViewById(R.id.recent_hospital_card);
        fetchRecentHospitalTreatmentAndUpdateUi(user, RecentHospitalCardContainer);
        setupDepartmentsUi();
    }

    public void setupDepartmentsUi() {
        Map<String, Integer> iconMap = new HashMap<>();

        // Map your keys to drawable icons
        iconMap.put("general", R.drawable.ic_stethoscope);
        iconMap.put("child", R.drawable.ic_child_care);
        iconMap.put("eye", R.drawable.ic_eye_care);
        iconMap.put("ear", R.drawable.ic_ear_care);
        iconMap.put("lungs", R.drawable.ic_pulmonology);
        iconMap.put("stomach", R.drawable.ic_stomach_care);
        iconMap.put("kidney", R.drawable.ic_kidney_care);
        iconMap.put("hormones", R.drawable.ic_hormones);
        iconMap.put("skin", R.drawable.ic_skin_care);
        iconMap.put("surgery", R.drawable.ic_surgery);
        iconMap.put("heart", R.drawable.ic_heart_care);
        iconMap.put("brain", R.drawable.ic_neurology);
        iconMap.put("bone", R.drawable.ic_bone);

        for (Map.Entry<String, Integer> entry : iconMap.entrySet()) {
            String departmentName = entry.getKey();
            int iconResId = entry.getValue();

            // Get the ID of the include/MaterialCardView from activity_patient_dashboard.xml
            @SuppressLint("DiscouragedApi") int viewId = getResources().getIdentifier(departmentName, "id", getPackageName());
            MaterialCardView departmentCard = findViewById(viewId);

            if (departmentCard != null) {
                // 1. Set the Icon
                ImageView ivGridIcon = departmentCard.findViewById(R.id.ivGridIcon);
                if (ivGridIcon != null) {
                    ivGridIcon.setImageResource(iconResId);
                }

                // 2. Add Click Listener to redirect to Department activity
                departmentCard.setOnClickListener(v -> {
                    Intent intent = new Intent(Dashboard.this, Department.class);
                    // Pass the department name (e.g., "heart", "brain") as an extra
                    intent.putExtra("department_name", departmentName);
                     overridePendingTransition(0, 0);
                    startActivity(intent);
                });
            }
        }
    }

    public void fetchRecentHospitalTreatmentAndUpdateUi(FirebaseUser currUser, MaterialCardView RecentHospitalCardContainner) {
        if (currUser == null || !isValidUid(currUser.getUid())) {
        return;
    }
        String currPatientId = currUser.getUid();

        TextView tvRecentHospitalAddress=RecentHospitalCardContainner.findViewById(R.id.tvRecentHospitalAddress);
        TextView tvRecentHospitalName = RecentHospitalCardContainner.findViewById(R.id.tvRecentHospitalName);
        TextView tvRecentDoctorName = RecentHospitalCardContainner.findViewById(R.id.tvRecentDoctorName);
        TextView tvNoRecents = RecentHospitalCardContainner.findViewById(R.id.tvNoRecents);
        ConstraintLayout btnRecentHospitalCard = RecentHospitalCardContainner.findViewById(R.id.btnRecentHospitalCard);
        MaterialCardView RecentTreatmentCard=findViewById(R.id.RecentTreatmentCard);
        db.collectionGroup("treatments")
                .whereEqualTo("patient_id", currPatientId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((t, e) -> {
                    if (e != null) {
                        // This is likely where it fails if the INDEX is missing
                        Log.e("Dashboard", "Firestore Error: " + e.getMessage());
                        runOnUiThread(() -> Toast.makeText(this, "Query Error: Index may be missing", Toast.LENGTH_LONG).show());
                        return;
                    }

                    if (t != null && !t.isEmpty()) {
                        runOnUiThread(() -> {
                            tvNoRecents.setVisibility(View.GONE);

                            btnRecentHospitalCard.setVisibility(View.VISIBLE);
                        });

                        DocumentSnapshot doc = t.getDocuments().get(0);
                        String recentHospitalId = doc.getString("hospital_id");

                        if (isValidUid(recentHospitalId)) {
                            db.collection("hospitals").document(recentHospitalId).get()
                                    .addOnSuccessListener(d -> {
                                        if (d.exists()) {
                                            RecentTreatmentCard.setVisibility(View.VISIBLE);
                                            tvRecentHospitalAddress.setText(d.getString("address"));
                                            updateRecentTreatmentUI(RecentTreatmentCard, doc.toObject(Treatment.class));
                                            tvRecentDoctorName.setText("Dr. " + doc.getString("examiner_name"));
                                            tvRecentHospitalName.setText(d.getString("legal_name"));
                                            btnRecentHospitalCard.setOnClickListener(v -> {
                                                Intent intent = new Intent(Dashboard.this, Treatments.class);
                                                intent.putExtra("hospital_id", recentHospitalId);
                                                startActivity(intent);
                                            });
                                        }
                                    });
                        }


                    } else {
                        // Logic reaches here if no treatments exist for this user
                        Log.d("Dashboard", "Empty treatments list");
                        runOnUiThread(() -> {
                            tvNoRecents.setVisibility(android.view.View.VISIBLE);
                            RecentTreatmentCard.setVisibility(View.GONE);
                            btnRecentHospitalCard.setVisibility(android.view.View.GONE);
                            Toast.makeText(this, "No past treatments found", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void updateRecentTreatmentUI(MaterialCardView RecentTreatmentCard,Treatment t) {
        // 1. General Info
        if(RecentTreatmentCard==null)return;
        ((TextView) findViewById(R.id.tvType)).setText(t.type);
        ((TextView) findViewById(R.id.tvDoctorName)).setText("Examined by " + t.getDoctorName());
        ((TextView) findViewById(R.id.tvStatus)).setText(t.status);

        if (t.getTimestamp() != null) {
            ((TextView) findViewById(R.id.tvDateTime)).setText(dateFormat.format(t.getTimestamp().toDate()));
        }
    }
    private void updateNavbarUI(FirebaseUser currUser) {
        TextView tvUserName = findViewById(R.id.tvUserName);
        TextView tvUserSubtext = findViewById(R.id.tvUserSubtext);
        TextView tvUserInitial = findViewById(R.id.tvUserInitial);

        if (currUser != null) {
            String name = currUser.getDisplayName() != null ? currUser.getDisplayName() : "Patient User";
            tvUserName.setText(name);
            tvUserSubtext.setText("Patient Account");
            tvUserInitial.setText(name.substring(0, 1).toUpperCase());
        }
    }
    public void redirectToActivity(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        overridePendingTransition(0, 0);
        startActivity(intent);
    }
}