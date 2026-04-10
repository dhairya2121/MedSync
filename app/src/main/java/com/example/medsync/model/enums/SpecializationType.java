package com.example.medsync.model.enums;

public enum SpecializationType {

    GENERAL_PHYSICIAN("General Physician"),
    CARDIOLOGIST("Cardiologist"),
    DERMATOLOGIST("Dermatologist"),
    ORTHOPEDIC("Orthopedic"),
    PEDIATRICIAN("Pediatrician"),
    GYNECOLOGIST("Gynecologist"),
    NEUROLOGIST("Neurologist"),
    PSYCHIATRIST("Psychiatrist"),
    ENT_SPECIALIST("ENT Specialist"),
    OPHTHALMOLOGIST("Ophthalmologist"),
    GASTROENTEROLOGIST("Gastroenterologist"),
    NEPHROLOGIST("Nephrologist"),
    PULMONOLOGIST("Pulmonologist"),
    ENDOCRINOLOGIST("Endocrinologist"),
    GENERAL_SURGEON("General Surgeon"),
    ANESTHESIOLOGIST("Anesthesiologist"),
    EMERGENCY_MEDICINE("Emergency Medicine");

    private final String displayName;

    SpecializationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static SpecializationType fromString(String text) {
        for (SpecializationType type : SpecializationType.values()) {
            if (type.displayName.equalsIgnoreCase(text)) {
                return type;
            }
        }
        return GENERAL_PHYSICIAN; // default
    }
}
