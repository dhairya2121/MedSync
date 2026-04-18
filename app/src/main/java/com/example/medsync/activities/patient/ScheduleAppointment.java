package com.example.medsync.activities.patient;

import static com.example.medsync.utils.ViewUtils.isValidUid;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.CalendarView;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.medsync.R;
import com.example.medsync.model.Doctor;
import com.example.medsync.model.TimeSlot;
import com.example.medsync.model.Treatment;
import com.example.medsync.model.enums.TreatmentStatus;
import com.example.medsync.model.enums.TreatmentType;
import com.example.medsync.utils.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ScheduleAppointment extends BaseActivity {

    private FirebaseFirestore db;
    private String doctorId, hospitalId, doctorName;
    private GridLayout gridTimeSlots;
    private long selectedDateMillis;
    private MaterialButton lastSelectedBtn = null;
    private Doctor currentDoctor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_appointment);

        applyEdgeToEdgePadding(findViewById(R.id.main));
        setupBaseActivityNavbar("P", "Schedule Appointment");
        setupBaseActivityFooter("home", "P");

        db = FirebaseFirestore.getInstance();
        gridTimeSlots = findViewById(R.id.gridTimeSlots);
        CalendarView calendarView = findViewById(R.id.calendarView);

        doctorId = getIntent().getStringExtra("doctor_id");

        // Initialize with today's date at start of day
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        selectedDateMillis = cal.getTimeInMillis();
        calendarView.setDate(selectedDateMillis);

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar newCal = Calendar.getInstance();
            newCal.set(year, month, dayOfMonth, 0, 0, 0);
            newCal.set(Calendar.MILLISECOND, 0);
            selectedDateMillis = newCal.getTimeInMillis();

            // Refresh slots whenever date changes
            if (currentDoctor != null) {
                fetchBookedSlotsAndRender(currentDoctor);
            }
        });

        if (isValidUid(doctorId)) {
            fetchDoctorData();
        }

        findViewById(R.id.btn_confirm_appointment).setOnClickListener(v -> saveAppointment());
    }

    private void fetchDoctorData() {
        db.collection("doctors").document(doctorId).get().addOnSuccessListener(doc -> {
            currentDoctor = doc.toObject(Doctor.class);
            if (currentDoctor != null) {
                this.hospitalId = currentDoctor.hospital_id;
                this.doctorName = currentDoctor.name;
                fetchBookedSlotsAndRender(currentDoctor);
            }
        });
    }

    private void fetchBookedSlotsAndRender(Doctor doctor) {
        if (hospitalId == null) return;

        // --- Loading State ---
        gridTimeSlots.removeAllViews();
        TextView tvLoading = new TextView(this);
        tvLoading.setText("Checking availability...");
        tvLoading.setPadding(20, 50, 20, 50);
        gridTimeSlots.addView(tvLoading);
        lastSelectedBtn = null;

        // Define the start and end of the specific selected day
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(selectedDateMillis);
        Date startOfDay = cal.getTime();

        cal.add(Calendar.DAY_OF_MONTH, 1);
        Date endOfDay = cal.getTime();

        db.collection("hospitals").document(hospitalId)
                .collection("treatments")
                .whereEqualTo("examiner_id", doctorId)
                .whereGreaterThanOrEqualTo("timestamp", new Timestamp(startOfDay))
                .whereLessThan("timestamp", new Timestamp(endOfDay))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> bookedStarts = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Treatment t = doc.toObject(Treatment.class);
                        if (t.start != null) bookedStarts.add(t.start);
                    }
                    renderSlots(doctor.working_slots, bookedStarts);
                })
                .addOnFailureListener(e -> {
                    Log.e("Schedule", "Error fetching bookings", e);
                    renderSlots(doctor.working_slots, new ArrayList<>());
                });
    }

    private void renderSlots(List<TimeSlot> workingSlots, List<String> bookedStarts) {
        gridTimeSlots.removeAllViews();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault());

        if (workingSlots == null || workingSlots.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No working slots defined for this doctor.");
            gridTimeSlots.addView(tvEmpty);
            return;
        }

        int hardGreyColor = getColor(R.color.hard_grey);
        int hardGreenColor = getColor(R.color.hard_green);
        int softGreenColor = getColor(R.color.soft_green);
        int softGreyColor = getColor(R.color.soft_grey);

        for (TimeSlot slot : workingSlots) {
            MaterialButton btn = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);

            String timeDisplay = sdf.format(slot.start.toDate());
            btn.setText(timeDisplay);
            btn.setAllCaps(false);
            btn.setTag(slot);

            boolean isActuallyBooked = bookedStarts.contains(timeDisplay);
            boolean isFree = Boolean.TRUE.equals(slot.free) && !isActuallyBooked;

            if (isFree) {
                btn.setStrokeColor(ColorStateList.valueOf(hardGreyColor));
                btn.setTextColor(hardGreyColor);
                btn.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));

                btn.setOnClickListener(v -> {
                    if (lastSelectedBtn != null) {
                        lastSelectedBtn.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                        lastSelectedBtn.setTextColor(hardGreyColor);
                        lastSelectedBtn.setStrokeColor(ColorStateList.valueOf(hardGreyColor));
                    }
                    btn.setBackgroundTintList(ColorStateList.valueOf(softGreenColor));
                    btn.setTextColor(hardGreenColor);
                    btn.setStrokeColor(ColorStateList.valueOf(hardGreenColor));
                    lastSelectedBtn = btn;
                });
            } else {
                btn.setEnabled(false);
                btn.setAlpha(0.6f);
                btn.setStrokeColor(ColorStateList.valueOf(softGreyColor));
                btn.setTextColor(hardGreyColor);
                if (isActuallyBooked) {
                    btn.setText(timeDisplay + "\n(Booked)");
                }
            }
            gridTimeSlots.addView(btn);
        }
    }

    private void saveAppointment() {
        if (lastSelectedBtn == null) {
            Toast.makeText(this, "Please select an available time slot", Toast.LENGTH_SHORT).show();
            return;
        }

        TimeSlot selectedSlot = (TimeSlot) lastSelectedBtn.getTag();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault());

        Treatment t = new Treatment();
        t.type = TreatmentType.APPOINTMENT.name();
        t.status = TreatmentStatus.UPCOMING.name();
        t.hospital_id = hospitalId;
        t.patient_id = FirebaseAuth.getInstance().getUid();
        t.patient_name = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        t.examiner_id = doctorId;
        t.examiner_name = doctorName;

        Calendar finalCal = Calendar.getInstance();
        finalCal.setTimeInMillis(selectedDateMillis);

        Calendar timeCal = Calendar.getInstance();
        timeCal.setTime(selectedSlot.start.toDate());

        finalCal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
        finalCal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
        finalCal.set(Calendar.SECOND, 0);
        finalCal.set(Calendar.MILLISECOND, 0);

        t.setTimestamp(new Timestamp(finalCal.getTime()));
        t.start = sdf.format(selectedSlot.start.toDate());
        t.end = sdf.format(selectedSlot.end.toDate());

        db.collection("hospitals").document(hospitalId)
                .collection("treatments").add(t)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Appointment Confirmed!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Booking failed", Toast.LENGTH_SHORT).show());
    }
}