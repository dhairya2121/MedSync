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
    public Departments getDepartment() {
        switch (this) {

            case GENERAL_PHYSICIAN:
            case EMERGENCY_MEDICINE:
                return Departments.GENERAL;

            case CARDIOLOGIST:
                return Departments.HEART;

            case NEUROLOGIST:
            case PSYCHIATRIST:
                return Departments.BRAIN;

            case PEDIATRICIAN:
            case GYNECOLOGIST:
                return Departments.WOMEN_CHILD;

            case ORTHOPEDIC:
                return Departments.BONES;

            case OPHTHALMOLOGIST:
                return Departments.EYE;

            case ENT_SPECIALIST:
                return Departments.ENT;

            case PULMONOLOGIST:
                return Departments.LUNGS;

            case GASTROENTEROLOGIST:
                return Departments.DIGESTIVE;

            case NEPHROLOGIST:
                return Departments.KIDNEY;

            case ENDOCRINOLOGIST:
                return Departments.HORMONES;

            case DERMATOLOGIST:
                return Departments.SKIN;

            case GENERAL_SURGEON:
            case ANESTHESIOLOGIST:
                return Departments.SURGERY;

            default:
                return Departments.GENERAL;
        }
    }
}
