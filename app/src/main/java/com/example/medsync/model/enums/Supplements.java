package com.example.medsync.model.enums;

public enum Supplements {

    // Vitamins
    VITAMIN_A("Vitamin A"),
    VITAMIN_B_COMPLEX("Vitamin B Complex"),
    VITAMIN_C("Vitamin C"),
    VITAMIN_D3("Vitamin D3"),
    VITAMIN_E("Vitamin E"),

    // Minerals
    CALCIUM("Calcium"),
    IRON("Iron"),
    ZINC("Zinc"),
    MAGNESIUM("Magnesium"),
    POTASSIUM("Potassium"),

    // General Supplements
    MULTIVITAMIN("Multivitamin"),
    PROTEIN_SUPPLEMENT("Protein Supplement"),
    FISH_OIL("Fish Oil (Omega-3)"),
    PROBIOTICS("Probiotics"),

    // Common Medicines (basic)
    PARACETAMOL("Paracetamol"),
    IBUPROFEN("Ibuprofen"),
    ANTIBIOTICS("Antibiotics"),
    ANTACID("Antacid"),
    COUGH_SYRUP("Cough Syrup"),
    ANTIHISTAMINE("Antihistamine"),

    // Specialized
    INSULIN("Insulin"),
    ORS("ORS (Oral Rehydration Solution)"),
    ELECTROLYTES("Electrolyte Solution");

    private final String displayName;

    // Constructor
    Supplements(String displayName) {
        this.displayName = displayName;
    }

    // For UI
    public String getDisplayName() {
        return displayName;
    }

    // Safe conversion
    public static Supplements fromString(String text) {
        for (Supplements s : Supplements.values()) {
            if (s.displayName.equalsIgnoreCase(text)) {
                return s;
            }
        }
        return MULTIVITAMIN; // default
    }
}