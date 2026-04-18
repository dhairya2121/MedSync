package com.example.medsync.activities.doctor;

import android.util.Log;
import android.widget.Toast;

import com.example.medsync.utils.BaseProfileActivity;
import com.google.firebase.firestore.DocumentSnapshot;

public class Profile extends BaseProfileActivity {
    @Override
    protected String getCollectionName() {
        return "doctors"; // Matches your Firestore collection
    }

    @Override
    protected String getUserRole() {
        return "D"; // Used for footer/logic
    }
}