package com.martin.otpauth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class AuthenticationActivity extends AppCompatActivity {
    EditText edtCountryCode, edtPhoneNumber, edtOTP;
    Button btnSendOTP, btnVerify, btnResendOTP;
    String userPhoneNumber, verificationId;
    //firebase instance
    PhoneAuthProvider.ForceResendingToken token;
    FirebaseAuth fAuth;
    PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);

        edtCountryCode = findViewById(R.id.edtCoCode);
        edtPhoneNumber = findViewById(R.id.edtPhoneNumber);
        edtOTP = findViewById(R.id.edtEnterOTP);
        btnSendOTP = findViewById(R.id.btnSendOTP);
        btnVerify = findViewById(R.id.btnVerify);
        btnResendOTP = findViewById(R.id.btnResendOTP);
        btnResendOTP.setEnabled(false);

        fAuth = FirebaseAuth.getInstance();

        btnSendOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(edtCountryCode.getText().toString().isEmpty()) {
                    edtCountryCode.setError("Required");
                    return;
                }

                if(edtPhoneNumber.getText().toString().isEmpty()) {
                    edtPhoneNumber.setError("Required");
                    return;
                }
                userPhoneNumber = "+"+edtCountryCode.getText().toString()+edtPhoneNumber.getText().toString();
                Timber.d("Phone Number : %s", userPhoneNumber);
                verifyPhoneNumber(userPhoneNumber);
                Toast.makeText(AuthenticationActivity.this, "OTP sent to "+userPhoneNumber, Toast.LENGTH_SHORT).show();
            }
        });

        btnVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get the otp
                if(edtOTP.getText().toString().isEmpty()) {
                    edtOTP.setError("Enter OTP First");
                    return;
                }

                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, edtOTP.getText().toString());
                authUser(credential);
            }
        });

        btnResendOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyPhoneNumber(userPhoneNumber);
                btnResendOTP.setEnabled(false);
            }
        });

        callbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                authUser(phoneAuthCredential);
                Timber.d("onVerificationCompleted");
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Toast.makeText(AuthenticationActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                Timber.d("callbacks failure : %s", e.getMessage());
            }

            @Override
            public void onCodeSent(@NonNull String s, @NonNull @org.jetbrains.annotations.NotNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(s, forceResendingToken);
                Timber.d("onCodeSent");
                verificationId = s;
                token = forceResendingToken;

                //
                edtCountryCode.setVisibility(View.GONE);
                edtPhoneNumber.setVisibility(View.GONE);
                btnSendOTP.setVisibility(View.GONE);

                edtOTP.setVisibility(View.VISIBLE);
                btnVerify.setVisibility(View.VISIBLE);
                btnResendOTP.setVisibility(View.VISIBLE);
                btnResendOTP.setEnabled(false);
            }

            @Override
            public void onCodeAutoRetrievalTimeOut(@NonNull @org.jetbrains.annotations.NotNull String s) {
                super.onCodeAutoRetrievalTimeOut(s);
                btnResendOTP.setEnabled(true);
            }
        };



    }

    @Override
    protected void onStart() {
        super.onStart();
        if(FirebaseAuth.getInstance().getCurrentUser() != null) {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
        }
    }

    public void verifyPhoneNumber(String phoneNum) {
        //send OTP
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(fAuth)
                .setActivity(this)
                .setPhoneNumber(phoneNum)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setCallbacks(callbacks)
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);

    }

    public void authUser(PhoneAuthCredential credential) {

        fAuth.signInWithCredential(credential).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                Toast.makeText(AuthenticationActivity.this, "Success", Toast.LENGTH_SHORT).show();
                Timber.d("User authenticated");

                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                finish();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull @NotNull Exception e) {
                Timber.d("User not authenticated : %s", e.getMessage());
                Toast.makeText(AuthenticationActivity.this, "Wrong OTP", Toast.LENGTH_SHORT).show();
            }
        });
    }
}