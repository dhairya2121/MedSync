package com.example.medsync.model;

import com.google.firebase.Timestamp;

public class BookedSlot {
    public Timestamp start; // Standard Timestamp
    public String treatment_id;
    public BookedSlot() {}
    public BookedSlot(Timestamp start, String treatment_id) {
        this.start = start;
        this.treatment_id=treatment_id;
    }

}
