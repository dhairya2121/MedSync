package com.example.medsync.model.enums;

import java.util.ArrayList;import java.util.List;

public enum Departments {
    GENERAL("general"),
    HEART("heart"),
    BRAIN("brain"),
    WOMEN_CHILD("child"),
    BONES("bone"),
    EYE("eye"),
    ENT("ear"),
    LUNGS("lungs"),
    DIGESTIVE("stomach"),
    KIDNEY("kidney"),
    HORMONES("hormones"),
    SKIN("skin"),
    SURGERY("surgery");

    public final String symbol;

    /**
     * Returns the symbol with the first letter capitalized (e.g., "heart" -> "Heart")
     */
    public String getDisplayName() {
        if (symbol == null || symbol.isEmpty()) return "";
        return symbol.substring(0, 1).toUpperCase() + symbol.substring(1);
    }

    Departments(String symbol) {
        this.symbol = symbol;
    }

    /**
     * Returns a list of SpecializationType names (standard Enum .name())
     * associated with the given department symbol.
     */
    public static List<String> getSpecializationsForDept(String deptSymbol) {
        List<String> specs = new ArrayList<>();

        // Find the enum constant matching the symbol
        Departments dept = null;
        for (Departments d : Departments.values()) {
            if (d.symbol.equalsIgnoreCase(deptSymbol)) {
                dept = d;
                break;
            }
        }

        if (dept == null) return specs;

        // Map Department to SpecializationType names (Uppercase standard)
        switch (dept) {
            case GENERAL:
                specs.add(SpecializationType.GENERAL_PHYSICIAN.name());
                specs.add(SpecializationType.EMERGENCY_MEDICINE.name());
                break;
            case HEART:
                specs.add(SpecializationType.CARDIOLOGIST.name());
                break;
            case BRAIN:
                specs.add(SpecializationType.NEUROLOGIST.name());
                specs.add(SpecializationType.PSYCHIATRIST.name());
                break;
            case WOMEN_CHILD:
                specs.add(SpecializationType.PEDIATRICIAN.name());
                specs.add(SpecializationType.GYNECOLOGIST.name());
                break;
            case BONES:
                specs.add(SpecializationType.ORTHOPEDIC.name());
                break;
            case EYE:
                specs.add(SpecializationType.OPHTHALMOLOGIST.name());
                break;
            case ENT:
                specs.add(SpecializationType.ENT_SPECIALIST.name());
                break;
            case LUNGS:
                specs.add(SpecializationType.PULMONOLOGIST.name());
                break;
            case DIGESTIVE:
                specs.add(SpecializationType.GASTROENTEROLOGIST.name());
                break;
            case KIDNEY:
                specs.add(SpecializationType.NEPHROLOGIST.name());
                break;
            case HORMONES:
                specs.add(SpecializationType.ENDOCRINOLOGIST.name());
                break;
            case SKIN:
                specs.add(SpecializationType.DERMATOLOGIST.name());
                break;
            case SURGERY:
                specs.add(SpecializationType.GENERAL_SURGEON.name());
                specs.add(SpecializationType.ANESTHESIOLOGIST.name());
                break;
        }
        return specs;
    }
}