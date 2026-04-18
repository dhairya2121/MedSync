package com.example.medsync.activities.doctor;import static com.example.medsync.utils.ViewUtils.isValidUid;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medsync.R;
import com.example.medsync.adapters.PatientAdapter;
import com.example.medsync.model.Doctor;
import com.example.medsync.model.Patient;
import com.example.medsync.model.Treatment;
import com.example.medsync.model.enums.SpecializationType;
import com.example.medsync.utils.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Dashboard extends BaseActivity {
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db;

    private View dashboardSection;
    private MaterialButton btnAddHospital;
    private ImageView dashboardWatermark;
    private MaterialCardView hospitalHeroCard;

    private ListenerRegistration doctorListener;
    private ListenerRegistration appointmentsListener;

    private RecyclerView rvAppointments;
    private PatientAdapter patientAdapter; // Switched to PatientAdapter
    private List<Patient> patientsForToday = new ArrayList<>();
    private TextView tvNoAppointments;
    private String currentHospitalId;
    private long selectedDateMillis;

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
        tvNoAppointments = findViewById(R.id.tvNoAppointments);

        // 1. Setup RecyclerView with PatientAdapter
        rvAppointments = findViewById(R.id.rvAppointments);
        rvAppointments.setLayoutManager(new LinearLayoutManager(this));

        // Pass a listener to handle clicking on an appointment card
        // Inside Dashboard.java -> onCreate
        patientAdapter = new PatientAdapter(patientsForToday, patient -> {    // Find the treatment ID for this patient from the filtered slots
            db.collection("hospitals").document(currentHospitalId)
                    .collection("treatments")
                    .whereEqualTo("patient_id", patient.id)
                    .whereEqualTo("examiner_id", user.getUid())
                    .whereEqualTo("status", "UPCOMING") // or ONGOING
                    .limit(1).get().addOnSuccessListener(query -> {
                        if (!query.isEmpty()) {
                            Intent intent = new Intent(this, AppointmentDetails.class);
                            intent.putExtra("treatment_id", query.getDocuments().get(0).getId());
                            intent.putExtra("hospital_id", currentHospitalId);
                            intent.putExtra("patient_id", patient.id);
                            startActivity(intent);
                        }
                    });
        });
        rvAppointments.setAdapter(patientAdapter);

        setupBaseActivityFooter("home", "D");

        // 2. Initialize Calendar logic
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        selectedDateMillis = cal.getTimeInMillis();

        CalendarView calendarView = findViewById(R.id.calendarView);
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar newCal = Calendar.getInstance();
            newCal.set(year, month, dayOfMonth, 0, 0, 0);
            newCal.set(Calendar.MILLISECOND, 0);
            selectedDateMillis = newCal.getTimeInMillis();
            Toast.makeText(this,"Date Changed",Toast.LENGTH_SHORT).show();
            if (isValidUid(currentHospitalId)) {
                fetchAppointmentsForDate();
            }
        });

        btnAddHospital.setOnClickListener(v -> startActivity(new Intent(this, SearchHospitals.class)));
        startDoctorListener();

    }

    private void fetchAppointmentsForDate() {
        if (appointmentsListener != null) appointmentsListener.remove();
        if (user == null || currentHospitalId == null) return;

        // 1. Define the selected day boundaries
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(selectedDateMillis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        Date startOfDay = cal.getTime();

        cal.add(Calendar.DAY_OF_MONTH, 1);
        Date endOfDay = cal.getTime();

        // 2. Fetch Doctor's booked_slots from their profile
        db.collection("doctors").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    Doctor d = doc.toObject(Doctor.class);
                    if (d == null || d.booked_slots == null || d.booked_slots.isEmpty()) {
                        showNoAppointments();
                        return;
                    }

                    // 3. Filter slots belonging to the selected date
                    List<String> treatmentIds = new ArrayList<>();
                    for (com.example.medsync.model.BookedSlot slot : d.booked_slots) {
                        Date slotDate = slot.start.toDate();
                        if (slotDate.compareTo(startOfDay) >= 0 && slotDate.before(endOfDay)) {
                            treatmentIds.add(slot.treatment_id);
                        }
                    }

                    if (treatmentIds.isEmpty()) {
                        showNoAppointments();
                    } else {
                        // 4. Fetch actual Treatment docs to get patient_ids
                        fetchTreatmentsByIds(treatmentIds);
                    }
                });
    }

    private void fetchTreatmentsByIds(List<String> treatmentIds) {
        // Firestore 'whereIn' supports up to 30 IDs per query
        db.collection("hospitals").document(currentHospitalId)
                .collection("treatments")
                .whereIn(FieldPath.documentId(), treatmentIds)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> patientIds = new ArrayList<>();
                    for (var doc : queryDocumentSnapshots) {
                        String pId = doc.getString("patient_id");
                        if (pId != null && !patientIds.contains(pId)) {
                            patientIds.add(pId);
                        }
                    }

                    if (patientIds.isEmpty()) {
                        showNoAppointments();
                    } else {
                        fetchPatientDetails(patientIds);
                    }
                });
    }

    private void fetchPatientDetails(List<String> patientIds) {
        // 5. Fetch actual Patient objects (Initial, Name, Gender, Age)
        db.collection("patients")
                .whereIn(FieldPath.documentId(), patientIds)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    patientsForToday.clear();
                    for (var doc : queryDocumentSnapshots) {
                        Patient p = doc.toObject(Patient.class);
                        if (p != null) {
                            p.id = doc.getId();
                            patientsForToday.add(p);
                        }
                    }

                    if (patientsForToday.isEmpty()) {
                        showNoAppointments();
                    } else {
                        tvNoAppointments.setVisibility(View.GONE);
                        patientAdapter.notifyDataSetChanged();
                        Toast.makeText(this, "Appointments Loaded", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showNoAppointments() {
        patientsForToday.clear();
        patientAdapter.notifyDataSetChanged();
        tvNoAppointments.setVisibility(View.VISIBLE);
    }

    private void startDoctorListener() {
        if (user == null) return;
        doctorListener = db.collection("doctors").document(user.getUid())
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;

                    String spec = snapshot.getString("specialization");
                    setupBaseActivityNavbar("D", spec != null ? SpecializationType.valueOf(spec).getDisplayName() : "Doctor");

                    String hospitalId = snapshot.getString("hospital_id");
                    if (isValidUid(hospitalId)) {
                        this.currentHospitalId = hospitalId;
                        dashboardSection.setVisibility(View.VISIBLE);
                        btnAddHospital.setVisibility(View.GONE);
                        dashboardWatermark.setVisibility(View.GONE);
                        fetchHospitalDetails(hospitalId);
                        updateAdmittedCount(hospitalId);
                        fetchAppointmentsForDate();
                    } else {
                        currentHospitalId = null;
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
                        hospitalHeroCard.setOnClickListener(v -> startActivity(new Intent(this, ManageAdmitted.class)));
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
        if (appointmentsListener != null) appointmentsListener.remove();
    }
}