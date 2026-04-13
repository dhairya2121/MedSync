package com.example.medsync.activities.careTaker;

import com.example.medsync.utils.BaseProfileActivity;
public class Profile extends BaseProfileActivity {
    @Override
    protected String getCollectionName() {
        return "careTakers"; // Matches your Firestore collection
    }

    @Override
    protected String getUserRole() {
        return "C"; // Used for footer/logic
    }
}