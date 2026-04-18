package com.example.medsync.model;

import com.google.firebase.Timestamp;

public class TimeSlot {
    public Timestamp start; // Standard Timestamp
    public Timestamp end;   // Standard Timestamp
    public Boolean free;

    public TimeSlot() {}
    public TimeSlot(Timestamp start, Timestamp end) {
        this.start = start;
        this.end = end;
        this.free = true;
    }
}