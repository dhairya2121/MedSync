package com.example.medsync.activities.patient;

import static com.example.medsync.utils.ViewUtils.isValidUid;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CalendarView;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.medsync.R;
import com.example.medsync.model.Bill;
import com.example.medsync.model.BookedSlot;
import com.example.medsync.model.Doctor;
import com.example.medsync.model.TimeSlot;
import com.example.medsync.model.Treatment;
import com.example.medsync.model.enums.TreatmentStatus;
import com.example.medsync.model.enums.TreatmentType;
import com.example.medsync.utils.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ScheduleAppointment extends BaseActivity {

    private FirebaseFirestore db;
    private String hospitalId, doctorName, doctorId;
    private List<BookedSlot> globalBookedSlots;
    private GridLayout gridTimeSlots;
    private long selectedDateMillis;
    private MaterialButton lastSelectedBtn = null;
    private Doctor currentDoctor;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

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

        // --- FIX: RESTRICT PREVIOUS DATES ---
        // Get the current time in milliseconds
        long today = System.currentTimeMillis();
        // Set the minimum date to today (Subtract 1000ms to avoid edge-case
        // synchronization issues where the current millisecond is slightly past the check)
        calendarView.setMinDate(today - 1000);
        // ------------------------------------

        doctorId = getIntent().getStringExtra("doctor_id");

        // Initialize with today's date at 00:00:00
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        selectedDateMillis = cal.getTimeInMillis();

        // Ensure the initial visual selection matches the logic
        calendarView.setDate(selectedDateMillis);

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar newCal = Calendar.getInstance();
            newCal.set(year, month, dayOfMonth, 0, 0, 0);
            newCal.set(Calendar.MILLISECOND, 0);
            selectedDateMillis = newCal.getTimeInMillis();

            // Refresh UI whenever date changes
            if (currentDoctor != null) {
                filterAndRenderSlots();
            }
        });

        if (isValidUid(doctorId)) {
            fetchDoctorData();
        }

        findViewById(R.id.btn_confirm_appointment).setOnClickListener(v -> saveAppointment());
    }

    private void fetchDoctorData() {
        db.collection("doctors").document(doctorId).get()
                .addOnSuccessListener(doc -> {
                    currentDoctor = doc.toObject(Doctor.class);
                    if (currentDoctor != null) {
                        this.hospitalId = currentDoctor.hospital_id;
                        this.doctorName = currentDoctor.name;
                        this.globalBookedSlots = currentDoctor.booked_slots != null ? currentDoctor.booked_slots : new ArrayList<BookedSlot>();
                        filterAndRenderSlots();
                    }
                });
    }

    private void filterAndRenderSlots() {
        if (currentDoctor == null) return;

        gridTimeSlots.removeAllViews();
        lastSelectedBtn = null;

        // Determine the time window for the selected day
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(selectedDateMillis);
        Date startOfDay = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        Date endOfDay = cal.getTime();

        // Identify which slots (by start time string) are booked on THIS specific date
        List<String> bookedTimesForThisDate = new ArrayList<>();
        for (BookedSlot ts : globalBookedSlots) {
            Date bookedDate = ts.start.toDate();
            if (bookedDate.compareTo(startOfDay) >= 0 && bookedDate.before(endOfDay)) {
                bookedTimesForThisDate.add(timeFormat.format(bookedDate));
            }
        }

        renderGrid(currentDoctor.working_slots, bookedTimesForThisDate);
    }

    private void renderGrid(List<TimeSlot> workingSlots, List<String> bookedTimesForThisDate) {
        if (workingSlots == null || workingSlots.isEmpty()) {
            showEmptyState("No slots available for this doctor.");
            return;
        }

        int hardGrey = getColor(R.color.hard_grey);
        int hardGreen = getColor(R.color.hard_green);
        int softGreen = getColor(R.color.soft_green);
        int softGrey = getColor(R.color.soft_grey);

        for (TimeSlot slot : workingSlots) {
            MaterialButton btn = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);

            String startTimeStr = timeFormat.format(slot.start.toDate());
            String endTimeStr = timeFormat.format(slot.end.toDate());

            // UI Requirement: "9:00 AM - 9:30 AM"
            btn.setText(String.format("%s - %s", startTimeStr, endTimeStr));
            btn.setAllCaps(false);
            btn.setTag(slot);

            boolean isOccupied = bookedTimesForThisDate.contains(startTimeStr);

            if (!isOccupied) {
                btn.setStrokeColor(ColorStateList.valueOf(hardGrey));
                btn.setTextColor(hardGrey);
                btn.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));

                btn.setOnClickListener(v -> {
                    if (lastSelectedBtn != null) {
                        lastSelectedBtn.setBackgroundTintList(ColorStateList.valueOf(softGrey));
                        lastSelectedBtn.setTextColor(hardGrey);
                        lastSelectedBtn.setStrokeColor(ColorStateList.valueOf(hardGrey));
                    }
                    btn.setBackgroundTintList(ColorStateList.valueOf(softGreen));
                    btn.setTextColor(hardGreen);
                    btn.setStrokeColor(ColorStateList.valueOf(hardGreen));
                    lastSelectedBtn = btn;
                });
            } else {
                btn.setEnabled(false);
                btn.setAlpha(0.6f); // Clearer "disabled" look
                btn.setStrokeColor(ColorStateList.valueOf(softGrey));
                btn.setTextColor(hardGrey);
                btn.setText(btn.getText() + "\n(Booked)");
            }
            gridTimeSlots.addView(btn);
        }
    }

    private void saveAppointment() {
        if (lastSelectedBtn == null) {
            Toast.makeText(this, "Please select a time slot", Toast.LENGTH_SHORT).show();
            return;
        }

        TimeSlot selectedSlot = (TimeSlot) lastSelectedBtn.getTag();

        // 1. Calculate the exact START Timestamp (Selected Date + Slot Start Time)
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(selectedDateMillis); // Midnight of selected day

        Calendar slotTime = Calendar.getInstance();
        slotTime.setTime(selectedSlot.start.toDate()); // Time from the slot

        cal.set(Calendar.HOUR_OF_DAY, slotTime.get(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, slotTime.get(Calendar.MINUTE));
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Timestamp startTimestamp = new Timestamp(cal.getTime());

        // 2. Calculate the exact END Timestamp (Selected Date + Slot End Time)
        // Re-using the calendar but setting to Slot End Time
        slotTime.setTime(selectedSlot.end.toDate());
        cal.set(Calendar.HOUR_OF_DAY, slotTime.get(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, slotTime.get(Calendar.MINUTE));
        Timestamp endTimestamp = new Timestamp(cal.getTime());

        // 3. Populate Treatment object
        Treatment t = new Treatment();
        t.type = TreatmentType.APPOINTMENT.name();
        t.status = TreatmentStatus.UPCOMING.name();
        t.hospital_id = hospitalId;
        t.patient_id = FirebaseAuth.getInstance().getUid();
        t.patient_name = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        t.examiner_id = doctorId;
        t.examiner_name = doctorName;

        // Use the Timestamp objects directly
        t.setTimestamp(startTimestamp); // Main searchable field
        t.start = startTimestamp;
        t.end = endTimestamp;

        // 4. Save to Firestore
        db.collection("hospitals").document(hospitalId)
                .collection("treatments").add(t)
                .addOnSuccessListener(docRef -> {
                    String treatmentId = docRef.getId();
                    BookedSlot bs = new BookedSlot(startTimestamp, treatmentId);

                    // Update doctor's booked_slots using arrayUnion
                    db.collection("doctors").document(doctorId)
                            .update("booked_slots", FieldValue.arrayUnion(bs))
                            .addOnSuccessListener(aVoid -> {

                                // Create the corresponding Bill
                                Bill b = new Bill();
                                b.treatment_id = treatmentId;
                                b.patient_id = t.patient_id;
                                b.hospital_id = hospitalId;
                                b.generated_at = Timestamp.now();
                                b.status = "PENDING";

                                if (currentDoctor != null) {
                                    b.items.put("Appointment Fee (" + doctorName + ")", (double) currentDoctor.appointmentFee);
                                }
                                b.calculateTotal();

                                db.collection("hospitals").document(hospitalId)
                                        .collection("bills").document(treatmentId)
                                        .set(b)
                                        .addOnSuccessListener(billVoid -> {
                                            db.collection("patients").document(t.patient_id)
                                                            .update("hospital_id",hospitalId)
                                                                    .addOnSuccessListener(p->{
                                                                        Toast.makeText(this, "Appointment Confirmed!", Toast.LENGTH_SHORT).show();
                                                                        finish();
                                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.e("Patient", "hosptial_id update failed", e);
                                                        finish();
                                                    });

                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("Schedule", "Bill creation failed", e);
                                            finish();
                                        });
                            });
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Booking failed", Toast.LENGTH_SHORT).show());
    }

    private void showEmptyState(String msg) {
        TextView tv = new TextView(this);
        tv.setText(msg);
        tv.setPadding(20, 50, 20, 50);
        gridTimeSlots.addView(tv);
    }
}