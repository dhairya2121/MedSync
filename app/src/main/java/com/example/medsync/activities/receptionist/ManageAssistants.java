package com.example.medsync.activities.receptionist;

import android.content.Intent;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.medsync.R;
import com.example.medsync.adapters.AssistantAdapter;
import com.example.medsync.model.Assistant;
import com.example.medsync.utils.BaseActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class ManageAssistants extends BaseActivity {
    private RecyclerView rvAssistants;
    private AssistantAdapter adapter;
    private List<Assistant> assistantList = new ArrayList<>();
    private FirebaseFirestore db;
    private String hospitalId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_assistants);

        db = FirebaseFirestore.getInstance();
        hospitalId = getSharedPreferences("medsync_prefs", MODE_PRIVATE).getString("hospital_id", "");

        setupBaseActivityNavbar("R", "Receptionist");
        setupBaseActivityFooter("home", "R");

        rvAssistants = findViewById(R.id.rvAssistants);
        rvAssistants.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AssistantAdapter(assistantList, a -> {
            Intent intent = new Intent(this, AssistantDetails.class);
            intent.putExtra("assistant_id", a.id);
            startActivity(intent);
        });
        rvAssistants.setAdapter(adapter);

        fetchAssistants();
    }

    private void fetchAssistants() {
        db.collection("assistants").whereEqualTo("hospital_id", hospitalId)
                .addSnapshotListener((value, error) -> {
                    if (value == null) return;
                    assistantList.clear();
                    for (var doc : value.getDocuments()) {
                        Assistant a = doc.toObject(Assistant.class);
                        if (a != null) {
                            a.setId(doc.getId());
                            assistantList.add(a);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}