package com.eb.ankitdubey021;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import butterknife.BindView;
import butterknife.ButterKnife;

import android.content.Intent;
import android.os.Bundle;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;

import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {
    @BindView(R.id.fb_login_btn) LoginButton fbLoginButton;
    private CallbackManager callbackManager;
    AccessToken token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        callbackManager = CallbackManager.Factory.create();


        // Callback registration
        fbLoginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                token=loginResult.getAccessToken();
                goToHome();
            }

            @Override
            public void onCancel() { }
            @Override
            public void onError(FacebookException exception) {
                Snackbar bar=Snackbar.make(fbLoginButton,"Something went wrong! please try again after sometime.",Snackbar.LENGTH_LONG);
                bar.setBackgroundTint(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
                bar.show();
            }
        });
    }

    private void goToHome() {
        Intent intent=new Intent(MainActivity.this,StartDrawerActivity.class);
        intent.putExtra("token",token);
        startActivity(intent);
        finish();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode,
                resultCode, data);
    }


    public void onStart() {
        super.onStart();
        token = AccessToken.getCurrentAccessToken();

        if (token!= null)
            goToHome();
    }


}
