package com.andrea.appsinstaller;

import android.app.ActionBar;
import android.opengl.Visibility;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.RecoverySystem;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.content.Intent;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.tasks.*;
import com.google.android.gms.common.api.ApiException;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SignInActivity extends AppCompatActivity implements OnClickListener {

    private static final int RC_SIGN_IN = 100;
    private static final String TAG = "SignInActivity";
    private static final int SIGNIN_DELAY = 1300;

    private GoogleSignInClient mGoogleSignInClient;

    private String idToken;
    private TextView textViewStatus;
    private ProgressBar spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().hide();
        setContentView(R.layout.activity_login);

        // Set the dimensions of the sign-in button.
        SignInButton signInButton = findViewById(R.id.signInButton);
        signInButton.setSize(SignInButton.SIZE_STANDARD);
        signInButton.setOnClickListener(this);
        textViewStatus = findViewById(R.id.textViewStatus);
        spinner = findViewById(R.id.progressSpinner);

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        // NOTE: this validation service ID is now registered under Andrea's personal Google Account
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("129249635246-36plnebf8psf525r48uuvuiuhs1o7aum.apps.googleusercontent.com")
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        // Always ask for access
        mGoogleSignInClient.revokeAccess();
        mGoogleSignInClient.signOut();
        // Delay sign-in UI a little, so user can figure what's going on
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something after 100ms
                // Always initiate login
                signIn();
            }
        }, SIGNIN_DELAY);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.textViewStatus:
                spinner.setVisibility(View.VISIBLE);
                textViewStatus.setText(R.string.signing_in_string);
                textViewStatus.setOnClickListener(null);
                signIn();
                break;
            case R.id.signInButton:
                signIn();
                break;
            // ...
        }
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        Log.v(TAG, "onActivityResult returned: " + requestCode);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account.getIdToken() != null)   {
                idToken = account.getIdToken();
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        validateToken(idToken);
                    }
                });

            } else {
                Log.e(TAG, "Null token returned");
            }
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            updateLoginResult(false);
        }
    }

    private void validateToken(String tokenId) {
        OkHttpClient client = new OkHttpClient();
        RequestBody formBody = new FormBody.Builder()
                .build();

        Request request = new Request.Builder()
                .url("https://andreacarlevato.pythonanywhere.com/validate?idToken="+tokenId)
                .post(formBody)
                .build();

        boolean result = false;
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful())    {
                Log.i(TAG, "Success: " + response.body().string());
                result = true;
            } else {
                Log.i(TAG, "Fail: " + response.body().string());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        final boolean r = result;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateLoginResult(r);
            }
        });
    }

    private void updateLoginResult(boolean loginResult)   {
        spinner.setVisibility(View.GONE);
        if (loginResult)    {
            // signed in, move on to the apps list
            Intent intent = new Intent(this, AppsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else  {
            // failed, inform and provide action
            textViewStatus.setText(R.string.sign_in_error);
            textViewStatus.setOnClickListener(this);
        }
    }
}
