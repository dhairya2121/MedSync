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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

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

                if (!pass.equals(confirmPass)) {
                    confirmPassLayout.setError("Password do not match");
                    confirmPassEt.requestFocus();
                } else {
                    confirmPassLayout.setError(null);
                    setLoading(true, signupBtn);
                    mAuth.createUserWithEmailAndPassword(email, pass)
                            .addOnCompleteListener(SignupActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        // Sign in success, update UI with the signed-in user's information
                                        FirebaseUser user = mAuth.getCurrentUser();
                                        Toast.makeText(SignupActivity.this, "Signup successful.",
                                                Toast.LENGTH_SHORT).show();

//                                        startActivity(main); start role-based dashboard.

                                        finish();
                                    } else {
                                        // If sign in fails, display a message to the user.
                                         Toast.makeText(SignupActivity.this, "Signup failed.",
                                                Toast.LENGTH_SHORT).show();
                                         setLoading(false, signupBtn);
                                    }
                                }
                            });
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
    private void setLoading(boolean isLoading, MaterialButton btn) {
        if (isLoading) {
            CircularProgressDrawable progressDrawable = new CircularProgressDrawable(SignupActivity.this);
            progressDrawable.setStrokeWidth(5f);
            progressDrawable.setCenterRadius(20f); // Reduced slightly to fit better in the button
            progressDrawable.setColorSchemeColors(Color.WHITE);
            progressDrawable.start();
            btn.setIconTint(null);
            btn.setIcon(progressDrawable);
            btn.setText("Please wait...");
            btn.setEnabled(false);
        } else {
            btn.setIcon(null);
            btn.setText("Sign Up");
            btn.setEnabled(true);
        }
    }

}