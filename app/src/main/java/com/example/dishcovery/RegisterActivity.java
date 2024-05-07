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

import androidx.activity.EdgeToEdge;
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

public class RegisterActivity extends AppCompatActivity {

    private StringRequest stringRequest;
    private RequestQueue requestQueue;
    private EditText userName, userPass, email; // Added email field

    GoogleSignInOptions gso;
    GoogleSignInClient gsc;
    ImageView googleBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        requestQueue = Volley.newRequestQueue(this);
        userName = findViewById(R.id.username_register);
        userPass = findViewById(R.id.password_register);
        email = findViewById(R.id.email_register);
        googleBtn = findViewById(R.id.google_register);
        // Find the login button by its ID
        Button registerButton = findViewById(R.id.register);

        if (isLoggedIn()) {
            navigateToSecondActivity();
            finish();
        }
        TextView text = findViewById(R.id.login_activity);
        text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Call the login method when the button is clicked
                register();
            }
        });

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
                String userEmail = account.getEmail();
                sendGoogleSignInData(username, userEmail);
            } catch (ApiException e) {
                Toast.makeText(getApplicationContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendGoogleSignInData(String username, String email) {
        String url = "http://admin.plantiq.info/api_dishcovery/add_account.php";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.has("error")) {
                        String errorMessage = jsonResponse.getString("error");
                        showToast(errorMessage);
                    } else if (jsonResponse.has("success")) {
                        // Handle successful sign-in if needed
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
        Intent intent = new Intent(RegisterActivity.this, DashboardActivity.class);
        startActivity(intent);
    }

    // Moved register method outside onCreate
    public void register() {
        String url = "http://admin.plantiq.info/api_dishcovery/sign_up.php";

        stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.has("error")) {
                        String errorMessage = jsonResponse.getString("error");
                        // Handle error
                        showToast(errorMessage);
                    } else if (jsonResponse.has("success")) {
                        String successMessage = jsonResponse.getString("success");
                        // Handle successful login
                        showToast(successMessage);
                        // Clear input fields
                        clearInputFields();
                    }
                } catch (JSONException e) {
                    showToast("JSON parsing error: " + e.getMessage());
                    Log.e("RegisterActivity", "JSON parsing error: " + e.getMessage(), e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                showToast("Volley error: " + volleyError.getMessage());
                Log.e("RegisterActivity", "Volley error: " + volleyError.getMessage(), volleyError);
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("username", userName.getText().toString());
                params.put("password", userPass.getText().toString());
                params.put("email", email.getText().toString()); // Add email to params
                return params;
            }
        };
        requestQueue.add(stringRequest);
    }

    // Method to clear input fields
    private void clearInputFields() {
        userName.setText(""); // Clear username field
        userPass.setText(""); // Clear password field
        email.setText(""); // Clear email field
    }

    private void showToast(String message) {
        Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_SHORT).show();
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
