package com.example.medsync.activities;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import com.example.medsync.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Apply padding so content isn't hidden behind the status/nav bars
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        mAuth=FirebaseAuth.getInstance();

        MaterialButton loginBtn = findViewById(R.id.loginBtn);
        EditText emailEt=findViewById(R.id.etEmail);
        EditText passEt=findViewById(R.id.etPass);

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email=emailEt.getText().toString();
                String pass=passEt.getText().toString();
                setLoading(true, loginBtn);
                mAuth.signInWithEmailAndPassword(email, pass)
                        .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    Toast.makeText(LoginActivity.this, "Logged In Successfully!",Toast.LENGTH_SHORT).show();
//                                    startActivity(main);
                                    finish();
                                } else {
                                    Toast.makeText(LoginActivity.this, "Authentication failed.",
                                            Toast.LENGTH_SHORT).show();
                                    setLoading(false, loginBtn);
                                }
                            }
                        });
                }

        });
        TextView signupLinkBtn=findViewById(R.id.tvSignupLink);
        signupLinkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signupIntent=new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(signupIntent);
                finish();
            }
        });
    }
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            Toast.makeText(LoginActivity.this, "Already Signed In", Toast.LENGTH_SHORT).show();
//            mAuth.signOut();
//            Intent i=new Intent(LoginActivity.this, Main.class);
//            startActivity(i);redirect to role-based dashboard
            finish();

        }
    }
private void setLoading(boolean isLoading, MaterialButton btn) {
    if (isLoading) {
        CircularProgressDrawable progressDrawable = new CircularProgressDrawable(LoginActivity.this);
        progressDrawable.setStrokeWidth(5f);
        progressDrawable.setCenterRadius(20f); // Reduced slightly to fit better in the button
        progressDrawable.setColorSchemeColors(Color.WHITE);
        progressDrawable.start();
        btn.setIconTint(null);
        btn.setIcon(progressDrawable);
        btn.setText("Verifying...");
        btn.setEnabled(false);
    } else {
        btn.setIcon(null);
        btn.setText("Login");
        btn.setEnabled(true);
    }
}


}