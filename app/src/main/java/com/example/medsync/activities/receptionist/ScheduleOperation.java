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

        // 3. Time Pickers
        ViewUtils.setupEditableInfoCard(this, cardStart, R.drawable.ic_clock, "Start Time", "00:00", val -> {});
        cardStart.findViewById(R.id.btn_edit).setOnClickListener(v -> showTimePicker(cardStart, true));

        ViewUtils.setupEditableInfoCard(this, cardEnd, R.drawable.ic_clock, "End Time", "00:00", val -> {});
        cardEnd.findViewById(R.id.btn_edit).setOnClickListener(v -> showTimePicker(cardEnd, false));

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

    private void showTimePicker(View card, boolean isStart) {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(this, (view, hour, minute) -> {
            String time = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
            ((TextView) card.findViewById(R.id.edit_text_input)).setText(time);
            if (isStart) treatment.start = time; else treatment.end = time;
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }

    private void saveToFirestore() {
        if (treatment.type == null || treatment.room_no == 0 || treatment.start == null) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        treatment.hospital_id = hospitalId;
        treatment.status = "SCHEDULED";
        treatment.setTimestamp(Timestamp.now());

        db.collection("hospitals").document(hospitalId).collection("treatments")
                .add(treatment)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Operation Scheduled", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}