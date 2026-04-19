package com.example.medsync.activities.receptionist;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import com.example.medsync.R;
import com.example.medsync.adapters.CareTakerAdapter; // You'll need to create a simple Adapter similar to PatientAdapter
import com.example.medsync.model.CareTaker;
import com.example.medsync.utils.BaseActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class ManageCareTakers extends BaseActivity {
    private List<CareTaker> careTakerList = new ArrayList<>();
    private CareTakerAdapter adapter;
    private FirebaseFirestore db;
    private String hospitalId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_care_takers);

        db = FirebaseFirestore.getInstance();
        hospitalId = getSharedPreferences("medsync_prefs", MODE_PRIVATE).getString("hospital_id", "");

        setupBaseActivityNavbar("R", "Receptionist");
        setupBaseActivityFooter("home", "R");
        applyEdgeToEdgePadding(findViewById(R.id.main));

        androidx.recyclerview.widget.RecyclerView rv = findViewById(R.id.rvCareTakerList);
        adapter = new CareTakerAdapter(careTakerList, ct -> {
            Intent intent = new Intent(this, CareTakerDetails.class);
            intent.putExtra("care_taker_id", ct.id);
            startActivity(intent);
        });
        rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        rv.setAdapter(adapter);

        findViewById(R.id.btn_search_go).setOnClickListener(v -> loadCareTakers(((EditText)findViewById(R.id.et_search)).getText().toString()));
        loadCareTakers(null);
    }

    private void loadCareTakers(String search) {
        Query query = db.collection("careTakers").whereEqualTo("hospital_id", hospitalId).orderBy("name");
        if (search != null && !search.isEmpty()) {
            query = query.startAt(search).endAt(search + "\uf8ff");
        }
        query.addSnapshotListener((value, error) -> {
            if (value != null) {
                careTakerList.clear();
                for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                    CareTaker ct = doc.toObject(CareTaker.class);
                    if (ct != null) { ct.setId(doc.getId()); careTakerList.add(ct); }
                }
                adapter.notifyDataSetChanged();
            }
        });
    }
}