package com.example.medsync.activities.receptionist;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.example.medsync.R;
import com.example.medsync.model.Bill;
import com.example.medsync.model.Patient;
import com.example.medsync.model.Room;
import com.example.medsync.model.Treatment;
import com.example.medsync.model.enums.TreatmentType;
import com.example.medsync.utils.BaseActivity;
import com.example.medsync.utils.ViewUtils;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdmitPatient extends BaseActivity {

    private FirebaseFirestore db;
    private String hospitalId, patientId, patientName;
    private Treatment admissionTreatment = new Treatment();
    private Room selectedRoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admit_patient);
        setupBaseActivityNavbar("R", "Admit Patient");
        setupBaseActivityFooter("rolebased", "R");

        db = FirebaseFirestore.getInstance();
        hospitalId = getSharedPreferences("medsync_prefs", MODE_PRIVATE).getString("hospital_id", null);

        // Data from previous activity (Patient Selection/Details)
        patientId = getIntent().getStringExtra("patient_id");
        patientName = getIntent().getStringExtra("patient_name");

        if (patientId == null || hospitalId == null) {
            Toast.makeText(this, "Critical Error: IDs missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initUI();
    }

    private void initUI() {
        // 1. Patient Name (View Only)
        setupViewableField(findViewById(R.id.view_patient_name), R.drawable.ic_filled_user, patientName);

        // 2. Doctor Picker
        View cardDoc = findViewById(R.id.card_doctor);
        ViewUtils.setupEditableInfoCard(this, cardDoc, R.drawable.ic_doctor, "Assigned Doctor", "Select Doctor", v -> {});
        cardDoc.findViewById(R.id.btn_edit).setOnClickListener(v -> fetchAndPicker("doctors", "name", cardDoc, true));

        // 3. Assistant Picker
        View cardAsst = findViewById(R.id.card_assistant);
        ViewUtils.setupEditableInfoCard(this, cardAsst, R.drawable.ic_filled_user, "Assistant", "Select Assistant", v -> {});
        cardAsst.findViewById(R.id.btn_edit).setOnClickListener(v -> fetchAndPicker("assistants", "name", cardAsst, false));

        // 4. Room Picker
        View cardRoom = findViewById(R.id.card_room);
        ViewUtils.setupEditableInfoCard(this, cardRoom, R.drawable.ic_landline, "Allocate Room", "Select Vacant Room", v -> {});
        cardRoom.findViewById(R.id.btn_edit).setOnClickListener(v -> fetchVacantRooms(cardRoom));

        findViewById(R.id.btn_confirm_admission).setOnClickListener(v -> finalizeAdmission());
    }

    private void fetchAndPicker(String collection, String field, View card, boolean isDoctor) {
        ViewUtils.setInputState(this, card, "LOADING");
        db.collection(collection).whereEqualTo("hospital_id", hospitalId).get()
                .addOnSuccessListener(query -> {
                    List<String> names = new ArrayList<>();
                    List<String> ids = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        names.add(doc.getString(field));
                        ids.add(doc.getId());
                    }
                    ViewUtils.setInputState(this, card, "IDLE");
                    new AlertDialog.Builder(this).setTitle("Select " + collection)
                            .setItems(names.toArray(new String[0]), (d, which) -> {
                                if (isDoctor) {
                                    admissionTreatment.examiner_id = ids.get(which);
                                    admissionTreatment.examiner_name = names.get(which);
                                } else {
                                    admissionTreatment.assistant_id = ids.get(which);
                                }
                                ((TextView) card.findViewById(R.id.edit_text_input)).setText(names.get(which));
                            }).show();
                });
    }

    private void fetchVacantRooms(View card) {
        ViewUtils.setInputState(this, card, "LOADING");
        db.collection("hospitals").document(hospitalId).collection("rooms")
                .whereEqualTo("isOccupied", false).get()
                .addOnSuccessListener(query -> {
                    List<Room> rooms = query.toObjects(Room.class);
                    String[] labels = new String[rooms.size()];
                    for (int i = 0; i < rooms.size(); i++) {
                        labels[i] = "Room " + rooms.get(i).room_no + " (" + rooms.get(i).type + ")";
                    }
                    ViewUtils.setInputState(this, card, "IDLE");
                    new AlertDialog.Builder(this).setTitle("Select Room")
                            .setItems(labels, (d, which) -> {
                                selectedRoom = rooms.get(which);
                                selectedRoom.room_id = query.getDocuments().get(which).getId();
                                ((TextView) card.findViewById(R.id.edit_text_input)).setText("Room " + selectedRoom.room_no);
                            }).show();
                });
    }

    private void finalizeAdmission() {
        if (admissionTreatment.examiner_id == null || selectedRoom == null) {
            Toast.makeText(this, "Doctor and Room must be selected", Toast.LENGTH_SHORT).show();
            return;
        }

        WriteBatch batch = db.batch();
        String dateNow = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

        // 1. Prepare Treatment Object (MEDICATION type as requested)
        admissionTreatment.patient_id = patientId;
        admissionTreatment.patient_name = patientName;
        admissionTreatment.hospital_id = hospitalId;
        admissionTreatment.room_id = selectedRoom.room_id;
        admissionTreatment.room_no = selectedRoom.room_no;
        admissionTreatment.type = TreatmentType.MEDICATION.getDisplayName();
        admissionTreatment.status = "ONGOING";
        admissionTreatment.setTimestamp(Timestamp.now());

        // Create a reference for the new treatment to link it with the bill
        com.google.firebase.firestore.DocumentReference treatmentRef = db.collection("hospitals")
                .document(hospitalId).collection("treatments").document();
        batch.set(treatmentRef, admissionTreatment);

        // 2. Initialize New Bill for this Admission
        Bill initialBill = new com.example.medsync.model.Bill();
        initialBill.patient_id = patientId;
        initialBill.hospital_id = hospitalId;
        initialBill.treatment_id = treatmentRef.getId();
        initialBill.status = "PENDING";
        initialBill.total_amount = 0.0;
        initialBill.generated_at = Timestamp.now();
        // Initial items map is already initialized to empty in Bill.java

        batch.set(db.collection("hospitals").document(hospitalId).collection("bills").document(), initialBill);

        // 3. Update Patient Document
        batch.update(db.collection("patients").document(patientId),
                "isAdmitted", true,
                "room_id", selectedRoom.room_id,
                "room_no", selectedRoom.room_no,
                "doctor_id", admissionTreatment.examiner_id,
                "admittedOn", dateNow);

        // 4. Update Room Document
        batch.update(db.collection("hospitals").document(hospitalId).collection("rooms").document(selectedRoom.room_id),
                "isOccupied", true, "patient_id", patientId);

        // Commit all changes
        batch.commit().addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Patient Admitted & Bill Initialized Successfully", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    private void setupViewableField(View view, int icon, String value) {
        ((TextView) view.findViewById(R.id.tv_field)).setText(value != null ? value : "N/A");
        ((android.widget.ImageView) view.findViewById(R.id.icon_start)).setImageResource(icon);
    }
}