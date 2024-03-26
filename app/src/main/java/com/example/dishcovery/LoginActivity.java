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

public class LoginActivity extends AppCompatActivity {
    private StringRequest stringRequest;
    private RequestQueue requestQueue;
    private EditText userName, userPass;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        requestQueue = Volley.newRequestQueue(this);
        progressDialog = new ProgressDialog(this);
        userName = findViewById(R.id.username_login);
        userPass = findViewById(R.id.password_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Find the login button by its ID
        Button loginButton = findViewById(R.id.login);

        // Set an OnClickListener for the login button
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Call the login method when the button is clicked
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

    public void login() {
        String url = "http://192.168.1.15/dishcovery/api/sign_in.php";
        progressDialog.setMessage("Logging in");
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
                        // Redirect user if needed
                    }
                } catch (JSONException e) {
                    showToast("JSON parsing error: " + e.getMessage());
                    Log.e("LoginActivity", "JSON parsing error: " + e.getMessage(), e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                progressDialog.dismiss();
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

    private void showToast(String message) {
        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
    }
}
