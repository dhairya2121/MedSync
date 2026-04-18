package com.example.medsync.activities.doctor;

import static com.example.medsync.utils.ViewUtils.isValidUid;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.example.medsync.R;
import com.example.medsync.model.Doctor;
import com.example.medsync.model.TimeSlot;
import com.example.medsync.model.enums.SpecializationType;
import com.example.medsync.utils.BaseProfileActivity;
import com.example.medsync.utils.ViewUtils;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class Profile extends BaseProfileActivity {

    private GridLayout gridWorkingSlots;
    private Doctor currentDoctor;
    private boolean isEditModeSlots = false;
    private List<TimeSlot> tempSlots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_profile);

        applyEdgeToEdgePadding(findViewById(R.id.main));
        setupBaseActivityNavbar("D", "Doctor Profile");
        setupBaseActivityFooter("profile", "D");

        gridWorkingSlots = findViewById(R.id.gridWorkingSlots);

        fetchDoctorData();
        setupSlotActions();
        setupLogout(findViewById(R.id.logout));
        MaterialButton btn_exit_hospital=findViewById(R.id.btn_exit_hospital);
        db.collection("doctors").document(user.getUid()).get()
                        .addOnSuccessListener(v->{
                            if(isValidUid(v.getString("hospital_id"))){
                                btn_exit_hospital.setEnabled(true);
                                btn_exit_hospital.setOnClickListener(v2 -> {
                                    exitHospitalLogic(btn_exit_hospital, "D");
                                });
                            }
                            else{
                                btn_exit_hospital.setEnabled(false);
                            }
                        });

    }

    private void setupSlotActions() {
        View editBtn = findViewById(R.id.btn_edit_slots);
        View saveBtn = findViewById(R.id.btn_save_slots);
        View progress = findViewById(R.id.slot_progress);

        editBtn.setOnClickListener(v -> {
            isEditModeSlots = true;
            tempSlots = new ArrayList<>(currentDoctor.working_slots != null ? currentDoctor.working_slots : new ArrayList<>());
            editBtn.setVisibility(View.GONE);
            saveBtn.setVisibility(View.VISIBLE);
            renderSlotManagement();
        });

        saveBtn.setOnClickListener(v -> {
            saveBtn.setVisibility(View.GONE);
            progress.setVisibility(View.VISIBLE);

            db.collection("doctors").document(user.getUid())
                    .update("working_slots", tempSlots)
                    .addOnSuccessListener(a -> {
                        currentDoctor.working_slots = new ArrayList<>(tempSlots);
                        isEditModeSlots = false;
                        progress.setVisibility(View.GONE);
                        editBtn.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "Slots Saved", Toast.LENGTH_SHORT).show();
                        renderSlotManagement(); // Refresh to show saved slots highlighted in IDLE
                    })
                    .addOnFailureListener(e -> {
                        progress.setVisibility(View.GONE);
                        saveBtn.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "Failed to save slots", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void fetchDoctorData() {
        if (user == null) return;
        db.collection("doctors").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    currentDoctor = doc.toObject(Doctor.class);
                    if (currentDoctor != null) {
                        bindViews();
                        renderSlotManagement();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show());
    }

    private void bindViews() {
        if (user == null || currentDoctor == null) return;

        // Auth Fields
        setupAuthField(R.id.card_name, R.drawable.ic_filled_user, "Name", user.getDisplayName(), "NAME");
        setupAuthField(R.id.card_email, R.drawable.ic_mail, "Email", user.getEmail(), "EMAIL");
        setupAuthField(R.id.card_phone, R.drawable.ic_contact_book, "Phone", user.getPhoneNumber(), "PHONE");
        setupAuthField(R.id.card_password, R.drawable.ic_passkey, "Password", "******", "PASSWORD");

        setupSpecializationField();

        // Professional Fields
        setupDoctorField(R.id.card_reg_no, R.drawable.ic_contact_book, "Reg No", currentDoctor.reg_no, "reg_no", InputType.TYPE_CLASS_TEXT);
        setupDoctorField(R.id.card_exp, R.drawable.ic_clock, "Experience (Yrs)", String.valueOf(currentDoctor.exp), "exp", InputType.TYPE_CLASS_NUMBER);
        setupDoctorField(R.id.card_fees, R.drawable.ic_rupee, "Consultation Fee", String.valueOf(currentDoctor.appointmentFee), "appointmentFee", InputType.TYPE_CLASS_NUMBER);

        TextView tvInitial = findViewById(R.id.tvProfileInitial);
        if (tvInitial != null && currentDoctor.name != null) {
            tvInitial.setText(currentDoctor.name.substring(0, 1).toUpperCase());
        }
    }

    private void setupAuthField(int cardId, int icon, String label, String value, String type) {
        View card = findViewById(cardId);
        ViewUtils.setupEditableInfoCard(this, card, icon, label, value, newVal -> {
            if (newVal.equals(value)) {
                ViewUtils.setInputState(this, card, "IDLE");
                return;
            }
            ViewUtils.setInputState(this, card, "LOADING");
            if (type.equals("NAME")) updateAuthName(newVal, card);
            else if (type.equals("EMAIL")) updateAuthEmail(newVal, card);
            else if (type.equals("PHONE")) updateAuthPhone(newVal, card);
            else if (type.equals("PASSWORD")) updateAuthPassword(newVal, card);
        });
    }

    private void setupSpecializationField() {
        View card = findViewById(R.id.card_specialization);
        String display = currentDoctor.specialization;
        try { display = SpecializationType.valueOf(currentDoctor.specialization).getDisplayName(); } catch (Exception ignored) {}

        ViewUtils.setupEditableInfoCard(this, card, R.drawable.ic_doctor, "Specialization", display, null);

        View btnEdit = card.findViewById(R.id.btn_edit);
        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> {
                String[] items = new String[SpecializationType.values().length];
                for (int i = 0; i < items.length; i++) items[i] = SpecializationType.values()[i].getDisplayName();

                new AlertDialog.Builder(this).setTitle("Select Specialization").setItems(items, (dialog, which) -> {
                    String enumName = SpecializationType.values()[which].name();
                    ViewUtils.setInputState(this, card, "LOADING");
                    db.collection("doctors").document(user.getUid()).update("specialization", enumName)
                            .addOnSuccessListener(a -> {
                                ViewUtils.setInputState(this, card, "IDLE");
                                ((EditText)card.findViewById(R.id.edit_text_input)).setText(SpecializationType.values()[which].getDisplayName());
                            })
                            .addOnFailureListener(e -> ViewUtils.setInputState(this, card, "ERROR"));
                }).show();
            });
        }
    }

    private void setupDoctorField(int cardId, int icon, String label, String value, String dbKey, int inputType) {
        View card = findViewById(cardId);
        ViewUtils.setupEditableInfoCard(this, card, icon, label, value, newVal -> {
            if (newVal.isEmpty()) { ViewUtils.setInputState(this, card, "IDLE"); return; }
            ViewUtils.setInputState(this, card, "LOADING");

            Object finalVal = (inputType == InputType.TYPE_CLASS_NUMBER) ? Long.parseLong(newVal) : newVal;
            db.collection("doctors").document(user.getUid()).update(dbKey, finalVal)
                    .addOnSuccessListener(a -> ViewUtils.setInputState(this, card, "IDLE"))
                    .addOnFailureListener(e -> ViewUtils.setInputState(this, card, "ERROR"));
        });
        ((EditText)card.findViewById(R.id.edit_text_input)).setInputType(inputType);
    }

    private void renderSlotManagement() {
        gridWorkingSlots.removeAllViews();
        // Determine which list to use based on mode
        List<TimeSlot> slotsToRender = isEditModeSlots ? tempSlots : currentDoctor.working_slots;
        if (slotsToRender == null) slotsToRender = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        // Standardize: Start at 9 AM today, clear seconds/millis for clean comparison
        cal.set(Calendar.HOUR_OF_DAY, 9);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("hh:mm a", Locale.US);

        for (int i = 0; i < 18; i++) {
            final Timestamp start = new Timestamp(cal.getTime());
            long startSeconds = start.getSeconds(); // Use seconds for comparison

            cal.add(Calendar.MINUTE, 30);
            final Timestamp end = new Timestamp(cal.getTime());

            MaterialButton btn = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btn.setText(sdf.format(start.toDate()));
            btn.setAllCaps(false);
            btn.setTextSize(10);
            btn.setPadding(0, 0, 0, 0);

            // Check if this slot is active by comparing Seconds (avoids nanosecond mismatch)
            boolean isActive = false;
            for (TimeSlot ts : slotsToRender) {
                if (ts.start != null && ts.start.getSeconds() == startSeconds) {
                    isActive = true;
                    break;
                }
            }

            updateSlotBtnUI(btn, isActive);

            // Logic: In IDLE state, we want the button visible but not interactive
            // We use setClickable instead of setEnabled to keep the Green colors bright
            if (isEditModeSlots) {
                btn.setClickable(true);
                btn.setOnClickListener(v -> {
                    boolean removed = tempSlots.removeIf(ts -> ts.start.getSeconds() == startSeconds);
                    if (!removed) tempSlots.add(new TimeSlot(start, end));
                    updateSlotBtnUI(btn, !removed);
                });
            } else {
                btn.setClickable(false);
            }

            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = 0;
            p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            btn.setLayoutParams(p);
            gridWorkingSlots.addView(btn);
        }
    }

    private void updateSlotBtnUI(MaterialButton btn, boolean isActive) {
        if (isActive) {
            // Highlighted state
            btn.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.soft_green)));
            btn.setStrokeColor(ColorStateList.valueOf(getColor(R.color.hard_green)));
            btn.setTextColor(getColor(R.color.hard_green));
            btn.setStrokeWidth(4);
            btn.setAlpha(1.0f);
        } else {
            // Standard/Empty state
            btn.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            btn.setStrokeColor(ColorStateList.valueOf(getColor(R.color.soft_grey)));
            btn.setTextColor(getColor(R.color.hard_grey));
            btn.setStrokeWidth(2);
            // Dim unused slots in IDLE to make active ones pop, keep them clear in EDITING
            btn.setAlpha(isEditModeSlots ? 1.0f : 0.4f);
        }
    }
    @Override protected String getCollectionName() { return "doctors"; }
    @Override protected String getUserRole() { return "D"; }
}