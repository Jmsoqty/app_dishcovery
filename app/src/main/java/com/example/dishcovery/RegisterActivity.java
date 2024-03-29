package com.example.dishcovery;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private StringRequest stringRequest;
    private RequestQueue requestQueue;
    private EditText userName, userPass, email; // Added email field
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        requestQueue = Volley.newRequestQueue(this);
        progressDialog = new ProgressDialog(this);
        userName = findViewById(R.id.username_register);
        userPass = findViewById(R.id.password_register);
        email = findViewById(R.id.email_register); // Initialize email EditText

        // Find the login button by its ID
        Button registerButton = findViewById(R.id.register);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Call the login method when the button is clicked
                register();
            }
        });

        TextView text = findViewById(R.id.login_activity);
        text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }

    // Moved register method outside onCreate
    public void register() {
        String url = "http://192.168.1.18/dishcovery/api/sign_up.php";
        progressDialog.setMessage("Signing up...");
        progressDialog.show();

        stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                progressDialog.dismiss();
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
                progressDialog.dismiss();
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
}

