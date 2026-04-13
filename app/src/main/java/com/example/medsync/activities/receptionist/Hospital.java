package com.example.medsync.activities.receptionist;

import static com.example.medsync.utils.ViewUtils.isValidUid;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medsync.R;
import com.example.medsync.model.Room;
import com.example.medsync.utils.BaseActivity;
import com.example.medsync.utils.ViewUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class Hospital extends BaseActivity {
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private String cachedHospitalId;
    private RecyclerView rvInfrastructure;
    private RoomAdapter roomAdapter;
    private final List<Room> roomList = new ArrayList<>();
    private ListenerRegistration receptionistListener;

    // ✅ Request code to detect when RoomDetail returns
    private static final int REQUEST_ROOM_DETAIL = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hospital);
        applyEdgeToEdgePadding(findViewById(R.id.main));
        setupBaseActivityNavbar("R", "Receptionist");
        setupBaseActivityFooter("rolebased", "R");

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        View nameCard     = findViewById(R.id.hospital_name_card_container);
        View phoneCard    = findViewById(R.id.hospital_phone_card_container);
        View landlineCard = findViewById(R.id.hospital_landline_card_container);
        View emailCard    = findViewById(R.id.hospital_email_card_container);
        View addressCard  = findViewById(R.id.hospital_address_card_container);
        View ratingCard   = findViewById(R.id.hospital_rating_card_container);
        RatingBar ratingBar = (ratingCard != null) ? ratingCard.findViewById(R.id.rating_bar) : null;

        // ── RecyclerView setup ───────────────────────────────────────────────
        rvInfrastructure = findViewById(R.id.rv_infrastructure);
        roomAdapter = new RoomAdapter(
                roomList,
                // Card click → open RoomDetail with existing room_id
                room -> openRoomDetail(room.hospital_id, room.room_id),
                // Delete icon click → confirm then delete
                room -> new AlertDialog.Builder(this)
                        .setTitle("Delete Room")
                        .setMessage("Delete Room " + room.room_no + "? This cannot be undone.")
                        .setPositiveButton("Delete", (d, w) -> deleteRoom(room))
                        .setNegativeButton("Cancel", null)
                        .show()
        );
        rvInfrastructure.setAdapter(roomAdapter);
        if (rvInfrastructure.getLayoutManager() == null) {
            rvInfrastructure.setLayoutManager(new GridLayoutManager(this, 2));
        }

        // ── Receptionist snapshot listener ───────────────────────────────────
        if (user != null) {
            receptionistListener = db.collection("receptionists").document(user.getUid())
                    .addSnapshotListener((v, error) -> {
                        if (error != null) { Log.e("Hospital", "Listen failed.", error); return; }
                        if (v != null && v.exists()) {
                            String newId = v.getString("hospital_id");
                            if (isValidUid(newId) && !newId.equals(cachedHospitalId)) {
                                cachedHospitalId = newId;
                                fetchHospitalDetails(cachedHospitalId, nameCard, phoneCard,
                                        landlineCard, emailCard, addressCard, ratingCard, ratingBar);
                            } else {
                                navigateToSearch();
                            }
                        } else {
                            Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        // ── Delete hospital button ───────────────────────────────────────────
        View deleteBtn = findViewById(R.id.btn_delete_hospital_profile);
        if (deleteBtn != null) {
            deleteBtn.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle("Delete Hospital")
                    .setMessage("This will permanently delete the hospital profile. Are you sure?")
                    .setPositiveButton("Delete", (dialog, which) -> deleteHospital())
                    .setNegativeButton("Cancel", null)
                    .show());
        }

        // ── Exit hospital button ─────────────────────────────────────────────
        View exitBtn = findViewById(R.id.btn_exit_hospital);
        if (exitBtn != null) {
            exitBtn.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle("Exit Hospital")
                    .setMessage("You will be unlinked from this hospital. Continue?")
                    .setPositiveButton("Exit", (dialog, which) -> exitHospital())
                    .setNegativeButton("Cancel", null)
                    .show());
        }

        // ── Add new room button → create draft doc first, then open RoomDetail ──
        View addNewRoomBtn = findViewById(R.id.btn_add_new_room);
        if (addNewRoomBtn != null) {
            addNewRoomBtn.setOnClickListener(v -> createDraftRoomAndOpen());
        }
    }

    // ── Creates a blank/draft Room doc in Firestore, then opens RoomDetail ───
    private void createDraftRoomAndOpen() {
        if (!isValidUid(cachedHospitalId)) {
            Toast.makeText(this, "Hospital not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build a draft room with defaults
        Room draft = new Room();
        draft.hospital_id = cachedHospitalId;
        draft.room_no     = 0;       // RoomDetail will let user set this
        draft.type        = "General";
        draft.isOccupied  = false;
        draft.patient_id  = "";
        // passkey auto-generated by Room constructor — re-generate here explicitly
        draft.passkey     = String.valueOf(new java.util.Random().nextInt(9000) + 1000);

        db.collection("hospitals")
                .document(cachedHospitalId)
                .collection("rooms")
                .add(draft)
                .addOnSuccessListener(docRef -> {
                    // ✅ Draft created — pass its ID to RoomDetail
                    openRoomDetail(cachedHospitalId, docRef.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e("Hospital", "Failed to create draft room", e);
                    Toast.makeText(this, "Could not create room, try again", Toast.LENGTH_SHORT).show();
                });
    }

    // ── Single entry point to open RoomDetail ────────────────────────────────
    private void openRoomDetail(String hospitalId, String roomId) {
        if (!isValidUid(hospitalId) || !isValidUid(roomId)) {
            Toast.makeText(this, "Invalid room data", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, RoomDetails.class);
        intent.putExtra("HOSPITAL_ID", hospitalId);
        intent.putExtra("ROOM_ID", roomId);
        startActivityForResult(intent, REQUEST_ROOM_DETAIL);
    }

    // ── Refresh rooms list when RoomDetail finishes ───────────────────────────
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ROOM_DETAIL && isValidUid(cachedHospitalId)) {
            // Always refresh — whether room was saved, deleted, or discarded
            fetchAndDisplayRooms(cachedHospitalId);
        }
    }

    // ── Fetch hospital info cards ─────────────────────────────────────────────
    public void fetchHospitalDetails(String hospitalId, View nameCard, View phoneCard,
                                     View landlineCard, View emailCard, View addressCard,
                                     View ratingCard, RatingBar ratingBar) {
        if (!isValidUid(hospitalId)) {
            navigateToSearch();
            return;
        }
        db.collection("hospitals").document(hospitalId).get()
                .addOnSuccessListener(v -> {
                    if (v.exists()) {
                        ViewUtils.setupEditableInfoCard(this, nameCard,
                                R.drawable.ic_filled_user, "Hospital Name",
                                v.contains("legal_name") ? v.getString("legal_name") : "New Hospital",
                                newValue -> updateDataEntry("legal_name", newValue, nameCard));

                        ViewUtils.setupEditableInfoCard(this, addressCard,
                                R.drawable.ic_location_pin, "Address",
                                v.contains("address") ? v.getString("address") : "New address",
                                newValue -> updateDataEntry("address", newValue, addressCard));

                        ViewUtils.setupEditableInfoCard(this, phoneCard,
                                R.drawable.ic_contact_book, "Phone",
                                v.contains("phone") ? v.getString("phone") : "9999999999",
                                newValue -> updateDataEntry("phone", newValue, phoneCard));

                        ViewUtils.setupEditableInfoCard(this, landlineCard,
                                R.drawable.ic_landline, "Landline",
                                v.contains("landline") ? v.getString("landline") : "999999",
                                newValue -> updateDataEntry("landline", newValue, landlineCard));

                        ViewUtils.setupEditableInfoCard(this, emailCard,
                                R.drawable.ic_mail, "Email",
                                v.contains("email") ? v.getString("email") : "hospital@aimsn.ac.in",
                                newValue -> updateDataEntry("email", newValue, emailCard));

                        if (ratingBar != null) {
                            Double rating = v.getDouble("rating");
                            Long count    = v.getLong("reviewCount");
                            if (rating == null || count == null) {
                                ViewUtils.setupRatingBar(ratingBar, 0.0, 0);
                            } else {
                                ratingCard.setVisibility(View.VISIBLE);
                                ViewUtils.setupRatingBar(ratingBar, rating, count);
                            }
                        }

                        fetchAndDisplayRooms(hospitalId);
                    } else {
                        navigateToSearch();
                    }
                })
                .addOnFailureListener(e -> Log.e("Hospital", "Error fetching details", e));
    }

    // ── Fetch rooms subcollection ─────────────────────────────────────────────
    // ── Fetch rooms subcollection with error handling ─────────────────────────────
    private void fetchAndDisplayRooms(String hospitalId) {
        if (!isValidUid(hospitalId)) return;

        db.collection("hospitals").document(hospitalId).collection("rooms")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Room> results = new ArrayList<>();
                    Log.d("Hospital", "Total docs found in Firestore: " + querySnapshot.size());

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        try {
                            Room room = doc.toObject(Room.class);
                            if (room != null) {
                                room.hospital_id = hospitalId;
                                room.room_id     = doc.getId();
                                results.add(room);
                            }
                        } catch (Exception e) {
                            // This happens if Firestore data type doesn't match Java model (e.g. String vs Long)
                            Log.e("Hospital", "Error parsing room doc: " + doc.getId() + ". Check if room_no is a Number in DB.", e);
                        }
                    }

                    if (results.isEmpty()) {
                        Toast.makeText(this, "No valid rooms found", Toast.LENGTH_SHORT).show();
                    }

                    // Update the adapter and ensure UI refreshes
                    roomAdapter.updateData(results);
                    Log.d("Hospital", "Rooms successfully parsed and displayed: " + results.size());
                })
                .addOnFailureListener(e -> {
                    Log.e("Hospital", "Failed to load rooms", e);
                    Toast.makeText(this, "Failed to load rooms", Toast.LENGTH_SHORT).show();
                });
    }
    // ── Delete a single room by doc ID ───────────────────────────────────────
    private void deleteRoom(Room room) {
        if (!isValidUid(cachedHospitalId) || !isValidUid(room.room_id)) return;

        db.collection("hospitals").document(cachedHospitalId)
                .collection("rooms").document(room.room_id)
                .delete()
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "Room " + room.room_no + " deleted", Toast.LENGTH_SHORT).show();
                    fetchAndDisplayRooms(cachedHospitalId);
                })
                .addOnFailureListener(e -> {
                    Log.e("Hospital", "Failed to delete room", e);
                    Toast.makeText(this, "Failed to delete room", Toast.LENGTH_SHORT).show();
                });
    }

    // ── Adapter ───────────────────────────────────────────────────────────────
    public interface OnRoomClickListener {
        void onRoomClick(Room room);
    }

    // ── Fixed Adapter Implementation ──────────────────────────────────────────────
    private static class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.ViewHolder> {
        private final List<Room> rooms;
        private final OnRoomClickListener clickListener;
        private final OnRoomClickListener deleteListener;

        public RoomAdapter(List<Room> rooms, OnRoomClickListener clickListener, OnRoomClickListener deleteListener) {
            this.rooms = rooms;
            this.clickListener = clickListener;
            this.deleteListener = deleteListener;
        }

        // ✅ This method is crucial for refreshing the grid
        public void updateData(List<Room> newRooms) {
            this.rooms.clear();
            this.rooms.addAll(newRooms);
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_room_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Room room = rooms.get(position);

            // Bind your data to the views (example)
            holder.tvRoomNo.setText(String.valueOf(room.room_no));
            holder.tvType.setText(room.type);

            holder.itemView.setOnClickListener(v -> clickListener.onRoomClick(room));

            // Delete button logic
            if (holder.ivDelete != null) {
                holder.ivDelete.setOnClickListener(v -> deleteListener.onRoomClick(room));
            }
        }

        @Override
        public int getItemCount() {
            return rooms.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvRoomNo, tvType;
            ImageView ivDelete;

            public ViewHolder(View itemView) {
                super(itemView);
                tvRoomNo = itemView.findViewById(R.id.tv_count); // Based on your item_room_card.xml
                tvType   = itemView.findViewById(R.id.tv_label);
                ivDelete = itemView.findViewById(R.id.iv_delete_icon); // Optional delete icon
            }
        }
    }
    // ── Firestore field update ────────────────────────────────────────────────
    public void updateDataEntry(String key, String value, View cardRoot) {
        if (user != null && isValidUid(cachedHospitalId)) {
            db.collection("hospitals").document(cachedHospitalId)
                    .update(key, value)
                    .addOnSuccessListener(aVoid -> {
                        ViewUtils.setInputState(this, cardRoot, "IDLE");
                        Toast.makeText(this, "Updated successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        ViewUtils.setInputState(this, cardRoot, "ERROR");
                        Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
                    });
        } else {
            navigateToSearch();
        }
    }

    // ── Delete entire hospital ────────────────────────────────────────────────
    private void deleteHospital() {
        if (!isValidUid(cachedHospitalId) || user == null) { navigateToSearch(); return; }
        db.collection("hospitals").document(cachedHospitalId)
                .delete()
                .addOnSuccessListener(aVoid ->
                        db.collection("receptionists").document(user.getUid())
                                .update("hospital_id", "")
                                .addOnSuccessListener(aVoid2 -> {
                                    Toast.makeText(this, "Hospital deleted", Toast.LENGTH_SHORT).show();
                                    cachedHospitalId = null;
                                    navigateToSearch();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("Hospital", "Failed to clear receptionist link", e);
                                    Toast.makeText(this, "Hospital deleted, but failed to update profile", Toast.LENGTH_LONG).show();
                                    navigateToSearch();
                                }))
                .addOnFailureListener(e -> {
                    Log.e("Hospital", "Failed to delete hospital", e);
                    Toast.makeText(this, "Failed to delete hospital", Toast.LENGTH_SHORT).show();
                });
    }

    // ── Exit hospital ─────────────────────────────────────────────────────────
    private void exitHospital() {
        if (user == null) return;
        db.collection("receptionists").document(user.getUid())
                .update("hospital_id", "")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Exited hospital", Toast.LENGTH_SHORT).show();
                    cachedHospitalId = null;
                    navigateToSearch();
                })
                .addOnFailureListener(e -> {
                    Log.e("Hospital", "Failed to exit hospital", e);
                    Toast.makeText(this, "Failed to exit hospital", Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToSearch() {
        startActivity(new Intent(Hospital.this, SearchHospitals.class));
        overridePendingTransition(0, 0);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (receptionistListener != null) receptionistListener.remove();
    }
}