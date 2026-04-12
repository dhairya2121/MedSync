package com.example.medsync.activities.receptionist;

import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.medsync.R;
import com.example.medsync.utils.BaseActivity;
import com.google.firebase.auth.FirebaseAuth;

public class Profile extends BaseActivity {
    FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receptionist_profile);
        applyEdgeToEdgePadding(findViewById(R.id.main));
        setupBaseActivityFooter("profile","R");
        mAuth=FirebaseAuth.getInstance();
        Button btn=findViewById(R.id.logout);
        btn.setOnClickListener(v->{
            mAuth.signOut();
            finish();
        });
    }
}