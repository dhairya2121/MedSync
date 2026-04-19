package com.example.medsync.activities.receptionist;

import android.app.TimePickerDialog;
import android.os.Bundle;import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.example.medsync.R;
import com.example.medsync.model.Room;
import com.example.medsync.model.Treatment;
import com.example.medsync.utils.BaseActivity;
import com.example.medsync.utils.ViewUtils;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ScheduleOperation extends BaseActivity {

    private FirebaseFirestore db;
    private String hospitalId;
    private Treatment treatment = new Treatment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_operation);
        setupBaseActivityNavbar("R", "Schedule");
        setupBaseActivityFooter("rolebased", "R");

        db = FirebaseFirestore.getInstance();
        hospitalId = getSharedPreferences("medsync_prefs", MODE_PRIVATE).getString("hospital_id", null);

        // Get data passed from AdmittedPatientDetails
        treatment.patient_id = getIntent().getStringExtra("PATIENT_ID");
        treatment.patient_name = getIntent().getStringExtra("PATIENT_NAME");
        treatment.examiner_name = getIntent().getStringExtra("DOCTOR_NAME");
        treatment.examiner_id=getIntent().getStringExtra("DOCTOR_ID");
        initUI();
    }

    private void initUI() {
        // Set Viewable Fields (Patient and Doctor)
        setupViewableField(findViewById(R.id.view_patient_name), R.drawable.ic_filled_user, treatment.patient_name);
        setupViewableField(findViewById(R.id.view_doctor_name), R.drawable.ic_doctor, "Dr. " + treatment.examiner_name);

        // Editable Layouts
        View cardType = findViewById(R.id.card_op_type);
        View cardRoom = findViewById(R.id.card_room_no);
        View cardStart = findViewById(R.id.card_start_time);
        View cardEnd = findViewById(R.id.card_end_time);

        // 1. Operation Type Picker
        ViewUtils.setupEditableInfoCard(this, cardType, R.drawable.ic_operation_theatre, "Type", "Select Type", val -> {});
        cardType.findViewById(R.id.btn_edit).setOnClickListener(v -> showTypePicker(cardType));

        // 2. Room Selection (Fetched from DB)
        ViewUtils.setupEditableInfoCard(this, cardRoom, R.drawable.ic_landline, "Room No", "Select Room", val -> {});
        cardRoom.findViewById(R.id.btn_edit).setOnClickListener(v -> fetchAndShowVacantRooms(cardRoom));

        // 3. Date & Time Pickers
        ViewUtils.setupEditableInfoCard(this, cardStart, R.drawable.ic_clock, "Start Time", "Select Date & Time", val -> {});
        cardStart.findViewById(R.id.btn_edit).setOnClickListener(v -> showDateTimePicker(cardStart, true));

        ViewUtils.setupEditableInfoCard(this, cardEnd, R.drawable.ic_clock, "End Time", "Select Date & Time", val -> {});
        cardEnd.findViewById(R.id.btn_edit).setOnClickListener(v -> showDateTimePicker(cardEnd, false));
        findViewById(R.id.btn_save_operation).setOnClickListener(v -> saveToFirestore());
    }

    private void fetchAndShowVacantRooms(View cardRoom) {
        ViewUtils.setInputState(this, cardRoom, "LOADING");

        db.collection("hospitals").document(hospitalId)
                .collection("rooms")
                .whereEqualTo("isOccupied", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Room> vacantRooms = new ArrayList<>();
                    List<String> roomLabels = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Room room = doc.toObject(Room.class);
                        vacantRooms.add(room);
                        roomLabels.add("Room " + room.room_no + " (" + room.type + ")");
                    }

                    ViewUtils.setInputState(this, cardRoom, "IDLE");

                    if (vacantRooms.isEmpty()) {
                        Toast.makeText(this, "No vacant rooms available", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    new AlertDialog.Builder(this)
                            .setTitle("Select Vacant Room")
                            .setItems(roomLabels.toArray(new String[0]), (dialog, which) -> {
                                Room selected = vacantRooms.get(which);
                                treatment.room_no = selected.room_no;
                                treatment.room_id = selected.room_id; // If needed for reference
                                ((TextView) cardRoom.findViewById(R.id.edit_text_input)).setText(String.valueOf(selected.room_no));
                            })
                            .show();
                })
                .addOnFailureListener(e -> {
                    ViewUtils.setInputState(this, cardRoom, "ERROR");
                    Toast.makeText(this, "Failed to fetch rooms", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupViewableField(View view, int icon, String value) {
        ((TextView) view.findViewById(R.id.tv_field)).setText(value != null ? value : "N/A");
        ((android.widget.ImageView) view.findViewById(R.id.icon_start)).setImageResource(icon);
    }

    private void showTypePicker(View card) {
        String[] types = {"SURGERY", "DIAGNOSTIC", "THERAPY", "EMERGENCY"};
        new AlertDialog.Builder(this).setTitle("Select Type").setItems(types, (d, which) -> {
            treatment.type = types[which];
            ((TextView) card.findViewById(R.id.edit_text_input)).setText(treatment.type);
        }).show();
    }

    private void showDateTimePicker(View card, boolean isStart) {
        Calendar currentCalendar = Calendar.getInstance();

        // 1. Pick the Date
        new android.app.DatePickerDialog(this, (datePicker, year, month, day) -> {
            Calendar selectedCalendar = Calendar.getInstance();
            selectedCalendar.set(Calendar.YEAR, year);
            selectedCalendar.set(Calendar.MONTH, month);
            selectedCalendar.set(Calendar.DAY_OF_MONTH, day);

            // 2. Pick the Time
            new TimePickerDialog(this, (timePicker, hour, minute) -> {
                selectedCalendar.set(Calendar.HOUR_OF_DAY, hour);
                selectedCalendar.set(Calendar.MINUTE, minute);
                selectedCalendar.set(Calendar.SECOND, 0);
                selectedCalendar.set(Calendar.MILLISECOND, 0);

                // 3. Create the Timestamp
                Timestamp resultTimestamp = new Timestamp(selectedCalendar.getTime());

                // 4. Update Model
                if (isStart) {
                    treatment.start = resultTimestamp;
                } else {
                    treatment.end = resultTimestamp;
                }

                // 5. Update UI Display (Format: 19-Apr-2024, 10:30 AM)
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MMM-yyyy, hh:mm a", Locale.getDefault());
                String displayString = sdf.format(selectedCalendar.getTime());
                ((TextView) card.findViewById(R.id.edit_text_input)).setText(displayString);

            }, currentCalendar.get(Calendar.HOUR_OF_DAY), currentCalendar.get(Calendar.MINUTE), false).show();

        }, currentCalendar.get(Calendar.YEAR), currentCalendar.get(Calendar.MONTH), currentCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void saveToFirestore() {
        if (treatment.type == null || treatment.room_no == 0 || treatment.start == null || treatment.end == null) {
            Toast.makeText(this, "Please fill all fields, including End Time", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Generate the list of 30-minute slots required for this operation
        List<Timestamp> requiredSlots = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.setTime(treatment.start.toDate());

        // Standardize: clear seconds/millis for comparison
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        while (cal.getTime().before(treatment.end.toDate())) {
            requiredSlots.add(new Timestamp(cal.getTime()));
            cal.add(Calendar.MINUTE, 30);
        }

        // 2. Check Doctor's availability
        db.collection("doctors").document(treatment.examiner_id).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                com.example.medsync.model.Doctor doctor = doc.toObject(com.example.medsync.model.Doctor.class);

                if (doctor != null && doctor.booked_slots != null) {
                    for (com.example.medsync.model.BookedSlot booked : doctor.booked_slots) {
                        for (Timestamp req : requiredSlots) {
                            // Compare seconds to avoid nanosecond mismatch
                            if (booked.start.getSeconds() == req.getSeconds()) {
                                Toast.makeText(this, "Doctor is already busy at " +
                                                new java.text.SimpleDateFormat("hh:mm a", Locale.US).format(req.toDate()),
                                        Toast.LENGTH_LONG).show();
                                return; // EXIT: Slot is taken
                            }
                        }
                    }
                }

                // 3. If code reaches here, doctor is FREE. Proceed to save.
                performFinalSave(requiredSlots);
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Scheduling failed", Toast.LENGTH_SHORT).show());
    }

    private void performFinalSave(List<Timestamp> requiredSlots) {
        treatment.hospital_id = hospitalId;
        treatment.status = "UPCOMING";
        treatment.setTimestamp(Timestamp.now());

        // A. Add the treatment record
        db.collection("hospitals").document(hospitalId).collection("treatments")
                .add(treatment)
                .addOnSuccessListener(ref -> {
                    String treatmentId = ref.getId();

                    // B. Link all booked blocks to this treatment ID in doctor's profile
                    List<com.example.medsync.model.BookedSlot> bookedSlotsToUpdate = new ArrayList<>();
                    for (Timestamp ts : requiredSlots) {
                        bookedSlotsToUpdate.add(new com.example.medsync.model.BookedSlot(ts, treatmentId));
                    }

                    db.collection("doctors").document(treatment.examiner_id)
                            .update("booked_slots", FieldValue.arrayUnion(bookedSlotsToUpdate.toArray()))
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Operation Scheduled Successfully", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}