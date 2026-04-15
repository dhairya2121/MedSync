package com.example.medsync.activities.receptionist;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import com.example.medsync.R;
import com.example.medsync.model.Assistant;
import com.example.medsync.utils.BaseActivity;
import com.google.firebase.firestore.FirebaseFirestore;

public class AssistantDetails extends BaseActivity {
    private FirebaseFirestore db;
    private String assistantId;
    private TextView tvInitial, tvName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assistant_details);

        db = FirebaseFirestore.getInstance();
        assistantId = getIntent().getStringExtra("assistant_id");

        setupBaseActivityNavbar("R", "Assistant Profile");
        setupBaseActivityFooter("home", "R");

        tvInitial = findViewById(R.id.tvProfileInitial);
        tvName = findViewById(R.id.tvAssistantName);

        loadAssistantData();

        findViewById(R.id.btn_remove_assistant).setOnClickListener(v -> {
            db.collection("assistants").document(assistantId).update("hospital_id", null)
                    .addOnSuccessListener(a -> {
                        Toast.makeText(this, "Assistant Removed", Toast.LENGTH_SHORT).show();
                        finish();
                    });
        });
    }

    private void loadAssistantData() {
        db.collection("assistants").document(assistantId).get().addOnSuccessListener(doc -> {
            Assistant a = doc.toObject(Assistant.class);
            if (a != null) {
                tvName.setText(a.name);
                tvInitial.setText(a.name != null ? a.name.substring(0,1).toUpperCase() : "A");
                setupField(findViewById(R.id.view_email), R.drawable.ic_mail, a.email);
                setupField(findViewById(R.id.view_phone), R.drawable.ic_contact_book, a.phone);
                setupField(findViewById(R.id.view_gender), R.drawable.ic_filled_user, a.gender);
                setupField(findViewById(R.id.view_exp), R.drawable.ic_clock, a.exp + " yrs Experience");
            }
        });
    }

    private void setupField(android.view.View root, int icon, String value) {
        if (root == null) return;
        ((TextView) root.findViewById(R.id.tv_field)).setText(value != null ? value : "N/A");
        ((android.widget.ImageView) root.findViewById(R.id.icon_start)).setImageResource(icon);
    }
}