package com.example.medsync.model.enums;

public enum TreatmentStatus {

    UPCOMING("Upcoming"),
    ONGOING("Ongoing"),
    SUCCESS("Success"),
    FAILED("Failed");

    private final String displayName;

    // Constructor
    TreatmentStatus(String displayName) {
        this.displayName = displayName;
    }

    // For UI display
    public String getDisplayName() {
        return displayName;
    }

    // Convert String → Enum safely
    public static TreatmentStatus fromString(String text) {
        for (TreatmentStatus status : TreatmentStatus.values()) {
            if (status.displayName.equalsIgnoreCase(text)) {
                return status;
            }
        }
        return UPCOMING; // default
    }
}