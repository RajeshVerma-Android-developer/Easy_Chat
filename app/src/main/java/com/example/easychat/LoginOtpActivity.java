package com.example.easychat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.easychat.utils.AndroidUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class LoginOtpActivity extends AppCompatActivity {

   String phoneNumber;
   Long timeoutSeconds = 60L;
   String varificationCode;
   PhoneAuthProvider.ForceResendingToken resendingToken;
   EditText otpInput;
   Button nextBtn;
   ProgressBar progressBar;
   TextView resendOtpTextView;
   FirebaseAuth mAuth = FirebaseAuth.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_otp);

        otpInput = findViewById(R.id.login_otp);
        nextBtn = findViewById(R.id.login_next_btn);
        progressBar = findViewById(R.id.login_progress_bar);
        resendOtpTextView = findViewById(R.id.resend_otp_textview);

        phoneNumber = getIntent().getExtras().getString("phone");
        //Toast.makeText(this, phoneNumber, Toast.LENGTH_SHORT).show();
        sendOtp(phoneNumber, false);

        nextBtn.setOnClickListener(v -> {
            String enteredOtp = otpInput.getText().toString();
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(varificationCode, enteredOtp);
            signIn(credential);
        });

        resendOtpTextView.setOnClickListener((v)->{
            sendOtp(phoneNumber, true);
        });

    }

    void sendOtp(String phoneNumber, boolean isResend) {
        startResendTimer();
        setInProgress(true);
        PhoneAuthOptions.Builder builder = new
                PhoneAuthOptions.Builder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                        signIn(phoneAuthCredential);
                        setInProgress(false);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        AndroidUtil.showToast(getApplicationContext(), "OTP varification failed");
                        setInProgress(false);
                    }

                    @Override
                    public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        super.onCodeSent(s, forceResendingToken);
                        varificationCode = s;
                        resendingToken = forceResendingToken;
                        AndroidUtil.showToast(getApplicationContext(), "OTP sent successfully");
                        setInProgress(false);
                    }
                });

        if(isResend) {
            PhoneAuthProvider.verifyPhoneNumber(builder.setForceResendingToken(resendingToken).build());
        } else {
            PhoneAuthProvider.verifyPhoneNumber(builder.build());
        }

    }

    private void signIn(PhoneAuthCredential phoneAuthCredential) {
        //login and goto next activity
        setInProgress(true);
        mAuth.signInWithCredential(phoneAuthCredential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                setInProgress(false);
                if(task.isSuccessful()) {
                    Intent intent = new Intent(LoginOtpActivity.this, LoginUsernameActivity.class);
                    intent.putExtra("phone", phoneNumber);
                    startActivity(intent);
                } else {
                    AndroidUtil.showToast(getApplicationContext(), "OTP varification failed");
                }
            }
        });
    }

    void setInProgress(boolean inProgress) {
        if(inProgress) {
            progressBar.setVisibility(View.VISIBLE);
            nextBtn.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            nextBtn.setVisibility(View.VISIBLE);
        }
    }

    void startResendTimer() {
        resendOtpTextView.setEnabled(false);
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                timeoutSeconds--;
                resendOtpTextView.setText("Resend OTP in "+timeoutSeconds+ " seconds");
                if(timeoutSeconds<=0) {
                    timeoutSeconds = 60L;
                    timer.cancel();
                    runOnUiThread(() -> {
                        resendOtpTextView.setEnabled(true);
                    });
                }
            }
        },0, 1000);
    }

}