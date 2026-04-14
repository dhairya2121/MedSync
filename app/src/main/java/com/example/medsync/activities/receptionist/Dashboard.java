package com.example.medsync.activities.receptionist;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.medsync.R;
import com.example.medsync.model.Patient;
import com.example.medsync.model.Treatment;
import com.example.medsync.model.enums.TreatmentType;
import com.example.medsync.utils.BaseActivity;
import com.example.medsync.utils.ViewUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.text.SimpleDateFormat; // Add this import
import java.util.Locale;           // Add this import


public class Dashboard extends BaseActivity {
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db;

    private ListenerRegistration dashboardListener;
    private List<ListenerRegistration> subListeners = new ArrayList<>();
    private String currentHospitalId = null;

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
        LinearLayout manageUsersSection = findViewById(R.id.manage_users_section);
        MaterialCardView appointmentsSection = findViewById(R.id.btnSectionAppointments);
        MaterialCardView admittedSection = findViewById(R.id.btnSectionAdmitted);
        MaterialCardView operationsSection = findViewById(R.id.btnSectionOperations);

        btnAddHospital.setOnClickListener(v -> startActivity(new Intent(this, SearchHospitals.class)));

        // Navigation to Manage Appointments
        appointmentsSection.setOnClickListener(v -> redirectToActivity(ManageAppointments.class));
        admittedSection.setOnClickListener(v -> redirectToActivity(ManageAdmitted.class));
        operationsSection.setOnClickListener(v->redirectToActivity(ManageOperations.class));
        if (user != null) {
            dashboardListener = db.collection("receptionists").document(user.getUid())
                    .addSnapshotListener((snapshot, e) -> {
                        if (e != null || snapshot == null || !snapshot.exists()) return;

                        String hospitalId = snapshot.getString("hospital_id");

                        if (ViewUtils.isValidUid(hospitalId)) {
                            dashboardSection.setVisibility(View.VISIBLE);
                            btnAddHospital.setVisibility(View.GONE);
                            dashboardWatermark.setVisibility(View.GONE);

                            // Only reload all listeners if the hospital context actually changes
                            if (!hospitalId.equals(currentHospitalId)) {
                                currentHospitalId = hospitalId;
                                getSharedPreferences("medsync_prefs", MODE_PRIVATE).edit().putString("hospital_id", hospitalId).apply();
                                removeSubListeners(); // Clean old ones
                                updateManageUsersUI(hospitalId, manageUsersSection);
                                updateOverviewUI(hospitalId, appointmentsSection, admittedSection, operationsSection);
                            }
                        } else {
                            currentHospitalId = null;
                            dashboardSection.setVisibility(View.GONE);
                            btnAddHospital.setVisibility(View.VISIBLE);
                            dashboardWatermark.setVisibility(View.VISIBLE);
                            removeSubListeners();
                        }
                    });
        }
    }

    private void removeSubListeners() {
        for (ListenerRegistration reg : subListeners) {
            if (reg != null) reg.remove();
        }
        subListeners.clear();
    }

    public void updateManageUsersUI(String hospitalId, LinearLayout manageUsersSection) {
        // Hospital Name Listener
        subListeners.add(db.collection("hospitals").document(hospitalId)
                .addSnapshotListener((doc, e) -> {
                    if (doc != null && doc.exists()) {
                        TextView hospitalNameTextView = manageUsersSection.findViewById(R.id.tv_hospital_name);
                        hospitalNameTextView.setText(doc.getString("legal_name"));
                    }
                }));

        TextView tvPatientCount = manageUsersSection.findViewById(R.id.patient_count);
        TextView tvCareTakersCount = manageUsersSection.findViewById(R.id.care_taker_count);
        TextView tvAssistantCount = manageUsersSection.findViewById(R.id.assistant_count);
        TextView tvDoctorCount = manageUsersSection.findViewById(R.id.doctor_count);

        // Patients Count
        subListeners.add(db.collection("patients").whereEqualTo("hospital_id", hospitalId)
                .addSnapshotListener((s, e) -> { if (s != null) tvPatientCount.setText(String.valueOf(s.size())); }));

        // Caretakers (Note: CollectionGroup requires a Firestore Index to be created via the link in Logcat)
        subListeners.add(db.collectionGroup("careTakers").whereEqualTo("hospital_id", hospitalId)
                .addSnapshotListener((s, e) -> { if (s != null) tvCareTakersCount.setText(String.valueOf(s.size())); }));

        // Assistants
        subListeners.add(db.collection("assistants").whereEqualTo("hospital_id", hospitalId)
                .addSnapshotListener((s, e) -> { if (s != null) tvAssistantCount.setText(String.valueOf(s.size())); }));

        // Doctors
        subListeners.add(db.collection("doctors").whereEqualTo("hospital_id", hospitalId)
                .addSnapshotListener((s, e) -> { if (s != null) tvDoctorCount.setText(String.valueOf(s.size())); }));

        findViewById(R.id.btnUsersPatients).setOnClickListener(v -> redirectToActivity(ManagePatients.class));
    }


    public void updateOverviewUI(String hospitalId, MaterialCardView appointmentsSection, MaterialCardView admittedSection, MaterialCardView operationsSection) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());

        // FIX: Capture startOfTodayStr BEFORE changing the calendar to the end of the day
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        String startOfTodayStr = sdf.format(cal.getTime());

        // Now set to End of Day (23:59:59)
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        String endOfTodayStr = sdf.format(cal.getTime());

        // 1. Appointments Count
        TextView tvAppointmentCount = appointmentsSection.findViewById(R.id.appointment_count);
        subListeners.add(db.collection("hospitals").document(hospitalId).collection("treatments")
                .whereEqualTo("type", TreatmentType.APPOINTMENT.name())
                .whereGreaterThanOrEqualTo("start", startOfTodayStr)
                .whereLessThanOrEqualTo("start", endOfTodayStr)
                .addSnapshotListener((s, e) -> {
                    if (s != null && tvAppointmentCount != null) {
                        tvAppointmentCount.setText(String.valueOf(s.size()));
                    }
                }));

        // 2. Currently Admitted Count
        TextView tvAdmittedCount = admittedSection.findViewById(R.id.admitted_count);
        subListeners.add(db.collection("patients")
                .whereEqualTo("hospital_id", hospitalId)
                .whereEqualTo("isAdmitted", true)
                .addSnapshotListener((s, e) -> {
                    if (s != null && tvAdmittedCount != null) tvAdmittedCount.setText(String.valueOf(s.size()));
                }));

        // 3. Daily Operations Count
        List<String> types = new ArrayList<>();
        types.add(TreatmentType.OPERATION.name());
        types.add(TreatmentType.CHECKUP.name());
        types.add(TreatmentType.TEST.name());
        types.add(TreatmentType.EMERGENCY.name());
        types.add(TreatmentType.THERAPY.name());

        TextView tvOperationsCount = operationsSection.findViewById(R.id.operations_count);
        subListeners.add(db.collection("hospitals").document(hospitalId).collection("treatments")
                .whereIn("type", types)
                .whereGreaterThanOrEqualTo("start", startOfTodayStr)
                .whereLessThanOrEqualTo("start", endOfTodayStr)
                .addSnapshotListener((s, error) -> {
                    if (error != null) {
                        Log.e("Dashboard", "Firestore error: " + error.getMessage());
                        return;
                    }
                    if (s != null && tvOperationsCount != null) {
                        tvOperationsCount.setText(String.valueOf(s.size()));
                    }
                }));
    }
    public void redirectToActivity(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        overridePendingTransition(0, 0);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dashboardListener != null) dashboardListener.remove();
        removeSubListeners();
    }


}