package com.example.medsync.activities;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.medsync.R;
import com.example.medsync.utils.ViewUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Apply padding so content isn't hidden behind the status/nav bars
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        mAuth=FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String role=getIntent().getStringExtra("role");

        MaterialButton signupBtn = findViewById(R.id.signupBtn);
        EditText nameEt=findViewById(R.id.etName);
        EditText emailEt=findViewById(R.id.etEmail);
        EditText passEt=findViewById(R.id.etPass);
        EditText confirmPassEt=findViewById(R.id.etConfirmPass);

        signupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name=nameEt.getText().toString();
                String email=emailEt.getText().toString();
                String pass=passEt.getText().toString();
                String confirmPass=confirmPassEt.getText().toString();
                TextInputLayout passLayout = findViewById(R.id.tlPass);
                TextInputLayout confirmPassLayout=findViewById(R.id.tlConfirmPass);
                if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(SignupActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!pass.equals(confirmPass)) {
                    confirmPassLayout.setError("Password do not match");
                    confirmPassEt.requestFocus();
                } else {
                    confirmPassLayout.setError(null);
                    ViewUtils.setLoading(SignupActivity.this,true,signupBtn,"Creating Account...","Sign Up");
                    signupUser(signupBtn,name,email,pass,role,db);
                }
            }
        });

        TextView loginLinkBtn=findViewById(R.id.tvLoginLink);
        loginLinkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent loginIntent=new Intent(SignupActivity.this, LoginActivity.class);
                startActivity(loginIntent);
                finish();
            }
        });
    }
    public void signupUser(MaterialButton signupBtn,String name, String email, String pass,String role,FirebaseFirestore db){
        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(SignupActivity.this, signupTask -> {
                    if (signupTask.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user == null) {
                            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String uid = user.getUid();
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("role", role);
                        db.collection("users").document(uid).set(userMap)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "User added to Firestore");
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Firestore error creating user", e);
                                });
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build();
                        user.updateProfile(profileUpdates)
                                .addOnCompleteListener(profileTask -> {
                                    ViewUtils.setLoading(SignupActivity.this,false,signupBtn,"","Sign Up");
                                    if (profileTask.isSuccessful()) {
                                        Toast.makeText(SignupActivity.this, "Account Created!", Toast.LENGTH_SHORT).show();
//                                                    Intent intent = new Intent(SignupActivity.this, MainActivity.class);
//                                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clears backstack
//                                                    startActivity(intent);
                                        finish();
                                    }
                                });
                    } else {
                        ViewUtils.setLoading(SignupActivity.this,false,signupBtn,"","Sign Up");
                        String error = signupTask.getException() != null ? signupTask.getException().getMessage() : "Signup Failed";
                        Toast.makeText(SignupActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
    }

}