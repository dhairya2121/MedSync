package com.example.medsync.activities.doctor;

import static com.example.medsync.utils.ViewUtils.isValidUid;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.medsync.R;
import com.example.medsync.utils.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class Dashboard extends BaseActivity {
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db;

    private View dashboardSection;
    private MaterialButton btnAddHospital;
    private ImageView dashboardWatermark;
    private MaterialCardView hospitalHeroCard;
    private ListenerRegistration doctorListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_dashboard);
        applyEdgeToEdgePadding(findViewById(R.id.main));

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        dashboardSection = findViewById(R.id.dashboard_section);
        btnAddHospital = findViewById(R.id.btn_add_hospital);
        dashboardWatermark = findViewById(R.id.iv_dashboard_watermark);
        hospitalHeroCard = findViewById(R.id.hospital_hero_card);

        setupBaseActivityFooter("home", "D");

        btnAddHospital.setOnClickListener(v -> startActivity(new Intent(this, SearchHospitals.class)));

        startDoctorListener();
    }
//now add feature for doctor to view treatments on the select day on the Schedule Calendar, decide the layout wether to put in rolebased section or home?
    private void startDoctorListener() {
        if (user == null) return;

        doctorListener = db.collection("doctors").document(user.getUid())
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;

                    String spec = snapshot.getString("specialization");
                    setupBaseActivityNavbar("D", spec != null ? spec : "Doctor");

                    String hospitalId = snapshot.getString("hospital_id");

                    if (isValidUid(hospitalId)) {
                        // Linked to hospital
                        dashboardSection.setVisibility(View.VISIBLE);
                        btnAddHospital.setVisibility(View.GONE);
                        dashboardWatermark.setVisibility(View.GONE);

                        fetchHospitalDetails(hospitalId);
                        updateAdmittedCount(hospitalId);
                    } else {
                        // Not linked
                        dashboardSection.setVisibility(View.GONE);
                        btnAddHospital.setVisibility(View.VISIBLE);
                        dashboardWatermark.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void fetchHospitalDetails(String hospitalId) {
        db.collection("hospitals").document(hospitalId).get()
                .addOnSuccessListener(h -> {
                    if (h.exists()) {
                        ((TextView) findViewById(R.id.tvHospitalName)).setText(h.getString("legal_name"));
                        ((TextView) findViewById(R.id.tvHospitalAddress)).setText(h.getString("address"));

                        hospitalHeroCard.setOnClickListener(v -> {
                            Intent intent = new Intent(this, ManageAdmitted.class);
                            startActivity(intent);
                        });
                    }
                });
    }

    private void updateAdmittedCount(String hospitalId) {
        db.collection("patients")
                .whereEqualTo("hospital_id", hospitalId)
                .whereEqualTo("isAdmitted", true)
                .whereEqualTo("examiner_id", user.getUid())
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        ((TextView) findViewById(R.id.tvAdmittedCount)).setText(value.size() + " Admitted");
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (doctorListener != null) doctorListener.remove();
    }
}