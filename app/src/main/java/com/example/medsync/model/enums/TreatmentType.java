package com.example.medsync.model.enums;

public enum TreatmentType {

    APPOINTMENT("Appointment"),
    OPERATION("Operation / Surgery"),
    MEDICATION("Medication"),
    CHECKUP("General Checkup"),
    TEST("Diagnostic Test"),

    EMERGENCY("Emergency Treatment"),
    FOLLOW_UP("Follow-up Visit"),
    THERAPY("Therapy Session"),
    VACCINATION("Vaccination"),
    ICU_CARE("ICU Care");

    private final String displayName;

    TreatmentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TreatmentType fromString(String text) {
        for (TreatmentType type : TreatmentType.values()) {
            if (type.displayName.equalsIgnoreCase(text)) {
                return type;
            }
        }
        return APPOINTMENT; // default
    }
}