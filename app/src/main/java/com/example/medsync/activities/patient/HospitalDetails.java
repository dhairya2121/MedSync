package com.example.medsync.activities.patient;

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

public class HospitalDetails extends BaseActivity {
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private String hospitalId;
    private RecyclerView rvInfrastructure;
    private RoomAdapter roomAdapter;
    private final List<Room> roomList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hospital_details);
        applyEdgeToEdgePadding(findViewById(R.id.main));
        setupBaseActivityNavbar("P", "Patient");
        setupBaseActivityFooter("rolebased", "P");

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        // Inside onCreate
// First, find the parent containers
        View nameContainer = findViewById(R.id.hospital_name_card_container);
        View phoneContainer = findViewById(R.id.hospital_phone_card_container);
        View landlineContainer = findViewById(R.id.hospital_landline_card_container);
        View emailContainer = findViewById(R.id.hospital_email_card_container);
        View addressContainer = findViewById(R.id.hospital_address_card_container);

// Then extract the text views safely
        TextView nameCard     = nameContainer.findViewById(R.id.tv_field);
        TextView phoneCard    = phoneContainer.findViewById(R.id.tv_field);
        TextView landlineCard = landlineContainer.findViewById(R.id.tv_field);
        TextView emailCard    = emailContainer.findViewById(R.id.tv_field);
        TextView addressCard  = addressContainer.findViewById(R.id.tv_field);

// For the Rating Section
        View ratingCard = findViewById(R.id.hospital_rating_card_container);
        RatingBar ratingBar = (ratingCard != null) ? ratingCard.findViewById(R.id.rating_bar) : null;
        // ── RecyclerView setup ───────────────────────────────────────────────
        rvInfrastructure = findViewById(R.id.rv_infrastructure);
        roomAdapter = new RoomAdapter(roomList);
        rvInfrastructure.setAdapter(roomAdapter);
        if (rvInfrastructure.getLayoutManager() == null) {
            rvInfrastructure.setLayoutManager(new GridLayoutManager(this, 2));
        }

        // ── Receptionist snapshot listener ───────────────────────────────────
        // Inside onCreate, change how you call fetchHospitalDetails
        hospitalId = getIntent().getStringExtra("hospital_id");
        if (!isValidUid(hospitalId)) {
            Toast.makeText(this, "Hospital details not found", Toast.LENGTH_SHORT).show();
        }

// Pass the View containers instead of individual TextViews
        fetchHospitalDetails(hospitalId, nameContainer, phoneContainer, landlineContainer, emailContainer, addressContainer, ratingCard, ratingBar);
    }


    public void fetchHospitalDetails(String hospitalId, View nameCont, View phoneCont,
                                     View landlineCont, View emailCont, View addressCont,
                                     View ratingCard, RatingBar ratingBar) {
        if (!isValidUid(hospitalId)) {
            navigateToSearch();
            return;
        }
        db.collection("hospitals").document(hospitalId)
                .addSnapshotListener((v, err) -> {
                    if (err != null) {
                        Log.e("Hospital", "Error fetching details", err);
                        return;
                    }

                    if (v != null && v.exists()) {
                        // Use helper method: setupField(container, iconRes, value)
                        setupField(nameCont, R.drawable.ic_hospital, v.getString("legal_name"), "New Hospital");
                        setupField(phoneCont, R.drawable.ic_emergency_call, v.getString("phone"), "9999999999");
                        setupField(landlineCont, R.drawable.ic_landline, v.getString("landline"), "999999");
                        setupField(emailCont, R.drawable.ic_mail, v.getString("email"), "hospital@medsync.com");
                        setupField(addressCont, R.drawable.ic_location_pin, v.getString("address"), "No Address");

                        if (ratingBar != null) {
                            Double rating = v.getDouble("rating");
                            Long count = v.getLong("reviewCount");
                            if (rating != null && count != null) {
                                ratingCard.setVisibility(View.VISIBLE);
                                ViewUtils.setupRatingBar(ratingBar, rating, count);
                                // Also update the large text values in the rating card
                                ((TextView) ratingCard.findViewById(R.id.tv_rating_value)).setText(String.valueOf(rating));
                                ((TextView) ratingCard.findViewById(R.id.tv_review_count)).setText(String.valueOf(count));
                            }
                        }
                        fetchAndDisplayRooms(hospitalId);
                    } else {
                        navigateToSearch();
                    }
                });
    }

    // Helper method to prevent NullPointerExceptions
    private void setupField(View root, int iconRes, String value, String defaultValue) {
        if (root != null) {
            TextView tv = root.findViewById(R.id.tv_field);
            ImageView img = root.findViewById(R.id.icon_start);
            if (tv != null) tv.setText(value != null ? value : defaultValue);
            if (img != null) img.setImageResource(iconRes);
        }
    }
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
    private static class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.ViewHolder> {
        private final List<Room> rooms;

        public RoomAdapter(List<Room> rooms) {
            this.rooms = rooms;
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
            holder.iv_delete_icon.setVisibility(View.GONE);
        }

        @Override
        public int getItemCount() {
            return rooms.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvRoomNo, tvType;
            ImageView iv_delete_icon;
            public ViewHolder(View itemView) {
                super(itemView);
                tvRoomNo = itemView.findViewById(R.id.tv_count); // Based on your item_room_card.xml
                tvType   = itemView.findViewById(R.id.tv_label);
                iv_delete_icon=itemView.findViewById(R.id.iv_delete_icon);
            }
        }
    }

    private void navigateToSearch() {
        startActivity(new Intent(HospitalDetails.this, SearchHospitals.class));
        overridePendingTransition(0, 0);
        finish();
    }

}