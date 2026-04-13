package com.example.medsync.activities.patient;

import com.example.medsync.utils.BaseProfileActivity;
import com.google.firebase.firestore.DocumentSnapshot;

public class Profile extends BaseProfileActivity {
    @Override
    protected String getCollectionName() {
        return "patients"; // Matches your Firestore collection
    }

    @Override
    protected String getUserRole() {
        return "P"; // Used for footer/logic
    }
}