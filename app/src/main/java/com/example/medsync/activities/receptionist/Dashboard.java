package com.example.medsync.activities.receptionist;

import android.content.Intent;import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.medsync.R;
import com.example.medsync.model.enums.TreatmentType;
import com.example.medsync.utils.BaseActivity;
import com.example.medsync.utils.ViewUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Calendar;
import java.util.Date;

public class Dashboard extends BaseActivity {
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private ListenerRegistration dashboardListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receptionist_dashboard);
        applyEdgeToEdgePadding(findViewById(R.id.main));
        setupBaseActivityNavbar("R", "Receptionist");
        setupBaseActivityFooter("home", "R");

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        View dashboardSection = findViewById(R.id.dashboard_section);
        MaterialButton btnAddHospital = findViewById(R.id.btn_add_hospital);
        ImageView dashboardWatermark = findViewById(R.id.iv_dashboard_watermark);
        TextView tvHospitalName = findViewById(R.id.tv_hospital_name);
        LinearLayout manageUsersSection=findViewById(R.id.manage_users_section);
        MaterialCardView appointmentsSection=findViewById(R.id.btnSectionAppointments);
        MaterialCardView admittedSection=findViewById(R.id.btnSectionAdmitted);
        MaterialCardView operationsSection=findViewById(R.id.btnSectionOperations);
        // Redirect to Search/Hospital linking screen
        btnAddHospital.setOnClickListener(v -> {
            startActivity(new Intent(this, SearchHospitals.class));
        });

        if (user != null) {
            // Listen to the specific receptionist document
            dashboardListener = db.collection("receptionists").document(user.getUid())
                    .addSnapshotListener((snapshot, e) -> {
                        if (e != null) {
                            Toast.makeText(this, "Error loading dashboard", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (snapshot != null && snapshot.exists()) {
                            String hospitalId = snapshot.getString("hospital_id");

                            // Check if hospital_id is valid (not null and not empty)
                            if (ViewUtils.isValidUid(hospitalId)) {
                                dashboardSection.setVisibility(View.VISIBLE);
                                btnAddHospital.setVisibility(View.GONE);
                                dashboardWatermark.setVisibility(View.GONE);

                                updateManageUsersUI(hospitalId,manageUsersSection);
                                updateOverviewUI(hospitalId,appointmentsSection,admittedSection,operationsSection);

                            } else {
                                dashboardSection.setVisibility(View.GONE);
                                btnAddHospital.setVisibility(View.VISIBLE);
                                dashboardWatermark.setVisibility(View.VISIBLE);

                            }
                        }
                    });
        }
    }

    public void updateManageUsersUI(String hospitalId,LinearLayout manageUsersSection) {
        db.collection("hospitals").document(hospitalId)
                .addSnapshotListener((doc,e)->{
                    if(e!=null){
                        Toast.makeText(this,"Error loading dashboard",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if(doc!=null && doc.exists()){
                        String hospitalName=doc.getString("legal_name");
                        TextView hospitalNameTextView=manageUsersSection.findViewById(R.id.tv_hospital_name);
                        hospitalNameTextView.setText(hospitalName);
                    }else{
                        Toast.makeText(this,"Error loading dashboard",Toast.LENGTH_SHORT).show();
                    }
                });

        TextView tvPatientCount = manageUsersSection.findViewById(R.id.patient_count);
        TextView tvCareTakersCount = manageUsersSection.findViewById(R.id.care_taker_count);
        TextView tvAssistentCount = manageUsersSection.findViewById(R.id.assistant_count);
        TextView tvDoctorCount = manageUsersSection.findViewById(R.id.doctor_count);

        db.collection("patients")
                .whereEqualTo("hospital_id", hospitalId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    tvPatientCount.setText(String.valueOf(snapshots.size()));
                });
        // Total Caretakers count using Collection Group
        db.collectionGroup("careTakers")
                .whereEqualTo("hospital_id", hospitalId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    tvCareTakersCount.setText(String.valueOf(snapshots.size()));
                });
        db.collection("assistants")
                .whereEqualTo("hospital_id", hospitalId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    tvAssistentCount.setText(String.valueOf(snapshots.size()));
                });
        db.collection("doctors")
                .whereEqualTo("hospital_id", hospitalId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    tvDoctorCount.setText(String.valueOf(snapshots.size()));
                });

        tvPatientCount.setOnClickListener(v->{
            redirectToActivity(ManageUsers.class);
        });
        tvAssistentCount.setOnClickListener(v->{
            redirectToActivity(ManageUsers.class);
        });
        tvDoctorCount.setOnClickListener(v->{
            redirectToActivity(ManageUsers.class);
        });
        tvCareTakersCount.setOnClickListener(v->{
            redirectToActivity(ManageUsers.class);
        });

    }

    public void redirectToActivity(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        overridePendingTransition(0, 0);
        startActivity(intent);
    }


    public void updateOverviewUI(String hospitalId,MaterialCardView appointmentsSection,MaterialCardView admittedSection,MaterialCardView operationsSection){
        // 1. Get Start and End of Current Day
        Calendar cal = Calendar.getInstance();

        // Set to 00:00:00.000 today
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date startOfDay = cal.getTime();

        // Set to 23:59:59.999 today
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        Date endOfDay = cal.getTime();

        // 2. Query Firestore using Range for "today"
        db.collection("treatments")
                .whereEqualTo("hospital_id", hospitalId)
                .whereEqualTo("type", TreatmentType.APPOINTMENT.name()) // Use .name() if type is stored as String in DB
                .whereGreaterThanOrEqualTo("start", new Timestamp(startOfDay))
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    TextView tvCount = appointmentsSection.findViewById(R.id.appointment_count);
                    if (tvCount != null) {
                        tvCount.setText(String.valueOf(snapshots.size()));
                    }
                });
        db.collection("patients")
                .whereEqualTo("hospital_id", hospitalId)
                .whereEqualTo("isAdmitted", "true")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    TextView tvCount = admittedSection.findViewById(R.id.admitted_count);
                    if (tvCount != null) {
                        tvCount.setText(String.valueOf(snapshots.size()));
                    }
                });
        db.collection("treatments")
                .whereEqualTo("hospital_id", hospitalId)
                .whereEqualTo("type", TreatmentType.OPERATION.name()) // Use .name() if type is stored as String in DB
                .whereGreaterThanOrEqualTo("start", new Timestamp(startOfDay))
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    TextView tvCount = operationsSection.findViewById(R.id.operations_count);
                    if (tvCount != null) {
                        tvCount.setText(String.valueOf(snapshots.size()));
                    }
                });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Always remove listeners to prevent memory leaks
        if (dashboardListener != null) {
            dashboardListener.remove();
        }
    }
}