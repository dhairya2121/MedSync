package com.example.medsync.activities.assistant;

import com.example.medsync.utils.BaseProfileActivity;
import com.google.firebase.firestore.DocumentSnapshot;

public class Profile extends BaseProfileActivity {

    @Override
    protected String getCollectionName() {
        return "assistants";
    }

    @Override
    protected String getUserRole() {
        return "A";
    }

}