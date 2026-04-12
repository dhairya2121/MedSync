package com.example.medsync.model;

public class TimeSlot {
    public String start; // "09:00"
    public String end;   // "09:30"

    public TimeSlot() {}
    public TimeSlot(String start, String end) {
        this.start = start;
        this.end = end;
    }
}
