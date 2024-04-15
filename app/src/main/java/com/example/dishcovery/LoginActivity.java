package com.example.dishcovery;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private StringRequest stringRequest;
    private RequestQueue requestQueue;
    private EditText userName, userPass;

    GoogleSignInOptions gso;
    GoogleSignInClient gsc;
    ImageView googleBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        requestQueue = Volley.newRequestQueue(this);
        userName = findViewById(R.id.username_login);
        userPass = findViewById(R.id.password_login);
        googleBtn = findViewById(R.id.google_login);

        if (isLoggedIn()) {
            navigateToSecondActivity();
            finish();
        }

        Button loginButton = findViewById(R.id.login);

        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        gsc = GoogleSignIn.getClient(this, gso);

        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
        if (acct != null) {
            navigateToSecondActivity();
            finish();
        }

        googleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GsignIn();
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });

        TextView text = findViewById(R.id.register_activity);
        text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    void GsignIn() {
        Intent signInIntent = gsc.getSignInIntent();
        startActivityForResult(signInIntent, 1000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1000) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                String username = account.getDisplayName();
                String email = account.getEmail();
                sendGoogleSignInData(username, email);
            } catch (ApiException e) {
                Toast.makeText(getApplicationContext(), "Google sign in failed", Toast.LENGTH_SHORT).show();
                Log.e("GoogleSignIn", "Google sign in failed", e);
            }
        }
    }


    private void sendGoogleSignInData(String username, String email) {
        String url = "http://192.168.1.18/dishcovery/api/add_account.php";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.has("error")) {
                        String errorMessage = jsonResponse.getString("error");
                        showToast(errorMessage);
                    } else if (jsonResponse.has("success")) {
                        saveLoginStatus(true);
                        navigateToSecondActivity();
                    }
                } catch (JSONException e) {
                    Log.e("LoginActivity", "JSON parsing error: " + e.getMessage(), e);
                    showToast("Error parsing response");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e("LoginActivity", "Volley error: " + volleyError.getMessage(), volleyError);
                showToast("Error occurred. Please try again.");
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("username", username);
                params.put("email", email);
                return params;
            }
        };

        // Add the request to the RequestQueue
        if (requestQueue != null) {
            requestQueue.add(stringRequest);
        } else {
            Log.e("LoginActivity", "RequestQueue is null");
            showToast("Error occurred. Please try again.");
        }
    }




    void navigateToSecondActivity() {
        finish();
        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
        startActivity(intent);
    }

    public void login() {
        String url = "http://192.168.1.18/dishcovery/api/sign_in.php";

        stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.has("error")) {
                        String errorMessage = jsonResponse.getString("error");
                        showToast(errorMessage);
                    } else if (jsonResponse.has("success")) {
                        String successMessage = jsonResponse.getString("success");
                        if (jsonResponse.has("email") && jsonResponse.has("name")) {
                            String email = jsonResponse.getString("email");
                            saveUserData(email);
                        }
                        saveLoginStatus(true);
                        navigateToSecondActivity();
                        finish();
                        showToast(successMessage);
                    }
                } catch (JSONException e) {
                    showToast("JSON parsing error: " + e.getMessage());
                    Log.e("LoginActivity", "JSON parsing error: " + e.getMessage(), e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                showToast("Volley error: " + volleyError.getMessage());
                Log.e("LoginActivity", "Volley error: " + volleyError.getMessage(), volleyError);
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("username", userName.getText().toString());
                params.put("password", userPass.getText().toString());
                return params;
            }
        };
        requestQueue.add(stringRequest);
    }

    private void saveUserData(String email) {
        SharedPreferences preferences = getSharedPreferences("user_preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("userEmail", email);
        editor.apply();
    }



    private void showToast(String message) {
        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    private void saveLoginStatus(boolean isLoggedIn) {
        SharedPreferences preferences = getSharedPreferences("user_preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isLoggedIn", isLoggedIn);
        editor.apply();
    }
    private boolean isLoggedIn() {
        SharedPreferences preferences = getSharedPreferences("user_preferences", Context.MODE_PRIVATE);
        return preferences.getBoolean("isLoggedIn", false); // Default value is false if not found
    }
}
