package com.example.medsync.activities.receptionist;

import static com.example.medsync.utils.ViewUtils.isValidUid;

import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.medsync.R;
import com.example.medsync.model.Room;
import com.example.medsync.model.enums.RoomType;
import com.example.medsync.utils.BaseActivity;
import com.example.medsync.utils.ViewUtils;
import com.google.firebase.firestore.FirebaseFirestore;

public class RoomDetails extends BaseActivity {

    private FirebaseFirestore db;
    private String hospitalId;
    private String roomId;

    private Room room = new Room();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_details);
        applyEdgeToEdgePadding(findViewById(R.id.main));
        setupBaseActivityNavbar("R", "Room Details");
        setupBaseActivityFooter("rolebased", "R");

        db         = FirebaseFirestore.getInstance();
        hospitalId = getIntent().getStringExtra("HOSPITAL_ID");
        roomId     = getIntent().getStringExtra("ROOM_ID");

        if (!isValidUid(hospitalId) || !isValidUid(roomId)) {
            Toast.makeText(this, "Invalid room reference", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Hide the global save button if it exists in layout, as we save per-card
        View saveBtn = findViewById(R.id.btn_save);
        if (saveBtn != null) saveBtn.setVisibility(View.GONE);

        loadRoomAndSetupUI();
    }

    private void loadRoomAndSetupUI() {
        db.collection("hospitals").document(hospitalId)
                .collection("rooms").document(roomId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        room = doc.toObject(Room.class);
                        if (room == null) room = new Room();
                        room.hospital_id = hospitalId;
                        room.room_id     = roomId;
                    }
                    setupUI();
                })
                .addOnFailureListener(e -> {
                    Log.e("RoomDetail", "Failed to load room", e);
                    setupUI();
                });
    }

    private void setupUI() {
        View cardNo   = findViewById(R.id.card_room_number);
        View cardType = findViewById(R.id.card_room_type);
        View cardPass = findViewById(R.id.card_room_passkey);

        // 1. Room Number - Saves on check icon click
        ViewUtils.setupEditableInfoCard(this, cardNo,
                R.drawable.ic_landline, "Room Number",
                room.room_no > 0 ? String.valueOf(room.room_no) : "",
                val -> {
                    try {
                        long newNo = Long.parseLong(val.trim());
                        updateRoomField("room_no", newNo, cardNo);
                    } catch (NumberFormatException e) {
                        ViewUtils.setInputState(this, cardNo, "ERROR");
                        Toast.makeText(this, "Invalid Number", Toast.LENGTH_SHORT).show();
                    }
                });

        // 2. Room Type - Trigger Dialog immediately on Edit Icon click
        // To fix your issue: We set up the card normally, but we also attach a click listener
        // to the edit button specifically to intercept the transition and show the dialog.
        setupRoomTypeCard(cardType);

        // 3. Passkey - Saves on check icon click
        setupPasswordField(cardPass);
        ViewUtils.setupEditableInfoCard(this, cardPass,
                R.drawable.ic_passkey, "Passkey",
                room.passkey != null ? room.passkey : "",
                val -> updateRoomField("passkey", val, cardPass));

        // 4. Delete button
        findViewById(R.id.btn_delete_room).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Delete Room")
                        .setMessage("Delete this room permanently?")
                        .setPositiveButton("Delete", (d, w) -> deleteRoomAndExit())
                        .setNegativeButton("Cancel", null)
                        .show()
        );
    }

    private void setupRoomTypeCard(View cardType) {
        ViewUtils.setupEditableInfoCard(this, cardType,
                R.drawable.ic_operation_theatre, "Room Type",
                room.type != null ? room.type : "GENERAL",
                val -> {
                    updateRoomField("type", val, cardType);
                });

        // FIX: Find the edit button inside the included layout and override its behavior
        View editBtn = cardType.findViewById(R.id.btn_edit);
        if (editBtn != null) {
            editBtn.setOnClickListener(v -> {
                // Instead of letting ViewUtils switch to EditText, show the Dialog
                showRoomTypePicker(cardType);
            });
        }
    }

    private void updateRoomField(String key, Object value, View card) {
        ViewUtils.setInputState(this, card, "LOADING");

        db.collection("hospitals").document(hospitalId)
                .collection("rooms").document(roomId)
                .update(key, value)
                .addOnSuccessListener(aVoid -> {
                    ViewUtils.setInputState(this, card, "IDLE");
                    Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("RoomDetail", "Update failed", e);
                    ViewUtils.setInputState(this, card, "ERROR");
                    Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void showRoomTypePicker(View cardType) {
        RoomType[] enumValues = RoomType.values();
        String[] types = new String[enumValues.length];
        for (int i = 0; i < enumValues.length; i++) {
            types[i] = enumValues[i].name();
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Room Type")
                .setItems(types, (dialog, which) -> {
                    String selectedType = types[which];
                    room.type = selectedType;

                    // Immediately update Firestore
                    updateRoomField("type", selectedType, cardType);

                    // Refresh the card UI with the new value
                    setupRoomTypeCard(cardType);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    ViewUtils.setInputState(this, cardType, "IDLE");
                })
                .show();
    }

    private void setupPasswordField(View card) {
        EditText et = card.findViewById(R.id.edit_text_input);
        if (et != null) {
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
    }

    private void deleteRoomAndExit() {
        db.collection("hospitals").document(hospitalId)
                .collection("rooms").document(roomId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Room deleted", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show();
                });
    }
}